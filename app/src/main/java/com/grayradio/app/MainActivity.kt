package com.grayradio.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grayradio.app.ui.screens.HomeScreen
import com.grayradio.app.ui.theme.Gray900
import com.grayradio.app.ui.theme.GrayRadioTheme
import com.grayradio.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Media notification may be limited if denied; playback still works. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as GrayRadioApp
        app.radioPlayer.onPlayRequested = { ensureNotificationPermission() }
        setContent {
            GrayRadioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Gray900,
                ) {
                    val vm: MainViewModel = viewModel(
                        factory = MainViewModel.Factory(app.repository, app.radioPlayer),
                    )
                    HomeScreen(viewModel = vm)
                }
            }
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
