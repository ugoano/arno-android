package network.arno.android.ui.components

import android.net.Uri
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import coil.compose.AsyncImage
import io.noties.markwon.Markwon
import network.arno.android.chat.ChatMessage
import network.arno.android.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val isUser = message.role == ChatMessage.Role.USER
    val isTool = message.role == ChatMessage.Role.TOOL
    val isSystem = message.role == ChatMessage.Role.SYSTEM

    // JARVIS colour mapping (matching web client)
    val borderColor = when (message.role) {
        ChatMessage.Role.USER -> JarvisCyan
        ChatMessage.Role.ASSISTANT -> JarvisGreen
        ChatMessage.Role.TOOL -> JarvisMagenta
        ChatMessage.Role.SYSTEM -> JarvisYellow
    }

    val bgTint = when (message.role) {
        ChatMessage.Role.USER -> JarvisUserBg
        ChatMessage.Role.ASSISTANT -> JarvisAssistantBg
        ChatMessage.Role.TOOL -> JarvisToolBg
        ChatMessage.Role.SYSTEM -> JarvisSystemBg
    }

    val roleBadge = when (message.role) {
        ChatMessage.Role.USER -> "USER"
        ChatMessage.Role.ASSISTANT -> "ARNO"
        ChatMessage.Role.TOOL -> message.toolName?.uppercase() ?: "TOOL"
        ChatMessage.Role.SYSTEM -> "SYSTEM"
    }

    val timestamp = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.UK).format(Date(message.timestamp))
    }

    // Log-entry style: full-width with left border
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .drawBehind {
                // Left border (3dp)
                drawRect(
                    color = borderColor,
                    topLeft = Offset(0f, 0f),
                    size = size.copy(width = 3.dp.toPx()),
                )
            }
            .background(bgTint, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
            .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Column {
            // Header row: role badge + timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = roleBadge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    ),
                    color = borderColor,
                )
                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisTimestamp,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Attachments — split into images and other files
            if (message.localImageUris.isNotEmpty()) {
                val imageExts = setOf(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp")
                val imageUris = message.localImageUris.filter { uri ->
                    imageExts.any { uri.lowercase().endsWith(it) }
                }
                val fileUris = message.localImageUris.filter { uri ->
                    imageExts.none { uri.lowercase().endsWith(it) }
                }

                if (imageUris.isNotEmpty()) {
                    MessageImageGrid(uris = imageUris)
                }
                if (fileUris.isNotEmpty()) {
                    if (imageUris.isNotEmpty()) Spacer(modifier = Modifier.height(4.dp))
                    MessageFileList(uris = fileUris)
                }
                if (message.content.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Message content
            if (message.content.isNotEmpty()) {
                if (isSystem) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = JarvisTextSecondary,
                    )
                } else {
                    MarkdownText(
                        markdown = message.content,
                        color = JarvisText,
                    )
                }
            }

            // Streaming cursor
            if (message.isStreaming) {
                Text(
                    text = "\u258C",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JarvisGreen,
                )
            }
        }
    }
}

@Composable
private fun MessageImageGrid(uris: List<String>) {
    val imageShape = RoundedCornerShape(4.dp)
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
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageFileList(uris: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        uris.forEach { uri ->
            val fileName = Uri.parse(uri).lastPathSegment
                ?: uri.substringAfterLast("/").ifEmpty { "file" }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(JarvisToolBg, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = null,
                    tint = JarvisTextSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = JarvisText,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun MarkdownText(markdown: String, color: Color) {
    val context = LocalContext.current
    val markwon = remember { Markwon.create(context) }
    val colorArgb = color.toArgb()

    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(colorArgb)
                textSize = 13f
                typeface = android.graphics.Typeface.MONOSPACE
                movementMethod = LinkMovementMethod.getInstance()
                setLineSpacing(4f, 1f)
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, markdown)
        },
    )
}
