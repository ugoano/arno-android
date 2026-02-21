package network.arno.android.ui.components

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import io.noties.markwon.Markwon
import network.arno.android.chat.ChatMessage
import network.arno.android.ui.theme.*

@Composable
fun MessageBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val isUser = message.role == ChatMessage.Role.USER
    val isTool = message.role == ChatMessage.Role.TOOL
    val isSystem = message.role == ChatMessage.Role.SYSTEM

    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = when {
        isUser -> ArnoUserBubble
        isTool -> ArnoSurfaceVariant
        isSystem -> ArnoSurfaceVariant
        else -> ArnoAssistantBubble
    }
    val shape = when {
        isUser -> RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
        else -> RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    val maxWidth = if (isUser) 0.85f else 0.95f

    Box(
        contentAlignment = alignment,
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(maxWidth),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (isTool && message.toolName != null) {
                    Text(
                        text = "\uD83D\uDD27 ${message.toolName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ArnoAccent,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }

                // Display image attachments
                if (message.localImageUris.isNotEmpty()) {
                    MessageImageGrid(uris = message.localImageUris)
                    if (message.content.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (isSystem) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = ArnoTextSecondary,
                    )
                } else if (message.content.isNotEmpty()) {
                    MarkdownText(
                        markdown = message.content,
                        color = ArnoText,
                    )
                }

                if (message.isStreaming) {
                    Text(
                        text = "\u258C",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ArnoAccent,
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageImageGrid(uris: List<String>) {
    val imageShape = RoundedCornerShape(8.dp)
    if (uris.size == 1) {
        AsyncImage(
            model = uris.first(),
            contentDescription = "Attached image",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .clip(imageShape),
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            uris.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "Attached image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(imageShape),
                        )
                    }
                    // Fill empty space if odd number
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownText(markdown: String, color: androidx.compose.ui.graphics.Color) {
    val context = LocalContext.current
    val markwon = remember { Markwon.create(context) }
    val colorArgb = color.toArgb()

    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(colorArgb)
                textSize = 15f
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, markdown)
        },
    )
}
