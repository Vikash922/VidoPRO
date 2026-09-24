package com.example.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Clip
import com.example.core.model.ClipType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBottomSheet(
    clip: Clip,
    playheadPositionMs: Long,
    onDismiss: () -> Unit,
    onSplit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onSpeed: () -> Unit,
    onVolume: () -> Unit,
    onAudio: () -> Unit,
    onText: () -> Unit,
    onOverlay: () -> Unit,
    onFilters: () -> Unit,
    onTransform: () -> Unit,
    onCanvas: () -> Unit,
    onKeyframe: () -> Unit,
    onBeats: () -> Unit,
    onMask: () -> Unit = {},
    onBlend: () -> Unit = {}
) {
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Clip",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            val items = mutableListOf<BottomSheetItem>()
            
            // Common Tools
            items.add(BottomSheetItem("Split", Icons.Default.CallSplit, onSplit))
            items.add(BottomSheetItem("Duplicate", Icons.Default.ContentCopy, onDuplicate))
            items.add(BottomSheetItem("Delete", Icons.Default.Delete, onDelete))
            
            // Contextual Tools
            if (clip.type == ClipType.VIDEO) {
                items.add(BottomSheetItem("Speed", Icons.Default.Speed, onSpeed))
                items.add(BottomSheetItem("Transform", Icons.Default.CropRotate, onTransform))
            }
            if (clip.type == ClipType.VIDEO || clip.type == ClipType.AUDIO) {
                items.add(BottomSheetItem("Volume", Icons.Default.VolumeUp, onVolume))
                items.add(BottomSheetItem("Audio", Icons.Default.Audiotrack, onAudio))
            }
            if (clip.type == ClipType.VIDEO || clip.type == ClipType.IMAGE) {
                items.add(BottomSheetItem("Filters", Icons.Default.ColorLens, onFilters))
                items.add(BottomSheetItem("Mask", Icons.Default.CropFree, onMask))
                items.add(BottomSheetItem("Blend", Icons.Default.Opacity, onBlend))
                items.add(BottomSheetItem("Keyframe", Icons.Default.Star, onKeyframe))
                items.add(BottomSheetItem("Canvas", Icons.Default.AspectRatio, onCanvas))
            }
            if (clip.type == ClipType.AUDIO) {
                items.add(BottomSheetItem("Beats", Icons.Default.GraphicEq, onBeats))
            }
            if (clip.type == ClipType.TEXT) {
                items.add(BottomSheetItem("Text", Icons.Default.Title, onText))
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
            ) {
                items(items) { item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(onClick = item.onClick)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF161925)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.label,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

data class BottomSheetItem(val label: String, val icon: ImageVector, val onClick: () -> Unit)
