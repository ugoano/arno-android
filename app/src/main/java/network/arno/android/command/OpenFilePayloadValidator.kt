package network.arno.android.command

/**
 * Pure validation logic for open_file command payloads.
 * Validates path or URI mode and resolves MIME types.
 * No Android dependencies — fully unit testable.
 */
sealed class OpenFileValidation {
    data class ValidPath(
        val directory: String,
        val filename: String,
        val mimeType: String,
    ) : OpenFileValidation()

    data class ValidUri(val uri: String) : OpenFileValidation()
    data class Invalid(val reason: String) : OpenFileValidation()
}

object OpenFilePayloadValidator {

    fun validate(path: String?, uri: String?): OpenFileValidation {
        // Path mode takes priority when both are provided
        if (!path.isNullOrBlank()) {
            if (!FilePathResolver.hasFilename(path)) {
                return OpenFileValidation.Invalid("Path must include a filename")
            }
            val parsed = FilePathResolver.parse(path)
            val mimeType = MimeTypeResolver.fromFilename(parsed.filename)
            return OpenFileValidation.ValidPath(
                directory = parsed.directory,
                filename = parsed.filename,
                mimeType = mimeType,
            )
        }

        if (!uri.isNullOrBlank()) {
            return OpenFileValidation.ValidUri(uri)
        }

        return OpenFileValidation.Invalid("Missing 'path' or 'uri' in payload")
    }
}
