package com.tidefetch.app.download

import com.tidefetch.app.model.DownloadConfig
import com.tidefetch.app.model.OutputFormat
import com.tidefetch.app.model.ResolutionPreset
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatSelectorTest {
    @Test
    fun `default request preserves best resolution and prefers h264 in mp4`() {
        val command = FormatSelector.createRequest(
            DownloadConfig("https://example.com/video"),
            File("staging"),
            File("staging/result.txt"),
        ).buildCommand()

        assertContainsPair(command, "-f", "bv*+ba/b")
        assertContainsPair(command, "-S", "res,vcodec:h264,acodec:aac")
        assertContainsPair(command, "--merge-output-format", "mp4")
        assertTrue("--no-playlist" in command)
    }

    @Test
    fun `resolution limit is added only for video`() {
        val videoCommand = FormatSelector.createRequest(
            DownloadConfig(
                "https://example.com/video",
                ResolutionPreset.FULL_HD_1080,
                OutputFormat.WEBM,
            ),
            File("staging"),
            File("staging/result.txt"),
        ).buildCommand()
        val audioCommand = FormatSelector.createRequest(
            DownloadConfig(
                "https://example.com/video",
                ResolutionPreset.FULL_HD_1080,
                OutputFormat.WAV,
            ),
            File("staging"),
            File("staging/result.txt"),
        ).buildCommand()

        assertTrue(videoCommand[videoCommand.indexOf("-f") + 1].contains("[height<=1080]"))
        assertTrue("-S" !in audioCommand)
        assertContainsPair(audioCommand, "--audio-format", "wav")
    }

    @Test
    fun `final path manifest arguments stay distinct`() {
        val manifest = File("staging/result.txt")
        val command = FormatSelector.createRequest(
            DownloadConfig("https://example.com/video", format = OutputFormat.MP3),
            File("staging"),
            manifest,
        ).buildCommand()
        val marker = command.indexOf("--print-to-file")

        assertTrue(marker >= 0)
        assertEquals("after_move:%(filepath)s", command[marker + 1])
        assertEquals(manifest.absolutePath, command[marker + 2])
        assertEquals("https://example.com/video", command.last())
    }

    @Test
    fun `requests include bounded retry policy`() {
        val command = FormatSelector.createRequest(
            DownloadConfig("https://example.com/video"),
            File("staging"),
            File("staging/result.txt"),
        ).buildCommand()

        assertContainsPair(command, "--retries", "3")
        assertContainsPair(command, "--fragment-retries", "3")
        assertContainsPair(command, "--extractor-retries", "3")
    }

    @Test
    fun `twitter fallback uses documented syndication extractor`() {
        val command = FormatSelector.createRequest(
            DownloadConfig("https://x.com/example/status/123"),
            File("staging"),
            File("staging/result.txt"),
            useTwitterSyndicationFallback = true,
        ).buildCommand()

        assertContainsPair(command, "--extractor-args", "twitter:api=syndication")
    }

    private fun assertContainsPair(command: List<String>, option: String, value: String) {
        val index = command.indexOf(option)
        assertTrue("Missing $option in $command", index >= 0)
        assertEquals(value, command[index + 1])
    }
}
