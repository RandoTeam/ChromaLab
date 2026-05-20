package com.chromalab.feature.capture

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing

/**
 * Capture hub - entry point for the digitization pipeline.
 *
 * Smart Scan camera flow also supports gallery import through the scanner UI.
 */
@Composable
fun CaptureHubScreen(
    onCamera: () -> Unit,
    onImportFile: () -> Unit,
    onRunValidationFixture: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Оцифровка хроматограммы",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Сфотографируйте хроматограмму или импортируйте файл данных",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        ) {
            CaptureOptionCard(
                icon = Icons.Filled.PhotoCamera,
                label = "Камера",
                onClick = onCamera,
                modifier = Modifier.weight(1f),
            )
            CaptureOptionCard(
                icon = Icons.Filled.UploadFile,
                label = "Импорт CSV",
                onClick = onImportFile,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
            )
        }
        onRunValidationFixture?.let { runValidation ->
            Spacer(modifier = Modifier.height(Spacing.lg))
            OutlinedButton(
                onClick = runValidation,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Filled.BugReport,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Developer: Run validation fixture")
            }
        }
    }
}

/**
 * Square card with an icon and label for a capture option.
 */
@Composable
private fun CaptureOptionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = contentColor,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
            )
        }
    }
}
