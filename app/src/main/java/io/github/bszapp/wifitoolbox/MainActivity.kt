package io.github.bszapp.wifitoolbox

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import io.github.bszapp.wifitoolbox.contract.ToolboxControllerProvider
import io.github.bszapp.wifitoolbox.contract.ToolboxStatus
import io.github.bszapp.wifitoolbox.uidefault.DefaultUI
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainActivity : ComponentActivity() {

    private val controller by lazy { ToolboxControllerProvider.get() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        controller.state
            .onEach { state ->
                setContent {
                    // 动态主题包裹，抄自旧代码
                    val context = LocalContext.current
                    val isDark = isSystemInDarkTheme()
                    val seedColor = remember {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                            dynamicLightColorScheme(context).primary
                        else Color(0xFF6750A4)
                    }

                    DynamicMaterialTheme(
                        seedColor = seedColor,
                        isDark = isDark,
                        style = PaletteStyle.TonalSpot,
                        animate = true
                    ) {
                        when (state.status) {
                            ToolboxStatus.RUNNING -> DefaultUI()
                            else -> AuthUI(
                                state = state,
                                onShizuku = { controller.launchShizuku() },
                                onRoot = { controller.launchRoot() }
                            )
                        }
                    }
                }
            }
            .launchIn(lifecycleScope)
    }
}