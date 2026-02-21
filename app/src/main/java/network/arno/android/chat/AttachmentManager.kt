package network.arno.android.chat

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class Attachment(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val uploadedId: String? = null,
    val isUploading: Boolean = false,
    val error: String? = null,
)

class AttachmentManager(private val context: Context) {

    companion object {
        private const val TAG = "AttachmentManager"
        private const val MAX_FILE_SIZE = 20L * 1024 * 1024 // 20MB
    }

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private val _attachments = MutableStateFlow<List<Attachment>>(emptyList())
    val attachments: StateFlow<List<Attachment>> = _attachments

    fun addAttachment(uri: Uri) {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"

        var name = "file"
        var size = 0L
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: "file"
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }

        if (size > MAX_FILE_SIZE) {
            Log.w(TAG, "File too large: $name ($size bytes)")
            _attachments.update {
                it + Attachment(uri, name, mimeType, size, error = "File exceeds 20MB limit")
            }
            return
        }

        _attachments.update {
            it + Attachment(uri, name, mimeType, size)
        }
    }

    fun removeAttachment(uri: Uri) {
        _attachments.update { list -> list.filter { it.uri != uri } }
    }

    fun clearAttachments() {
        _attachments.value = emptyList()
    }

    fun hasAttachments(): Boolean = _attachments.value.isNotEmpty()

    fun getPendingAttachments(): List<Attachment> =
        _attachments.value.filter { it.uploadedId == null && it.error == null }

    fun getUploadedIds(): List<String> =
        _attachments.value.mapNotNull { it.uploadedId }

    suspend fun uploadAll(serverUrl: String, sessionId: String): Boolean {
        val pending = getPendingAttachments()
        if (pending.isEmpty()) return true

        var allSuccess = true
        for (attachment in pending) {
            val success = uploadOne(attachment, serverUrl, sessionId)
            if (!success) allSuccess = false
        }
        return allSuccess
    }

    private suspend fun uploadOne(
        attachment: Attachment,
        serverUrl: String,
        sessionId: String,
    ): Boolean = withContext(Dispatchers.IO) {
        // Mark as uploading
        _attachments.update { list ->
            list.map { if (it.uri == attachment.uri) it.copy(isUploading = true) else it }
        }

        try {
            val resolver = context.contentResolver
            val bytes = resolver.openInputStream(attachment.uri)?.readBytes()
                ?: throw IllegalStateException("Cannot read file: ${attachment.name}")

            val mediaType = attachment.mimeType.toMediaType()
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", attachment.name, bytes.toRequestBody(mediaType))
                .build()

            val uploadUrl = "${serverUrl.trimEnd('/')}/api/sessions/$sessionId/upload"
            val request = Request.Builder()
                .url(uploadUrl)
                .post(body)
                .build()

            Log.d(TAG, "Uploading ${attachment.name} to $uploadUrl")

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("Upload failed: ${response.code} ${response.message}")
            }

            val responseBody = response.body?.string() ?: "{}"
            val jsonResponse = json.parseToJsonElement(responseBody).jsonObject
            // Bridge returns {"image_id": "uuid", "filename": "...", "url": "..."}
            val uploadedId = jsonResponse["image_id"]?.jsonPrimitive?.content
                ?: throw IllegalStateException("No image_id returned from upload")

            Log.i(TAG, "Uploaded ${attachment.name} -> $uploadedId")

            _attachments.update { list ->
                list.map {
                    if (it.uri == attachment.uri) it.copy(
                        uploadedId = uploadedId,
                        isUploading = false,
                    ) else it
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed for ${attachment.name}: ${e.message}")
            _attachments.update { list ->
                list.map {
                    if (it.uri == attachment.uri) it.copy(
                        isUploading = false,
                        error = e.message ?: "Upload failed",
                    ) else it
                }
            }
            false
        }
    }
}
