package com.tidefetch.app.download

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
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
            YoutubeDL.getInstance().init(context.applicationContext)
            FFmpeg.getInstance().init(context.applicationContext)
            initialized = true
            DownloadEvents.addLog("Download engine ready")
        }
    }
}
