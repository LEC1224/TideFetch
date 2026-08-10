package com.tidefetch.app.download

import com.tidefetch.app.util.LogSanitizer
import com.tidefetch.app.util.UrlTools

data class DownloadErrorReport(
    val message: String,
    val suggestion: String? = null,
)

object DownloadErrorFormatter {
    private val ansiEscape = Regex("\u001B\\[[;?0-9]*[ -/]*[@-~]")
    private val opaqueClassName = Regex("^[a-z][0-9]*\\.[A-Za-z][A-Za-z0-9]*$")

    fun describe(
        error: Throwable,
        diagnostics: List<String>,
        sourceUrl: String,
    ): DownloadErrorReport {
        val details = diagnostics + throwableDetails(error)
        val combined = details.joinToString("\n").lowercase()
        val isX = UrlTools.isXUrl(sourceUrl)

        return when {
            error is DownloadEngineInitializationException ||
                "commons.compress" in combined || "extrafieldutils" in combined ->
                DownloadErrorReport(
                    "The portable download engine could not start.",
                    "Install this update, restart TideFetch, and try again.",
                )

            "no space left" in combined || "enospc" in combined ->
                DownloadErrorReport(
                    "There is not enough free storage to finish this download.",
                    "Free some space and try again. Finalizing a download temporarily needs extra room.",
                )

            isX && ("429" in combined || "rate limit" in combined || "too many requests" in combined) ->
                DownloadErrorReport(
                    "X temporarily rate-limited this download.",
                    "Wait a few minutes, then try again. TideFetch already tried X's public fallback.",
                )

            isX && containsAny(
                combined,
                "protected tweet",
                "private",
                "login required",
                "log in",
                "sign in",
                "authentication",
                "cookies",
                "not authorized",
            ) ->
                DownloadErrorReport(
                    "This X post is not publicly accessible.",
                    "TideFetch cannot use a signed-in X session, so protected or restricted posts cannot be saved.",
                )

            isX && containsAny(combined, "no video could be found", "no downloadable video", "no formats") ->
                DownloadErrorReport(
                    "No downloadable video was found in this X post.",
                    "The post may contain only images, an external player, or media that X no longer exposes publicly.",
                )

            isX && containsAny(combined, "not found", "does not exist", "unavailable", "suspended") ->
                DownloadErrorReport(
                    "This X post is unavailable or has been removed.",
                    "Check that the link opens while signed out in a browser, then try again.",
                )

            "unsupported url" in combined ->
                DownloadErrorReport(
                    "This site or link type is not supported by the bundled yt-dlp version.",
                    "Check that you pasted the direct post or video page, not a profile or search page.",
                )

            "requested format is not available" in combined || "format is not available" in combined ->
                DownloadErrorReport(
                    "That resolution or format is not available for this link.",
                    "Try Original resolution or another output format.",
                )

            containsAny(combined, "private", "login", "cookies", "sign in", "authentication") ->
                DownloadErrorReport(
                    "This media needs an account or is not publicly available.",
                    "TideFetch does not import browser sessions or cookies.",
                )

            containsAny(
                combined,
                "network is unreachable",
                "timed out",
                "connection reset",
                "connection refused",
                "temporary failure in name resolution",
                "unable to download",
            ) ->
                DownloadErrorReport(
                    "The source could not be reached.",
                    "Check your connection and try again. The site may also be temporarily blocking requests.",
                )

            else -> {
                val engineMessage = lastUsefulEngineError(diagnostics)
                if (engineMessage != null) {
                    DownloadErrorReport(
                        engineMessage,
                        "The original engine response is shown below in Technical details.",
                    )
                } else {
                    DownloadErrorReport(
                        "The download engine stopped before it could save this item.",
                        "Technical details now includes the underlying exception and engine output.",
                    )
                }
            }
        }
    }

    fun shouldRetryXWithSyndication(
        sourceUrl: String,
        error: Throwable,
        diagnostics: List<String>,
    ): Boolean {
        if (!UrlTools.isXUrl(sourceUrl)) return false
        val combined = (diagnostics + throwableDetails(error)).joinToString("\n").lowercase()
        if (containsAny(
                combined,
                "protected tweet",
                "private",
                "login required",
                "sign in",
                "cookies",
                "requested format is not available",
                "no space left",
                "enospc",
            )
        ) return false

        return containsAny(
            combined,
            "429",
            "403",
            "twitter api",
            "graphql",
            "guest token",
            "dependency: unspecified",
            "unable to download api",
            "api.twitter.com",
            "api.x.com",
            "tweet result",
        )
    }

    fun throwableDetails(error: Throwable): List<String> =
        generateSequence(error) { it.cause }
            .take(8)
            .map { cause ->
                val className = cause::class.java.name
                val message = cause.message?.trim().orEmpty()
                if (message.isBlank()) className else "$className: $message"
            }
            .distinct()
            .toList()

    private fun lastUsefulEngineError(diagnostics: List<String>): String? = diagnostics
        .asReversed()
        .asSequence()
        .map { ansiEscape.replace(LogSanitizer.sanitize(it), "").trim() }
        .mapNotNull { line ->
            val marker = line.indexOf("ERROR:", ignoreCase = true)
            if (marker >= 0) line.substring(marker + "ERROR:".length).trim() else null
        }
        .firstOrNull { message ->
            message.length >= 8 && !opaqueClassName.matches(message) &&
                !message.equals("Unable to download webpage", ignoreCase = true)
        }
        ?.replace(Regex("\\s+"), " ")
        ?.take(260)

    private fun containsAny(value: String, vararg needles: String): Boolean =
        needles.any(value::contains)
}

class DownloadEngineInitializationException(cause: Throwable) :
    Exception("Portable download engine initialization failed", cause)
