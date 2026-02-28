package network.arno.android.command

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.Base64

/**
 * Handles transfer_file command — receives base64-encoded file data
 * and saves to device storage via MediaStore API (scoped storage).
 *
 * ## Payload:
 * ```json
 * {
 *   "filename": "ironman_theme.mp3",
 *   "data": "base64encodeddata...",
 *   "save_to": "Downloads"
 * }
 * ```
 *
 * save_to options: Downloads (default), Music, Pictures, Documents
 */
class TransferFileHandler(private val context: Context) {

    companion object {
        private const val TAG = "TransferFileHandler"
    }

    fun handle(payload: JsonObject): HandlerResult {
        val filename = payload["filename"]?.jsonPrimitive?.content ?: ""
        val base64Data = payload["data"]?.jsonPrimitive?.content ?: ""
        val saveToStr = payload["save_to"]?.jsonPrimitive?.content ?: "Downloads"

        // Validate using pure logic
        val validation = TransferFileValidator.validate(filename, base64Data)
        if (validation is TransferValidation.Invalid) {
            return HandlerResult.Error(validation.reason)
        }

        val saveLocation = SaveLocation.fromString(saveToStr)
        val mimeType = MimeTypeResolver.fromFilename(filename)

        return try {
            val decoded = Base64.getDecoder().decode(base64Data)
            val savedPath = saveToMediaStore(filename, decoded, saveLocation, mimeType)

            Log.i(TAG, "Saved $filename (${decoded.size} bytes) to $saveLocation")

            val data = buildJsonObject {
                put("status", "saved")
                put("path", savedPath)
                put("size_bytes", decoded.size)
                put("filename", filename)
                put("mime_type", mimeType)
            }
            HandlerResult.Success(data)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save file: $filename", e)
            HandlerResult.Error("Failed to save file: ${e.message}")
        }
    }

    private fun saveToMediaStore(
        filename: String,
        data: ByteArray,
        location: SaveLocation,
        mimeType: String,
    ): String {
        val (contentUri, relativePath) = when (location) {
            SaveLocation.DOWNLOADS -> Pair(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                Environment.DIRECTORY_DOWNLOADS
            )
            SaveLocation.MUSIC -> Pair(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                Environment.DIRECTORY_MUSIC
            )
            SaveLocation.PICTURES -> Pair(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                Environment.DIRECTORY_PICTURES
            )
            SaveLocation.DOCUMENTS -> Pair(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                Environment.DIRECTORY_DOCUMENTS
            )
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(contentUri, values)
            ?: throw IllegalStateException("MediaStore insert returned null for $filename")

        resolver.openOutputStream(uri)?.use { output ->
            output.write(data)
        } ?: throw IllegalStateException("Cannot open output stream for $uri")

        return "$relativePath/$filename"
    }
}
