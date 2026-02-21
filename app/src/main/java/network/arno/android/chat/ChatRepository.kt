package network.arno.android.chat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import network.arno.android.transport.IncomingMessage

class ChatRepository {

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
            "content_delta" -> {
                val delta = msg.content ?: return
                _messages.update { messages ->
                    val last = messages.lastOrNull()
                    if (last != null && last.role == ChatMessage.Role.ASSISTANT && last.isStreaming) {
                        messages.dropLast(1) + last.copy(content = last.content + delta)
                    } else {
                        messages + ChatMessage(
                            role = ChatMessage.Role.ASSISTANT,
                            content = delta,
                            isStreaming = true,
                        )
                    }
                }
            }
            "tool_use_start" -> {
                _messages.update { messages ->
                    // Finalise any streaming assistant message
                    val finalised = messages.map {
                        if (it.isStreaming) it.copy(isStreaming = false) else it
                    }
                    finalised + ChatMessage(
                        role = ChatMessage.Role.TOOL,
                        content = "",
                        toolName = msg.tool,
                        toolInput = msg.input,
                    )
                }
            }
            "tool_result" -> {
                // Tool results are informational; we could update the last tool message
                // but for v1 we just note it arrived
            }
            "message_end", "result" -> {
                _messages.update { messages ->
                    messages.map {
                        if (it.isStreaming) it.copy(isStreaming = false) else it
                    }
                }
                _isProcessing.value = false
            }
            "system" -> {
                _messages.update { it + ChatMessage(
                    role = ChatMessage.Role.SYSTEM,
                    content = msg.content ?: "Connected",
                )}
            }
            "error", "stderr" -> {
                _messages.update { it + ChatMessage(
                    role = ChatMessage.Role.SYSTEM,
                    content = "Error: ${msg.content ?: msg.error ?: "Unknown error"}",
                )}
                _isProcessing.value = false
            }
        }
    }

    fun clear() {
        _messages.value = emptyList()
        _isProcessing.value = false
    }
}
