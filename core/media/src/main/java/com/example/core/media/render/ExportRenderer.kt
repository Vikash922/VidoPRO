package com.example.core.media.render

import com.example.core.model.ExportSettings
import java.io.File

/**
 * Export renderer boundary.
 *
 * Consumes the universal [RenderScene] and produces the final exported media file
 * using hardware-accelerated Media3 Transformer or compatible rendering backend.
 */
interface ExportRenderer {

    /**
     * Renders and encodes a [RenderScene] into [outputFile] according to [settings].
     */
    suspend fun render(
        scene: RenderScene,
        settings: ExportSettings,
        outputFile: File,
        onProgress: (progressPercent: Int) -> Unit
    ): Result<File>

    /**
     * Cancels the active export operation and cleans up scratch resources.
     */
    fun cancel()
}
