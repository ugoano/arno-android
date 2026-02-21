package network.arno.android.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import network.arno.android.transport.ArnoWebSocket
import network.arno.android.transport.ConnectionState

class ChatViewModel(
    private val webSocket: ArnoWebSocket,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    val messages: StateFlow<List<ChatMessage>> = chatRepository.messages
    val isProcessing: StateFlow<Boolean> = chatRepository.isProcessing
    val connectionState: StateFlow<ConnectionState> = webSocket.connectionState

    init {
        viewModelScope.launch {
            webSocket.incomingMessages.collect { msg ->
                chatRepository.handleIncoming(msg)
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        chatRepository.addUserMessage(text)
        webSocket.sendMessage(text)
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
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(webSocket, chatRepository) as T
        }
    }
}
