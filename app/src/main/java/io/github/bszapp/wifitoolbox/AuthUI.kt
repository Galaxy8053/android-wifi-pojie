package io.github.bszapp.wifitoolbox

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.bszapp.wifitoolbox.contract.LaunchMode
import io.github.bszapp.wifitoolbox.contract.ToolboxState
import io.github.bszapp.wifitoolbox.contract.ToolboxStatus

private val ALL_MODES = listOf(
    LaunchMode.SHIZUKU to "通过 Shizuku 启动",
    LaunchMode.SHIZUKU_TERMINAL to "通过 Shizuku Terminal 启动",
    LaunchMode.ROOT to "通过 Root 启动"
)

@Composable
fun AuthScreen(viewModel: AuthViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    AuthUI(
        state = state,
        onLaunch = { viewModel.launch(it) },
        onCancel = { viewModel.cancel() }
    )
}

@Composable
fun ModeCard(
    label: String,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    enabled: Boolean = true
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surface

    OutlinedCard(
        onClick = { onClick?.invoke() },
        enabled = enabled && onClick != null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor)
    ) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AuthUI(
    state: ToolboxState,
    onLaunch: (LaunchMode) -> Unit,
    onCancel: () -> Unit
) {
    // 快速获取当前选中模式的文本标签
    val selectedLabel = ALL_MODES.find { it.first == state.selectedMode }?.second ?: "未知模式"

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        AnimatedContent(
            targetState = state.status,
            label = "StateTransition",
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { targetStatus ->
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (targetStatus) {
                    ToolboxStatus.IDLE -> {
                        Text(text = "选择启动方式", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(24.dp))
                        ALL_MODES.forEach { (mode, label) ->
                            ModeCard(label = label, onClick = { onLaunch(mode) })
                        }
                    }

                    ToolboxStatus.LAUNCHING -> {
                        Text(text = "正在启动", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(24.dp))
                        // 显示选中的卡片（禁用点击）
                        ModeCard(label = selectedLabel, selected = true, enabled = false)
                        Spacer(Modifier.height(32.dp))
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(32.dp))
                        TextButton(onClick = onCancel) { Text("取消操作") }
                    }

                    ToolboxStatus.ERROR -> {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(text = "启动失败", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(24.dp))

                        // 显示出错的模式卡片
                        ModeCard(label = selectedLabel, enabled = false)

                        Text(
                            text = state.errorMessage ?: "权限请求被拒绝或环境异常",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedButton(modifier = Modifier.weight(1f), onClick = onCancel) {
                                Text("返回")
                            }
                            Button(modifier = Modifier.weight(1f), onClick = { state.selectedMode?.let { onLaunch(it) } }) {
                                Text("重试")
                            }
                        }
                    }

                    ToolboxStatus.RUNNING -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(text = "环境已就绪", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
    }
}