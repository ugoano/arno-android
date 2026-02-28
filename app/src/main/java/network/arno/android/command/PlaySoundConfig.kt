package network.arno.android.command

/**
 * Pure configuration and validation for play_sound command.
 * No Android dependencies — fully unit testable.
 */
object PlaySoundConfig {
    const val DEFAULT_VOLUME = 1.0f
    const val PLAY_TO_COMPLETION = -1L

    fun clampVolume(volume: Float): Float =
        volume.coerceIn(0.0f, 1.0f)
}
