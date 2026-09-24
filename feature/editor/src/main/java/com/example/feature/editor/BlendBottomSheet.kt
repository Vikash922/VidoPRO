package com.example.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.core.model.BlendMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlendBottomSheet(
    currentBlendMode: BlendMode,
    currentOpacity: Float,
    onBlendModeChanged: (BlendMode) -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedBlendMode by remember(currentBlendMode) { mutableStateOf(currentBlendMode) }
    var opacity by remember(currentOpacity) { mutableFloatStateOf(currentOpacity) }

    val blendModes = listOf(
        BlendMode.NORMAL to "Normal",
        BlendMode.MULTIPLY to "Multiply",
        BlendMode.SCREEN to "Screen",
        BlendMode.OVERLAY to "Overlay",
        BlendMode.DARKEN to "Darken",
        BlendMode.LIGHTEN to "Lighten",
        BlendMode.ADD to "Add"
    )

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
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Blend Mode & Opacity",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        selectedBlendMode = BlendMode.NORMAL
                        opacity = 1.0f
                        onBlendModeChanged(BlendMode.NORMAL)
                        onOpacityChanged(1.0f)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Blend",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
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

            // Opacity Slider
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Layer Opacity", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    Text(
                        text = "${(opacity * 100).roundToInt()}%",
                        color = Color(0xFF6C5CE7),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Slider(
                    value = opacity,
                    onValueChange = {
                        opacity = it
                        onOpacityChanged(it)
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF6C5CE7),
                        activeTrackColor = Color(0xFF6C5CE7),
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                    )
                )
            }

            // Blend Mode Grid
            Text(
                text = "BLEND MODE",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
            ) {
                items(blendModes) { (mode, label) ->
                    val isSelected = selectedBlendMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFF6C5CE7) else Color(0xFF1E2235))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF8B7CF7) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                selectedBlendMode = mode
                                onBlendModeChanged(mode)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
