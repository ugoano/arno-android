package network.arno.android.command

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Handles open_file command — opens a file with the device's default app
 * via Intent.ACTION_VIEW.
 *
 * ## Payload modes:
 *
 * **By relative path** (queries MediaStore for content URI):
 * ```json
 * { "path": "Music/Iron_Man_Suit_Up.mp3" }
 * ```
 *
 * **By content URI** (uses URI directly):
 * ```json
 * { "uri": "content://media/external/audio/123" }
 * ```
 *
 * Uses [FilePathResolver] for path parsing and [MimeTypeResolver] for
 * MIME type resolution (both pure logic, unit tested).
 */
class OpenFileHandler(private val context: Context) {

    companion object {
        private const val TAG = "OpenFileHandler"
    }

    fun handle(payload: JsonObject): HandlerResult {
        val path = payload["path"]?.jsonPrimitive?.content
        val uri = payload["uri"]?.jsonPrimitive?.content

        return when (val validation = OpenFilePayloadValidator.validate(path, uri)) {
            is OpenFileValidation.ValidPath -> openByPath(validation, path!!)
            is OpenFileValidation.ValidUri -> openByUri(validation.uri)
            is OpenFileValidation.Invalid -> HandlerResult.Error(validation.reason)
        }
    }

    private fun openByPath(validation: OpenFileValidation.ValidPath, originalPath: String): HandlerResult {
        val contentUri = resolveContentUri(validation.directory, validation.filename)
            ?: return HandlerResult.Error("File not found: $originalPath")

        return launchIntent(contentUri, validation.mimeType, originalPath)
    }

    private fun openByUri(uriString: String): HandlerResult {
        val contentUri = try {
            Uri.parse(uriString)
        } catch (e: Exception) {
            return HandlerResult.Error("Invalid URI: $uriString")
        }

        // Try to infer MIME type from the URI's last path segment
        val lastSegment = contentUri.lastPathSegment ?: ""
        val mimeType = if (lastSegment.contains('.')) {
            MimeTypeResolver.fromFilename(lastSegment)
        } else {
            context.contentResolver.getType(contentUri) ?: "application/octet-stream"
        }

        return launchIntent(contentUri, mimeType, uriString)
    }

    private fun resolveContentUri(directory: String, filename: String): Uri? {
        val resolver = context.contentResolver

        // Query MediaStore for the file by RELATIVE_PATH and DISPLAY_NAME
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?" +
            if (directory.isNotEmpty()) {
                " AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            } else ""

        val selectionArgs = if (directory.isNotEmpty()) {
            arrayOf(filename, "%$directory%")
        } else {
            arrayOf(filename)
        }

        // Search across all media collections
        val collections = listOf(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        )

        for (collection in collections) {
            resolver.query(
                collection,
                arrayOf(MediaStore.MediaColumns._ID),
                selection,
                selectionArgs,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(0)
                    val uri = Uri.withAppendedPath(collection, id.toString())
                    Log.d(TAG, "Resolved $directory/$filename -> $uri")
                    return uri
                }
            }
        }

        Log.w(TAG, "File not found in MediaStore: $directory/$filename")
        return null
    }

    private fun launchIntent(contentUri: Uri, mimeType: String, originalRef: String): HandlerResult {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, mimeType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val resolvedActivity = intent.resolveActivity(context.packageManager)

        return try {
            context.startActivity(intent)
            val activityName = resolvedActivity?.flattenToShortString() ?: "unknown"
            Log.i(TAG, "Opened $originalRef with $activityName (MIME: $mimeType)")

            val data = buildJsonObject {
                put("status", "opened")
                put("path", originalRef)
                put("mime_type", mimeType)
                put("activity", activityName)
            }
            HandlerResult.Success(data)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No app available for MIME type: $mimeType", e)
            HandlerResult.Error("No app available to open file type: $mimeType")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open file: $originalRef", e)
            HandlerResult.Error("Failed to open file: ${e.message}")
        }
    }
}
