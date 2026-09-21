package com.example.core.media

import com.example.core.model.Asset
import com.example.core.model.ExportSettings
import com.example.core.model.Project
import java.io.File

/**
 * Interface defining the media export contract (DEV-069, DEV-070).
 */
interface ProjectExporter {
    /**
     * Exports the given [project] with its associated [assets] into the specified [outputFile].
     *
     * @param project The project data model containing tracks and trimmed clips.
     * @param assets Map of asset IDs to resolved [Asset] records with local URIs.
     * @param settings Configuration for output resolution, FPS, bitrate, and codecs.
     * @param outputFile Target file where the resulting MP4 will be rendered.
     * @param onProgress Progress listener invoked with percentage (0 to 100).
     */
    suspend fun export(
        project: Project,
        assets: Map<String, Asset>,
        settings: ExportSettings,
        outputFile: File,
        onProgress: (progressPercent: Int) -> Unit
    ): Result<File>

    /**
     * Cancels any active export operation immediately and releases resources.
     */
    fun cancel()
}
