package network.arno.android.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for FileDownloadUtils - filename extraction logic.
 * Tests written FIRST following TDD methodology.
 */
class FileDownloadUtilsTest {

    @Test
    fun `extractFilename parses Content-Disposition with quoted filename`() {
        val contentDisposition = "attachment; filename=\"arno-v1.10.0.apk\""
        val url = "https://chat.arno.network/static/downloads/arno-v1.10.0.apk"

        val result = FileDownloadUtils.extractFilename(contentDisposition, url)

        assertEquals("arno-v1.10.0.apk", result)
    }

    @Test
    fun `extractFilename parses Content-Disposition without quotes`() {
        val contentDisposition = "attachment; filename=boarding-pass.pdf"
        val url = "https://chat.arno.network/static/downloads/boarding-pass.pdf"

        val result = FileDownloadUtils.extractFilename(contentDisposition, url)

        assertEquals("boarding-pass.pdf", result)
    }

    @Test
    fun `extractFilename handles Content-Disposition with extra parameters`() {
        val contentDisposition = "attachment; filename=\"comic_social_logo.png\"; size=162356"
        val url = "https://chat.arno.network/static/downloads/comic_social_logo.png"

        val result = FileDownloadUtils.extractFilename(contentDisposition, url)

        assertEquals("comic_social_logo.png", result)
    }

    @Test
    fun `extractFilename falls back to URL when Content-Disposition is null`() {
        val contentDisposition: String? = null
        val url = "https://chat.arno.network/static/downloads/mighty_morphin_power_rangers.png"

        val result = FileDownloadUtils.extractFilename(contentDisposition, url)

        assertEquals("mighty_morphin_power_rangers.png", result)
    }

    @Test
    fun `extractFilename falls back to URL when Content-Disposition is empty`() {
        val contentDisposition = ""
        val url = "https://chat.arno.network/static/downloads/test-file.apk"

        val result = FileDownloadUtils.extractFilename(contentDisposition, url)

        assertEquals("test-file.apk", result)
    }

    @Test
    fun `extractFilename falls back to URL when Content-Disposition has no filename`() {
        val contentDisposition = "attachment; size=1234"
        val url = "https://chat.arno.network/playground/play/downloads/index.html"

        val result = FileDownloadUtils.extractFilename(contentDisposition, url)

        assertEquals("index.html", result)
    }

    @Test
    fun `extractFilename handles URL with query parameters`() {
        val contentDisposition: String? = null
        val url = "https://chat.arno.network/static/downloads/file.pdf?v=123&cache=bust"

        val result = FileDownloadUtils.extractFilename(contentDisposition, url)

        assertEquals("file.pdf", result)
    }

    @Test
    fun `extractFilename handles URL with fragment`() {
        val contentDisposition: String? = null
        val url = "https://chat.arno.network/static/downloads/image.png#section"

        val result = FileDownloadUtils.extractFilename(contentDisposition, url)

        assertEquals("image.png", result)
    }

    @Test
    fun `extractFilename handles Content-Disposition with filename*`() {
        // RFC 6266 extended filename format
        val contentDisposition = "attachment; filename*=UTF-8''test%20file.apk"
        val url = "https://chat.arno.network/static/downloads/test%20file.apk"

        val result = FileDownloadUtils.extractFilename(contentDisposition, url)

        // Should extract from filename* or fallback to URL-decoded filename
        assertEquals("test file.apk", result)
    }

    @Test
    fun `extractFilename handles malformed Content-Disposition gracefully`() {
        val contentDisposition = "totally-invalid-format"
        val url = "https://chat.arno.network/static/downloads/fallback.apk"

        val result = FileDownloadUtils.extractFilename(contentDisposition, url)

        assertEquals("fallback.apk", result)
    }
}
