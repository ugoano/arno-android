package network.arno.android.chat

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import network.arno.android.data.local.dao.MessageDao
import network.arno.android.data.local.dao.SessionDao
import network.arno.android.data.local.entity.MessageEntity
import network.arno.android.data.local.entity.SessionEntity
import network.arno.android.tasks.TasksRepository
import network.arno.android.transport.IncomingMessage

class ChatRepository(
    private val tasksRepository: TasksRepository,
    private val messageDao: MessageDao,
    private val sessionDao: SessionDao,
    private val scope: CoroutineScope,
) {

    companion object {
        private const val TAG = "ChatRepository"
        private const val MAX_SESSIONS = 20
        private const val SESSION_TTL_MS = 7L * 24 * 60 * 60 * 1000 // 7 days
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private var currentSessionId: String = java.util.UUID.randomUUID().toString()

    init {
        scope.launch {
            pruneOldSessions()
            loadOrCreateSession()
        }
    }

    fun addUserMessage(
        text: String,
        viaVoice: Boolean = false,
        imageIds: List<String> = emptyList(),
        localImageUris: List<String> = emptyList(),
    ) {
        val message = ChatMessage(
            role = ChatMessage.Role.USER,
            content = text,
            viaVoice = viaVoice,
            imageIds = imageIds,
            localImageUris = localImageUris,
        )
        _messages.update { it + message }
        _isProcessing.value = true
        persistMessage(message)
    }

    fun handleIncoming(msg: IncomingMessage) {
        when (msg.type) {
            // Bridge sends "assistant" with message.content[] containing text and tool_use blocks
            "assistant" -> {
                val messageObj = msg.message as? JsonObject ?: return
                val contentArray = messageObj["content"] as? JsonArray ?: return

                for (block in contentArray) {
                    val blockObj = block as? JsonObject ?: continue
                    val blockType = blockObj["type"]?.jsonPrimitive?.contentOrNull ?: continue

                    when (blockType) {
                        "text" -> {
                            val text = blockObj["text"]?.jsonPrimitive?.contentOrNull ?: continue
                            Log.d(TAG, "Text block: ${text.take(80)}...")
                            _messages.update { messages ->
                                // Finalise any previous streaming message
                                val finalised = messages.map {
                                    if (it.isStreaming) it.copy(isStreaming = false) else it
                                }
                                finalised + ChatMessage(
                                    role = ChatMessage.Role.ASSISTANT,
                                    content = text,
                                    isStreaming = true,
                                )
                            }
                        }
                        "tool_use" -> {
                            val toolName = blockObj["name"]?.jsonPrimitive?.contentOrNull
                            Log.d(TAG, "Tool use: $toolName")
                            _messages.update { messages ->
                                val finalised = messages.map {
                                    if (it.isStreaming) it.copy(isStreaming = false) else it
                                }
                                finalised + ChatMessage(
                                    role = ChatMessage.Role.TOOL,
                                    content = "",
                                    toolName = toolName,
                                )
                            }
                        }
                    }
                }
            }

            // Bridge sends "content_block_delta" with delta.text for streaming
            "content_block_delta" -> {
                val deltaText = msg.delta?.get("text")?.jsonPrimitive?.contentOrNull ?: return
                _messages.update { messages ->
                    val last = messages.lastOrNull()
                    if (last != null && last.role == ChatMessage.Role.ASSISTANT && last.isStreaming) {
                        messages.dropLast(1) + last.copy(content = last.content + deltaText)
                    } else {
                        messages + ChatMessage(
                            role = ChatMessage.Role.ASSISTANT,
                            content = deltaText,
                            isStreaming = true,
                        )
                    }
                }
            }

            // Bridge sends "user" with tool_result blocks
            "user" -> {
                val messageObj = msg.message as? JsonObject ?: return
                val contentArray = messageObj["content"] as? JsonArray ?: return

                for (block in contentArray) {
                    val blockObj = block as? JsonObject ?: continue
                    val blockType = blockObj["type"]?.jsonPrimitive?.contentOrNull ?: continue

                    if (blockType == "tool_result") {
                        val resultContent = blockObj["content"]
                        val preview = when (resultContent) {
                            is JsonPrimitive -> resultContent.contentOrNull?.take(200) ?: ""
                            else -> resultContent?.toString()?.take(200) ?: ""
                        }
                        _messages.update { it + ChatMessage(
                            role = ChatMessage.Role.TOOL,
                            content = "Result: $preview",
                        )}
                    }
                }
            }

            // Bridge sends "result" when Claude finishes
            "result" -> {
                _messages.update { messages ->
                    messages.map {
                        if (it.isStreaming) it.copy(isStreaming = false) else it
                    }
                }
                _isProcessing.value = false
                persistAllMessages()
            }

            // Bridge confirms task was cancelled
            "task_cancelled" -> {
                _messages.update { messages ->
                    val finalised = messages.map {
                        if (it.isStreaming) it.copy(isStreaming = false) else it
                    }
                    // Append [CANCELLED] to the last assistant message
                    val lastAssistantIdx = finalised.indexOfLast {
                        it.role == ChatMessage.Role.ASSISTANT
                    }
                    if (lastAssistantIdx >= 0) {
                        finalised.toMutableList().apply {
                            val msg = this[lastAssistantIdx]
                            this[lastAssistantIdx] = msg.copy(
                                content = msg.content + " [CANCELLED]"
                            )
                        }
                    } else {
                        finalised
                    }
                }
                _isProcessing.value = false
                persistAllMessages()
                Log.i(TAG, "Task cancelled")
            }

            // Status and error messages
            "system", "status" -> {
                val text = msg.content?.jsonPrimitive?.contentOrNull ?: return
                // Skip "Processing..." status messages to avoid clutter
                if (text == "Processing...") return
                _messages.update { it + ChatMessage(
                    role = ChatMessage.Role.SYSTEM,
                    content = text,
                )}
            }
            "error", "stderr" -> {
                val errorText = msg.content?.jsonPrimitive?.contentOrNull
                    ?: msg.error
                    ?: "Unknown error"
                _messages.update { it + ChatMessage(
                    role = ChatMessage.Role.SYSTEM,
                    content = "Error: $errorText",
                )}
                _isProcessing.value = false
            }

            // Parse tasks_summary for the Tasks view
            "tasks_summary" -> {
                tasksRepository.handleTasksSummary(msg.summary)
            }

            // Bridge sends chat_history on reconnect/session switch
            "chat_history" -> {
                val messagesArray = msg.messages ?: return
                Log.i(TAG, "Received chat_history with ${messagesArray.size} messages")

                val chatMessages = messagesArray.mapNotNull { element ->
                    val obj = element as? JsonObject ?: return@mapNotNull null
                    val role = obj["role"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val content = obj["content"]?.jsonPrimitive?.contentOrNull ?: ""
                    val chatRole = when (role) {
                        "user" -> ChatMessage.Role.USER
                        "assistant" -> ChatMessage.Role.ASSISTANT
                        else -> return@mapNotNull null
                    }
                    ChatMessage(role = chatRole, content = content)
                }

                if (chatMessages.isNotEmpty()) {
                    _messages.value = chatMessages
                    _isProcessing.value = false
                    Log.i(TAG, "Loaded ${chatMessages.size} messages from bridge chat_history")
                }
            }

            // Ignore internal bridge messages that don't need UI display
            "task_created", "task_completed", "task_failed",
            "task_queued", "queue_task_started", "queued_task_cancelled",
            "task_progress", "replay", "system_message",
            "init", "raw", "auth_error", "queue_full", "cancel_queued_failed" -> {
                Log.d(TAG, "Ignoring bridge message type: ${msg.type}")
            }

            else -> {
                Log.w(TAG, "Unhandled message type: ${msg.type}")
            }
        }
    }

    fun clear() {
        _messages.value = emptyList()
        _isProcessing.value = false
        // Start a new session
        currentSessionId = java.util.UUID.randomUUID().toString()
        scope.launch { loadOrCreateSession() }
    }

    fun getCurrentSessionId(): String = currentSessionId

    fun loadSession(sessionId: String) {
        currentSessionId = sessionId
        scope.launch {
            val entities = messageDao.getBySession(sessionId)
            val chatMessages = entities.map { it.toChatMessage() }
            _messages.value = chatMessages
            _isProcessing.value = false
            Log.i(TAG, "Loaded ${chatMessages.size} messages for session $sessionId")
        }
    }

    /**
     * Load messages from bridge REST API history response.
     * Called when switching sessions — the bridge has the authoritative history.
     */
    fun loadFromHistory(sessionId: String, history: List<Pair<String, String>>) {
        if (sessionId != currentSessionId) return // Stale response

        val chatMessages = history.mapNotNull { (role, content) ->
            val chatRole = when (role) {
                "user" -> ChatMessage.Role.USER
                "assistant" -> ChatMessage.Role.ASSISTANT
                else -> return@mapNotNull null
            }
            ChatMessage(role = chatRole, content = content)
        }

        if (chatMessages.isNotEmpty()) {
            _messages.value = chatMessages
            _isProcessing.value = false
            Log.i(TAG, "Loaded ${chatMessages.size} messages from bridge history for session $sessionId")
        }
    }

    suspend fun getSessions(): List<SessionEntity> {
        return sessionDao.getAll()
    }

    suspend fun deleteSession(sessionId: String) {
        messageDao.deleteBySession(sessionId)
        sessionDao.delete(sessionId)
        if (sessionId == currentSessionId) {
            clear()
        }
    }

    private suspend fun loadOrCreateSession() {
        val existing = sessionDao.getById(currentSessionId)
        if (existing != null) {
            val entities = messageDao.getBySession(currentSessionId)
            val chatMessages = entities.map { it.toChatMessage() }
            _messages.value = chatMessages
            Log.i(TAG, "Resumed session $currentSessionId with ${chatMessages.size} messages")
        } else {
            sessionDao.insert(SessionEntity(id = currentSessionId))
            Log.i(TAG, "Created new session $currentSessionId")
        }
    }

    private fun persistMessage(chatMessage: ChatMessage) {
        scope.launch {
            try {
                messageDao.insert(chatMessage.toEntity(currentSessionId))
                updateSessionMetadata()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist message", e)
            }
        }
    }

    private fun persistAllMessages() {
        scope.launch {
            try {
                // Persist all non-streaming, non-system messages
                val messagesToPersist = _messages.value.filter {
                    it.role != ChatMessage.Role.SYSTEM
                }
                messageDao.deleteBySession(currentSessionId)
                messageDao.insertAll(messagesToPersist.map { it.toEntity(currentSessionId) })
                updateSessionMetadata()
                Log.d(TAG, "Persisted ${messagesToPersist.size} messages for session $currentSessionId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist messages", e)
            }
        }
    }

    private suspend fun updateSessionMetadata() {
        val messages = _messages.value
        val preview = messages.lastOrNull { it.role == ChatMessage.Role.USER }?.content?.take(100) ?: ""
        val title = messages.firstOrNull { it.role == ChatMessage.Role.USER }?.content?.take(50) ?: "New Chat"
        val session = SessionEntity(
            id = currentSessionId,
            title = title,
            preview = preview,
            messageCount = messages.size,
            lastActivity = System.currentTimeMillis(),
            createdAt = sessionDao.getById(currentSessionId)?.createdAt ?: System.currentTimeMillis(),
        )
        sessionDao.insert(session) // REPLACE strategy
    }

    private suspend fun pruneOldSessions() {
        try {
            val cutoff = System.currentTimeMillis() - SESSION_TTL_MS
            sessionDao.deleteOlderThan(cutoff)

            val count = sessionDao.count()
            if (count > MAX_SESSIONS) {
                sessionDao.deleteOldest(count - MAX_SESSIONS)
            }
            Log.d(TAG, "Pruned old sessions, $count remaining")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prune sessions", e)
        }
    }
}

private fun ChatMessage.toEntity(sessionId: String) = MessageEntity(
    id = id,
    sessionId = sessionId,
    role = role.name,
    content = content,
    toolName = toolName,
    toolInput = toolInput,
    isStreaming = false, // Never persist streaming state
    viaVoice = viaVoice,
    timestamp = timestamp,
)

private fun MessageEntity.toChatMessage() = ChatMessage(
    id = id,
    role = ChatMessage.Role.valueOf(role),
    content = content,
    toolName = toolName,
    toolInput = toolInput,
    isStreaming = false,
    viaVoice = viaVoice,
    timestamp = timestamp,
)
