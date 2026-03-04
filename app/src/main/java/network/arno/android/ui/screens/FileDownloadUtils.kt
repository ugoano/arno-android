package network.arno.android.ui.screens

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Utilities for handling file downloads in WebView.
 *
 * Extracted from PlaygroundScreen for testability following TDD methodology.
 */
object FileDownloadUtils {
    /**
     * Extracts filename from Content-Disposition header or falls back to URL.
     *
     * Handles multiple Content-Disposition formats:
     * - `attachment; filename="file.apk"`
     * - `attachment; filename=file.apk`
     * - `attachment; filename*=UTF-8''file%20name.apk` (RFC 6266)
     *
     * Falls back to extracting filename from URL if Content-Disposition is null, empty,
     * or doesn't contain a filename.
     *
     * @param contentDisposition Content-Disposition header value (can be null)
     * @param url Download URL
     * @return Extracted filename
     */
    fun extractFilename(contentDisposition: String?, url: String): String {
        // Try to extract from Content-Disposition header first
        contentDisposition?.let { disposition ->
            if (disposition.isNotBlank()) {
                // Try standard filename="..." format
                val quotedMatch = Regex("""filename="([^"]+)"""").find(disposition)
                if (quotedMatch != null) {
                    return quotedMatch.groupValues[1]
                }

                // Try unquoted filename=... format
                val unquotedMatch = Regex("""filename=([^;]+)""").find(disposition)
                if (unquotedMatch != null) {
                    return unquotedMatch.groupValues[1].trim()
                }

                // Try RFC 6266 extended filename*=UTF-8''... format
                val extendedMatch = Regex("""filename\*=UTF-8''([^;]+)""").find(disposition)
                if (extendedMatch != null) {
                    val encodedFilename = extendedMatch.groupValues[1]
                    return try {
                        URLDecoder.decode(encodedFilename, StandardCharsets.UTF_8.name())
                    } catch (e: Exception) {
                        // If decoding fails, use as-is
                        encodedFilename
                    }
                }
            }
        }

        // Fallback: extract from URL
        return extractFilenameFromUrl(url)
    }

    /**
     * Extracts filename from URL path, removing query parameters and fragments.
     *
     * @param url Download URL
     * @return Filename extracted from URL
     */
    private fun extractFilenameFromUrl(url: String): String {
        // Remove query parameters and fragments
        val urlWithoutParams = url.split('?', '#').first()

        // Extract last path segment
        return urlWithoutParams.substringAfterLast('/')
    }
}
