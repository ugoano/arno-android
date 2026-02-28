package network.arno.android.notification

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class NotificationDataTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `NotificationData holds all required fields`() {
        val data = NotificationData(
            title = "John Doe",
            text = "Hey, can you review this PR?",
            packageName = "com.Slack",
            timestamp = 1709136000000L,
        )
        assertEquals("John Doe", data.title)
        assertEquals("Hey, can you review this PR?", data.text)
        assertEquals("com.Slack", data.packageName)
        assertEquals(1709136000000L, data.timestamp)
    }

    @Test
    fun `NotificationData serialises to JSON with correct field names`() {
        val data = NotificationData(
            title = "John Doe",
            text = "Hello",
            packageName = "com.Slack",
            timestamp = 1709136000000L,
        )
        val jsonStr = json.encodeToString(data)
        assertTrue(jsonStr.contains("\"title\""))
        assertTrue(jsonStr.contains("\"text\""))
        assertTrue(jsonStr.contains("\"package_name\""))
        assertTrue(jsonStr.contains("\"timestamp\""))
    }

    @Test
    fun `NotificationData deserialises from JSON`() {
        val jsonStr = """{"title":"Alice","text":"Meeting in 5","package_name":"com.Slack","timestamp":1709136000000}"""
        val data = json.decodeFromString<NotificationData>(jsonStr)
        assertEquals("Alice", data.title)
        assertEquals("Meeting in 5", data.text)
        assertEquals("com.Slack", data.packageName)
        assertEquals(1709136000000L, data.timestamp)
    }

    @Test
    fun `NotificationBridgeMessage wraps data with correct type`() {
        val data = NotificationData(
            title = "Test",
            text = "Message",
            packageName = "com.Slack",
            timestamp = 1709136000000L,
        )
        val msg = NotificationBridgeMessage(data = data)
        assertEquals("notification_bridge", msg.type)
        assertEquals(data, msg.data)
    }

    @Test
    fun `NotificationBridgeMessage serialises with type field`() {
        val data = NotificationData(
            title = "Test",
            text = "Message",
            packageName = "com.Slack",
            timestamp = 1709136000000L,
        )
        val msg = NotificationBridgeMessage(data = data)
        val jsonStr = json.encodeToString(msg)
        assertTrue(jsonStr.contains("\"type\":\"notification_bridge\""))
        assertTrue(jsonStr.contains("\"data\""))
    }

    @Test
    fun `NotificationData handles null text gracefully`() {
        val data = NotificationData(
            title = "App Update",
            text = null,
            packageName = "com.android.vending",
            timestamp = 1709136000000L,
        )
        assertNull(data.text)
        val jsonStr = json.encodeToString(data)
        assertTrue(jsonStr.contains("\"text\":null"))
    }

    @Test
    fun `NotificationData handles empty strings`() {
        val data = NotificationData(
            title = "",
            text = "",
            packageName = "com.Slack",
            timestamp = 0L,
        )
        assertEquals("", data.title)
        assertEquals("", data.text)
    }

    @Test
    fun `NotificationBridgeMessage includes client_id when provided`() {
        val data = NotificationData(
            title = "Test",
            text = "Msg",
            packageName = "com.Slack",
            timestamp = 1709136000000L,
        )
        val msg = NotificationBridgeMessage(data = data, clientId = "android_abc123")
        val jsonStr = json.encodeToString(msg)
        assertTrue(jsonStr.contains("\"client_id\":\"android_abc123\""))
    }
}
