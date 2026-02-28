package network.arno.android.command

import java.util.Base64

/**
 * Pure validation logic for file transfers.
 * Validates filename, data, size limits, and base64 integrity.
 * Uses java.util.Base64 (not android.util.Base64) for JVM test compatibility.
 */
sealed class TransferValidation {
    data class Valid(val filename: String, val decodedSize: Int) : TransferValidation()
    data class Invalid(val reason: String) : TransferValidation()
}

object TransferFileValidator {
    private const val MAX_SIZE_BYTES = 10_485_760 // 10MB

    fun validate(filename: String, base64Data: String): TransferValidation {
        if (filename.isBlank()) {
            return TransferValidation.Invalid("Missing filename")
        }
        if (base64Data.isBlank()) {
            return TransferValidation.Invalid("Missing file data")
        }

        // Estimate decoded size: base64 produces ~3 bytes per 4 chars
        val estimatedSize = (base64Data.length * 3) / 4
        if (estimatedSize > MAX_SIZE_BYTES) {
            return TransferValidation.Invalid(
                "File size (~${estimatedSize / 1_048_576}MB) exceeds 10MB limit"
            )
        }

        // Validate base64 by attempting decode
        val decoded = try {
            Base64.getDecoder().decode(base64Data)
        } catch (e: IllegalArgumentException) {
            return TransferValidation.Invalid("Invalid base64 data: ${e.message}")
        }

        return TransferValidation.Valid(filename, decoded.size)
    }
}
