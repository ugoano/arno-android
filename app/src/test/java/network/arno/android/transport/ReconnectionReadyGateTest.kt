package network.arno.android.transport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconnectionReadyGateTest {

    private var now = 1_000L
    private val gate = ReconnectionReadyGate(
        timeoutMs = 5_000L,
        nowMs = { now },
    )

    @Test
    fun `shows Ready on first-time connection`() {
        val decision = gate.onStatusMessage("Ready - send a message to start")

        assertEquals(ReconnectionReadyGate.StatusDecision.SHOW, decision)
        assertFalse(gate.isReconnectInProgress)
    }

    @Test
    fun `suppresses and buffers Ready during reconnect`() {
        gate.onReconnectStarted()

        val decision = gate.onStatusMessage("Ready - send a message to start")

        assertEquals(ReconnectionReadyGate.StatusDecision.SUPPRESS, decision)
        assertTrue(gate.isReconnectInProgress)
        assertNull(gate.consumeTimedOutReadyMessage())
    }

    @Test
    fun `discards buffered Ready when chat history arrives`() {
        gate.onReconnectStarted()
        gate.onStatusMessage("Ready - send a message to start")

        gate.onChatHistoryReceived()
        now += 6_000L

        assertFalse(gate.isReconnectInProgress)
        assertNull(gate.consumeTimedOutReadyMessage())
    }

    @Test
    fun `flushes buffered Ready after timeout`() {
        gate.onReconnectStarted()
        gate.onStatusMessage("Ready - send a message to start")
        now += 5_001L

        val flushed = gate.consumeTimedOutReadyMessage()

        assertEquals("Ready - send a message to start", flushed)
        assertFalse(gate.isReconnectInProgress)
    }

    @Test
    fun `does not suppress non-Ready status during reconnect`() {
        gate.onReconnectStarted()

        val decision = gate.onStatusMessage("Reconnected")

        assertEquals(ReconnectionReadyGate.StatusDecision.SHOW, decision)
        assertTrue(gate.isReconnectInProgress)
    }
}
