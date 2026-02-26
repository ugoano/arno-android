package network.arno.android.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import network.arno.android.chat.Attachment
import network.arno.android.ui.theme.*
import network.arno.android.voice.VoiceInputManager
import network.arno.android.voice.VoiceMode

@Composable
fun InputBar(
    onSend: (String, Boolean) -> Unit,
    onCancel: () -> Unit,
    isProcessing: Boolean,
    onRequestMicPermission: () -> Unit,
    voiceMode: VoiceMode,
    silenceTimeoutMs: Long = VoiceInputManager.DEFAULT_SILENCE_TIMEOUT_MS,
    attachments: List<Attachment>,
    onAttachClick: () -> Unit,
    onRemoveAttachment: (android.net.Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current

    val voiceInputManager = remember(silenceTimeoutMs) {
        VoiceInputManager(
            context = context,
            onResult = { transcript, viaVoice ->
                onSend(transcript, viaVoice)
            },
            onTextAccumulated = { transcript ->
                // Dictation mode: append to text field, user sends manually
                text = if (text.isBlank()) transcript else "$text $transcript"
            },
            silenceTimeoutMs = silenceTimeoutMs,
        )
    }

    LaunchedEffect(voiceInputManager) {
        voiceInputManager.warmUp()
    }

    val isListening by voiceInputManager.isListening.collectAsState()
    val isContinuousActive by voiceInputManager.isContinuousActive.collectAsState()
    val partialText by voiceInputManager.partialText.collectAsState()
    val hasContent = text.isNotBlank() || attachments.any { it.error == null }

    // Show partial text from voice in the text field
    val displayText = if (partialText.isNotBlank() && (isListening || isContinuousActive)) partialText else text

    // Determine mic colour based on mode and state
    val micColour by animateColorAsState(
        targetValue = when {
            isContinuousActive && voiceMode == VoiceMode.WAKE_WORD -> JarvisYellow
            isContinuousActive && voiceMode == VoiceMode.DICTATION -> JarvisGreen
            isListening -> JarvisRed
            else -> JarvisCyan
        },
        label = "micColour",
    )

    // Border colour follows mic state
    val borderColour by animateColorAsState(
        targetValue = when {
            isContinuousActive && voiceMode == VoiceMode.WAKE_WORD -> JarvisYellow
            isContinuousActive && voiceMode == VoiceMode.DICTATION -> JarvisGreen
            isListening -> JarvisRed
            else -> JarvisCyan
        },
        label = "borderColour",
    )

    DisposableEffect(Unit) {
        onDispose { voiceInputManager.destroy() }
    }

    fun handleMicClick() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            onRequestMicPermission()
            return
        }
        // WAKE_WORD is handled by ArnoService — InputBar only handles PTT and dictation
        if (voiceMode == VoiceMode.WAKE_WORD) return
        voiceInputManager.toggle(voiceMode)
    }

    Surface(
        color = JarvisBg,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            // Top border line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(JarvisBorder),
            )

            // Voice mode indicator (shown when continuous mode is active)
            if (isContinuousActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when (voiceMode) {
                                VoiceMode.WAKE_WORD -> JarvisYellow.copy(alpha = 0.08f)
                                VoiceMode.DICTATION -> JarvisGreen.copy(alpha = 0.08f)
                                else -> JarvisCyan.copy(alpha = 0.08f)
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = when (voiceMode) {
                            VoiceMode.WAKE_WORD -> "\uD83D\uDC42 Listening for \"Arno\"..."
                            VoiceMode.DICTATION -> "\uD83C\uDFA4 Dictation active — speak freely"
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 0.5.sp,
                        ),
                        color = when (voiceMode) {
                            VoiceMode.WAKE_WORD -> JarvisYellow
                            VoiceMode.DICTATION -> JarvisGreen
                            else -> JarvisCyan
                        },
                    )
                }
            }

            // Attachment preview strip
            if (attachments.isNotEmpty()) {
                AttachmentPreviewStrip(
                    attachments = attachments,
                    onRemove = onRemoveAttachment,
                )
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                // Attachment button
                IconButton(
                    onClick = onAttachClick,
                    enabled = !isProcessing,
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach file",
                        tint = if (!isProcessing) JarvisTextSecondary else JarvisBorder,
                    )
                }

                OutlinedTextField(
                    value = displayText,
                    onValueChange = { text = it },
                    placeholder = {
                        Text(
                            text = when {
                                isContinuousActive && voiceMode == VoiceMode.WAKE_WORD -> "Say \"Arno\" to activate..."
                                isContinuousActive && voiceMode == VoiceMode.DICTATION -> "Speak now..."
                                isListening -> "LISTENING..."
                                else -> "> message arno..."
                            },
                            color = JarvisTextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    shape = RoundedCornerShape(4.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = JarvisText),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = borderColour,
                        unfocusedBorderColor = if (isContinuousActive) borderColour.copy(alpha = 0.5f) else JarvisBorder,
                        cursorColor = JarvisCyan,
                        focusedContainerColor = JarvisSurface,
                        unfocusedContainerColor = JarvisSurface,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (hasContent && !isProcessing) {
                            if (isContinuousActive) voiceInputManager.stop()
                            onSend(text, false)
                            text = ""
                        }
                    }),
                    maxLines = 4,
                    modifier = Modifier.weight(1f),
                )

                // Mic button — icon changes per mode
                IconButton(
                    onClick = { handleMicClick() },
                    enabled = !isProcessing,
                    modifier = Modifier.padding(start = 4.dp),
                ) {
                    Text(
                        text = when {
                            isContinuousActive -> "\u23F9" // stop
                            voiceMode == VoiceMode.WAKE_WORD -> "\uD83D\uDC42" // ear
                            voiceMode == VoiceMode.DICTATION -> "\uD83C\uDF99" // studio mic
                            else -> "\uD83C\uDFA4" // mic
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = if (!isProcessing) micColour else JarvisBorder,
                    )
                }

                if (isProcessing) {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.padding(start = 4.dp),
                    ) {
                        Text(
                            text = "\u2715",
                            style = MaterialTheme.typography.titleLarge,
                            color = JarvisRed,
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (hasContent) {
                                if (isContinuousActive) voiceInputManager.stop()
                                onSend(text, false)
                                text = ""
                            }
                        },
                        enabled = hasContent,
                        modifier = Modifier.padding(start = 4.dp),
                    ) {
                        Text(
                            text = "\u2191",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (hasContent) JarvisCyan else JarvisBorder,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentPreviewStrip(
    attachments: List<Attachment>,
    onRemove: (android.net.Uri) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(attachments, key = { it.uri.toString() }) { attachment ->
            AttachmentPreviewItem(attachment = attachment, onRemove = { onRemove(attachment.uri) })
        }
    }
}

@Composable
private fun AttachmentPreviewItem(
    attachment: Attachment,
    onRemove: () -> Unit,
) {
    val isImage = attachment.mimeType.startsWith("image/")

    Box(
        modifier = Modifier.size(72.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (isImage) {
                AsyncImage(
                    model = attachment.uri,
                    contentDescription = attachment.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                ) {
                    Text(
                        text = "\uD83D\uDCC4",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = attachment.name,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Upload progress overlay
            if (attachment.isUploading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            JarvisBg.copy(alpha = 0.7f),
                            RoundedCornerShape(8.dp),
                        ),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = JarvisCyan,
                    )
                }
            }

            // Error overlay
            if (attachment.error != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            JarvisRed.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp),
                        ),
                ) {
                    Text(
                        text = "\u26A0",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }

        // Remove button
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remove",
            tint = JarvisText,
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp)
                .background(JarvisSurface, CircleShape)
                .clickable { onRemove() }
                .padding(2.dp),
        )
    }
}
