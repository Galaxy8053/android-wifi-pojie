package io.github.bszapp.wifitoolbox.defaultui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle

@Composable
fun DefaultUI(
    viewModel: ToolboxViewModel = viewModel()
) {
    val context = LocalContext.current

    val textState by viewModel.uiState.collectAsState()

    //界面主题
    val isDark = isSystemInDarkTheme()
    val seedColor = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicLightColorScheme(context).primary
        } else {
            Color(0xFF6750A4)
        }
    }

    DynamicMaterialTheme(
        seedColor = seedColor,
        isDark = isDark,
        style = PaletteStyle.TonalSpot,
        animate = true
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Text(
                text = textState,
                modifier = Modifier.padding(innerPadding),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}