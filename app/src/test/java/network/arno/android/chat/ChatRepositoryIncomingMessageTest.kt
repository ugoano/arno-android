package network.arno.android.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import network.arno.android.data.local.dao.MessageDao
import network.arno.android.data.local.dao.SessionDao
import network.arno.android.data.local.entity.MessageEntity
import network.arno.android.data.local.entity.SessionEntity
import network.arno.android.tasks.TasksRepository
import network.arno.android.transport.IncomingMessage
import network.arno.android.transport.ReconnectionReadyGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRepositoryIncomingMessageTest {

    @Test
    fun assistantTopLevelContent_rendersTextAndToolUseBlocks() = runTest {
        val repository = createRepository()

        val assistantBlocks = buildJsonArray {
            add(buildJsonObject {
                put("type", "text")
                put("text", "Hello from top-level content")
            })
            add(buildJsonObject {
                put("type", "tool_use")
                put("name", "Read")
            })
        }

        repository.handleIncoming(
            IncomingMessage(
                type = "assistant",
                content = assistantBlocks,
            )
        )

        val messages = repository.messages.value
        assertEquals(2, messages.size)
        assertEquals(ChatMessage.Role.ASSISTANT, messages[0].role)
        assertEquals("Hello from top-level content", messages[0].content)
        // Text block's streaming is finalised when the subsequent tool_use block is processed
        assertFalse(messages[0].isStreaming)
        assertEquals(ChatMessage.Role.TOOL, messages[1].role)
        assertEquals("Read", messages[1].toolName)
    }

    @Test
    fun userTopLevelContent_rendersToolResultPreviewText() = runTest {
        val repository = createRepository()

        val userBlocks = buildJsonArray {
            add(buildJsonObject {
                put("type", "tool_result")
                putJsonArray("content") {
                    add(buildJsonObject {
                        put("type", "text")
                        put("text", "Weather fetched successfully")
                    })
                }
            })
        }

        repository.handleIncoming(
            IncomingMessage(
                type = "user",
                content = userBlocks,
            )
        )

        val messages = repository.messages.value
        assertEquals(1, messages.size)
        assertEquals(ChatMessage.Role.TOOL, messages[0].role)
        assertTrue(messages[0].content.contains("Weather fetched successfully"))
    }

    @Test
    fun assistantNestedContent_rendersTextBlocks() = runTest {
        val repository = createRepository()

        val contentArray = buildJsonArray {
            add(buildJsonObject {
                put("type", "text")
                put("text", "Hello from nested message.content")
            })
        }
        val messageObj = buildJsonObject {
            put("content", contentArray)
        }

        repository.handleIncoming(
            IncomingMessage(
                type = "assistant",
                message = messageObj,
            )
        )

        val messages = repository.messages.value
        assertEquals(1, messages.size)
        assertEquals(ChatMessage.Role.ASSISTANT, messages[0].role)
        assertEquals("Hello from nested message.content", messages[0].content)
    }

    @Test
    fun userNestedContent_rendersToolResult() = runTest {
        val repository = createRepository()

        val contentArray = buildJsonArray {
            add(buildJsonObject {
                put("type", "tool_result")
                put("content", "Simple string result")
            })
        }
        val messageObj = buildJsonObject {
            put("content", contentArray)
        }

        repository.handleIncoming(
            IncomingMessage(
                type = "user",
                message = messageObj,
            )
        )

        val messages = repository.messages.value
        assertEquals(1, messages.size)
        assertEquals(ChatMessage.Role.TOOL, messages[0].role)
        assertTrue(messages[0].content.contains("Simple string result"))
    }

    @Test
    fun resultMessage_clearsProcessingIndicator() = runTest {
        val repository = createRepository()
        repository.addUserMessage("Check calendar")

        repository.handleIncoming(IncomingMessage(type = "result"))

        assertFalse(repository.isProcessing.value)
    }

    private fun createRepository(): ChatRepository {
        return ChatRepository(
            tasksRepository = TasksRepository(),
            messageDao = InMemoryMessageDao(),
            sessionDao = InMemorySessionDao(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            reconnectionReadyGate = ReconnectionReadyGate(),
        )
    }

    private class InMemoryMessageDao : MessageDao {
        private val messages = mutableListOf<MessageEntity>()

        override suspend fun insert(message: MessageEntity) {
            messages.removeAll { it.id == message.id }
            messages.add(message)
        }

        override suspend fun insertAll(messages: List<MessageEntity>) {
            this.messages.removeAll { existing -> messages.any { it.id == existing.id } }
            this.messages.addAll(messages)
        }

        override suspend fun getBySession(sessionId: String): List<MessageEntity> {
            return messages.filter { it.sessionId == sessionId }.sortedBy { it.timestamp }
        }

        override suspend fun deleteBySession(sessionId: String) {
            messages.removeAll { it.sessionId == sessionId }
        }

        override suspend fun deleteOlderThan(cutoff: Long) {
            messages.removeAll { it.timestamp < cutoff }
        }
    }

    private class InMemorySessionDao : SessionDao {
        private val sessions = linkedMapOf<String, SessionEntity>()

        override suspend fun insert(session: SessionEntity) {
            sessions[session.id] = session
        }

        override suspend fun update(session: SessionEntity) {
            sessions[session.id] = session
        }

        override suspend fun getAll(): List<SessionEntity> {
            return sessions.values.sortedByDescending { it.lastActivity }
        }

        override suspend fun getById(sessionId: String): SessionEntity? {
            return sessions[sessionId]
        }

        override suspend fun delete(sessionId: String) {
            sessions.remove(sessionId)
        }

        override suspend fun deleteOlderThan(cutoff: Long) {
            val ids = sessions.values.filter { it.lastActivity < cutoff }.map { it.id }
            ids.forEach { sessions.remove(it) }
        }

        override suspend fun count(): Int = sessions.size

        override suspend fun deleteOldest(count: Int) {
            sessions.values.sortedBy { it.lastActivity }.take(count).forEach { sessions.remove(it.id) }
        }
    }
}
