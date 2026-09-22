package com.example.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyframeBottomSheet(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F111A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    Spacer(Modifier.width(12.dp))
                    Text("Keyframe", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fake Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF161925)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fake Timeline
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("00:00", color = Color.Gray, fontSize = 10.sp)
                Text("00:10", color = Color.Gray, fontSize = 10.sp)
                Text("00:20", color = Color.Gray, fontSize = 10.sp)
                Text("00:30", color = Color.Gray, fontSize = 10.sp)
            }
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF7B61FF)))
            
            Spacer(modifier = Modifier.height(24.dp))

            // Properties
            KeyframePropertyRow(title = "Position", value = "0.0, 0.0", icon = Icons.Default.OpenWith)
            KeyframePropertyRow(title = "Scale", value = "100%", icon = Icons.Default.ZoomIn)
            KeyframePropertyRow(title = "Rotation", value = "0°", icon = Icons.Default.RotateRight)
            KeyframePropertyRow(title = "Opacity", value = "100%", icon = Icons.Default.Visibility)

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reset")
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B61FF))
                ) {
                    Text("Apply")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun KeyframePropertyRow(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    var sliderValue by remember { mutableFloatStateOf(100f) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            }
            Text(value, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFF7B61FF),
                inactiveTrackColor = Color(0xFF2C3248)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeatsBottomSheet(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F111A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFF00D2FF))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Audio Ducking & Beat Sync",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text("Automatically detect beats and sync cuts, while ducking background audio.", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            var isDuckingEnabled by remember { mutableStateOf(false) }
            var isBeatSyncEnabled by remember { mutableStateOf(false) }
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Audio Ducking", color = Color.White, fontSize = 16.sp)
                Switch(checked = isDuckingEnabled, onCheckedChange = { isDuckingEnabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00D2FF), checkedTrackColor = Color(0xFF00D2FF).copy(alpha = 0.5f)))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Auto Beat Sync (Auto Cut)", color = Color.White, fontSize = 16.sp)
                Switch(checked = isBeatSyncEnabled, onCheckedChange = { isBeatSyncEnabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00D2FF), checkedTrackColor = Color(0xFF00D2FF).copy(alpha = 0.5f)))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B61FF))) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Apply & Sync")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
