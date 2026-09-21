package com.example.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.core.model.AspectRatio
import com.example.core.ui.components.AppBottomSheet
import com.example.core.ui.components.AppChip
import com.example.core.ui.components.AppPrimaryButton
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

    AppBottomSheet(
        title = "New Project",
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Project Name",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            AppTextField(
                value = projectName,
                onValueChange = { projectName = it },
                placeholder = "Enter project name",
                singleLine = true
            )

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            Text(
                text = "Aspect Ratio",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                AspectRatio.entries.forEach { ratio ->
                    AppChip(
                        selected = ratio == selectedAspectRatio,
                        onClick = { selectedAspectRatio = ratio },
                        label = ratio.label
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.xl))

            AppPrimaryButton(
                text = "Create Project",
                onClick = {
                    val finalName = if (projectName.isBlank()) "Untitled Project" else projectName.trim()
                    onCreateProject(finalName, selectedAspectRatio)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
