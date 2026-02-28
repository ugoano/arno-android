package network.arno.android.command

/**
 * Pure path parsing logic for open_file command.
 * Extracts directory and filename from a relative path string.
 * No Android dependencies — fully unit testable.
 */
object FilePathResolver {

    data class ParsedPath(val directory: String, val filename: String)

    fun parse(path: String): ParsedPath {
        val cleaned = path.trimStart('/')
        if (cleaned.isEmpty()) return ParsedPath("", "")

        val lastSlash = cleaned.lastIndexOf('/')
        return if (lastSlash < 0) {
            ParsedPath("", cleaned)
        } else {
            ParsedPath(
                directory = cleaned.substring(0, lastSlash),
                filename = cleaned.substring(lastSlash + 1),
            )
        }
    }

    fun hasFilename(path: String): Boolean {
        val parsed = parse(path)
        return parsed.filename.isNotEmpty()
    }
}
