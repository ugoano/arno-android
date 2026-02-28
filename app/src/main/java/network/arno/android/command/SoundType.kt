package network.arno.android.command

/**
 * Supported sound types for the play_sound command.
 * Maps to Android's RingtoneManager constants in PlaySoundHandler.
 */
enum class SoundType {
    ALARM,
    NOTIFICATION,
    RINGTONE;

    companion object {
        fun fromString(value: String): SoundType? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}
