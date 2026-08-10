package com.tidefetch.app.download

import android.content.Context
import com.tidefetch.app.R
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object DownloaderRuntime {
    private val initializationMutex = Mutex()
    @Volatile private var initialized = false

    suspend fun ensureInitialized(context: Context) {
        if (initialized) return
        initializationMutex.withLock {
            if (initialized) return
            DownloadEvents.addLog("Initializing portable yt-dlp and FFmpeg")
            try {
                val installedNewVersion = installBundledYtDlp(context.applicationContext)
                if (installedNewVersion) {
                    DownloadEvents.addLog("Installed verified yt-dlp $YTDLP_VERSION")
                }
                YoutubeDL.getInstance().init(context.applicationContext)
                FFmpeg.getInstance().init(context.applicationContext)
                initialized = true
                DownloadEvents.addLog("Download engine ready (yt-dlp $YTDLP_VERSION)")
            } catch (error: Throwable) {
                DownloadErrorFormatter.throwableDetails(error).forEach(DownloadEvents::addLog)
                throw DownloadEngineInitializationException(error)
            }
        }
    }

    private fun installBundledYtDlp(context: Context): Boolean {
        val destination = File(
            context.noBackupFilesDir,
            "youtubedl-android/yt-dlp/yt-dlp",
        )
        val parent = checkNotNull(destination.parentFile)
        check(parent.mkdirs() || parent.isDirectory) { "Could not prepare the yt-dlp directory" }

        if (destination.isFile && sha256(destination) == YTDLP_SHA256) return false

        val temporary = File(parent, "yt-dlp.$YTDLP_VERSION.tmp")
        runCatching { temporary.delete() }
        try {
            context.resources.openRawResource(R.raw.ytdlp).use { input ->
                temporary.outputStream().use(input::copyTo)
            }
            check(sha256(temporary) == YTDLP_SHA256) {
                "Bundled yt-dlp checksum did not match the audited release"
            }
            if (!temporary.renameTo(destination)) {
                temporary.copyTo(destination, overwrite = true)
                check(temporary.delete() || !temporary.exists()) {
                    "Could not remove the temporary yt-dlp file"
                }
            }
            check(destination.setExecutable(true, true) || destination.canExecute()) {
                "Could not make yt-dlp executable"
            }
            return true
        } finally {
            if (temporary.exists()) runCatching { temporary.delete() }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private const val YTDLP_VERSION = "2026.07.04"
    private const val YTDLP_SHA256 =
        "495be29ff4d9d4e9be7eabdfef225221e5d5282e77f2f505abc6dca80349f3fd"
}
