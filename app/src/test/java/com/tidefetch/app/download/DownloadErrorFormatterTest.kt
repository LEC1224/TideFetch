package com.tidefetch.app.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadErrorFormatterTest {
    private val xUrl = "https://x.com/example/status/123"

    @Test
    fun `initialization failures receive a useful action instead of an obfuscated class`() {
        val report = DownloadErrorFormatter.describe(
            DownloadEngineInitializationException(InstantiationException("e3.f")),
            emptyList(),
            xUrl,
        )

        assertEquals("The portable download engine could not start.", report.message)
        assertTrue(report.suggestion.orEmpty().contains("restart", ignoreCase = true))
    }

    @Test
    fun `x rate limits get an x specific explanation`() {
        val report = DownloadErrorFormatter.describe(
            RuntimeException(),
            listOf("ERROR: HTTP Error 429: Too Many Requests"),
            xUrl,
        )

        assertTrue(report.message.contains("rate-limited"))
        assertTrue(report.suggestion.orEmpty().contains("fallback"))
    }

    @Test
    fun `useful engine error is surfaced when no friendly mapping exists`() {
        val report = DownloadErrorFormatter.describe(
            RuntimeException(),
            listOf("[debug] connecting", "ERROR: The broadcaster has ended this event"),
            "https://example.com/watch/1",
        )

        assertEquals("The broadcaster has ended this event", report.message)
    }

    @Test
    fun `x api failures retry but protected posts do not`() {
        assertTrue(
            DownloadErrorFormatter.shouldRetryXWithSyndication(
                xUrl,
                RuntimeException(),
                listOf("ERROR: GraphQL API returned HTTP Error 403"),
            ),
        )
        assertFalse(
            DownloadErrorFormatter.shouldRetryXWithSyndication(
                xUrl,
                RuntimeException(),
                listOf("ERROR: This tweet is private; login required"),
            ),
        )
    }
}
