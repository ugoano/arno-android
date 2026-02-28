package network.arno.android.chat

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for the history comparison logic used in local-first rendering.
 * When switching sessions, we show Room DB messages instantly, then fetch
 * bridge history in background. We only update UI if the history differs.
 */
class HistoryComparisonTest {

    @Test
    fun `identical histories return false - no update needed`() {
        val current = listOf(
            ChatMessage(role = ChatMessage.Role.USER, content = "Hello"),
            ChatMessage(role = ChatMessage.Role.ASSISTANT, content = "Hi there"),
        )
        val incoming = listOf("user" to "Hello", "assistant" to "Hi there")

        assertFalse(HistoryComparison.shouldUpdateFromHistory(current, incoming))
    }

    @Test
    fun `different message count returns true`() {
        val current = listOf(
            ChatMessage(role = ChatMessage.Role.USER, content = "Hello"),
        )
        val incoming = listOf(
            "user" to "Hello",
            "assistant" to "Hi there",
        )

        assertTrue(HistoryComparison.shouldUpdateFromHistory(current, incoming))
    }

    @Test
    fun `different last message content returns true`() {
        val current = listOf(
            ChatMessage(role = ChatMessage.Role.USER, content = "Hello"),
            ChatMessage(role = ChatMessage.Role.ASSISTANT, content = "Old response"),
        )
        val incoming = listOf(
            "user" to "Hello",
            "assistant" to "Updated response",
        )

        assertTrue(HistoryComparison.shouldUpdateFromHistory(current, incoming))
    }

    @Test
    fun `empty current with non-empty incoming returns true`() {
        val current = emptyList<ChatMessage>()
        val incoming = listOf("user" to "Hello")

        assertTrue(HistoryComparison.shouldUpdateFromHistory(current, incoming))
    }

    @Test
    fun `non-empty current with empty incoming returns false - keep local data`() {
        val current = listOf(
            ChatMessage(role = ChatMessage.Role.USER, content = "Hello"),
        )
        val incoming = emptyList<Pair<String, String>>()

        assertFalse(HistoryComparison.shouldUpdateFromHistory(current, incoming))
    }

    @Test
    fun `system messages in current are excluded from comparison`() {
        val current = listOf(
            ChatMessage(role = ChatMessage.Role.USER, content = "Hello"),
            ChatMessage(role = ChatMessage.Role.SYSTEM, content = "Connected"),
            ChatMessage(role = ChatMessage.Role.ASSISTANT, content = "Hi there"),
        )
        // Incoming won't have system messages - should still match
        val incoming = listOf("user" to "Hello", "assistant" to "Hi there")

        assertFalse(HistoryComparison.shouldUpdateFromHistory(current, incoming))
    }

    @Test
    fun `tool messages in current are excluded from comparison`() {
        val current = listOf(
            ChatMessage(role = ChatMessage.Role.USER, content = "Hello"),
            ChatMessage(role = ChatMessage.Role.TOOL, content = "Result: ok"),
            ChatMessage(role = ChatMessage.Role.ASSISTANT, content = "Done"),
        )
        val incoming = listOf("user" to "Hello", "assistant" to "Done")

        assertFalse(HistoryComparison.shouldUpdateFromHistory(current, incoming))
    }

    @Test
    fun `both empty returns false`() {
        assertFalse(HistoryComparison.shouldUpdateFromHistory(emptyList(), emptyList()))
    }

    @Test
    fun `different roles at same position returns true`() {
        val current = listOf(
            ChatMessage(role = ChatMessage.Role.USER, content = "Hello"),
            ChatMessage(role = ChatMessage.Role.USER, content = "Another message"),
        )
        val incoming = listOf(
            "user" to "Hello",
            "assistant" to "Another message",
        )

        assertTrue(HistoryComparison.shouldUpdateFromHistory(current, incoming))
    }
}
