package com.chromalab.feature.settings

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.chromalab.feature.processing.model.DownloadPhase
import com.chromalab.feature.processing.model.DownloadSpeedLimiter
import com.chromalab.feature.processing.model.ModelDownloader
import com.chromalab.feature.processing.model.ModelInfo
import com.chromalab.feature.processing.model.ModelManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ModelDownloadForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloader = ModelDownloader()
    private val speedLimiter = DownloadSpeedLimiter {
        manager.downloadSpeedLimitBytesPerSecond
    }
    private lateinit var manager: ModelManager
    private val jobs = mutableMapOf<String, Job>()
    private var lastNotificationUpdateMs = 0L

    override fun onCreate() {
        super.onCreate()
        manager = ModelManager(applicationContext)
        createNotificationChannel()
        startAsForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val modelId = intent.getStringExtra(EXTRA_MODEL_ID) ?: return START_STICKY
                val request = ModelDownloadRequestStore.read(applicationContext, modelId)
                if (request != null) {
                    startDownload(request)
                } else {
                    ModelDownloadForegroundState.remove(modelId)
                }
            }
            ACTION_CANCEL -> {
                intent.getStringExtra(EXTRA_MODEL_ID)?.let { cancelDownload(it) }
            }
            ACTION_RESUME -> resumePendingDownloads()
            else -> resumePendingDownloads()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    private fun startDownload(request: ModelDownloadRequest) {
        val model = request.model
        if (jobs[model.id]?.isActive == true) return

        if (manager.isDownloaded(model.id)) {
            ModelDownloadRequestStore.remove(applicationContext, model.id)
            ModelDownloadForegroundState.remove(model.id)
            stopIfIdle()
            return
        }

        ModelDownloadForegroundState.upsert(
            ModelDownloadUiState(
                modelId = model.id,
                phase = ModelDownloadUiPhase.QUEUED,
            )
        )
        updateNotification(force = true)

        val job = serviceScope.launch {
            try {
                val targetDir = manager.getModelDir(model.id)
                downloader.downloadModel(
                    model = model,
                    targetDir = targetDir,
                    parallelism = request.parallelism,
                    speedLimiter = speedLimiter,
                ) { progress ->
                    ModelDownloadForegroundState.upsert(
                        ModelDownloadUiState(
                            modelId = model.id,
                            phase = progress.phase.toUiPhase(),
                            progress = progress.fraction,
                            speedMbps = progress.speedMbPerSec,
                            fileName = progress.currentFileName,
                            error = progress.error,
                        )
                    )
                    updateNotification()
                }

                ModelDownloadRequestStore.remove(applicationContext, model.id)
                ModelDownloadForegroundState.upsert(
                    ModelDownloadUiState(
                        modelId = model.id,
                        phase = ModelDownloadUiPhase.COMPLETE,
                        progress = 1f,
                    )
                )
                updateNotification(force = true)
                delay(750L)
                ModelDownloadForegroundState.remove(model.id)
            } catch (e: CancellationException) {
                manager.delete(model.id)
                ModelDownloadRequestStore.remove(applicationContext, model.id)
                ModelDownloadForegroundState.remove(model.id)
            } catch (e: Exception) {
                manager.delete(model.id)
                ModelDownloadRequestStore.remove(applicationContext, model.id)
                ModelDownloadForegroundState.upsert(
                    ModelDownloadUiState(
                        modelId = model.id,
                        phase = ModelDownloadUiPhase.ERROR,
                        error = e.message ?: "Download failed",
                    )
                )
                updateNotification(force = true)
            } finally {
                jobs.remove(model.id)
                stopIfIdle()
            }
        }
        jobs[model.id] = job
    }

    private fun cancelDownload(modelId: String) {
        ModelDownloadRequestStore.remove(applicationContext, modelId)
        jobs.remove(modelId)?.cancel()
        ModelDownloadForegroundState.remove(modelId)
        updateNotification(force = true)
        stopIfIdle()
    }

    private fun resumePendingDownloads() {
        ModelDownloadRequestStore.readAll(applicationContext).forEach { request ->
            startDownload(request)
        }
        stopIfIdle()
    }

    private fun stopIfIdle() {
        if (jobs.isNotEmpty()) return
        if (ModelDownloadRequestStore.hasPending(applicationContext)) return
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastNotificationUpdateMs < NOTIFICATION_UPDATE_INTERVAL_MS) return
        lastNotificationUpdateMs = now
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val jobs = ModelDownloadForegroundState.snapshot().values.toList()
        val running = jobs.filter { it.phase != ModelDownloadUiPhase.ERROR && it.phase != ModelDownloadUiPhase.COMPLETE }
        val title = when {
            running.isEmpty() -> "Model downloads"
            running.size == 1 -> "Downloading model"
            else -> "Downloading ${running.size} models"
        }
        val progress = if (running.isEmpty()) 0 else (running.sumOf { (it.progress * 100).toInt() } / running.size)
        val text = running.firstOrNull()?.let { state ->
            val file = state.fileName.ifBlank { state.modelId }
            "$file · ${state.speedMbps.formatSpeed()} MB/s"
        } ?: "Preparing downloads"

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(buildLaunchPendingIntent())
            .setOngoing(running.isNotEmpty())
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, running.isEmpty())
            .build()
    }

    private fun buildLaunchPendingIntent(): PendingIntent? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        return PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Model downloads",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows active ChromaLab model downloads"
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun DownloadPhase.toUiPhase(): ModelDownloadUiPhase = when (this) {
        DownloadPhase.CONNECTING -> ModelDownloadUiPhase.CONNECTING
        DownloadPhase.DOWNLOADING -> ModelDownloadUiPhase.DOWNLOADING
        DownloadPhase.VALIDATING -> ModelDownloadUiPhase.VALIDATING
        DownloadPhase.COMPLETE -> ModelDownloadUiPhase.COMPLETE
        DownloadPhase.ERROR -> ModelDownloadUiPhase.ERROR
    }

    private fun Float.formatSpeed(): String = "%.1f".format(this)

    companion object {
        private const val ACTION_START = "com.chromalab.action.START_MODEL_DOWNLOAD"
        private const val ACTION_CANCEL = "com.chromalab.action.CANCEL_MODEL_DOWNLOAD"
        private const val ACTION_RESUME = "com.chromalab.action.RESUME_MODEL_DOWNLOADS"
        private const val EXTRA_MODEL_ID = "model_id"
        private const val CHANNEL_ID = "chromalab_model_downloads"
        private const val NOTIFICATION_ID = 4201
        private const val NOTIFICATION_UPDATE_INTERVAL_MS = 1_000L

        fun startDownload(context: Context, model: ModelInfo, parallelism: Int) {
            val appContext = context.applicationContext
            ModelDownloadRequestStore.save(appContext, model, parallelism)
            ModelDownloadForegroundState.upsert(
                ModelDownloadUiState(
                    modelId = model.id,
                    phase = ModelDownloadUiPhase.QUEUED,
                )
            )
            appContext.startForegroundService(
                Intent(appContext, ModelDownloadForegroundService::class.java)
                    .setAction(ACTION_START)
                    .putExtra(EXTRA_MODEL_ID, model.id)
            )
        }

        fun cancelDownload(context: Context, modelId: String) {
            val appContext = context.applicationContext
            ModelDownloadRequestStore.remove(appContext, modelId)
            ModelDownloadForegroundState.remove(modelId)
            appContext.startForegroundService(
                Intent(appContext, ModelDownloadForegroundService::class.java)
                    .setAction(ACTION_CANCEL)
                    .putExtra(EXTRA_MODEL_ID, modelId)
            )
        }

        fun resumePendingDownloads(context: Context) {
            val appContext = context.applicationContext
            if (!ModelDownloadRequestStore.hasPending(appContext)) return
            appContext.startForegroundService(
                Intent(appContext, ModelDownloadForegroundService::class.java)
                    .setAction(ACTION_RESUME)
            )
        }
    }
}
