package com.example.feature.editor.text

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.TextClipData
import com.example.core.ui.components.AppPrimaryButton
import com.example.core.ui.theme.AppRadius
import com.example.core.ui.theme.AppSpacing

private val PRESET_COLORS = listOf(
    "#FFFFFF", // White
    "#000000", // Black
    "#FFEB3B", // Yellow
    "#F44336", // Red
    "#4CAF50", // Green
    "#2196F3", // Blue
    "#FF9800", // Orange
    "#E91E63", // Pink
    "#9C27B0"  // Purple
)

private val FONT_OPTIONS = listOf(
    "Default",
    "Serif",
    "SansSerif",
    "Monospace"
)

/**
 * Bottom sheet for adding or editing a Text Clip (DEV-062, DEV-063).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorBottomSheet(
    initialText: String = "Your Text Here",
    initialFontSize: Float = 28f,
    initialColor: String = "#FFFFFF",
    initialFontFamily: String = "Default",
    initialAlignment: String = "CENTER",
    onApply: (text: String, fontSize: Float, color: String, fontFamily: String, alignment: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var text by remember { mutableStateOf(initialText) }
    var fontSize by remember { mutableFloatStateOf(initialFontSize) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    var selectedFont by remember { mutableStateOf(initialFontFamily) }
    var selectedAlignment by remember { mutableStateOf(initialAlignment) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier.testTag("text_editor_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.xs)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Text Overlay",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("text_sheet_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // Input field
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Enter text") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("text_input_field")
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Text size slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Size: ${fontSize.toInt()}sp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(80.dp)
                )
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 14f..64f,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // Alignment Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Align:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(60.dp)
                )

                IconButton(
                    onClick = { selectedAlignment = "LEFT" },
                    modifier = Modifier
                        .clip(RoundedCornerShape(AppRadius.small))
                        .background(if (selectedAlignment == "LEFT") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatAlignLeft,
                        contentDescription = "Align Left",
                        tint = if (selectedAlignment == "LEFT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { selectedAlignment = "CENTER" },
                    modifier = Modifier
                        .clip(RoundedCornerShape(AppRadius.small))
                        .background(if (selectedAlignment == "CENTER") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatAlignCenter,
                        contentDescription = "Align Center",
                        tint = if (selectedAlignment == "CENTER") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { selectedAlignment = "RIGHT" },
                    modifier = Modifier
                        .clip(RoundedCornerShape(AppRadius.small))
                        .background(if (selectedAlignment == "RIGHT") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatAlignRight,
                        contentDescription = "Align Right",
                        tint = if (selectedAlignment == "RIGHT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // Font selection chips
            Text(
                text = "Font Family",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                items(FONT_OPTIONS) { font ->
                    val isSelected = selectedFont == font
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppRadius.small))
                            .clickable { selectedFont = font }
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(AppRadius.small)
                            ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = font,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Color palette selection
            Text(
                text = "Text Color",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                items(PRESET_COLORS) { hex ->
                    val color = parseColor(hex)
                    val isSelected = selectedColor.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = if (hex == "#FFFFFF" || hex == "#FFEB3B") Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            // Apply Button
            AppPrimaryButton(
                text = "Apply Text",
                onClick = {
                    if (text.isNotBlank()) {
                        onApply(text, fontSize, selectedColor, selectedFont, selectedAlignment)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("apply_text_button")
            )

            Spacer(modifier = Modifier.height(AppSpacing.xl))
        }
    }
}

private fun parseColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorLong = clean.toLong(16)
        if (clean.length == 6) {
            Color(0xFF000000 or colorLong)
        } else {
            Color(colorLong)
        }
    } catch (_: Exception) {
        Color.White
    }
}
