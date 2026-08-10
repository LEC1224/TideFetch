package com.tidefetch.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlToolsTest {
    @Test
    fun `extracts first complete web link from shared text`() {
        assertEquals(
            "https://youtu.be/example?t=4",
            UrlTools.firstWebUrl("Watch this: https://youtu.be/example?t=4 — nice"),
        )
    }

    @Test
    fun `trims sentence punctuation without losing query parameters`() {
        assertEquals(
            "https://x.com/user/status/123?lang=en",
            UrlTools.firstWebUrl("https://x.com/user/status/123?lang=en)."),
        )
    }

    @Test
    fun `rejects non web schemes and missing hosts`() {
        assertFalse(UrlTools.isValidWebUrl("javascript:alert(1)"))
        assertFalse(UrlTools.isValidWebUrl("https:///missing-host"))
        assertNull(UrlTools.firstWebUrl("nothing to download"))
    }

    @Test
    fun `recognizes common platform domains`() {
        assertEquals("YouTube", UrlTools.platformLabel("https://m.youtube.com/watch?v=1"))
        assertEquals("X / Twitter", UrlTools.platformLabel("https://x.com/user/status/1"))
        assertEquals("example.org", UrlTools.platformLabel("https://www.example.org/video"))
        assertTrue(UrlTools.isValidWebUrl("https://www.facebook.com/reel/123"))
        assertTrue(UrlTools.isXUrl("https://mobile.twitter.com/user/status/1"))
        assertFalse(UrlTools.isXUrl("https://notx.com/user/status/1"))
    }
}
