package network.arno.android.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.arno.android.chat.ChatViewModel
import network.arno.android.ui.components.ConnectionBanner
import network.arno.android.ui.components.InputBar
import network.arno.android.ui.components.MessageBubble
import network.arno.android.voice.VoiceInputManager
import network.arno.android.voice.VoiceMode

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onRequestMicPermission: () -> Unit,
    voiceMode: VoiceMode = VoiceMode.PUSH_TO_TALK,
    silenceTimeoutMs: Long = VoiceInputManager.DEFAULT_SILENCE_TIMEOUT_MS,
) {
    val messages by viewModel.messages.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isCompacting by viewModel.isCompacting.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val attachments by viewModel.attachments.collectAsState()
    val listState = rememberLazyListState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris ->
        uris.forEach { uri -> viewModel.addAttachment(uri) }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ConnectionBanner(state = connectionState)

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }
        }

        if (isCompacting) {
            Text(
                text = "Compacting context...",
                color = Color(0xFFFFB300),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        InputBar(
            onSend = { text, viaVoice -> viewModel.sendMessage(text, viaVoice) },
            onCancel = viewModel::cancelTask,
            isProcessing = isProcessing,
            onRequestMicPermission = onRequestMicPermission,
            voiceMode = voiceMode,
            silenceTimeoutMs = silenceTimeoutMs,
            attachments = attachments,
            onAttachClick = { filePickerLauncher.launch("*/*") },
            onRemoveAttachment = { uri -> viewModel.removeAttachment(uri) },
        )
    }
}
