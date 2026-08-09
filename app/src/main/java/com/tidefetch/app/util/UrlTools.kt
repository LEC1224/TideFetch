package com.tidefetch.app.util

import java.net.URI

object UrlTools {
    private val webUrl = Regex("""https?://[^\s<>\"']+""", RegexOption.IGNORE_CASE)

    fun firstWebUrl(text: CharSequence?): String? {
        val candidate = webUrl.find(text?.toString().orEmpty())
            ?.value
            ?.trimEnd('.', ',', ';', ')', ']', '}')
            ?: return null
        return candidate.takeIf(::isValidWebUrl)
    }

    fun isValidWebUrl(value: String): Boolean = runCatching {
        val uri = URI(value.trim())
        (uri.scheme.equals("http", ignoreCase = true) ||
            uri.scheme.equals("https", ignoreCase = true)) &&
            !uri.host.isNullOrBlank()
    }.getOrDefault(false)

    fun platformLabel(value: String): String? {
        val host = runCatching { URI(value.trim()).host?.lowercase() }.getOrNull() ?: return null
        return when {
            host == "youtu.be" || host.endsWith("youtube.com") -> "YouTube"
            host.endsWith("facebook.com") || host == "fb.watch" -> "Facebook"
            host.endsWith("twitter.com") || host.endsWith("x.com") -> "X / Twitter"
            host.endsWith("instagram.com") -> "Instagram"
            host.endsWith("tiktok.com") -> "TikTok"
            host.endsWith("vimeo.com") -> "Vimeo"
            host.endsWith("reddit.com") || host == "redd.it" -> "Reddit"
            host.endsWith("twitch.tv") -> "Twitch"
            else -> host.removePrefix("www.")
        }
    }
}
