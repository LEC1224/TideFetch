package com.tidefetch.app.util

object LogSanitizer {
    private val urlPattern = Regex("""https?://[^\s\"']+""", RegexOption.IGNORE_CASE)
    private val secretPattern = Regex(
        """(?i)(authorization|cookie|token|signature|sig|key)=([^&\s]+)""",
    )
    private val quotedHeaderSecretPattern = Regex(
        """(?i)(['"])(authorization|proxy-authorization|cookie|set-cookie)\1(\s*:\s*)(['"])(.*?)\4""",
    )
    private val headerSecretPattern = Regex(
        """(?i)\b(authorization|proxy-authorization|cookie|set-cookie)\s*:\s*[^\r\n]+""",
    )
    private val bearerPattern = Regex("""(?i)\b(bearer|basic)\s+[A-Za-z0-9._~+/=-]+""")
    private val urlUserInfoPattern = Regex("""(https?://)[^/@\s]+@""", RegexOption.IGNORE_CASE)
    private val privatePathPattern = Regex(
        """/(?:data/(?:user/\d+|data)|storage/emulated/\d+/Android/(?:data|media))/[^\s\"']+""",
    )

    fun sanitize(line: String): String {
        val withoutQuotedHeaders = quotedHeaderSecretPattern.replace(line) {
            "${it.groupValues[1]}${it.groupValues[2]}${it.groupValues[1]}" +
                "${it.groupValues[3]}${it.groupValues[4]}[redacted]${it.groupValues[4]}"
        }
        val withoutHeaders = headerSecretPattern.replace(withoutQuotedHeaders) {
            "${it.groupValues[1]}: [redacted]"
        }
        val withoutBearer = bearerPattern.replace(withoutHeaders) { "${it.groupValues[1]} [redacted]" }
        val withoutUserInfo = urlUserInfoPattern.replace(withoutBearer, "$1[redacted]@")
        val withoutSecrets = secretPattern.replace(withoutUserInfo) {
            "${it.groupValues[1]}=[redacted]"
        }
        val withoutQueries = urlPattern.replace(withoutSecrets) { match ->
            match.value.substringBefore('?').substringBefore('#')
        }
        return privatePathPattern.replace(withoutQueries, "[app files]")
    }
}
