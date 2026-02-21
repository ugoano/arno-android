package network.arno.android.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import network.arno.android.chat.Attachment
import network.arno.android.ui.theme.ArnoAccent

@Composable
fun InputBar(
    onSend: (String, Boolean) -> Unit,
    onCancel: () -> Unit,
    isProcessing: Boolean,
    onRequestMicPermission: () -> Unit,
    attachments: List<Attachment>,
    onAttachClick: () -> Unit,
    onRemoveAttachment: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val hasContent = text.isNotBlank() || attachments.any { it.error == null }

    val micColour by animateColorAsState(
        targetValue = if (isListening) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary,
        label = "micColour",
    )

    DisposableEffect(Unit) {
        onDispose { speechRecognizer.destroy() }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("InputBar", "Speech recognition not available")
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            onRequestMicPermission()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-GB")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                Log.w("InputBar", "Speech error: $error")
                isListening = false
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val finalText = matches?.firstOrNull()
                if (!finalText.isNullOrBlank()) {
                    onSend(finalText, true)
                    text = ""
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    text = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer.stopListening()
        isListening = false
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
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
                        tint = if (!isProcessing) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(if (isListening) "Listening..." else "Message Arno...") },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isListening) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (hasContent && !isProcessing) {
                            onSend(text, false)
                            text = ""
                        }
                    }),
                    maxLines = 4,
                    modifier = Modifier.weight(1f),
                )

                // Mic button
                IconButton(
                    onClick = {
                        if (isListening) stopListening() else startListening()
                    },
                    enabled = !isProcessing,
                    modifier = Modifier.padding(start = 4.dp),
                ) {
                    Text(
                        text = if (isListening) "\u23F9" else "\uD83C\uDFA4",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (!isProcessing) micColour
                        else MaterialTheme.colorScheme.onSurfaceVariant,
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
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (hasContent) {
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
                            color = if (hasContent)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
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
    onRemove: (Uri) -> Unit,
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
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            RoundedCornerShape(8.dp),
                        ),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = ArnoAccent,
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
                            MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
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
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .clickable { onRemove() }
                .padding(2.dp),
        )
    }
}
