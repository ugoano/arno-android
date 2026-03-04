package network.arno.android.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.DownloadListener
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Playground screen displaying the bridge playground directory via WebView.
 *
 * The playground hosts web apps at https://chat.arno.network/playground/play/
 * including Downloads (featured), dashboards, and ad-hoc tools.
 *
 * Downloads are handled via Android DownloadManager with system notifications.
 */
@Composable
fun PlaygroundScreen() {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true

                    // Critical for textarea rendering
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false

                    // Enable mixed content for local development
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                    // Improve text rendering
                    textZoom = 100

                    // Enable all caching for better performance
                    cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

                    // Enable database storage
                    databaseEnabled = true
                }

                // Disable hardware acceleration for better textarea compatibility
                setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)

                // Set download listener to handle file downloads
                setDownloadListener(PlaygroundDownloadListener(context))

                loadUrl("https://chat.arno.network/playground/")
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

/**
 * DownloadListener for Playground WebView downloads.
 *
 * Delegates file downloads to Android's system DownloadManager, which provides:
 * - Progress notifications
 * - Completion notifications
 * - Download failure handling
 * - Files saved to Downloads folder
 */
private class PlaygroundDownloadListener(
    private val context: Context
) : DownloadListener {
    override fun onDownloadStart(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long
    ) {
        if (url == null) return

        try {
            // Extract filename from Content-Disposition or URL
            val filename = FileDownloadUtils.extractFilename(contentDisposition, url)

            // Create download request
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                // Set MIME type if provided
                mimeType?.let { setMimeType(it) }

                // Set destination to Downloads folder
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)

                // Show notification with download progress and completion
                setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )

                // Set notification title
                setTitle("Downloading $filename")

                // Set notification description
                setDescription("From Arno Playground")

                // Require WiFi or mobile data (not just WiFi)
                setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
                )

                // Allow download over metered connections
                setAllowedOverMetered(true)

                // Allow download when roaming
                setAllowedOverRoaming(true)
            }

            // Get DownloadManager and enqueue download
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            // Show toast confirmation
            Toast.makeText(
                context,
                "Downloading $filename",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            // Handle download failure
            Toast.makeText(
                context,
                "Download failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
