package network.arno.android.schedules

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses schedule fields with serial names`() {
        val payload = """
            {
              "id": "abc-123",
              "command": "echo hello",
              "schedule": "*/5 * * * *",
              "description": "test job",
              "enabled": true,
              "last_run": "2026-02-26T12:00:00Z",
              "next_run": "2026-02-26T12:05:00Z",
              "created": "2026-02-25T00:00:00Z"
            }
        """.trimIndent()

        val parsed = json.decodeFromString<Schedule>(payload)

        assertEquals("abc-123", parsed.id)
        assertEquals("echo hello", parsed.command)
        assertEquals("*/5 * * * *", parsed.schedule)
        assertEquals("test job", parsed.description)
        assertEquals("2026-02-26T12:00:00Z", parsed.lastRun)
        assertEquals("2026-02-26T12:05:00Z", parsed.nextRun)
    }

    @Test
    fun `uses defaults when optional values missing`() {
        val payload = """
            {
              "id": "abc-456",
              "command": "pwd",
              "schedule": "0 * * * *"
            }
        """.trimIndent()

        val parsed = json.decodeFromString<Schedule>(payload)

        assertEquals("", parsed.description)
        assertFalse(parsed.enabled)
        assertNull(parsed.lastRun)
        assertNull(parsed.nextRun)
        assertNull(parsed.created)
    }
}
