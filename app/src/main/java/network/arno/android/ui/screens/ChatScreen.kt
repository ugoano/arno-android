package network.arno.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.arno.android.chat.ChatViewModel
import network.arno.android.ui.components.ConnectionBanner
import network.arno.android.ui.components.InputBar
import network.arno.android.ui.components.MessageBubble

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onRequestMicPermission: () -> Unit,
) {
    val messages by viewModel.messages.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val listState = rememberLazyListState()

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

        InputBar(
            onSend = { text, viaVoice -> viewModel.sendMessage(text, viaVoice) },
            onCancel = viewModel::cancelTask,
            isProcessing = isProcessing,
            onRequestMicPermission = onRequestMicPermission,
        )
    }
}
