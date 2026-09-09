package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.model.ThemeSetting
import com.example.ui.navigation.TrackaaApp
import com.example.ui.theme.TrackaaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as TrackaaApplication

        setContent {
            val themeSetting by app.userPreferencesRepository.themeSettingFlow.collectAsState(
                initial = ThemeSetting.SYSTEM
            )

            // Request POST_NOTIFICATIONS on Android 13+
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* Permission result handled gracefully */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            TrackaaTheme(themeSetting = themeSetting) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TrackaaApp()
                }
            }
        }
    }
}
