package network.arno.android.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import network.arno.android.chat.ChatRepository
import network.arno.android.chat.ChatViewModel
import network.arno.android.settings.SettingsRepository
import network.arno.android.settings.SettingsViewModel
import network.arno.android.transport.ArnoWebSocket

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArnoApp(
    webSocket: ArnoWebSocket,
    chatRepository: ChatRepository,
    settingsRepository: SettingsRepository,
) {
    val navController = rememberNavController()
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.Factory(webSocket, chatRepository)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(settingsRepository)
    )
    val connectionState by webSocket.connectionState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Arno") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Text("\u2699", style = MaterialTheme.typography.titleLarge)
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "chat",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("chat") {
                ChatScreen(viewModel = chatViewModel)
            }
            composable("settings") {
                SettingsScreen(
                    settingsViewModel = settingsViewModel,
                    connectionState = connectionState,
                    clientId = webSocket.clientId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
