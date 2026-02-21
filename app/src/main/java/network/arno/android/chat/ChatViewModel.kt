package network.arno.android.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.arno.android.settings.SettingsRepository
import network.arno.android.transport.ArnoWebSocket
import network.arno.android.transport.ConnectionState

class ChatViewModel(
    private val webSocket: ArnoWebSocket,
    private val chatRepository: ChatRepository,
    val attachmentManager: AttachmentManager,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val messages: StateFlow<List<ChatMessage>> = chatRepository.messages
    val isProcessing: StateFlow<Boolean> = chatRepository.isProcessing
    val connectionState: StateFlow<ConnectionState> = webSocket.connectionState
    val attachments: StateFlow<List<Attachment>> = attachmentManager.attachments

    init {
        viewModelScope.launch {
            webSocket.incomingMessages.collect { msg ->
                chatRepository.handleIncoming(msg)
            }
        }
    }

    fun sendMessage(text: String, viaVoice: Boolean = false) {
        if (text.isBlank() && !attachmentManager.hasAttachments()) return

        val localUris = attachmentManager.attachments.value
            .filter { it.error == null }
            .map { it.uri.toString() }

        if (attachmentManager.hasAttachments()) {
            val sessionId = webSocket.currentSessionId
            if (sessionId == null) {
                // No session yet — send without attachments
                chatRepository.addUserMessage(text, viaVoice)
                webSocket.sendMessage(text, viaVoice)
                attachmentManager.clearAttachments()
                return
            }

            viewModelScope.launch {
                val serverUrl = settingsRepository.serverUrl
                val uploaded = attachmentManager.uploadAll(serverUrl, sessionId)
                val imageIds = attachmentManager.getUploadedIds()

                chatRepository.addUserMessage(text, viaVoice, imageIds, localUris)
                webSocket.sendMessage(text, viaVoice, imageIds)
                attachmentManager.clearAttachments()
            }
        } else {
            chatRepository.addUserMessage(text, viaVoice)
            webSocket.sendMessage(text, viaVoice)
        }
    }

    fun addAttachment(uri: Uri) {
        attachmentManager.addAttachment(uri)
    }

    fun removeAttachment(uri: Uri) {
        attachmentManager.removeAttachment(uri)
    }

    fun cancelTask() {
        webSocket.sendCancel()
    }

    fun connect() {
        webSocket.connect()
    }

    fun disconnect() {
        webSocket.disconnect()
    }

    class Factory(
        private val webSocket: ArnoWebSocket,
        private val chatRepository: ChatRepository,
        private val attachmentManager: AttachmentManager,
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(webSocket, chatRepository, attachmentManager, settingsRepository) as T
        }
    }
}
