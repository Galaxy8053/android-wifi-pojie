package io.github.bszapp.wifitoolbox

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.bszapp.wifitoolbox.contract.LaunchMode
import io.github.bszapp.wifitoolbox.contract.ToolboxState
import io.github.bszapp.wifitoolbox.contract.ToolboxStatus
import kotlinx.coroutines.launch

@Composable
fun AuthUI(
    state: ToolboxState,
    onLaunch: (LaunchMode) -> Unit
) {
    val busy = state.status == ToolboxStatus.LAUNCHING

    val statusColor = when (state.status) {
        ToolboxStatus.RUNNING   -> MaterialTheme.colorScheme.primary
        ToolboxStatus.LAUNCHING -> MaterialTheme.colorScheme.tertiary
        ToolboxStatus.ERROR     -> MaterialTheme.colorScheme.error
        ToolboxStatus.IDLE      -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(16.dp))
            }

            Text(
                text = state.message,
                style = MaterialTheme.typography.titleMedium,
                color = statusColor
            )

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = { onLaunch(LaunchMode.SHIZUKU) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("通过 Shizuku 启动") }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { onLaunch(LaunchMode.SHIZUKU_TERMINAL) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("通过 Shizuku Terminal 启动") }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { onLaunch(LaunchMode.ROOT) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("通过 Root 启动") }
        }
    }
}