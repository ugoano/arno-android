package network.arno.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import network.arno.android.service.ArnoService
import network.arno.android.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import network.arno.android.chat.AttachmentManager
import network.arno.android.chat.ChatRepository
import network.arno.android.chat.ChatViewModel
import network.arno.android.command.CommandExecutor
import network.arno.android.schedules.SchedulesViewModel
import network.arno.android.sessions.SessionsRepository
import network.arno.android.sessions.SessionsViewModel
import network.arno.android.settings.SettingsRepository
import network.arno.android.settings.SettingsViewModel
import network.arno.android.tasks.TasksRepository
import network.arno.android.tasks.TasksViewModel
import network.arno.android.transport.ArnoWebSocket
import network.arno.android.voice.VoiceMode
import network.arno.android.ArnoApp as ArnoApplication

private enum class TopLevelRoute(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Chat("chat", "Chat", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat),
    Sessions("sessions", "Sessions", Icons.Filled.History, Icons.Outlined.History),
    Tasks("tasks", "Tasks", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
    Schedules("schedules", "Schedules", Icons.Filled.Schedule, Icons.Outlined.Schedule),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArnoApp(
    webSocket: ArnoWebSocket,
    chatRepository: ChatRepository,
    settingsRepository: SettingsRepository,
    commandExecutor: CommandExecutor,
    attachmentManager: AttachmentManager,
    tasksRepository: TasksRepository,
    sessionsRepository: SessionsRepository,
    onRequestMicPermission: () -> Unit = {},
) {
    val navController = rememberNavController()
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.Factory(webSocket, chatRepository, attachmentManager, settingsRepository)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(settingsRepository)
    )
    val tasksViewModel: TasksViewModel = viewModel(
        factory = TasksViewModel.Factory(tasksRepository)
    )
    val sessionsViewModel: SessionsViewModel = viewModel(
        factory = SessionsViewModel.Factory(sessionsRepository, webSocket, chatRepository) {
            navController.navigate(TopLevelRoute.Chat.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    )
    val connectionState by webSocket.connectionState.collectAsState()
    val speechEnabled by settingsViewModel.speechEnabled.collectAsState()
    val voiceMode by settingsViewModel.voiceMode.collectAsState()
    val bluetoothTriggerEnabled by settingsViewModel.bluetoothTriggerEnabled.collectAsState()
    val silenceTimeoutScreen by settingsViewModel.silenceTimeoutScreen.collectAsState()
    val context = LocalContext.current
    val container = (context.applicationContext as ArnoApplication).container
    val schedulesViewModel: SchedulesViewModel = viewModel(
        factory = SchedulesViewModel.Factory(container.schedulesRepository)
    )

    // Notify the foreground service when voice mode changes
    LaunchedEffect(voiceMode) {
        val intent = Intent(context, ArnoService::class.java).apply {
            action = ArnoService.ACTION_SET_VOICE_MODE
            putExtra(ArnoService.EXTRA_VOICE_MODE, voiceMode.name)
        }
        context.startService(intent)
    }

    // Notify the foreground service when Bluetooth trigger toggled
    LaunchedEffect(bluetoothTriggerEnabled) {
        val intent = Intent(context, ArnoService::class.java).apply {
            action = ArnoService.ACTION_SET_BT_TRIGGER
            putExtra(ArnoService.EXTRA_BT_TRIGGER_ENABLED, bluetoothTriggerEnabled)
        }
        context.startService(intent)
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(currentRoute) {
        if (currentRoute == TopLevelRoute.Schedules.route) {
            schedulesViewModel.refresh()
        }
    }

    // Badge: count of pending + running tasks
    val tasksSummary by tasksViewModel.summary.collectAsState()
    val queueCount = tasksSummary?.let { it.pending + it.queued + it.running } ?: 0

    val topLevelRoutes = TopLevelRoute.entries
    val showBottomBar = currentRoute in topLevelRoutes.map { it.route }

    Scaffold(
        topBar = {
            if (currentRoute != "settings") {
                TopAppBar(
                    title = {
                        Text(
                            text = "ARNO",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp,
                            ),
                            color = JarvisCyan,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = JarvisBg,
                        actionIconContentColor = JarvisTextSecondary,
                    ),
                    actions = {
                        // Voice mode cycle button
                        IconButton(onClick = { settingsViewModel.cycleVoiceMode() }) {
                            Text(
                                text = when (voiceMode) {
                                    VoiceMode.PUSH_TO_TALK -> "\uD83C\uDFA4"  // mic
                                    VoiceMode.DICTATION -> "\uD83C\uDF99"     // studio mic
                                    VoiceMode.WAKE_WORD -> "\uD83D\uDC42"    // ear
                                },
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        // Speech output toggle
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
                                tint = if (speechEnabled) JarvisGreen else JarvisTextSecondary,
                            )
                        }
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Text(
                                text = "\u2699",
                                style = MaterialTheme.typography.titleLarge,
                                color = JarvisTextSecondary,
                            )
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = JarvisBg,
                    contentColor = JarvisText,
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
                                if (destination == TopLevelRoute.Tasks && queueCount > 0) {
                                    BadgedBox(badge = {
                                        Badge(
                                            containerColor = JarvisCyan,
                                            contentColor = JarvisBg,
                                        ) { Text(queueCount.toString()) }
                                    }) {
                                        Icon(
                                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                            contentDescription = destination.label,
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                        contentDescription = destination.label,
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = destination.label.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.sp,
                                    ),
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = JarvisCyan,
                                selectedTextColor = JarvisCyan,
                                unselectedIconColor = JarvisTextSecondary,
                                unselectedTextColor = JarvisTextSecondary,
                                indicatorColor = JarvisSurfaceVariant,
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
                    voiceMode = voiceMode,
                    silenceTimeoutMs = silenceTimeoutScreen,
                )
            }
            composable("sessions") {
                SessionsScreen(viewModel = sessionsViewModel)
            }
            composable("tasks") {
                TasksScreen(viewModel = tasksViewModel)
            }
            composable("schedules") {
                SchedulesScreen(viewModel = schedulesViewModel)
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
