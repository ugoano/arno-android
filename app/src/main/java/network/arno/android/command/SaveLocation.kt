package network.arno.android.command

/**
 * Target directories for file transfers.
 * Maps to Android MediaStore collections in TransferFileHandler.
 */
enum class SaveLocation {
    DOWNLOADS,
    MUSIC,
    PICTURES,
    DOCUMENTS;

    companion object {
        fun fromString(value: String): SaveLocation =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: DOWNLOADS
    }
}
