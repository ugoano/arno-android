package network.arno.android.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.arno.android.chat.ChatRepository
import network.arno.android.transport.ArnoWebSocket

class SessionsViewModel(
    private val sessionsRepository: SessionsRepository,
    private val webSocket: ArnoWebSocket,
    private val chatRepository: ChatRepository,
    private val onNavigateToChat: () -> Unit,
) : ViewModel() {

    val sessions: StateFlow<List<Session>> = sessionsRepository.sessions
    val isLoading: StateFlow<Boolean> = sessionsRepository.isLoading
    val error: StateFlow<String?> = sessionsRepository.error

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId

    init {
        _activeSessionId.value = webSocket.currentSessionId
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            sessionsRepository.fetchSessions()
            _activeSessionId.value = webSocket.currentSessionId
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            sessionsRepository.deleteSession(sessionId)
        }
    }

    fun updateTitle(sessionId: String, title: String) {
        viewModelScope.launch {
            sessionsRepository.updateTitle(sessionId, title)
        }
    }

    fun switchSession(sessionId: String) {
        if (sessionId == _activeSessionId.value) return

        _activeSessionId.value = sessionId

        // Load local Room history immediately (may be empty for remote sessions)
        chatRepository.loadSession(sessionId)

        // Reconnect WebSocket targeting the new session.
        // This sends a reconnect message to the bridge which restores session
        // context and replays chat history (overwriting empty local state).
        webSocket.disconnect()
        webSocket.connectToSession(sessionId)

        // Navigate back to chat tab
        onNavigateToChat()
    }

    fun clearError() {
        sessionsRepository.clearError()
    }

    class Factory(
        private val sessionsRepository: SessionsRepository,
        private val webSocket: ArnoWebSocket,
        private val chatRepository: ChatRepository,
        private val onNavigateToChat: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SessionsViewModel(sessionsRepository, webSocket, chatRepository, onNavigateToChat) as T
        }
    }
}
