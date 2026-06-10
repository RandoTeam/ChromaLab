package com.chromalab.app

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ActivityInfo
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.chromalab.core.data.model.SourceType
import com.chromalab.feature.validation.AutonomousValidationArtifactExporter
import com.chromalab.feature.validation.AutonomousValidationFixtureRunner
import com.chromalab.feature.validation.AutonomousValidationModelMode
import com.chromalab.feature.validation.WHITE_TIGER_ION71_FIXTURE_ID
import com.chromalab.feature.processing.inference.GgufParityDiagnostics
import com.chromalab.feature.processing.inference.GgufVulkanMatrixDiagnostics
import com.chromalab.feature.processing.inference.GgufMtmdDiagnostics
import com.chromalab.feature.processing.inference.MtpAbDiagnostics
import com.chromalab.feature.processing.debug.RustAxisElementCropSmokeDiagnostics
import com.chromalab.feature.processing.debug.RustAxisElementCropCorpusDiagnostics
import com.chromalab.feature.processing.debug.RustCvBridgeSmokeDiagnostics
import com.chromalab.feature.processing.debug.TurboVecAppPrivateSmokeDiagnostics
import com.chromalab.feature.processing.debug.TurboVecKnowledgeIndexGateDiagnostics
import com.chromalab.feature.processing.inference.VlmEngineHolder
import com.chromalab.feature.processing.export.FileSharer
import com.chromalab.feature.settings.ModelManagerController

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "ChromaLabMain"
        private const val ACTION_DEBUG_GGUF_PARITY = "com.chromalab.app.DEBUG_GGUF_PARITY"
        private const val ACTION_DEBUG_MTP_AB = "com.chromalab.app.DEBUG_MTP_AB"
        private const val ACTION_DEBUG_VULKAN_MATRIX = "com.chromalab.app.DEBUG_VULKAN_MATRIX"
        private const val ACTION_DEBUG_MTMD_DIAGNOSTICS = "com.chromalab.app.DEBUG_MTMD_DIAGNOSTICS"
        private const val ACTION_DEBUG_RUST_CV_BRIDGE = "com.chromalab.app.DEBUG_RUST_CV_BRIDGE"
        private const val ACTION_DEBUG_RUST_AXIS_ELEMENT_CROPS =
            "com.chromalab.app.DEBUG_RUST_AXIS_ELEMENT_CROPS"
        private const val ACTION_DEBUG_RUST_AXIS_ELEMENT_CORPUS =
            "com.chromalab.app.DEBUG_RUST_AXIS_ELEMENT_CORPUS"
        private const val ACTION_DEBUG_TURBOVEC_APP_PRIVATE =
            "com.chromalab.app.DEBUG_TURBOVEC_APP_PRIVATE"
        private const val ACTION_DEBUG_TURBOVEC_KNOWLEDGE_INDEX_GATE =
            "com.chromalab.app.DEBUG_TURBOVEC_KNOWLEDGE_INDEX_GATE"
        private const val ACTION_RUN_VALIDATION_FIXTURE = "com.chromalab.app.RUN_VALIDATION_FIXTURE"
        private const val EXTRA_FIXTURE = "fixture"
        private const val EXTRA_MODEL_MODE = "modelMode"
    }

    private var pendingProcessingRequestState: MutableState<InitialProcessingRequest?>? = null
    private var activeValidationRunId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockToPortrait()
        com.chromalab.core.data.DatabaseProvider.init(application)
        FileSharer.contextProvider = { applicationContext }
        AutonomousValidationArtifactExporter.contextProvider = { applicationContext }
        AutonomousValidationArtifactExporter.validationRunIdProvider = { activeValidationRunId }

        if (
            intent?.action == ACTION_DEBUG_TURBOVEC_APP_PRIVATE ||
            intent?.action == ACTION_DEBUG_TURBOVEC_KNOWLEDGE_INDEX_GATE
        ) {
            showTurboVecDebugDiagnostics(intent)
            return
        }

        // Initialize ModelManagerController for VLM lazy loading (25.2B)
        // This must happen at startup so the pipeline can auto-load models
        if (VlmEngineHolder.controller == null) {
            VlmEngineHolder.controller = ModelManagerController(applicationContext, lifecycleScope)
        }

        val initialProcessingRequest = validationFixtureRequestFromIntent(intent)
        setContent {
            val pendingProcessingRequest = mutableStateOf(initialProcessingRequest)
            pendingProcessingRequestState = pendingProcessingRequest
            App(
                initialProcessingRequest = pendingProcessingRequest.value,
                onInitialProcessingRequestConsumed = {
                    pendingProcessingRequest.value = null
                },
                onRunValidationFixture = if (isDebuggable()) {
                    {
                        pendingProcessingRequest.value = validationFixtureRequest(
                            WHITE_TIGER_ION71_FIXTURE_ID,
                            AutonomousValidationModelMode.DETERMINISTIC_ONLY,
                        )
                    }
                } else {
                    null
                },
            )
        }

        handleDebugIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        lockToPortrait()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (
            intent.action == ACTION_DEBUG_TURBOVEC_APP_PRIVATE ||
            intent.action == ACTION_DEBUG_TURBOVEC_KNOWLEDGE_INDEX_GATE
        ) {
            showTurboVecDebugDiagnostics(intent)
            return
        }
        pendingProcessingRequestState?.value = validationFixtureRequestFromIntent(intent)
        handleDebugIntent(intent)
    }

    // 25.2C: Unload VLM model under memory pressure to prevent OOM
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // TRIM_MEMORY_RUNNING_LOW = 10
        if (level >= 10) {
            val engine = VlmEngineHolder.activeEngine
            if (engine != null && engine.isLoaded()) {
                println("VLM[MEMORY] Unloading model due to TRIM_MEMORY (level=$level)")
                VlmEngineHolder.activeEngine = null
                VlmEngineHolder.activeConfig = null
            }
        }
    }

    override fun onDestroy() {
        // Unload VLM engine on activity destruction
        VlmEngineHolder.activeEngine = null
        VlmEngineHolder.activeConfig = null
        super.onDestroy()
    }

    private fun handleDebugIntent(intent: Intent?) {
        if (intent?.action == ACTION_RUN_VALIDATION_FIXTURE) return
        if (intent?.action == ACTION_DEBUG_MTP_AB) {
            handleMtpAbDebugIntent(intent)
            return
        }
        if (intent?.action == ACTION_DEBUG_VULKAN_MATRIX) {
            handleVulkanMatrixDebugIntent(intent)
            return
        }
        if (intent?.action == ACTION_DEBUG_MTMD_DIAGNOSTICS) {
            handleMtmdDiagnosticsDebugIntent(intent)
            return
        }
        if (intent?.action == ACTION_DEBUG_RUST_CV_BRIDGE) {
            handleRustCvBridgeDebugIntent()
            return
        }
        if (intent?.action == ACTION_DEBUG_RUST_AXIS_ELEMENT_CROPS) {
            handleRustAxisElementCropsDebugIntent()
            return
        }
        if (intent?.action == ACTION_DEBUG_RUST_AXIS_ELEMENT_CORPUS) {
            handleRustAxisElementCorpusDebugIntent()
            return
        }
        if (intent?.action == ACTION_DEBUG_TURBOVEC_APP_PRIVATE) {
            handleTurboVecAppPrivateDebugIntent()
            return
        }
        if (intent?.action == ACTION_DEBUG_TURBOVEC_KNOWLEDGE_INDEX_GATE) {
            handleTurboVecKnowledgeIndexGateDebugIntent()
            return
        }
        if (intent?.action != ACTION_DEBUG_GGUF_PARITY) return
        if (!isDebuggable()) {
            Log.w(TAG, "Ignoring GGUF parity diagnostics intent in non-debug build")
            return
        }

        val modelId = intent.getStringExtra("modelId")
        val imagePath = intent.getStringExtra("imagePath")
        val backend = intent.getStringExtra("backend")
        Log.i(
            TAG,
            "Starting GGUF parity diagnostics modelId=${modelId.orEmpty()} " +
                "imagePath=${imagePath.orEmpty()} backend=${backend.orEmpty()}",
        )
        lifecycleScope.launch {
            GgufParityDiagnostics(applicationContext).run(
                requestedModelId = modelId,
                requestedImagePath = imagePath,
                requestedBackend = backend,
            )
        }
    }

    private fun handleMtpAbDebugIntent(intent: Intent) {
        if (!isDebuggable()) {
            Log.w(TAG, "Ignoring MTP A/B diagnostics intent in non-debug build")
            return
        }

        val modelId = intent.getStringExtra("modelId")
        val prompt = intent.getStringExtra("prompt")
        val backend = intent.getStringExtra("backend")
        val draft = intent.getIntExtra("draft", -1).takeIf { it > 0 }
        val ctx = intent.getIntExtra("ctx", -1).takeIf { it > 0 }
        val batch = intent.getIntExtra("batch", -1).takeIf { it > 0 }
        val maxTokens = intent.getIntExtra("maxTokens", -1).takeIf { it > 0 }
        Log.i(
            TAG,
            "Starting MTP A/B diagnostics modelId=${modelId.orEmpty()} " +
                "backend=${backend.orEmpty()} draft=${draft ?: 0} ctx=${ctx ?: 0} " +
                "batch=${batch ?: 0} maxTokens=${maxTokens ?: 0}",
        )
        lifecycleScope.launch {
            MtpAbDiagnostics(applicationContext).run(
                requestedModelId = modelId,
                requestedPrompt = prompt,
                requestedBackend = backend,
                requestedDraftTokens = draft,
                requestedContextTokens = ctx,
                requestedBatchTokens = batch,
                requestedMaxTokens = maxTokens,
            )
        }
    }

    private fun handleVulkanMatrixDebugIntent(intent: Intent) {
        if (!isDebuggable()) {
            Log.w(TAG, "Ignoring Vulkan matrix diagnostics intent in non-debug build")
            return
        }

        val modelId = intent.getStringExtra("modelId")
        val prompt = intent.getStringExtra("prompt")
        val ctx = intent.getIntExtra("ctx", -1).takeIf { it > 0 }
        val batch = intent.getIntExtra("batch", -1).takeIf { it > 0 }
        val maxTokens = intent.getIntExtra("maxTokens", -1).takeIf { it > 0 }
        Log.i(
            TAG,
            "Starting Vulkan matrix diagnostics modelId=${modelId.orEmpty()} " +
                "ctx=${ctx ?: 0} batch=${batch ?: 0} maxTokens=${maxTokens ?: 0}",
        )
        lifecycleScope.launch {
            GgufVulkanMatrixDiagnostics(applicationContext).run(
                requestedModelId = modelId,
                requestedPrompt = prompt,
                requestedContextTokens = ctx,
                requestedBatchTokens = batch,
                requestedMaxTokens = maxTokens,
            )
        }
    }

    private fun handleMtmdDiagnosticsDebugIntent(intent: Intent) {
        if (!isDebuggable()) {
            Log.w(TAG, "Ignoring mtmd diagnostics intent in non-debug build")
            return
        }

        val modelId = intent.getStringExtra("modelId")
        val imagePath = intent.getStringExtra("imagePath")
        val backend = intent.getStringExtra("backend")
        val runOcr = intent.getBooleanExtra("ocr", false)
        Log.i(
            TAG,
            "Starting mtmd diagnostics modelId=${modelId.orEmpty()} " +
                "backend=${backend.orEmpty()} imagePath=${imagePath.orEmpty()} ocr=$runOcr",
        )
        lifecycleScope.launch {
            GgufMtmdDiagnostics(applicationContext).run(
                requestedModelId = modelId,
                requestedImagePath = imagePath,
                requestedBackend = backend,
                requestedRunOcrProbe = runOcr,
            )
        }
    }

    private fun handleRustCvBridgeDebugIntent() {
        if (!isDebuggable()) {
            Log.w(TAG, "Ignoring Rust CV bridge diagnostics intent in non-debug build")
            return
        }

        lifecycleScope.launch {
            val summary = RustCvBridgeSmokeDiagnostics(applicationContext).run()
            Log.i(
                TAG,
                "Rust CV bridge smoke result runId=${summary.runId} decision=${summary.decision} " +
                    "loadResult=${summary.diagnostic.loadResult} artifacts=${summary.artifactDirectory}",
            )
        }
    }

    private fun handleRustAxisElementCropsDebugIntent() {
        if (!isDebuggable()) {
            Log.w(TAG, "Ignoring Rust axis element crop diagnostics intent in non-debug build")
            return
        }

        lifecycleScope.launch {
            val summary = RustAxisElementCropSmokeDiagnostics(applicationContext).run()
            Log.i(
                TAG,
                "Rust axis element crop smoke result runId=${summary.runId} " +
                    "decision=${summary.decision} rustStatus=${summary.rustStatus} " +
                    "accepted=${summary.acceptedCropCount} rejected=${summary.rejectedCropCount} " +
                    "artifacts=${summary.artifactDirectory}",
            )
        }
    }

    private fun handleRustAxisElementCorpusDebugIntent() {
        if (!isDebuggable()) {
            Log.w(TAG, "Ignoring Rust axis element corpus diagnostics intent in non-debug build")
            return
        }

        lifecycleScope.launch {
            val summary = RustAxisElementCropCorpusDiagnostics(applicationContext).run()
            Log.i(
                TAG,
                "Rust axis element corpus result runId=${summary.runId} " +
                    "decision=${summary.decision} items=${summary.itemCount} " +
                    "pass=${summary.passCount} fail=${summary.failCount} " +
                    "artifacts=${summary.artifactDirectory}",
            )
        }
    }

    private fun handleTurboVecAppPrivateDebugIntent() {
        if (!isDebuggable()) {
            Log.w(TAG, "Ignoring TurboVec app-private diagnostics intent in non-debug build")
            return
        }

        lifecycleScope.launch {
            val summary = TurboVecAppPrivateSmokeDiagnostics(applicationContext).run()
            Log.i(
                TAG,
                "TurboVec app-private smoke result runId=${summary.runId} " +
                    "decision=${summary.decision} status=${summary.status} " +
                    "topIds=${summary.topIds.joinToString()} " +
                    "artifacts=${summary.artifactDirectory}",
            )
        }
    }

    private fun handleTurboVecKnowledgeIndexGateDebugIntent() {
        if (!isDebuggable()) {
            Log.w(TAG, "Ignoring TurboVec Knowledge index diagnostics intent in non-debug build")
            return
        }

        lifecycleScope.launch {
            val summary = TurboVecKnowledgeIndexGateDiagnostics(applicationContext).run()
            Log.i(
                TAG,
                "TurboVec Knowledge index gate result runId=${summary.runId} " +
                    "decision=${summary.decision} status=${summary.status} " +
                    "realIndex=${summary.realIndexGatePassed} " +
                    "localEmbedding=${summary.localAndroidEmbeddingAvailable} " +
                    "artifacts=${summary.artifactDirectory}",
            )
        }
    }

    private fun showTurboVecDebugDiagnostics(intent: Intent) {
        val isKnowledgeGate = intent.action == ACTION_DEBUG_TURBOVEC_KNOWLEDGE_INDEX_GATE
        val title = if (isKnowledgeGate) {
            "TurboVec Knowledge Gate"
        } else {
            "TurboVec App-Private Probe"
        }
        val views = createDebugDiagnosticsView(title)
        setContentView(views.root)

        if (!isDebuggable()) {
            views.progress.visibility = ProgressBar.GONE
            views.status.text = "Ignored"
            views.detail.text = "This diagnostics action is available only in debuggable builds."
            return
        }

        lifecycleScope.launch {
            views.status.text = "Running"
            views.detail.text = "Collecting local diagnostics. The main app UI is not active during this probe."
            runCatching {
                if (isKnowledgeGate) {
                    val summary = TurboVecKnowledgeIndexGateDiagnostics(applicationContext).run()
                    Log.i(
                        TAG,
                        "TurboVec Knowledge index gate result runId=${summary.runId} " +
                            "decision=${summary.decision} status=${summary.status} " +
                            "realIndex=${summary.realIndexGatePassed} " +
                            "localEmbedding=${summary.localAndroidEmbeddingAvailable} " +
                            "artifacts=${summary.artifactDirectory}",
                    )
                    "Run ID: ${summary.runId}\n" +
                        "Decision: ${summary.decision}\n" +
                        "Status: ${summary.status}\n" +
                        "Real index gate: ${summary.realIndexGatePassed}\n" +
                        "Local Android embedding: ${summary.localAndroidEmbeddingAvailable}\n" +
                        "Artifacts: ${summary.artifactDirectory}"
                } else {
                    val summary = TurboVecAppPrivateSmokeDiagnostics(applicationContext).run()
                    Log.i(
                        TAG,
                        "TurboVec app-private smoke result runId=${summary.runId} " +
                            "decision=${summary.decision} status=${summary.status} " +
                            "topIds=${summary.topIds.joinToString()} " +
                            "artifacts=${summary.artifactDirectory}",
                    )
                    "Run ID: ${summary.runId}\n" +
                        "Decision: ${summary.decision}\n" +
                        "Status: ${summary.status}\n" +
                        "Top IDs: ${summary.topIds.joinToString()}\n" +
                        "Artifacts: ${summary.artifactDirectory}"
                }
            }.onSuccess { detail ->
                views.progress.visibility = ProgressBar.GONE
                views.status.text = "Finished"
                views.detail.text = detail
            }.onFailure { error ->
                Log.e(TAG, "TurboVec diagnostics failed", error)
                views.progress.visibility = ProgressBar.GONE
                views.status.text = "Failed"
                views.detail.text = error.message ?: error::class.java.name
            }
        }
    }

    private data class DebugDiagnosticsViews(
        val root: LinearLayout,
        val progress: ProgressBar,
        val status: TextView,
        val detail: TextView,
    )

    private fun createDebugDiagnosticsView(title: String): DebugDiagnosticsViews {
        val padding = dp(24)
        val gap = dp(14)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(padding, padding, padding, padding)
            setBackgroundColor(0xFF0B0F14.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        val titleView = TextView(this).apply {
            text = title
            setTextColor(0xFFF2F7F6.toInt())
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
        }
        val subtitleView = TextView(this).apply {
            text = "Debug diagnostics"
            setTextColor(0xFF8EA3A0.toInt())
            textSize = 14f
        }
        val progress = ProgressBar(this).apply {
            isIndeterminate = true
        }
        val status = TextView(this).apply {
            text = "Starting"
            setTextColor(0xFF68E0C2.toInt())
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }
        val detail = TextView(this).apply {
            text = "Preparing diagnostics..."
            setTextColor(0xFFD7E2E0.toInt())
            textSize = 14f
            setLineSpacing(0f, 1.12f)
        }
        val closeButton = Button(this).apply {
            text = "Open ChromaLab"
            setOnClickListener {
                startActivity(
                    Intent(this@MainActivity, MainActivity::class.java)
                        .setAction(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                )
                finish()
            }
        }
        root.addView(titleView, fullWidthParams())
        root.addView(subtitleView, fullWidthParams())
        root.addView(
            progress,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(28)
            },
        )
        root.addView(status, fullWidthParams(topMargin = gap))
        root.addView(detail, fullWidthParams(topMargin = gap))
        root.addView(closeButton, fullWidthParams(topMargin = dp(28)))
        return DebugDiagnosticsViews(root, progress, status, detail)
    }

    private fun fullWidthParams(topMargin: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            this.topMargin = topMargin
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun lockToPortrait() {
        if (requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun validationFixtureRequestFromIntent(intent: Intent?): InitialProcessingRequest? {
        if (intent?.action != ACTION_RUN_VALIDATION_FIXTURE) return null
        if (!isDebuggable()) {
            Log.w(TAG, "Ignoring validation fixture intent in non-debug build")
            return null
        }
        val fixtureId = intent.getStringExtra(EXTRA_FIXTURE) ?: WHITE_TIGER_ION71_FIXTURE_ID
        val modelMode = AutonomousValidationModelMode.parse(intent.getStringExtra(EXTRA_MODEL_MODE))
        return validationFixtureRequest(fixtureId, modelMode)
    }

    private fun validationFixtureRequest(
        fixtureId: String,
        modelMode: AutonomousValidationModelMode,
    ): InitialProcessingRequest? {
        val prepared = AutonomousValidationFixtureRunner(applicationContext)
            .prepareFixture(fixtureId, modelMode)
            .onFailure { error ->
                activeValidationRunId = null
                Log.e(TAG, "Validation fixture preparation failed", error)
            }
            .getOrNull()
            ?: return null
        activeValidationRunId = prepared.runId
        Log.i(
            TAG,
            "Validation fixture ready id=${prepared.fixtureId} runId=${prepared.runId} " +
                "modelMode=${prepared.modelMode} imagePath=${prepared.sourceImagePath} " +
                "artifacts=${prepared.publicArtifactDirectory}",
        )
        return InitialProcessingRequest(
            imagePath = prepared.sourceImagePath,
            sourceType = SourceType.VALIDATION_FIXTURE,
            validationModelMode = prepared.modelMode,
        )
    }

    private fun isDebuggable(): Boolean =
        (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
