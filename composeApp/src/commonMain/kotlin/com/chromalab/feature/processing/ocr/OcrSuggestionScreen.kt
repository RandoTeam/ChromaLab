package com.chromalab.feature.processing.ocr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing

/**
 * OCR suggestion screen.
 * Shows OCR-detected values as suggestions that the user must confirm or correct.
 * OCR is NEVER used as ground truth without user confirmation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrSuggestionScreen(
    ocrResult: AxisOcrResult,
    onAccept: (AxisOcrResult) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var xValues by remember {
        mutableStateOf(ocrResult.suggestedXValues.map { it.toString() })
    }
    var yValues by remember {
        mutableStateOf(ocrResult.suggestedYValues.map { it.toString() })
    }
    var xEdited by remember { mutableStateOf(false) }
    var yEdited by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OCR осей") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    OutlinedButton(
                        onClick = {
                            onSkip()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Пропустить")
                    }
                    Button(
                        onClick = {
                            val confirmedX = xValues.mapNotNull { it.toFloatOrNull() }
                            val confirmedY = yValues.mapNotNull { it.toFloatOrNull() }
                            val status = when {
                                xEdited || yEdited -> OcrStatus.CORRECTED
                                else -> OcrStatus.ACCEPTED
                            }
                            onAccept(
                                ocrResult.copy(
                                    confirmedXValues = confirmedX,
                                    confirmedYValues = confirmedY,
                                    status = status,
                                ),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Принять")
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // Info card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            "OCR результаты — это подсказка. Проверьте и скорректируйте " +
                                "найденные значения перед использованием.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }

            // X axis values
            item {
                Text(
                    "Ось X (${ocrResult.xUnit ?: "время"})",
                    style = MaterialTheme.typography.titleSmall,
                )
            }

            if (xValues.isEmpty()) {
                item {
                    Text(
                        "Значения не найдены — введите вручную при калибровке",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                itemsIndexed(xValues) { index, value ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = {
                            xValues = xValues.toMutableList().also { list -> list[index] = it }
                            xEdited = true
                        },
                        label = { Text("X[${index + 1}]") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Y axis values
            item {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    "Ось Y (${ocrResult.yUnit ?: "интенсивность"})",
                    style = MaterialTheme.typography.titleSmall,
                )
            }

            if (yValues.isEmpty()) {
                item {
                    Text(
                        "Значения не найдены — введите вручную при калибровке",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                itemsIndexed(yValues) { index, value ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = {
                            yValues = yValues.toMutableList().also { list -> list[index] = it }
                            yEdited = true
                        },
                        label = { Text("Y[${index + 1}]") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Raw OCR elements (debug info)
            if (ocrResult.rawElements.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        "Распознанный текст (${ocrResult.rawElements.size} элементов)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                itemsIndexed(ocrResult.rawElements.take(20)) { _, elem ->
                    Text(
                        "\"${elem.text}\" → ${elem.numericValue ?: "—"}  (${elem.x.toInt()}, ${elem.y.toInt()})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
