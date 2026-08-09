package com.tidefetch.app.model

import android.net.Uri

enum class ResolutionPreset(
    val displayName: String,
    val maxHeight: Int?,
) {
    ORIGINAL("Original (best)", null),
    UHD_2160("2160p · 4K", 2160),
    QHD_1440("1440p · QHD", 1440),
    FULL_HD_1080("1080p · Full HD", 1080),
    HD_720("720p · HD", 720),
    SD_480("480p", 480),
    LOW_360("360p", 360),
}

enum class OutputFormat(
    val displayName: String,
    val shortName: String,
    val isAudioOnly: Boolean,
    val expectedExtension: String,
    val mimeType: String,
) {
    MP4_H264(
        displayName = "MP4 · H.264 preferred",
        shortName = "MP4 H.264",
        isAudioOnly = false,
        expectedExtension = "mp4",
        mimeType = "video/mp4",
    ),
    MP4_SOURCE(
        displayName = "MP4 · source codecs",
        shortName = "MP4 source",
        isAudioOnly = false,
        expectedExtension = "mp4",
        mimeType = "video/mp4",
    ),
    WEBM(
        displayName = "WebM · source quality",
        shortName = "WebM",
        isAudioOnly = false,
        expectedExtension = "webm",
        mimeType = "video/webm",
    ),
    MP3(
        displayName = "MP3 · best quality",
        shortName = "MP3 audio",
        isAudioOnly = true,
        expectedExtension = "mp3",
        mimeType = "audio/mpeg",
    ),
    WAV(
        displayName = "WAV · lossless PCM",
        shortName = "WAV audio",
        isAudioOnly = true,
        expectedExtension = "wav",
        mimeType = "audio/wav",
    ),
    M4A(
        displayName = "M4A · source audio",
        shortName = "M4A audio",
        isAudioOnly = true,
        expectedExtension = "m4a",
        mimeType = "audio/mp4",
    ),
}

data class DownloadConfig(
    val url: String,
    val resolution: ResolutionPreset = ResolutionPreset.ORIGINAL,
    val format: OutputFormat = OutputFormat.MP4_H264,
)

enum class DownloadPhase {
    IDLE,
    PREPARING,
    DOWNLOADING,
    PROCESSING,
    SAVING,
    SUCCESS,
    ERROR,
    CANCELED,
}

data class DownloadSnapshot(
    val phase: DownloadPhase = DownloadPhase.IDLE,
    val progress: Float? = null,
    val etaSeconds: Long? = null,
    val status: String = "Ready",
    val logs: List<String> = emptyList(),
    val outputUri: Uri? = null,
    val outputMimeType: String? = null,
    val fileName: String? = null,
    val errorMessage: String? = null,
) {
    val isActive: Boolean
        get() = phase in setOf(
            DownloadPhase.PREPARING,
            DownloadPhase.DOWNLOADING,
            DownloadPhase.PROCESSING,
            DownloadPhase.SAVING,
        )
}
