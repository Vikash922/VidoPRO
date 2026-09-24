package com.example.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ClipMask
import com.example.core.model.MaskShape
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaskBottomSheet(
    currentMask: ClipMask?,
    onMaskChanged: (ClipMask?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedShape by remember(currentMask) { mutableStateOf(currentMask?.shape) }
    var x by remember(currentMask) { mutableFloatStateOf(currentMask?.x ?: 0.5f) }
    var y by remember(currentMask) { mutableFloatStateOf(currentMask?.y ?: 0.5f) }
    var width by remember(currentMask) { mutableFloatStateOf(currentMask?.width ?: 0.5f) }
    var height by remember(currentMask) { mutableFloatStateOf(currentMask?.height ?: 0.5f) }
    var feather by remember(currentMask) { mutableFloatStateOf(currentMask?.feather ?: 0f) }
    var rotation by remember(currentMask) { mutableFloatStateOf(currentMask?.rotation ?: 0f) }
    var isInverted by remember(currentMask) { mutableStateOf(currentMask?.isInverted ?: false) }
    var opacity by remember(currentMask) { mutableFloatStateOf(currentMask?.opacity ?: 1.0f) }

    fun updateMask(
        shape: MaskShape?,
        newX: Float = x,
        newY: Float = y,
        newW: Float = width,
        newH: Float = height,
        newFeather: Float = feather,
        newRot: Float = rotation,
        newInvert: Boolean = isInverted,
        newOp: Float = opacity
    ) {
        if (shape == null) {
            onMaskChanged(null)
        } else {
            onMaskChanged(
                ClipMask(
                    shape = shape,
                    x = newX,
                    y = newY,
                    width = newW,
                    height = newH,
                    feather = newFeather,
                    rotation = newRot,
                    isInverted = newInvert,
                    opacity = newOp
                )
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F111A),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 16.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mask",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedShape != null) {
                        IconButton(onClick = {
                            selectedShape = null
                            updateMask(null)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Mask",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Shape Selector
            Text(
                text = "SHAPE",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShapeChip(
                    label = "None",
                    isSelected = selectedShape == null,
                    onClick = {
                        selectedShape = null
                        updateMask(null)
                    }
                )
                ShapeChip(
                    label = "Rectangle",
                    isSelected = selectedShape == MaskShape.RECTANGLE,
                    onClick = {
                        selectedShape = MaskShape.RECTANGLE
                        updateMask(MaskShape.RECTANGLE)
                    }
                )
                ShapeChip(
                    label = "Circle",
                    isSelected = selectedShape == MaskShape.CIRCLE,
                    onClick = {
                        selectedShape = MaskShape.CIRCLE
                        updateMask(MaskShape.CIRCLE)
                    }
                )
                ShapeChip(
                    label = "Linear",
                    isSelected = selectedShape == MaskShape.LINEAR_GRADIENT,
                    onClick = {
                        selectedShape = MaskShape.LINEAR_GRADIENT
                        updateMask(MaskShape.LINEAR_GRADIENT)
                    }
                )
                ShapeChip(
                    label = "Radial",
                    isSelected = selectedShape == MaskShape.RADIAL_GRADIENT,
                    onClick = {
                        selectedShape = MaskShape.RADIAL_GRADIENT
                        updateMask(MaskShape.RADIAL_GRADIENT)
                    }
                )
            }

            if (selectedShape != null) {
                // Invert switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Invert Mask",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Switch(
                        checked = isInverted,
                        onCheckedChange = {
                            isInverted = it
                            updateMask(selectedShape, newInvert = it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF6C5CE7),
                            checkedTrackColor = Color(0xFF6C5CE7).copy(alpha = 0.5f)
                        )
                    )
                }

                // Width Slider
                MaskSlider(
                    label = "Width",
                    value = width,
                    range = 0.05f..1.0f,
                    displayValue = "${(width * 100).roundToInt()}%",
                    onValueChange = {
                        width = it
                        updateMask(selectedShape, newW = it)
                    }
                )

                // Height Slider
                MaskSlider(
                    label = "Height",
                    value = height,
                    range = 0.05f..1.0f,
                    displayValue = "${(height * 100).roundToInt()}%",
                    onValueChange = {
                        height = it
                        updateMask(selectedShape, newH = it)
                    }
                )

                // Position X Slider
                MaskSlider(
                    label = "Center X",
                    value = x,
                    range = 0.0f..1.0f,
                    displayValue = "${(x * 100).roundToInt()}%",
                    onValueChange = {
                        x = it
                        updateMask(selectedShape, newX = it)
                    }
                )

                // Position Y Slider
                MaskSlider(
                    label = "Center Y",
                    value = y,
                    range = 0.0f..1.0f,
                    displayValue = "${(y * 100).roundToInt()}%",
                    onValueChange = {
                        y = it
                        updateMask(selectedShape, newY = it)
                    }
                )

                // Feather Slider
                MaskSlider(
                    label = "Feather",
                    value = feather,
                    range = 0.0f..1.0f,
                    displayValue = "${(feather * 100).roundToInt()}%",
                    onValueChange = {
                        feather = it
                        updateMask(selectedShape, newFeather = it)
                    }
                )

                // Rotation Slider
                MaskSlider(
                    label = "Rotation",
                    value = rotation,
                    range = -180f..180f,
                    displayValue = "${rotation.roundToInt()}°",
                    onValueChange = {
                        rotation = it
                        updateMask(selectedShape, newRot = it)
                    }
                )

                // Opacity Slider
                MaskSlider(
                    label = "Mask Opacity",
                    value = opacity,
                    range = 0.0f..1.0f,
                    displayValue = "${(opacity * 100).roundToInt()}%",
                    onValueChange = {
                        opacity = it
                        updateMask(selectedShape, newOp = it)
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ShapeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF6C5CE7) else Color(0xFF1E2235))
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF8B7CF7) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun MaskSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            Text(text = displayValue, color = Color(0xFF6C5CE7), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF6C5CE7),
                activeTrackColor = Color(0xFF6C5CE7),
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}
