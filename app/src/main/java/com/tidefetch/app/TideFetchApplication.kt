package com.tidefetch.app

import android.app.Application
import com.tidefetch.app.download.DownloadNotifications

class TideFetchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DownloadNotifications.createChannel(this)
    }
}
