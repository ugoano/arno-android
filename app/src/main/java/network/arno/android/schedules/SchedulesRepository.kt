package network.arno.android.schedules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

open class SchedulesRepository(
    private val serverUrl: String,
) {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val baseUrl = serverUrl.trimEnd('/')

    @Serializable
    private data class SchedulesResponse(
        val schedules: List<Schedule> = emptyList(),
    )

    @Serializable
    private data class ToggleBody(
        val enabled: Boolean,
    )

    open suspend fun fetchSchedules(): List<Schedule> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/schedules")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw Exception("Empty response")
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: $body")
            }
            parseSchedulesResponse(body)
        }
    }

    open suspend fun toggleSchedule(id: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        val requestBody = json.encodeToString(ToggleBody(enabled))
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/api/schedules/$id/toggle")
            .patch(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: $body")
            }
        }
    }

    internal fun parseSchedulesResponse(body: String): List<Schedule> {
        return json.decodeFromString<SchedulesResponse>(body).schedules
    }
}
