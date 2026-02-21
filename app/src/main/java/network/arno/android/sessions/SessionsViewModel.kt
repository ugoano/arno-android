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

        // Clear chat immediately and show empty state while loading
        chatRepository.loadSession(sessionId)

        // Navigate to chat tab straight away
        onNavigateToChat()

        // Fetch history from bridge REST API (authoritative source)
        // then reconnect WebSocket for the new session
        viewModelScope.launch {
            val history = sessionsRepository.fetchSessionHistory(sessionId)
            if (history.isNotEmpty()) {
                chatRepository.loadFromHistory(sessionId, history)
            }

            // Reconnect WebSocket targeting the new session
            webSocket.disconnect()
            webSocket.connectToSession(sessionId)
        }
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
