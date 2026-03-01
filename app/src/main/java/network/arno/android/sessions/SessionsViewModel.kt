package network.arno.android.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
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

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

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

    fun pullToRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                sessionsRepository.fetchSessions()
                _activeSessionId.value = webSocket.currentSessionId
            } finally {
                _isRefreshing.value = false
            }
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

        // Local-first: show Room DB messages immediately
        chatRepository.loadSession(sessionId)

        // Navigate to chat tab straight away — user sees cached data
        onNavigateToChat()

        // Background: fetch REST history and reconnect WebSocket concurrently
        viewModelScope.launch {
            // Launch WebSocket reconnect in parallel — independent of REST fetch
            launch {
                webSocket.disconnect()
                webSocket.connectToSession(sessionId)
            }

            // Fetch bridge history concurrently — only updates UI if different
            val historyDeferred = async {
                sessionsRepository.fetchSessionHistory(sessionId)
            }
            val history = historyDeferred.await()
            if (history.isNotEmpty()) {
                chatRepository.loadFromHistory(sessionId, history)
            }
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
