package io.github.bszapp.wifitoolbox

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import io.github.bszapp.wifitoolbox.contract.AppControllerProvider
import io.github.bszapp.wifitoolbox.contract.startup.StartupStatus
import io.github.bszapp.wifitoolbox.ui.startup.StartupScreen
import io.github.bszapp.wifitoolbox.uidefault.DefaultUI
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val controller by lazy { AppControllerProvider.get() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            val context = LocalContext.current
            val isDark = isSystemInDarkTheme()
            val seedColor = remember {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    dynamicLightColorScheme(context).primary
                else Color(0xFF6750A4)
            }
            val state by controller.startup.state.collectAsState()
            val isExiting by controller.isExiting.collectAsState()

            LaunchedEffect(isExiting) {
                if (isExiting) {
                    finish()
                }
            }

            DynamicMaterialTheme(
                seedColor = seedColor,
                isDark = isDark,
                style = PaletteStyle.TonalSpot,
                animate = true
            ) {
                @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    AnimatedContent(targetState = state.status == StartupStatus.RUNNING) { isRunning ->
                        if (isRunning) {
                            DefaultUI()
                        } else {
                            StartupScreen()
                        }
                    }
                }
            }
        }
    }
}