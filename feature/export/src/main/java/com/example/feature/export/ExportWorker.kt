package com.example.feature.export

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.core.data.di.RepositoryModule
import com.example.core.media.Media3ProjectExporter
import com.example.core.media.MediaStoreSaver
import com.example.core.model.AudioCodec
import com.example.core.model.ExportSettings
import com.example.core.model.OutputFormat
import com.example.core.model.VideoCodec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * WorkManager CoroutineWorker for executing background video exports (DEV-070).
 */
class ExportWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "video_export_work"
        const val KEY_PROJECT_ID = "project_id"
        const val KEY_WIDTH = "width"
        const val KEY_HEIGHT = "height"
        const val KEY_FPS = "fps"
        const val KEY_VIDEO_BITRATE = "video_bitrate"
        const val KEY_AUDIO_BITRATE = "audio_bitrate"

        const val KEY_PROGRESS = "progress"
        const val KEY_STATUS_MESSAGE = "status_message"
        const val KEY_OUTPUT_URI = "output_uri"
        const val KEY_ERROR_MESSAGE = "error_message"
    }

    private var activeExporter: Media3ProjectExporter? = null
    private var tempOutputFile: File? = null

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val projectId = inputData.getString(KEY_PROJECT_ID)
            ?: return@withContext Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Missing project ID"))

        val width = inputData.getInt(KEY_WIDTH, 1080)
        val height = inputData.getInt(KEY_HEIGHT, 1920)
        val fps = inputData.getInt(KEY_FPS, 30)
        val videoBitrate = inputData.getInt(KEY_VIDEO_BITRATE, 10_000_000)
        val audioBitrate = inputData.getInt(KEY_AUDIO_BITRATE, 192_000)

        val settings = ExportSettings(
            width = width,
            height = height,
            fps = fps,
            videoBitrate = videoBitrate,
            audioBitrate = audioBitrate,
            format = OutputFormat.MP4,
            videoCodec = VideoCodec.H264,
            audioCodec = AudioCodec.AAC
        )

        try {
            // 1. Initial status
            setProgress(workDataOf(KEY_PROGRESS to 5, KEY_STATUS_MESSAGE to "Preparing project..."))

            // 2. Load Project and Assets from Database
            val projectRepository = RepositoryModule.provideProjectRepository(applicationContext)
            val assetRepository = RepositoryModule.provideAssetRepository(applicationContext)

            val project = projectRepository.getProjectById(projectId)
                ?: return@withContext Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Project not found"))

            val assetIds = project.tracks.flatMap { it.clips }.mapNotNull { it.assetId }.distinct()
            val assetsMap = assetIds.mapNotNull { id ->
                assetRepository.getAssetById(id)?.let { id to it }
            }.toMap()

            if (assetsMap.isEmpty() && project.tracks.any { it.clips.isNotEmpty() }) {
                return@withContext Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Required media assets could not be located."))
            }

            // 3. Prepare temporary output file
            val tempDir = File(applicationContext.cacheDir, "exports")
            tempDir.mkdirs()
            val tempFile = File(tempDir, "export_${projectId}_${System.currentTimeMillis()}.mp4")
            tempOutputFile = tempFile

            setProgress(workDataOf(KEY_PROGRESS to 10, KEY_STATUS_MESSAGE to "Starting render engine..."))

            // 4. Initialize Media3 exporter
            val exporter = Media3ProjectExporter(applicationContext)
            activeExporter = exporter

            val exportResult = exporter.export(
                project = project,
                assets = assetsMap,
                settings = settings,
                outputFile = tempFile
            ) { percent ->
                // Map exporter progress (0..100) to worker progress (10..90)
                val mappedProgress = (10 + (percent * 0.8f)).toInt().coerceIn(10, 90)
                setProgressAsync(workDataOf(
                    KEY_PROGRESS to mappedProgress,
                    KEY_STATUS_MESSAGE to "Rendering video ($percent%)..."
                ))
            }

            if (exportResult.isFailure) {
                val error = exportResult.exceptionOrNull()?.message ?: "Media rendering failed"
                tempFile.delete()
                return@withContext Result.failure(workDataOf(KEY_ERROR_MESSAGE to error))
            }

            // 5. Save to Android MediaStore
            setProgress(workDataOf(KEY_PROGRESS to 92, KEY_STATUS_MESSAGE to "Saving to Gallery..."))

            val savedUri = MediaStoreSaver.saveVideoToGallery(
                context = applicationContext,
                sourceFile = tempFile,
                displayName = project.name.ifBlank { "Exported_Video" }
            )

            // Cleanup temp file
            if (tempFile.exists()) {
                tempFile.delete()
            }

            if (savedUri != null) {
                setProgress(workDataOf(KEY_PROGRESS to 100, KEY_STATUS_MESSAGE to "Export complete!"))
                Result.success(workDataOf(KEY_OUTPUT_URI to savedUri.toString()))
            } else {
                Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Failed to save video to Gallery."))
            }

        } catch (e: CancellationException) {
            cleanup()
            Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Export cancelled"))
        } catch (e: Exception) {
            cleanup()
            Result.failure(workDataOf(KEY_ERROR_MESSAGE to (e.localizedMessage ?: "Unexpected export error")))
        } finally {
            cleanup()
        }
    }

    private fun cleanup() {
        try {
            activeExporter?.cancel()
            activeExporter = null
            tempOutputFile?.let {
                if (it.exists()) it.delete()
            }
            tempOutputFile = null
        } catch (_: Exception) {
            // Ignore cleanup errors
        }
    }
}
