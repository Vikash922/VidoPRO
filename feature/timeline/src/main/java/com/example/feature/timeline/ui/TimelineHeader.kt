package com.example.feature.timeline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Magnet
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Timeline header component handling sub-timeline modes (Overlay, Audio, Text)
 * and timeline status indicators (snapping, beat sync, zoom).
 */
@Composable
fun TimelineHeader(
    timelineMode: TimelineMode,
    isSnappingEnabled: Boolean = true,
    beatMarkerCount: Int = 0,
    onBackToMain: () -> Unit = {},
    onAddSubTrackMedia: () -> Unit = {},
    onToggleSnapping: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (timelineMode != TimelineMode.MAIN) {
        // Sub-mode navigation header (Overlay / Audio / Text)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(Color(0xFF13171F))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onBackToMain() }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to main timeline",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when (timelineMode) {
                        TimelineMode.OVERLAY -> "Overlay Track"
                        TimelineMode.AUDIO -> "Audio Track"
                        TimelineMode.TEXT -> "Text Track"
                        else -> ""
                    },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E2230))
                    .border(1.dp, Color(0xFF384055), RoundedCornerShape(6.dp))
                    .clickable { onAddSubTrackMedia() }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = when (timelineMode) {
                        TimelineMode.OVERLAY -> "Add Overlay"
                        TimelineMode.AUDIO -> "Add Audio"
                        TimelineMode.TEXT -> "Add Text"
                        else -> "Add"
                    },
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
