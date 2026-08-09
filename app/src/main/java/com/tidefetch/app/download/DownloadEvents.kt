package com.tidefetch.app.download

import android.net.Uri
import com.tidefetch.app.model.DownloadPhase
import com.tidefetch.app.model.DownloadSnapshot
import com.tidefetch.app.util.LogSanitizer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object DownloadEvents {
    private const val maxLogLines = 400
    private val clock = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    private val mutableState = MutableStateFlow(DownloadSnapshot())

    val state = mutableState.asStateFlow()

    fun begin() {
        mutableState.value = DownloadSnapshot(
            phase = DownloadPhase.PREPARING,
            progress = null,
            status = "Preparing the download engine…",
            logs = mutableState.value.logs,
        )
    }

    fun updateProgress(progressPercent: Float, etaSeconds: Long, rawLine: String) {
        val normalized = progressPercent
            .takeIf { it.isFinite() && it >= 0f }
            ?.coerceIn(0f, 100f)
            ?.div(100f)
        val stage = when {
            rawLine.contains("[Merger]", ignoreCase = true) ->
                DownloadPhase.PROCESSING to "Merging video and audio…"
            rawLine.contains("[ExtractAudio]", ignoreCase = true) ->
                DownloadPhase.PROCESSING to "Extracting audio…"
            rawLine.contains("[VideoConvertor]", ignoreCase = true) ||
                rawLine.contains("[VideoRemuxer]", ignoreCase = true) ->
                DownloadPhase.PROCESSING to "Preparing a compatible file…"
            else -> DownloadPhase.DOWNLOADING to "Downloading media…"
        }
        mutableState.update { current ->
            current.copy(
                phase = stage.first,
                progress = if (stage.first == DownloadPhase.DOWNLOADING) normalized else null,
                etaSeconds = etaSeconds.takeIf { it >= 0 },
                status = stage.second,
            )
        }
        addLog(rawLine)
    }

    fun markSaving(fileName: String) {
        mutableState.update {
            it.copy(
                phase = DownloadPhase.SAVING,
                progress = null,
                etaSeconds = null,
                status = "Saving to your media library…",
                fileName = fileName,
            )
        }
    }

    fun markCanceling() {
        mutableState.update {
            if (!it.isActive) it else it.copy(
                progress = null,
                etaSeconds = null,
                status = "Canceling safely…",
            )
        }
    }

    fun markSuccess(uri: Uri, mimeType: String, fileName: String) {
        addLog("Saved $fileName to the media library")
        mutableState.update {
            it.copy(
                phase = DownloadPhase.SUCCESS,
                progress = 1f,
                etaSeconds = null,
                status = "Saved to your media library",
                outputUri = uri,
                outputMimeType = mimeType,
                fileName = fileName,
                errorMessage = null,
            )
        }
    }

    fun markError(message: String) {
        addLog("ERROR: $message")
        mutableState.update {
            it.copy(
                phase = DownloadPhase.ERROR,
                progress = null,
                etaSeconds = null,
                status = "Download failed",
                errorMessage = message,
            )
        }
    }

    fun markCanceled() {
        if (mutableState.value.phase == DownloadPhase.CANCELED) return
        addLog("Download canceled")
        mutableState.update {
            it.copy(
                phase = DownloadPhase.CANCELED,
                progress = null,
                etaSeconds = null,
                status = "Canceled",
                errorMessage = null,
            )
        }
    }

    fun addLog(rawLine: String) {
        val timestamp = clock.format(Instant.now())
        val additions = rawLine.lineSequence()
            .map { LogSanitizer.sanitize(it.trim()) }
            .filter { it.isNotBlank() }
            .map { "$timestamp  $it" }
            .toList()
        if (additions.isEmpty()) return
        mutableState.update { current ->
            current.copy(logs = (current.logs + additions).takeLast(maxLogLines))
        }
    }

    fun clearLogs() {
        mutableState.update { it.copy(logs = emptyList()) }
    }

    fun resetOutcome() {
        if (mutableState.value.isActive) return
        mutableState.value = DownloadSnapshot(logs = mutableState.value.logs)
    }
}
