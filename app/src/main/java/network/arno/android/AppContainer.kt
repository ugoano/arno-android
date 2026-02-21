package network.arno.android

import android.content.Context
import network.arno.android.chat.ChatRepository
import network.arno.android.command.CommandExecutor
import network.arno.android.settings.SettingsRepository
import network.arno.android.transport.ArnoWebSocket

class AppContainer(context: Context) {
    val settingsRepository = SettingsRepository(context)

    // These are set by ArnoService when it starts
    var webSocket: ArnoWebSocket? = null
    var chatRepository: ChatRepository? = null
    var commandExecutor: CommandExecutor? = null
}
