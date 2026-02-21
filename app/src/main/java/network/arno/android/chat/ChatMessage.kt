package network.arno.android.chat

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: Role,
    val content: String,
    val isStreaming: Boolean = false,
    val toolName: String? = null,
    val toolInput: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val viaVoice: Boolean = false,
    val imageIds: List<String> = emptyList(),
    val localImageUris: List<String> = emptyList(),
) {
    enum class Role { USER, ASSISTANT, SYSTEM, TOOL }
}
