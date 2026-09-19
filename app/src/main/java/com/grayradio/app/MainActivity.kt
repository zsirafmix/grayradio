package com.grayradio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grayradio.app.ui.screens.HomeScreen
import com.grayradio.app.ui.theme.Gray900
import com.grayradio.app.ui.theme.GrayRadioTheme
import com.grayradio.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as GrayRadioApp
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
}
