package network.arno.android

import android.content.Context
import network.arno.android.chat.AttachmentManager
import network.arno.android.chat.ChatRepository
import network.arno.android.command.CommandExecutor
import network.arno.android.data.local.AppDatabase
import network.arno.android.schedules.SchedulesRepository
import network.arno.android.sessions.SessionsRepository
import network.arno.android.settings.SettingsRepository
import network.arno.android.tasks.TasksRepository
import network.arno.android.transport.ArnoWebSocket

class AppContainer(context: Context) {
    val settingsRepository = SettingsRepository(context)
    val attachmentManager = AttachmentManager(context)
    val tasksRepository = TasksRepository()
    val schedulesRepository = SchedulesRepository(settingsRepository.serverUrl)
    val sessionsRepository = SessionsRepository(settingsRepository)
    val database = AppDatabase.getInstance(context)

    // These are set by ArnoService when it starts
    var webSocket: ArnoWebSocket? = null
    var chatRepository: ChatRepository? = null
    var commandExecutor: CommandExecutor? = null
}
