package com.example.feature.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyframeBottomSheet(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Keyframe Animations",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text("Add keyframes to animate clip properties like Position, Scale (Zoom), and Rotation.", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { /* TODO */ }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Keyframe")
                    Spacer(Modifier.width(8.dp))
                    Text("Add Keyframe")
                }
                OutlinedButton(onClick = { /* TODO */ }) {
                    Icon(Icons.Default.Remove, contentDescription = "Remove Keyframe")
                    Spacer(Modifier.width(8.dp))
                    Text("Remove")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeatsBottomSheet(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Audio Ducking & Beat Sync",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text("Automatically detect beats and duck background audio.", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            var isDuckingEnabled by remember { mutableStateOf(false) }
            var isBeatSyncEnabled by remember { mutableStateOf(false) }
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Audio Ducking", style = MaterialTheme.typography.titleMedium)
                Switch(checked = isDuckingEnabled, onCheckedChange = { isDuckingEnabled = it })
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Auto Beat Sync", style = MaterialTheme.typography.titleMedium)
                Switch(checked = isBeatSyncEnabled, onCheckedChange = { isBeatSyncEnabled = it })
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.GraphicEq, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Apply Beats")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
