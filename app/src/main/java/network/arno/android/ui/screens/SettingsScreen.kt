package network.arno.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.arno.android.settings.SettingsViewModel
import network.arno.android.transport.ArnoWebSocket
import network.arno.android.transport.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    connectionState: ConnectionState,
    clientId: String,
    onBack: () -> Unit,
) {
    val serverUrl by settingsViewModel.serverUrl.collectAsState()
    var editUrl by remember { mutableStateOf(serverUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Text("\u2190", style = MaterialTheme.typography.titleLarge)
                }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Server URL", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = editUrl,
            onValueChange = { editUrl = it },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            singleLine = true,
        )

        if (editUrl != serverUrl) {
            Button(
                onClick = { settingsViewModel.updateServerUrl(editUrl) },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Save (requires restart)")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Connection", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        val statusText = when (connectionState) {
            is ConnectionState.Connected -> "Connected"
            is ConnectionState.Connecting -> "Connecting..."
            is ConnectionState.Reconnecting -> "Reconnecting (attempt ${connectionState.attempt})"
            is ConnectionState.Disconnected -> "Disconnected"
            is ConnectionState.Error -> "Error: ${connectionState.message}"
        }

        ListItem(headlineContent = { Text("Status") }, supportingContent = { Text(statusText) })
        ListItem(headlineContent = { Text("Client ID") }, supportingContent = { Text(clientId) })
        ListItem(
            headlineContent = { Text("Capabilities") },
            supportingContent = { Text(ArnoWebSocket.CAPABILITIES.joinToString(", ")) },
        )
    }
}
