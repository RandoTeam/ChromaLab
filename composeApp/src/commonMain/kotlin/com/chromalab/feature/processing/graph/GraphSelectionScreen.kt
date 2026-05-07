package com.chromalab.feature.processing.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.chromalab.core.ui.theme.Spacing

/**
 * Graph selection screen for multi-graph sheets.
 * Shows all detected graph regions with labels.
 * User can:
 * - Select which graph to process
 * - Assign labels (from presets or custom text)
 * - See position: upper/lower
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphSelectionScreen(
    imagePath: String,
    result: GraphRegionResult,
    onSelect: (GraphSelection) -> Unit,
    onEditRoi: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    var labels by remember {
        mutableStateOf(
            result.regions.mapIndexed { i, r ->
                r.label.ifBlank {
                    when {
                        result.regions.size == 1 -> ""
                        i == 0 -> "Верхний график"
                        i == result.regions.size - 1 -> "Нижний график"
                        else -> "График ${i + 1}"
                    }
                }
            },
        )
    }
    var editingLabelIndex by remember { mutableIntStateOf(-1) }
    var editText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (result.regions.size > 1) "Выбор графика (${result.regions.size} найдено)"
                        else "График",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onEditRoi) {
                        Icon(Icons.Filled.Edit, contentDescription = "Изменить область")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
                Button(
                    onClick = {
                        val region = result.regions[selectedIndex]
                        val label = labels[selectedIndex]
                        onSelect(
                            GraphSelection(
                                graphIndex = selectedIndex,
                                graphLabel = label,
                                region = region.copy(label = label),
                                totalGraphsFound = result.regions.size,
                                timestamp = System.currentTimeMillis(),
                            ),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(Spacing.md),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Выбрать этот график")
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
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // Image preview
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = imagePath,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Graph regions list
            itemsIndexed(result.regions) { index, region ->
                val isSelected = index == selectedIndex
                val label = labels[index]

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedIndex = index }
                        .then(
                            if (isSelected) Modifier.border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(12.dp),
                            )
                            else Modifier,
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    label.ifBlank { "График ${index + 1}" },
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    "${region.width} × ${region.height} px",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (result.regions.size > 1) {
                                    Text(
                                        when (index) {
                                            0 -> "Позиция: верхний"
                                            result.regions.size - 1 -> "Позиция: нижний"
                                            else -> "Позиция: ${index + 1} из ${result.regions.size}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            // Edit label button
                            IconButton(onClick = {
                                editingLabelIndex = index
                                editText = label
                            }) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Изменить метку",
                                    modifier = Modifier.size(18.dp),
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            // Label presets (when editing)
            if (result.regions.size > 1) {
                item {
                    Text(
                        "Совет: при работе с хроматограммами назначьте метки графикам " +
                            "(например, Ion 217, Ion 218) для последующего расчёта ratio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.sm),
                    )
                }
            }
        }
    }

    // Label edit dialog
    if (editingLabelIndex >= 0) {
        AlertDialog(
            onDismissRequest = { editingLabelIndex = -1 },
            title = { Text("Метка графика") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        label = { Text("Название") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        "Быстрый выбор:",
                        style = MaterialTheme.typography.labelMedium,
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        GraphSelection.LABEL_PRESETS.forEach { preset ->
                            FilterChip(
                                selected = editText == preset,
                                onClick = { editText = preset },
                                label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    labels = labels.toMutableList().also {
                        it[editingLabelIndex] = editText
                    }
                    editingLabelIndex = -1
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingLabelIndex = -1 }) {
                    Text("Отмена")
                }
            },
        )
    }
}
