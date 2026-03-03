package network.arno.android.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Playground screen displaying the bridge playground directory via WebView.
 *
 * The playground hosts web apps at https://chat.arno.network/playground/play/
 * including Downloads (featured), dashboards, and ad-hoc tools.
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
                }
                loadUrl("https://chat.arno.network/playground/")
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
