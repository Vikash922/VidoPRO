package com.example.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.AspectRatio
import com.example.core.ui.components.AppBottomSheet
import com.example.core.ui.components.AppTextField
import com.example.core.ui.theme.AppSpacing

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NewProjectBottomSheet(
    onDismissRequest: () -> Unit,
    onCreateProject: (name: String, aspectRatio: AspectRatio) -> Unit,
    modifier: Modifier = Modifier
) {
    var projectName by remember { mutableStateOf("") }
    var selectedAspectRatio by remember { mutableStateOf(AspectRatio.RATIO_9_16) }
    var selectedMediaTab by remember { mutableStateOf(0) } // 0: Videos, 1: Images, 2: Audio

    AppBottomSheet(
        title = "New Project",
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Project Name
            Text(
                text = "Project Name",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppTextField(
                value = projectName,
                onValueChange = { projectName = it },
                placeholder = "Untitled Project",
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Aspect Ratio
            Text(
                text = "Aspect Ratio",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AspectRatio.entries.forEach { ratio ->
                    RatioItem(
                        ratio = ratio,
                        isSelected = ratio == selectedAspectRatio,
                        onClick = { selectedAspectRatio = ratio },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Create Project Button
            Button(
                onClick = {
                    val finalName = if (projectName.isBlank()) "Untitled Project" else projectName.trim()
                    onCreateProject(finalName, selectedAspectRatio)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Create Project",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RatioItem(ratio: AspectRatio, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(2.dp) // border space
    ) {
        Box(modifier = Modifier.fillMaxSize().background(bgColor, RoundedCornerShape(10.dp)).padding(4.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                // Draw icon shape based on ratio
                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = if (ratio == AspectRatio.RATIO_9_16) 32.dp else if (ratio == AspectRatio.RATIO_16_9) 16.dp else 24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ratio.label,
                    fontSize = 10.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
