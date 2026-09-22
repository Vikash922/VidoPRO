import re

with open('feature/editor/src/main/java/com/example/feature/editor/EditorViewModel.kt', 'r') as f:
    content = f.read()

pattern = r'when \(event\.tool\) \{.*?(?=is EditorEvent\.SetEditSheetVisible ->)'
replacement = """when (event.tool) {
                    EditorTool.EDIT -> { _uiState.update { it.copy(isEditSheetVisible = true) } }
                    EditorTool.AUDIO -> { _uiState.update { it.copy(isVolumeSheetVisible = true) } }
                    EditorTool.TEXT -> { _uiState.update { it.copy(isTextSheetVisible = true) } }
                    EditorTool.FILTERS -> { _uiState.update { it.copy(isFiltersSheetVisible = true) } }
                    EditorTool.SPLIT -> { _uiState.update { it.copy(isEditSheetVisible = true) } }
                    EditorTool.SPEED -> { _uiState.update { it.copy(isSpeedSheetVisible = true) } }
                    EditorTool.VOLUME -> { _uiState.update { it.copy(isVolumeSheetVisible = true) } }
                    EditorTool.CANVAS -> { _uiState.update { it.copy(isCanvasSheetVisible = true) } }
                    EditorTool.KEYFRAME -> { _uiState.update { it.copy(isKeyframeSheetVisible = true) } }
                    EditorTool.BEATS -> { _uiState.update { it.copy(isBeatsSheetVisible = true) } }
                    EditorTool.TRANSFORM -> { _uiState.update { it.copy(isTransformSheetVisible = true) } }
                    EditorTool.DELETE -> {
                        currentClipId?.let { onTimelineAction(TimelineAction.DeleteClip(it)) }
                    }
                    else -> {
                        // For newly added UI tools, just open edit sheet as placeholder
                        _uiState.update { it.copy(isEditSheetVisible = true) }
                    }
                }
            }
            """

new_content = re.sub(pattern, replacement, content, flags=re.DOTALL)

with open('feature/editor/src/main/java/com/example/feature/editor/EditorViewModel.kt', 'w') as f:
    f.write(new_content)
