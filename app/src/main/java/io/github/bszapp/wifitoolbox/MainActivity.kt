package io.github.bszapp.wifitoolbox

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import io.github.bszapp.wifitoolbox.contract.ToolboxControllerProvider
import io.github.bszapp.wifitoolbox.contract.ToolboxStatus
import io.github.bszapp.wifitoolbox.uidefault.DefaultUI

class MainActivity : ComponentActivity() {

    private val controller by lazy { ToolboxControllerProvider.get() }

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
            val state by controller.state.collectAsState()

            DynamicMaterialTheme(
                seedColor = seedColor,
                isDark = isDark,
                style = PaletteStyle.TonalSpot,
                animate = true
            ) {
                AnimatedContent(targetState = state.status == ToolboxStatus.RUNNING) { isRunning ->
                    if (isRunning) {
                        DefaultUI()
                    } else {
                        AuthScreen()
                    }
                }
            }
        }
    }
}