package com.example.core.media.render

import com.example.core.model.Effect

/**
 * Universal Render Scene describing WHAT needs to be rendered across Preview and Export.
 *
 * It is a derived rendering representation generated directly from the Project and Timeline State.
 * Guarantees single source of truth:
 *
 * PROJECT / TIMELINE STATE
 *          ↓
 *     RenderScene
 *     ┌────┴────┐
 *     ↓         ↓
 *  Preview   Export
 *  Renderer  Renderer
 */
data class RenderScene(
    val canvasConfig: CanvasConfig,
    val durationMs: Long,
    val videoLayers: List<VideoRenderLayer>,
    val imageLayers: List<ImageRenderLayer>,
    val textLayers: List<TextRenderLayer>,
    val audioLayers: List<AudioRenderLayer>,
    val transitions: List<RenderTransition> = emptyList(),
    val globalEffects: List<Effect> = emptyList()
) {
    /**
     * Returns the active transition at [projectTimeMs], if any.
     */
    fun activeTransitionAt(projectTimeMs: Long): RenderTransition? =
        transitions.firstOrNull { it.isActiveAt(projectTimeMs) }
    /**
     * All visual layers sorted deterministically by z-index ascending.
     * Rendering order: lowest zIndex drawn first, highest zIndex drawn on top.
     */
    val allVisualLayers: List<RenderLayer> by lazy {
        (videoLayers + imageLayers + textLayers).sortedWith(
            compareBy<RenderLayer> { it.zIndex }
                .thenBy { it.timelineStartMs }
                .thenBy { it.id }
        )
    }

    /**
     * Returns all active visual layers at a given project timestamp, in z-index order.
     */
    fun activeVisualLayersAt(projectTimeMs: Long): List<RenderLayer> {
        return allVisualLayers.filter { it.isActiveAt(projectTimeMs) }
    }

    /**
     * Returns all active video layers at [projectTimeMs].
     */
    fun activeVideoLayersAt(projectTimeMs: Long): List<VideoRenderLayer> {
        return videoLayers.filter { it.isActiveAt(projectTimeMs) }
    }

    /**
     * Returns all active image layers at [projectTimeMs].
     */
    fun activeImageLayersAt(projectTimeMs: Long): List<ImageRenderLayer> {
        return imageLayers.filter { it.isActiveAt(projectTimeMs) }
    }

    /**
     * Returns all active text layers at [projectTimeMs].
     */
    fun activeTextLayersAt(projectTimeMs: Long): List<TextRenderLayer> {
        return textLayers.filter { it.isActiveAt(projectTimeMs) }
    }

    /**
     * Returns all active audio layers at [projectTimeMs].
     */
    fun activeAudioLayersAt(projectTimeMs: Long): List<AudioRenderLayer> {
        return audioLayers.filter { it.isActiveAt(projectTimeMs) }
    }

    /**
     * Convenience method to get the primary main video layer active at [projectTimeMs].
     */
    fun mainVideoLayerAt(projectTimeMs: Long): VideoRenderLayer? {
        return videoLayers.firstOrNull { it.isMainVideo && it.isActiveAt(projectTimeMs) }
            ?: videoLayers.firstOrNull { it.isMainVideo }
    }
}
