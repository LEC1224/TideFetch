package com.tidefetch.app.download

import com.tidefetch.app.model.DownloadConfig
import com.tidefetch.app.model.OutputFormat
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

object FormatSelector {
    fun createRequest(
        config: DownloadConfig,
        stagingDirectory: File,
        resultManifest: File,
    ): YoutubeDLRequest {
        val request = YoutubeDLRequest(config.url)
            .addOption("--no-playlist")
            .addOption("--newline")
            .addOption("--progress")
            .addOption("--progress-delta", 0.25)
            .addOption("--no-mtime")
            .addOption("-P", stagingDirectory.absolutePath)
            .addOption("-o", "%(title).160B [%(id)s].%(ext)s")

        val heightLimit = config.resolution.maxHeight?.let { "[height<=$it]" }.orEmpty()

        when (config.format) {
            OutputFormat.MP4_H264 -> request
                .addOption("-f", "bv*$heightLimit+ba/b$heightLimit")
                // Preserve the best resolution first, preferring H.264/AAC among
                // otherwise comparable streams. Remuxing does not invent quality.
                .addOption("-S", "res,vcodec:h264,acodec:aac")
                .addOption("--merge-output-format", "mp4")
                .addOption("--remux-video", "mp4")

            OutputFormat.MP4_SOURCE -> request
                .addOption(
                    "-f",
                    "bv*[ext=mp4]$heightLimit+ba[ext=m4a]/" +
                        "b[ext=mp4]$heightLimit",
                )
                .addOption("-S", "res")
                .addOption("--merge-output-format", "mp4")
                .addOption("--remux-video", "mp4")

            OutputFormat.WEBM -> request
                .addOption(
                    "-f",
                    "bv*[ext=webm]$heightLimit+ba[ext=webm]/" +
                        "b[ext=webm]$heightLimit",
                )
                .addOption("-S", "res")
                .addOption("--merge-output-format", "webm")

            OutputFormat.MP3 -> request
                .addOption("--preset-alias", "mp3")
                .addOption("--audio-quality", "0")

            OutputFormat.WAV -> request
                .addOption("-f", "ba/b")
                .addOption("-x")
                .addOption("--audio-format", "wav")

            OutputFormat.M4A -> request
                .addOption("-f", "ba[ext=m4a]/ba/b")
                .addOption("-x")
                .addOption("--audio-format", "m4a")
        }

        // addCommands keeps the three arguments distinct. after_move:filepath is the
        // authoritative final path after yt-dlp has merged/remuxed/extracted the media.
        request.addCommands(
            listOf(
                "--print-to-file",
                "after_move:%(filepath)s",
                resultManifest.absolutePath,
            ),
        )
        return request
    }
}
