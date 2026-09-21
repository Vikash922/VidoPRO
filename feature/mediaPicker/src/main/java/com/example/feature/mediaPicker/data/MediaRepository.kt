package com.example.feature.mediaPicker.data

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.core.model.MediaType
import com.example.feature.mediaPicker.model.MediaItem
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface MediaRepository {
    suspend fun getLocalMedia(filterType: MediaType? = null): List<MediaItem>
    suspend fun getMediaItemFromUri(uri: Uri): MediaItem?
}

class MediaRepositoryImpl(
    private val context: Context
) : MediaRepository {

    override suspend fun getLocalMedia(filterType: MediaType?): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()

        if (filterType == null || filterType == MediaType.VIDEO) {
            mediaList.addAll(queryVideos())
        }

        if (filterType == null || filterType == MediaType.IMAGE) {
            mediaList.addAll(queryImages())
        }

        mediaList.sortedByDescending { it.dateAdded }
    }

    private fun queryVideos(): List<MediaItem> {
        val videos = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED
        )

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val name = cursor.getString(nameColumn) ?: "Video_$id"
                    val mime = cursor.getString(mimeColumn) ?: "video/mp4"
                    val duration = cursor.getLong(durationColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateColumn)

                    videos.add(
                        MediaItem(
                            id = id.toString(),
                            uri = contentUri,
                            displayName = name,
                            mimeType = mime,
                            mediaType = MediaType.VIDEO,
                            durationMs = duration,
                            width = width,
                            height = height,
                            sizeBytes = size,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Graceful error handling for permission or querying issues
        }

        return videos
    }

    private fun queryImages(): List<MediaItem> {
        val images = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED
        )

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val name = cursor.getString(nameColumn) ?: "Image_$id"
                    val mime = cursor.getString(mimeColumn) ?: "image/jpeg"
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateColumn)

                    images.add(
                        MediaItem(
                            id = id.toString(),
                            uri = contentUri,
                            displayName = name,
                            mimeType = mime,
                            mediaType = MediaType.IMAGE,
                            durationMs = 3000L, // Default 3s for image clips
                            width = width,
                            height = height,
                            sizeBytes = size,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Graceful error handling
        }

        return images
    }

    override suspend fun getMediaItemFromUri(uri: Uri): MediaItem? = withContext(Dispatchers.IO) {
        try {
            // Attempt to take persistable URI permission if supported (e.g. Storage Access Framework)
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {
                // Ignore if not supported for this URI type (e.g. Photo Picker)
            }

            val mimeType = context.contentResolver.getType(uri) ?: "video/mp4"
            val effectiveUri = copyUriToInternalStorage(uri, mimeType)

            val isVideo = mimeType.startsWith("video")
            val mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE

            var durationMs = if (isVideo) 0L else 3000L
            var width = 0
            var height = 0

            if (isVideo) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, effectiveUri)
                    durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                    height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                } catch (_: Exception) {
                } finally {
                    retriever.release()
                }
            }

            val fileName = effectiveUri.lastPathSegment ?: (if (isVideo) "video.mp4" else "image.jpg")
            val fileSize = if (effectiveUri.scheme == "file") {
                effectiveUri.path?.let { File(it).length() } ?: 0L
            } else 0L

            MediaItem(
                id = effectiveUri.toString(),
                uri = effectiveUri,
                displayName = fileName,
                mimeType = mimeType,
                mediaType = mediaType,
                durationMs = durationMs,
                width = width,
                height = height,
                sizeBytes = fileSize,
                dateAdded = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun copyUriToInternalStorage(uri: Uri, mimeType: String): Uri {
        if (uri.scheme == "file") return uri

        return try {
            val extension = when {
                mimeType.contains("png") -> "png"
                mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
                mimeType.contains("webp") -> "webp"
                mimeType.contains("quicktime") || mimeType.contains("mov") -> "mov"
                mimeType.contains("mkv") -> "mkv"
                mimeType.startsWith("image") -> "jpg"
                else -> "mp4"
            }

            val mediaDir = File(context.filesDir, "imported_media").apply { mkdirs() }
            val targetFile = File(mediaDir, "media_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$extension")

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                targetFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (targetFile.exists() && targetFile.length() > 0) {
                Uri.fromFile(targetFile)
            } else {
                uri
            }
        } catch (_: Exception) {
            uri
        }
    }
}
