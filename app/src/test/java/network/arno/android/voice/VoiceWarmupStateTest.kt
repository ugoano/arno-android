package network.arno.android.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceWarmupStateTest {

    @Test
    fun `first warmup attempt is allowed and subsequent attempts are blocked after success`() {
        val state = VoiceWarmupState()

        assertTrue(state.beginWarmUp())
        state.markWarmUpSuccess()

        assertFalse(state.beginWarmUp())
        assertTrue(state.isWarm())
    }

    @Test
    fun `warmup attempt is blocked while one is already in progress`() {
        val state = VoiceWarmupState()

        assertTrue(state.beginWarmUp())
        assertFalse(state.beginWarmUp())
    }

    @Test
    fun `failed warmup can be retried and does not mark warm state`() {
        val state = VoiceWarmupState()

        assertTrue(state.beginWarmUp())
        state.markWarmUpFailure()

        assertFalse(state.isWarm())
        assertTrue(state.beginWarmUp())
    }

    @Test
    fun `reset allows re-warmup after previous success`() {
        val state = VoiceWarmupState()

        assertTrue(state.beginWarmUp())
        state.markWarmUpSuccess()
        assertTrue(state.isWarm())
        assertFalse(state.beginWarmUp())

        state.reset()

        assertFalse(state.isWarm())
        assertTrue(state.beginWarmUp())
    }
}
