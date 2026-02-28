package network.arno.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.compose.ui.platform.LocalContext
import network.arno.android.settings.SettingsRepository
import network.arno.android.settings.SettingsViewModel
import network.arno.android.BuildConfig
import network.arno.android.transport.ArnoWebSocket
import network.arno.android.transport.ConnectionState
import network.arno.android.ui.theme.*
import network.arno.android.voice.VoiceMode
import kotlin.math.roundToLong

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
            .verticalScroll(rememberScrollState())
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

        Text("Notification Bridge", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        val context = LocalContext.current
        val notifBridgeEnabled by settingsViewModel.notificationBridgeEnabled.collectAsState()
        val hasNotifPermission = remember(notifBridgeEnabled) {
            NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Forward Notifications",
                    style = MaterialTheme.typography.bodyLarge,
                    color = JarvisText,
                )
                Text(
                    text = "Capture notifications from whitelisted apps and forward to Arno",
                    style = MaterialTheme.typography.bodySmall,
                    color = JarvisTextSecondary,
                )
            }
            Switch(
                checked = notifBridgeEnabled,
                onCheckedChange = { settingsViewModel.toggleNotificationBridge() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = JarvisCyan,
                    checkedTrackColor = JarvisCyan.copy(alpha = 0.3f),
                ),
            )
        }

        if (notifBridgeEnabled && !hasNotifPermission) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Grant Notification Access", color = JarvisCyan)
            }
            Text(
                text = "Required: Enable Arno in Notification Access settings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else if (notifBridgeEnabled && hasNotifPermission) {
            Text(
                text = "Permission granted \u2022 Listening for: Slack",
                style = MaterialTheme.typography.bodySmall,
                color = JarvisCyan,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Silence Timeout", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "How long to wait for silence before ending speech recognition",
            style = MaterialTheme.typography.bodySmall,
            color = JarvisTextSecondary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        val screenTimeout by settingsViewModel.silenceTimeoutScreen.collectAsState()
        var screenSliderValue by remember { mutableFloatStateOf(screenTimeout.toFloat()) }
        LaunchedEffect(screenTimeout) { screenSliderValue = screenTimeout.toFloat() }

        Text(
            text = "Screen Tap: ${String.format("%.1f", screenSliderValue / 1000)}s",
            style = MaterialTheme.typography.bodyMedium,
            color = JarvisText,
        )
        Slider(
            value = screenSliderValue,
            onValueChange = { screenSliderValue = it },
            onValueChangeFinished = {
                val rounded = (screenSliderValue / 500).roundToLong() * 500
                settingsViewModel.setSilenceTimeoutScreen(rounded)
            },
            valueRange = 1500f..10000f,
            colors = SliderDefaults.colors(
                thumbColor = JarvisCyan,
                activeTrackColor = JarvisCyan,
                inactiveTrackColor = JarvisBorder,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Default: ${SettingsRepository.DEFAULT_SILENCE_TIMEOUT_SCREEN / 1000.0}s",
            style = MaterialTheme.typography.labelSmall,
            color = JarvisTextSecondary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        val btTimeout by settingsViewModel.silenceTimeoutBt.collectAsState()
        var btSliderValue by remember { mutableFloatStateOf(btTimeout.toFloat()) }
        LaunchedEffect(btTimeout) { btSliderValue = btTimeout.toFloat() }

        Text(
            text = "Bluetooth / Glasses: ${String.format("%.1f", btSliderValue / 1000)}s",
            style = MaterialTheme.typography.bodyMedium,
            color = JarvisText,
        )
        Slider(
            value = btSliderValue,
            onValueChange = { btSliderValue = it },
            onValueChangeFinished = {
                val rounded = (btSliderValue / 500).roundToLong() * 500
                settingsViewModel.setSilenceTimeoutBt(rounded)
            },
            valueRange = 1500f..10000f,
            colors = SliderDefaults.colors(
                thumbColor = JarvisCyan,
                activeTrackColor = JarvisCyan,
                inactiveTrackColor = JarvisBorder,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Default: ${SettingsRepository.DEFAULT_SILENCE_TIMEOUT_BT / 1000.0}s",
            style = MaterialTheme.typography.labelSmall,
            color = JarvisTextSecondary,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("App", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        ListItem(
            headlineContent = { Text("Version") },
            supportingContent = { Text("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})") },
        )
    }
}
