package network.arno.android.chat

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.*
import network.arno.android.transport.IncomingMessage

class ChatRepository {

    companion object {
        private const val TAG = "ChatRepository"
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    fun addUserMessage(text: String) {
        _messages.update { it + ChatMessage(role = ChatMessage.Role.USER, content = text) }
        _isProcessing.value = true
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

            // Ignore internal bridge messages that don't need UI display
            "task_created", "task_completed", "task_failed", "tasks_summary",
            "task_queued", "queue_task_started", "queued_task_cancelled",
            "task_progress", "replay", "chat_history", "system_message",
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
    }
}
