package network.arno.android.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import network.arno.android.chat.ChatRepository
import network.arno.android.chat.ChatViewModel
import network.arno.android.command.CommandExecutor
import network.arno.android.settings.SettingsRepository
import network.arno.android.settings.SettingsViewModel
import network.arno.android.transport.ArnoWebSocket

private enum class TopLevelRoute(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Chat("chat", "Chat", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat),
    Sessions("sessions", "Sessions", Icons.Filled.History, Icons.Outlined.History),
    Tasks("tasks", "Tasks", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArnoApp(
    webSocket: ArnoWebSocket,
    chatRepository: ChatRepository,
    settingsRepository: SettingsRepository,
    commandExecutor: CommandExecutor,
    onRequestMicPermission: () -> Unit = {},
) {
    val navController = rememberNavController()
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.Factory(webSocket, chatRepository)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(settingsRepository)
    )
    val connectionState by webSocket.connectionState.collectAsState()
    val speechEnabled by settingsViewModel.speechEnabled.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topLevelRoutes = TopLevelRoute.entries
    val showBottomBar = currentRoute in topLevelRoutes.map { it.route }

    Scaffold(
        topBar = {
            if (currentRoute != "settings") {
                TopAppBar(
                    title = { Text("Arno") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    actions = {
                        IconButton(onClick = {
                            val nowEnabled = settingsViewModel.toggleSpeech()
                            commandExecutor.setSpeechMuted(!nowEnabled)
                        }) {
                            Icon(
                                imageVector = if (speechEnabled)
                                    Icons.AutoMirrored.Filled.VolumeUp
                                else
                                    Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = if (speechEnabled) "Mute speech" else "Unmute speech",
                            )
                        }
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Text("\u2699", style = MaterialTheme.typography.titleLarge)
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    topLevelRoutes.forEach { destination ->
                        val selected = currentRoute == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.label,
                                )
                            },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "chat",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("chat") {
                ChatScreen(
                    viewModel = chatViewModel,
                    onRequestMicPermission = onRequestMicPermission,
                )
            }
            composable("sessions") {
                SessionsScreen()
            }
            composable("tasks") {
                TasksScreen()
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
