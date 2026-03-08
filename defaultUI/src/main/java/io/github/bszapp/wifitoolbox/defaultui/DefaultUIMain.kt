package io.github.bszapp.wifitoolbox.defaultui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import io.github.bszapp.wifitoolbox.core.launcher.LaunchStatus

@Composable
fun DefaultUI(viewModel: ToolboxViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.launchState.collectAsState()

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
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 状态展示
                val statusColor = when (state.status) {
                    LaunchStatus.RUNNING   -> MaterialTheme.colorScheme.primary
                    LaunchStatus.LAUNCHING -> MaterialTheme.colorScheme.tertiary
                    LaunchStatus.ERROR     -> MaterialTheme.colorScheme.error
                    LaunchStatus.IDLE      -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                if (state.status == LaunchStatus.LAUNCHING) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(16.dp))
                }

                Text(
                    text = state.message,
                    style = MaterialTheme.typography.titleMedium,
                    color = statusColor
                )

                Spacer(Modifier.height(48.dp))

                val busy = state.status == LaunchStatus.LAUNCHING

                Button(
                    onClick = { viewModel.launchWithShizuku() },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("通过 Shizuku 启动")
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.launchWithRoot() },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("通过 Root 启动")
                }

                if (state.status == LaunchStatus.RUNNING) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.stop() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("停止")
                    }
                }
            }
        }
    }
}