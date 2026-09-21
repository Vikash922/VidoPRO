package com.example.core.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream

/**
 * Utility for saving exported MP4 video files directly to the Android MediaStore gallery (DEV-071).
 */
object MediaStoreSaver {

    /**
     * Saves a temporary video file into Android's MediaStore Movies collection.
     *
     * @param context Application context.
     * @param sourceFile The generated MP4 file on internal/cache storage.
     * @param displayName The desired title/filename without extension.
     * @return The MediaStore [Uri] of the inserted video, or null if saving failed.
     */
    suspend fun saveVideoToGallery(
        context: Context,
        sourceFile: File,
        displayName: String
    ): Uri? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists() || sourceFile.length() == 0L) {
            return@withContext null
        }

        val cleanName = displayName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val fileName = "${cleanName}_${System.currentTimeMillis()}.mp4"
        val contentResolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/VideoEditor")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val itemUri = contentResolver.insert(collectionUri, contentValues) ?: return@withContext null

        try {
            contentResolver.openOutputStream(itemUri)?.use { outputStream: OutputStream ->
                FileInputStream(sourceFile).use { inputStream ->
                    inputStream.copyTo(outputStream, bufferSize = 64 * 1024)
                }
                outputStream.flush()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                contentResolver.update(itemUri, updateValues, null, null)
            }

            itemUri
        } catch (e: Exception) {
            // Clean up partial record on failure
            try {
                contentResolver.delete(itemUri, null, null)
            } catch (_: Exception) {
                // Ignore cleanup errors
            }
            null
        }
    }
}
