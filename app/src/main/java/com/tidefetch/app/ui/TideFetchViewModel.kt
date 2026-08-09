package com.tidefetch.app.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tidefetch.app.data.ThemeMode
import com.tidefetch.app.data.ThemePreferences
import com.tidefetch.app.download.DownloadEvents
import com.tidefetch.app.download.DownloadService
import com.tidefetch.app.model.OutputFormat
import com.tidefetch.app.model.ResolutionPreset
import com.tidefetch.app.util.UrlTools
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel

data class UiNotice(
    val message: String,
    val canUndoClipboard: Boolean = false,
)

class TideFetchViewModel(application: Application) : AndroidViewModel(application) {
    private val themePreferences = ThemePreferences(application)
    private val mutableUrl = MutableStateFlow("")
    private val mutableResolution = MutableStateFlow(ResolutionPreset.ORIGINAL)
    private val mutableFormat = MutableStateFlow(OutputFormat.MP4_H264)
    private val mutableNotices = Channel<UiNotice>(capacity = Channel.BUFFERED)
    private var lastClipboardValue: String? = null
    private var lastAutofilledClipboardLink: String? = null

    val url = mutableUrl.asStateFlow()
    val resolution = mutableResolution.asStateFlow()
    val format = mutableFormat.asStateFlow()
    val download = DownloadEvents.state
    val notices = mutableNotices.receiveAsFlow()
    val themeMode = themePreferences.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeMode.SYSTEM,
    )

    fun setUrl(value: String) {
        mutableUrl.value = value
        lastClipboardValue = null
    }

    fun acceptSharedText(text: CharSequence?) {
        val link = UrlTools.firstWebUrl(text) ?: return
        mutableUrl.value = link
        lastClipboardValue = null
        mutableNotices.trySend(UiNotice("Link received"))
    }

    fun considerClipboard(text: CharSequence?) {
        if (mutableUrl.value.isNotBlank()) return
        val link = UrlTools.firstWebUrl(text) ?: return
        if (link == lastAutofilledClipboardLink) return
        lastAutofilledClipboardLink = link
        mutableUrl.value = link
        lastClipboardValue = link
        mutableNotices.trySend(UiNotice("Pasted the link from your clipboard", true))
    }

    fun undoClipboardFill() {
        if (mutableUrl.value == lastClipboardValue) mutableUrl.value = ""
        // Keep lastAutofilledClipboardLink so the same clipboard contents do
        // not immediately return after a background/foreground cycle. A new
        // URL still differs and remains eligible for autofill.
        lastClipboardValue = null
    }

    fun setResolution(value: ResolutionPreset) {
        mutableResolution.value = value
    }

    fun setFormat(value: OutputFormat) {
        mutableFormat.value = value
    }

    fun setThemeMode(value: ThemeMode) {
        viewModelScope.launch { themePreferences.setThemeMode(value) }
    }

    fun startDownload() {
        val cleanUrl = mutableUrl.value.trim()
        if (!UrlTools.isValidWebUrl(cleanUrl)) {
            mutableNotices.trySend(UiNotice("Paste a complete http or https link first"))
            return
        }
        if (download.value.isActive) return
        val context = getApplication<Application>()
        val intent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_START
            putExtra(DownloadService.EXTRA_URL, cleanUrl)
            putExtra(DownloadService.EXTRA_RESOLUTION, mutableResolution.value.name)
            putExtra(DownloadService.EXTRA_FORMAT, mutableFormat.value.name)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun cancelDownload() {
        val context = getApplication<Application>()
        context.startService(
            Intent(context, DownloadService::class.java).apply {
                action = DownloadService.ACTION_CANCEL
            },
        )
    }

    fun clearLogs() = DownloadEvents.clearLogs()

    fun reportMediaOpenFailure() {
        mutableNotices.trySend(UiNotice("No app could open this media format."))
    }

    fun resetOutcome(clearUrl: Boolean = false) {
        DownloadEvents.resetOutcome()
        if (clearUrl) mutableUrl.value = ""
    }
}
