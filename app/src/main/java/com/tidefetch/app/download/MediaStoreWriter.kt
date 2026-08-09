package com.tidefetch.app.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.tidefetch.app.model.OutputFormat
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

data class PublishedMedia(
    val uri: Uri,
    val mimeType: String,
    val displayName: String,
)

object MediaStoreWriter {
    suspend fun publish(
        context: Context,
        source: File,
        requestedFormat: OutputFormat,
        beforeCommit: () -> Unit,
    ): PublishedMedia = withContext(Dispatchers.IO) {
        require(source.isFile) { "The completed media file could not be found" }

        val mimeType = mimeTypeFor(source, requestedFormat)
        val isAudio = requestedFormat.isAudioOnly ||
            source.extension.lowercase() in audioOnlyExtensions
        val collection = if (isAudio) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val relativePath = if (isAudio) {
            "${Environment.DIRECTORY_MUSIC}/TideFetch"
        } else {
            "${Environment.DIRECTORY_DCIM}/TideFetch"
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, source.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val target = resolver.insert(collection, values)
            ?: error("Android could not create a media-library entry")
        try {
            resolver.openOutputStream(target, "w")?.use { output ->
                source.inputStream().buffered().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 16)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                    output.flush()
                }
            } ?: error("Android could not open the media-library destination")

            beforeCommit()
            withContext(NonCancellable) {
                val published = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                check(resolver.update(target, published, null, null) > 0) {
                    "Android could not publish the media-library entry"
                }
                PublishedMedia(target, mimeType, source.name)
            }
        } catch (error: Throwable) {
            resolver.delete(target, null, null)
            throw error
        }
    }

    private fun mimeTypeFor(source: File, requestedFormat: OutputFormat): String {
        val extension = source.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: when (extension) {
                "m4a" -> "audio/mp4"
                "wav" -> "audio/wav"
                "mkv" -> "video/x-matroska"
                else -> requestedFormat.mimeType
            }
    }

    private val audioOnlyExtensions = setOf(
        "aac", "flac", "m4a", "mp3", "oga", "ogg", "opus", "wav", "weba",
    )
}
