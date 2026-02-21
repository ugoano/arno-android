package network.arno.android.sessions

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import network.arno.android.settings.SettingsRepository
import java.util.concurrent.TimeUnit

class SessionsRepository(
    private val settingsRepository: SettingsRepository,
) {
    companion object {
        private const val TAG = "SessionsRepository"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val baseUrl: String
        get() = settingsRepository.serverUrl.trimEnd('/')

    suspend fun fetchSessions() {
        _isLoading.value = true
        _error.value = null
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/sessions")
                .get()
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            val body = response.body?.string() ?: throw Exception("Empty response")
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: $body")
            }

            val sessionsResponse = json.decodeFromString<SessionsResponse>(body)
            _sessions.value = sessionsResponse.sessions
            Log.i(TAG, "Fetched ${sessionsResponse.sessions.size} sessions")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch sessions: ${e.message}")
            _error.value = e.message
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun deleteSession(sessionId: String): Boolean {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/sessions/$sessionId")
                .delete()
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val errorResponse = json.decodeFromString<DeleteResponse>(body)
                _error.value = errorResponse.error ?: "Delete failed"
                return false
            }

            // Remove from local list
            _sessions.value = _sessions.value.filter { it.sessionId != sessionId }
            Log.i(TAG, "Deleted session: $sessionId")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete session: ${e.message}")
            _error.value = e.message
            return false
        }
    }

    suspend fun updateTitle(sessionId: String, title: String): Boolean {
        try {
            val jsonBody = buildJsonObject { put("title", title) }.toString()
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/api/sessions/$sessionId")
                .patch(requestBody)
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val errorResponse = json.decodeFromString<UpdateTitleResponse>(body)
                _error.value = errorResponse.error ?: "Update failed"
                return false
            }

            // Update local list
            _sessions.value = _sessions.value.map {
                if (it.sessionId == sessionId) it.copy(title = title) else it
            }
            Log.i(TAG, "Updated title for session $sessionId: $title")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update title: ${e.message}")
            _error.value = e.message
            return false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
