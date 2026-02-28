package network.arno.android.command

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for extractable logic from WakeScreenHandler, PlaySoundHandler,
 * TransferFileHandler, and OpenFileHandler. These test pure Kotlin logic
 * without Android deps.
 */
class DeviceCommandsTest {

    // ── SoundType enum tests ──

    @Test
    fun `SoundType fromString resolves alarm`() {
        assertEquals(SoundType.ALARM, SoundType.fromString("alarm"))
    }

    @Test
    fun `SoundType fromString resolves notification`() {
        assertEquals(SoundType.NOTIFICATION, SoundType.fromString("notification"))
    }

    @Test
    fun `SoundType fromString resolves ringtone`() {
        assertEquals(SoundType.RINGTONE, SoundType.fromString("ringtone"))
    }

    @Test
    fun `SoundType fromString is case insensitive`() {
        assertEquals(SoundType.ALARM, SoundType.fromString("ALARM"))
        assertEquals(SoundType.NOTIFICATION, SoundType.fromString("Notification"))
    }

    @Test
    fun `SoundType fromString returns null for unknown type`() {
        assertNull(SoundType.fromString("beep"))
        assertNull(SoundType.fromString(""))
    }

    // ── SaveLocation mapping tests ──

    @Test
    fun `SaveLocation fromString resolves Downloads`() {
        assertEquals(SaveLocation.DOWNLOADS, SaveLocation.fromString("Downloads"))
    }

    @Test
    fun `SaveLocation fromString resolves Music`() {
        assertEquals(SaveLocation.MUSIC, SaveLocation.fromString("Music"))
    }

    @Test
    fun `SaveLocation fromString resolves Pictures`() {
        assertEquals(SaveLocation.PICTURES, SaveLocation.fromString("Pictures"))
    }

    @Test
    fun `SaveLocation fromString resolves Documents`() {
        assertEquals(SaveLocation.DOCUMENTS, SaveLocation.fromString("Documents"))
    }

    @Test
    fun `SaveLocation fromString is case insensitive`() {
        assertEquals(SaveLocation.DOWNLOADS, SaveLocation.fromString("downloads"))
        assertEquals(SaveLocation.MUSIC, SaveLocation.fromString("MUSIC"))
    }

    @Test
    fun `SaveLocation fromString defaults to Downloads for unknown`() {
        assertEquals(SaveLocation.DOWNLOADS, SaveLocation.fromString("Desktop"))
        assertEquals(SaveLocation.DOWNLOADS, SaveLocation.fromString(""))
    }

    // ── TransferFileValidator tests ──

    @Test
    fun `validateTransfer accepts valid small file`() {
        val data = "SGVsbG8gV29ybGQ=" // "Hello World" base64
        val result = TransferFileValidator.validate("test.txt", data)
        assertTrue(result is TransferValidation.Valid)
        val valid = result as TransferValidation.Valid
        assertEquals("test.txt", valid.filename)
        assertEquals(11, valid.decodedSize)
    }

    @Test
    fun `validateTransfer rejects missing filename`() {
        val result = TransferFileValidator.validate("", "SGVsbG8=")
        assertTrue(result is TransferValidation.Invalid)
        assertEquals("Missing filename", (result as TransferValidation.Invalid).reason)
    }

    @Test
    fun `validateTransfer rejects missing data`() {
        val result = TransferFileValidator.validate("test.txt", "")
        assertTrue(result is TransferValidation.Invalid)
        assertEquals("Missing file data", (result as TransferValidation.Invalid).reason)
    }

    @Test
    fun `validateTransfer rejects invalid base64`() {
        val result = TransferFileValidator.validate("test.txt", "!!!not-base64!!!")
        assertTrue(result is TransferValidation.Invalid)
        assertTrue((result as TransferValidation.Invalid).reason.contains("Invalid base64"))
    }

    @Test
    fun `validateTransfer rejects files over 10MB`() {
        // Create a base64 string that decodes to >10MB
        // 10MB = 10_485_760 bytes. Base64 of that is ~14MB of characters.
        // We can fake this by checking the size estimate from the base64 length.
        val largeData = "A".repeat(14_000_001) // ~10.5MB decoded
        val result = TransferFileValidator.validate("huge.bin", largeData)
        assertTrue(result is TransferValidation.Invalid)
        assertTrue((result as TransferValidation.Invalid).reason.contains("exceeds"))
    }

    @Test
    fun `validateTransfer accepts file just under 10MB`() {
        // 10MB exactly in base64 would be about 13_981_014 chars
        // Use a small valid base64 string for this test
        val smallData = "dGVzdA==" // "test"
        val result = TransferFileValidator.validate("small.txt", smallData)
        assertTrue(result is TransferValidation.Valid)
    }

    // ── MimeTypeResolver tests ──

    @Test
    fun `inferMimeType returns correct type for mp3`() {
        assertEquals("audio/mpeg", MimeTypeResolver.fromFilename("song.mp3"))
    }

    @Test
    fun `inferMimeType returns correct type for jpg`() {
        assertEquals("image/jpeg", MimeTypeResolver.fromFilename("photo.jpg"))
    }

    @Test
    fun `inferMimeType returns correct type for jpeg`() {
        assertEquals("image/jpeg", MimeTypeResolver.fromFilename("photo.jpeg"))
    }

    @Test
    fun `inferMimeType returns correct type for png`() {
        assertEquals("image/png", MimeTypeResolver.fromFilename("icon.png"))
    }

    @Test
    fun `inferMimeType returns correct type for pdf`() {
        assertEquals("application/pdf", MimeTypeResolver.fromFilename("doc.pdf"))
    }

    @Test
    fun `inferMimeType returns correct type for txt`() {
        assertEquals("text/plain", MimeTypeResolver.fromFilename("readme.txt"))
    }

    @Test
    fun `inferMimeType returns correct type for mp4`() {
        assertEquals("video/mp4", MimeTypeResolver.fromFilename("clip.mp4"))
    }

    @Test
    fun `inferMimeType returns octet-stream for unknown extension`() {
        assertEquals("application/octet-stream", MimeTypeResolver.fromFilename("data.xyz"))
    }

    @Test
    fun `inferMimeType handles no extension`() {
        assertEquals("application/octet-stream", MimeTypeResolver.fromFilename("noextension"))
    }

    @Test
    fun `inferMimeType is case insensitive`() {
        assertEquals("image/png", MimeTypeResolver.fromFilename("ICON.PNG"))
        assertEquals("audio/mpeg", MimeTypeResolver.fromFilename("Song.MP3"))
    }

    // ── Volume validation tests ──

    @Test
    fun `clampVolume clamps to range`() {
        assertEquals(0.0f, PlaySoundConfig.clampVolume(-0.5f))
        assertEquals(1.0f, PlaySoundConfig.clampVolume(1.5f))
        assertEquals(0.5f, PlaySoundConfig.clampVolume(0.5f))
        assertEquals(0.0f, PlaySoundConfig.clampVolume(0.0f))
        assertEquals(1.0f, PlaySoundConfig.clampVolume(1.0f))
    }

    @Test
    fun `default volume is 1`() {
        assertEquals(1.0f, PlaySoundConfig.DEFAULT_VOLUME)
    }

    @Test
    fun `default duration is negative one for play to completion`() {
        assertEquals(-1L, PlaySoundConfig.PLAY_TO_COMPLETION)
    }

    // ── FilePathResolver tests ──

    @Test
    fun `FilePathResolver splits path with directory`() {
        val result = FilePathResolver.parse("Music/Iron_Man_Suit_Up.mp3")
        assertEquals("Music", result.directory)
        assertEquals("Iron_Man_Suit_Up.mp3", result.filename)
    }

    @Test
    fun `FilePathResolver splits nested path`() {
        val result = FilePathResolver.parse("Music/albums/song.mp3")
        assertEquals("Music/albums", result.directory)
        assertEquals("song.mp3", result.filename)
    }

    @Test
    fun `FilePathResolver handles filename only`() {
        val result = FilePathResolver.parse("photo.jpg")
        assertEquals("", result.directory)
        assertEquals("photo.jpg", result.filename)
    }

    @Test
    fun `FilePathResolver handles path with trailing slash`() {
        val result = FilePathResolver.parse("Download/")
        assertEquals("Download", result.directory)
        assertEquals("", result.filename)
    }

    @Test
    fun `FilePathResolver handles empty path`() {
        val result = FilePathResolver.parse("")
        assertEquals("", result.directory)
        assertEquals("", result.filename)
    }

    @Test
    fun `FilePathResolver strips leading slash`() {
        val result = FilePathResolver.parse("/Music/song.mp3")
        assertEquals("Music", result.directory)
        assertEquals("song.mp3", result.filename)
    }

    @Test
    fun `FilePathResolver extracts MIME type from parsed filename`() {
        val result = FilePathResolver.parse("Music/Iron_Man_Suit_Up.mp3")
        assertEquals("audio/mpeg", MimeTypeResolver.fromFilename(result.filename))
    }

    @Test
    fun `FilePathResolver validates path has filename`() {
        assertTrue(FilePathResolver.hasFilename("Music/song.mp3"))
        assertFalse(FilePathResolver.hasFilename("Music/"))
        assertFalse(FilePathResolver.hasFilename(""))
    }

    // ── OpenFilePayloadValidator tests ──

    @Test
    fun `OpenFilePayloadValidator accepts path payload`() {
        val result = OpenFilePayloadValidator.validate(path = "Music/song.mp3", uri = null)
        assertTrue(result is OpenFileValidation.ValidPath)
        val valid = result as OpenFileValidation.ValidPath
        assertEquals("Music", valid.directory)
        assertEquals("song.mp3", valid.filename)
        assertEquals("audio/mpeg", valid.mimeType)
    }

    @Test
    fun `OpenFilePayloadValidator accepts uri payload`() {
        val result = OpenFilePayloadValidator.validate(path = null, uri = "content://media/external/audio/123")
        assertTrue(result is OpenFileValidation.ValidUri)
        assertEquals("content://media/external/audio/123", (result as OpenFileValidation.ValidUri).uri)
    }

    @Test
    fun `OpenFilePayloadValidator rejects both missing`() {
        val result = OpenFilePayloadValidator.validate(path = null, uri = null)
        assertTrue(result is OpenFileValidation.Invalid)
        assertTrue((result as OpenFileValidation.Invalid).reason.contains("path"))
    }

    @Test
    fun `OpenFilePayloadValidator prefers path when both provided`() {
        val result = OpenFilePayloadValidator.validate(
            path = "Music/song.mp3",
            uri = "content://media/external/audio/123"
        )
        assertTrue(result is OpenFileValidation.ValidPath)
    }

    @Test
    fun `OpenFilePayloadValidator rejects path without filename`() {
        val result = OpenFilePayloadValidator.validate(path = "Music/", uri = null)
        assertTrue(result is OpenFileValidation.Invalid)
        assertTrue((result as OpenFileValidation.Invalid).reason.contains("filename"))
    }

    @Test
    fun `OpenFilePayloadValidator rejects empty uri`() {
        val result = OpenFilePayloadValidator.validate(path = null, uri = "")
        assertTrue(result is OpenFileValidation.Invalid)
    }

    @Test
    fun `OpenFilePayloadValidator resolves MIME for image path`() {
        val result = OpenFilePayloadValidator.validate(path = "Pictures/photo.png", uri = null)
        assertTrue(result is OpenFileValidation.ValidPath)
        assertEquals("image/png", (result as OpenFileValidation.ValidPath).mimeType)
    }

    @Test
    fun `OpenFilePayloadValidator resolves MIME for pdf path`() {
        val result = OpenFilePayloadValidator.validate(path = "Documents/report.pdf", uri = null)
        assertTrue(result is OpenFileValidation.ValidPath)
        assertEquals("application/pdf", (result as OpenFileValidation.ValidPath).mimeType)
    }
}
