package io.github.bszapp.wifitoolbox

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.bszapp.wifitoolbox.contract.LaunchMode
import io.github.bszapp.wifitoolbox.contract.ToolboxStatus


@Composable
fun AuthScreen(viewModel: AuthViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val allModes = listOf(LaunchMode.SHIZUKU, LaunchMode.SHIZUKU_TERMINAL, LaunchMode.ROOT)

    val displayList = if (state.status == ToolboxStatus.IDLE) allModes
    else allModes.filter { it == state.selectedMode }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        item(key = "status_header") {
            val statusText = when (state.status) {
                ToolboxStatus.IDLE -> "工作模式"
                ToolboxStatus.LAUNCHING -> "启动中"
                ToolboxStatus.RUNNING -> "完成"
                ToolboxStatus.ERROR -> "启动失败"
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp),
                fontWeight = FontWeight.SemiBold
            )

            if (state.status == ToolboxStatus.ERROR) {
                Text(
                    text = "${state.errorMessage}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        items(displayList, key = { it.name }) { mode ->
            Card(
                onClick = { viewModel.launch(mode) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (mode) {
                            LaunchMode.SHIZUKU -> "通过 Shizuku 启动"
                            LaunchMode.SHIZUKU_TERMINAL -> "通过 Shizuku Terminal 启动"
                            LaunchMode.ROOT -> "通过 Root 启动"
                        },
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (state.status == ToolboxStatus.LAUNCHING) {
            item(key = "cancel_action") {
                Button(
                    onClick = { viewModel.cancel() },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .animateItem()
                ) {
                    Text(
                        "取消",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (state.status == ToolboxStatus.ERROR) {
            item(key = "error_actions") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .animateItem(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.cancel() },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "上一步",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = { state.selectedMode?.let { viewModel.launch(it) } },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "重试",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}