package network.arno.android.command

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import network.arno.android.settings.SettingsRepository
import network.arno.android.transport.ClientCommand
import network.arno.android.transport.CommandResponse

class CommandExecutor(
    private val context: Context,
    settingsRepository: SettingsRepository,
) {

    companion object {
        private const val TAG = "CommandExecutor"
    }

    private val speakHandler = SpeakHandler(context).apply {
        muted = !settingsRepository.speechEnabled
    }
    private val clipboardHandler = ClipboardHandler(context)
    private val linkHandler = LinkHandler(context)
    private val notificationHandler = NotificationHandler(context)
    private val closeTabHandler = CloseTabHandler(context)
    private val deviceControlHandler = DeviceControlHandler(context)
    private val readScreenHandler = ReadScreenHandler()
    private val tapElementHandler = TapElementHandler()
    private val typeTextHandler = TypeTextHandler()
    private val navigateHandler = NavigateHandler()
    private val scrollHandler = ScrollHandler()
    private val appLauncherHandler = AppLauncherHandler(context)

    private val handlers: Map<String, (JsonObject) -> HandlerResult> = mapOf(
        "speak" to speakHandler::handle,
        "clipboard_copy" to clipboardHandler::handleCopy,
        "clipboard_paste" to clipboardHandler::handlePaste,
        "open_link" to linkHandler::handle,
        "notification" to notificationHandler::handle,
        "close_tab" to closeTabHandler::handle,
        "device_control" to deviceControlHandler::handle,
        "read_screen" to readScreenHandler::handle,
        "tap_element" to tapElementHandler::handle,
        "type_text" to typeTextHandler::handle,
        "navigate" to navigateHandler::handle,
        "scroll" to scrollHandler::handle,
        "open_app" to appLauncherHandler::handleOpenApp,
        "list_apps" to appLauncherHandler::handleListApps,
    )

    fun execute(command: ClientCommand): CommandResponse {
        val handler = handlers[command.type]
            ?: return CommandResponse(
                id = command.id,
                status = "unsupported",
                error = "Unknown command type: ${command.type}",
            )

        return try {
            val result = handler(command.payload)
            when (result) {
                is HandlerResult.Success -> CommandResponse(
                    id = command.id,
                    status = "success",
                    result = result.data,
                )
                is HandlerResult.Error -> CommandResponse(
                    id = command.id,
                    status = "error",
                    error = result.message,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Command ${command.type} failed", e)
            CommandResponse(
                id = command.id,
                status = "error",
                error = e.message ?: "Unknown error",
            )
        }
    }

    fun setSpeechMuted(muted: Boolean) {
        speakHandler.muted = muted
    }

    fun shutdown() {
        speakHandler.shutdown()
    }
}

sealed class HandlerResult {
    data class Success(val data: JsonObject? = null) : HandlerResult()
    data class Error(val message: String) : HandlerResult()
}
