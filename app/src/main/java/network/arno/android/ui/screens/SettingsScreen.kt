package network.arno.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.arno.android.settings.SettingsViewModel
import network.arno.android.BuildConfig
import network.arno.android.transport.ArnoWebSocket
import network.arno.android.transport.ConnectionState
import network.arno.android.ui.theme.*
import network.arno.android.voice.VoiceMode

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
            title = { Text("SETTINGS", color = JarvisCyan) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Text("\u2190", style = MaterialTheme.typography.titleLarge, color = JarvisTextSecondary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = JarvisBg),
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

        Spacer(modifier = Modifier.height(24.dp))

        Text("Voice Mode", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        val currentVoiceMode by settingsViewModel.voiceMode.collectAsState()
        VoiceMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                RadioButton(
                    selected = currentVoiceMode == mode,
                    onClick = { settingsViewModel.setVoiceMode(mode) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = JarvisCyan,
                        unselectedColor = JarvisTextSecondary,
                    ),
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = JarvisText,
                    )
                    Text(
                        text = when (mode) {
                            VoiceMode.PUSH_TO_TALK -> "Tap mic, speak, auto-send"
                            VoiceMode.DICTATION -> "Continuous listening, each phrase auto-sends"
                            VoiceMode.WAKE_WORD -> "Always listening for \"Arno\" wake word"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = JarvisTextSecondary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Bluetooth Trigger", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        val btTriggerEnabled by settingsViewModel.bluetoothTriggerEnabled.collectAsState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Media Button Voice Trigger",
                    style = MaterialTheme.typography.bodyLarge,
                    color = JarvisText,
                )
                Text(
                    text = "Tap Ray-Ban Meta temple or BT media button to trigger voice input",
                    style = MaterialTheme.typography.bodySmall,
                    color = JarvisTextSecondary,
                )
            }
            Switch(
                checked = btTriggerEnabled,
                onCheckedChange = { settingsViewModel.toggleBluetoothTrigger() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = JarvisCyan,
                    checkedTrackColor = JarvisCyan.copy(alpha = 0.3f),
                ),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("App", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        ListItem(
            headlineContent = { Text("Version") },
            supportingContent = { Text("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})") },
        )
    }
}
