package network.arno.android

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import network.arno.android.service.ArnoService
import network.arno.android.ui.screens.ArnoApp
import network.arno.android.ui.theme.ArnoTheme

class MainActivity : ComponentActivity() {

    private val serviceBound = mutableStateOf(false)
    private var arnoService: ArnoService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            arnoService = (binder as ArnoService.ArnoBinder).service
            serviceBound.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            arnoService = null
            serviceBound.value = false
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* granted or not */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        startAndBindService()

        setContent {
            ArnoTheme {
                val app = application as network.arno.android.ArnoApp
                if (serviceBound.value) {
                    val ws = app.container.webSocket
                    val repo = app.container.chatRepository
                    if (ws != null && repo != null) {
                        ArnoApp(
                            webSocket = ws,
                            chatRepository = repo,
                            settingsRepository = app.container.settingsRepository,
                        )
                    }
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun startAndBindService() {
        val intent = Intent(this, ArnoService::class.java)
        startForegroundService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        if (serviceBound.value) {
            unbindService(connection)
            serviceBound.value = false
        }
        super.onDestroy()
    }
}
