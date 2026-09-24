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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TextFields
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
import com.example.feature.timeline.engine.TimelineEngineState

/**
 * Dedicated TimelineHeader responsibility:
 * - Sub-timeline mode navigation (Back to Main, Add Sub-Track Media)
 * - Timeline-level controls:
 *   - Magnetic snapping toggle
 *   - Timeline zoom controls (zoom in / zoom out / zoom level indicator)
 *   - Beat marker quick toggle and marker count badge
 *   - Sub-mode shortcuts
 */
@Composable
fun TimelineHeader(
    timelineMode: TimelineMode,
    zoomLevel: Float = 1.0f,
    isSnappingEnabled: Boolean = true,
    beatMarkerCount: Int = 0,
    playheadPositionMs: Long = 0L,
    onBackToMain: () -> Unit = {},
    onAddSubTrackMedia: () -> Unit = {},
    onToggleSnapping: () -> Unit = {},
    onZoomChange: (Float) -> Unit = {},
    onToggleBeatMarker: (Long) -> Unit = {},
    onSwitchMode: (TimelineMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val headerBg = Color(0xFF13171F)
    val snapActiveColor = Color(0xFF00D2FF)
    val snapInactiveColor = Color.White.copy(alpha = 0.35f)
    val beatColor = Color(0xFFFF2D75)

    if (timelineMode != TimelineMode.MAIN) {
        // Sub-mode navigation header (Overlay / Audio / Text)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(headerBg)
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Snapping toggle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSnappingEnabled) Color(0xFF1E2838) else Color.Transparent)
                        .clickable { onToggleSnapping() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "SNAP",
                        color = if (isSnappingEnabled) snapActiveColor else snapInactiveColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Add sub-track media button
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
    } else {
        // Main timeline header controls (Zoom, Snapping, Beat markers)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(headerBg)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Sub-track quick switches
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubModeQuickBadge(
                    label = "OVL",
                    icon = Icons.Default.Layers,
                    color = Color(0xFF9C27B0),
                    onClick = { onSwitchMode(TimelineMode.OVERLAY) }
                )
                Spacer(Modifier.width(6.dp))
                SubModeQuickBadge(
                    label = "Audio",
                    icon = Icons.Default.MusicNote,
                    color = Color(0xFF00D2FF),
                    onClick = { onSwitchMode(TimelineMode.AUDIO) }
                )
                Spacer(Modifier.width(6.dp))
                SubModeQuickBadge(
                    label = "Text",
                    icon = Icons.Default.TextFields,
                    color = Color(0xFF4CAF50),
                    onClick = { onSwitchMode(TimelineMode.TEXT) }
                )
            }

            // Right: Timeline controls (Snap, Beats, Zoom)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Beat marker button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (beatMarkerCount > 0) beatColor.copy(alpha = 0.18f) else Color.Transparent)
                        .clickable { onToggleBeatMarker(playheadPositionMs) }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(beatColor)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (beatMarkerCount > 0) "$beatMarkerCount" else "Beats",
                        color = if (beatMarkerCount > 0) beatColor else Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.width(10.dp))

                // Snapping toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSnappingEnabled) Color(0xFF0F2B3E) else Color.Transparent)
                        .clickable { onToggleSnapping() }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "SNAP",
                        color = if (isSnappingEnabled) snapActiveColor else snapInactiveColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(10.dp))

                // Zoom controls: [-] [1.0x] [+]
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E2230))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Zoom out",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable {
                                onZoomChange((zoomLevel - 0.25f).coerceIn(TimelineEngineState.MIN_ZOOM, TimelineEngineState.MAX_ZOOM))
                            }
                    )
                    Text(
                        text = "%.1fx".format(zoomLevel),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Zoom in",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable {
                                onZoomChange((zoomLevel + 0.25f).coerceIn(TimelineEngineState.MIN_ZOOM, TimelineEngineState.MAX_ZOOM))
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubModeQuickBadge(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable { onClick() }
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(3.dp))
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}
