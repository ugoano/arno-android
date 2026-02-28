package network.arno.android.command

/**
 * Pure MIME type inference from filename extension.
 * No Android dependencies — fully unit testable.
 */
object MimeTypeResolver {
    private val MIME_MAP = mapOf(
        "mp3" to "audio/mpeg",
        "wav" to "audio/wav",
        "ogg" to "audio/ogg",
        "m4a" to "audio/mp4",
        "flac" to "audio/flac",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "png" to "image/png",
        "gif" to "image/gif",
        "webp" to "image/webp",
        "bmp" to "image/bmp",
        "mp4" to "video/mp4",
        "webm" to "video/webm",
        "mkv" to "video/x-matroska",
        "pdf" to "application/pdf",
        "txt" to "text/plain",
        "json" to "application/json",
        "zip" to "application/zip",
        "apk" to "application/vnd.android.package-archive",
    )

    fun fromFilename(filename: String): String {
        val ext = filename.substringAfterLast('.', "").lowercase()
        return MIME_MAP[ext] ?: "application/octet-stream"
    }
}
