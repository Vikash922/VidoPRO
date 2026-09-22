package com.example.feature.export

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.common.TimeUtils
import com.example.core.ui.components.LoadingView
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    uiState: ExportUiState,
    onResolutionSelected: (ResolutionPreset) -> Unit,
    onFpsSelected: (FpsPreset) -> Unit,
    onQualitySelected: (QualityPreset) -> Unit,
    onStartExport: () -> Unit,
    onCancelExport: () -> Unit,
    onResetState: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0F111A),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.clickable { onNavigateBack() }
                )
                Spacer(Modifier.width(16.dp))
                Text("Export Settings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        if (uiState.isLoadingProject) {
            LoadingView(modifier = Modifier.fillMaxSize())
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
            ) {
                // Preview Thumbnail Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E2230)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                }

                Spacer(Modifier.height(32.dp))

                // Settings
                ExportOptionRow(
                    title = "Resolution",
                    options = ResolutionPreset.entries,
                    selected = uiState.selectedResolution,
                    onSelect = onResolutionSelected,
                    getLabel = { it.shortLabel }
                )

                Spacer(Modifier.height(24.dp))

                ExportOptionRow(
                    title = "Frame Rate",
                    options = FpsPreset.entries,
                    selected = uiState.selectedFps,
                    onSelect = onFpsSelected,
                    getLabel = { it.fps.toString() }
                )

                Spacer(Modifier.height(24.dp))

                ExportOptionRow(
                    title = "Quality",
                    options = QualityPreset.entries,
                    selected = uiState.selectedQuality,
                    onSelect = onQualitySelected,
                    getLabel = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
                )

                Spacer(Modifier.height(32.dp))

                // Bitrate Slider
                var bitrateValue by remember(uiState.selectedQuality) { mutableFloatStateOf(12f) }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Bitrate", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("Recommended", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                        }
                    }
                    Text("${bitrateValue.roundToInt()} Mbps", color = Color.White, fontSize = 14.sp)
                }
                Slider(
                    value = bitrateValue,
                    onValueChange = { bitrateValue = it },
                    valueRange = 2f..50f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFF7B61FF),
                        inactiveTrackColor = Color(0xFF2C3248)
                    )
                )

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Estimated File Size", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Text("~ ${uiState.estimatedSizeMb.roundToInt()} MB", color = Color.White, fontSize = 12.sp)
                }

                Spacer(Modifier.height(40.dp))

                if (uiState.isExporting) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Exporting... ${(uiState.progress * 100).roundToInt()}%", color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { uiState.progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = Color(0xFF7B61FF))
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = onCancelExport) {
                            Text("Cancel", color = Color.White)
                        }
                    }
                } else if (uiState.status == ExportStatus.SUCCESS) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00FFD1), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Export Successful!", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(onClick = onNavigateBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3248))) {
                                Text("Back to Editor")
                            }
                            Button(onClick = {
                                uiState.outputUri?.let { uri ->
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(uri), "video/mp4")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(intent)
                                }
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B61FF))) {
                                Text("Play Video")
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = onStartExport,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF6B4BFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Upload, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Export", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun <T> ExportOptionRow(
    title: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    getLabel: (T) -> String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF7B61FF) else Color(0xFF161925))
                        .clickable { onSelect(option) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getLabel(option),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
