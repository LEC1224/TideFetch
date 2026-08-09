package com.tidefetch.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogSanitizerTest {
    @Test
    fun `redacts URL queries secrets and private app paths`() {
        val sanitized = LogSanitizer.sanitize(
            "GET https://example.com/media?id=4&token=secret " +
                "cookie=abc /data/user/0/com.tidefetch.app/cache/file.part",
        )

        assertTrue("https://example.com/media" in sanitized)
        assertTrue("cookie=[redacted]" in sanitized)
        assertTrue("[app files]" in sanitized)
        assertFalse("secret" in sanitized)
        assertFalse("com.tidefetch.app" in sanitized)
    }

    @Test
    fun `redacts headers bearer values and URL credentials`() {
        val sanitized = LogSanitizer.sanitize(
            "Authorization: Bearer top.secret Cookie: SID=private " +
                "https://alice:password@example.com/file",
        )

        assertFalse("top.secret" in sanitized)
        assertFalse("SID=private" in sanitized)
        assertFalse("alice:password" in sanitized)
        assertTrue("[redacted]" in sanitized)
    }

    @Test
    fun `redacts quoted header maps without swallowing neighboring diagnostics`() {
        val sanitized = LogSanitizer.sanitize(
            "headers={'Cookie': 'SID=private', \"Authorization\": \"Basic dXNlcjpwYXNz\", " +
                "'Accept': '*/*'}",
        )

        assertFalse("SID=private" in sanitized)
        assertFalse("dXNlcjpwYXNz" in sanitized)
        assertTrue("'Cookie': '[redacted]'" in sanitized)
        assertTrue("\"Authorization\": \"[redacted]\"" in sanitized)
        assertTrue("'Accept': '*/*'" in sanitized)
    }
}
