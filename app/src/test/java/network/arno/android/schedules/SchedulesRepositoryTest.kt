package network.arno.android.schedules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulesRepositoryTest {

    @Test
    fun `parseSchedulesResponse parses schedules array`() {
        val repository = SchedulesRepository("https://example.com")
        val payload = """
            {
              "schedules": [
                {
                  "id": "s1",
                  "command": "echo one",
                  "schedule": "*/1 * * * *",
                  "enabled": true
                },
                {
                  "id": "s2",
                  "command": "echo two",
                  "schedule": "*/2 * * * *"
                }
              ]
            }
        """.trimIndent()

        val parsed = repository.parseSchedulesResponse(payload)

        assertEquals(2, parsed.size)
        assertEquals("s1", parsed[0].id)
        assertTrue(parsed[0].enabled)
        assertEquals("s2", parsed[1].id)
        assertFalse(parsed[1].enabled)
    }
}
