package io.github.bszapp.wifitoolbox.uidefault.screen

import android.os.Build
import android.net.wifi.ScanResult
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.bszapp.wifitoolbox.contract.wifilist.ScanStatus
import io.github.bszapp.wifitoolbox.uidefault.DefaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: DefaultViewModel = viewModel()) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scanStatus by vm.wifiList.status.collectAsStateWithLifecycle()
    val scanResults by vm.wifiList.results.collectAsStateWithLifecycle()
    val errorMsg by vm.wifiList.errorMessage.collectAsStateWithLifecycle()

    val isScanning = scanStatus == ScanStatus.SCANNING

    var selectedAp by rememberSaveable { mutableStateOf<ScanResult?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 刷新按钮图标旋转动画
    val rotation by rememberInfiniteTransition(label = "scan_rotate").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "rotate"
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "连接",
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(
                        onClick = { vm.wifiList.startScan() },
                        enabled = !isScanning,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "扫描",
                            modifier = if (isScanning) Modifier.rotate(rotation) else Modifier,
                            tint = if (isScanning)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                ),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 扫描状态提示卡（仅在有错误或首次加载前展示简短状态）
                AnimatedVisibility(
                    visible = scanStatus == ScanStatus.ERROR_UNKNOWN
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WifiOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "扫描失败，请重试",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }

                // WiFi 列表
                if (scanResults.isEmpty()) {
                    EmptyState(
                        status = scanStatus,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = scanResults.sortedByDescending { it.level },
                            key = { it.BSSID ?: it.hashCode().toString() },
                        ) { ap ->
                            WifiItem(
                                ap = ap,
                                onClick = { selectedAp = ap },
                                modifier = Modifier.animateItem(),
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }

            // 悬浮进度条：扫描中时显示在列表顶部
            AnimatedVisibility(
                visible = isScanning,
                modifier = Modifier.align(Alignment.TopCenter),
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(300)),
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
        }

        // 详情 Sheet
        if (selectedAp != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedAp = null },
                sheetState = sheetState,
                contentWindowInsets = { WindowInsets(0) },
            ) {
                val view = LocalView.current
                DisposableEffect(Unit) {
                    val window = (view.parent as? DialogWindowProvider)?.window
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window?.isNavigationBarContrastEnforced = false
                    }
                    onDispose { }
                }
                ApDetailSheet(ap = selectedAp!!)
            }
        }
    }
}

@Composable
private fun WifiItem(
    ap: ScanResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val signalLevel = WifiSignalLevel.from(ap.level)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 信号图标 + 强度色块
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(signalLevel.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Wifi,
                    contentDescription = null,
                    tint = signalLevel.color,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ap.SSID?.takeIf { it.isNotEmpty() } ?: "(隐藏网络)",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = ap.BSSID ?: "未知",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${ap.level} dBm",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = signalLevel.color,
                )
                Text(
                    text = "${ap.frequency} MHz",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun ApDetailSheet(ap: ScanResult) {
    val signalLevel = WifiSignalLevel.from(ap.level)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(signalLevel.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Wifi,
                    contentDescription = null,
                    tint = signalLevel.color,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = ap.SSID?.takeIf { it.isNotEmpty() } ?: "(隐藏网络)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = signalLevel.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = signalLevel.color,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        DetailRow(label = "BSSID", value = ap.BSSID ?: "未知")
        DetailRow(label = "信号强度", value = "${ap.level} dBm")
        DetailRow(
            label = "频率",
            value = "${ap.frequency} MHz (${if (ap.frequency < 3000) "2.4GHz" else "5GHz"})",
        )
        DetailRow(label = "信道宽度", value = ap.channelWidth.toChannelWidthLabel())
        @Suppress("DEPRECATION")
        DetailRow(label = "加密方式", value = ap.capabilities ?: "未知")
        if (ap.operatorFriendlyName?.isNotEmpty() == true) {
            DetailRow(label = "运营商", value = ap.operatorFriendlyName.toString())
        }
        if (ap.venueName?.isNotEmpty() == true) {
            DetailRow(label = "场所", value = ap.venueName.toString())
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyState(status: ScanStatus, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.WifiFind,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outlineVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = when (status) {
                ScanStatus.SCANNING -> "扫描中，结果将在此显示..."
                ScanStatus.ERROR_UNKNOWN -> "扫描失败，请重试"
                else -> "点击右上角「刷新」获取附近 WiFi"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

// ---- 辅助类 ----

private enum class WifiSignalLevel(val color: Color, val label: String) {
    EXCELLENT(Color(0xFF2E7D32), "信号极佳"),
    GOOD(Color(0xFF558B2F), "信号良好"),
    FAIR(Color(0xFFF57F17), "信号一般"),
    WEAK(Color(0xFFBF360C), "信号较弱"),
    NONE(Color(0xFF9E9E9E), "无信号");

    companion object {
        fun from(rssi: Int) = when {
            rssi >= -55 -> EXCELLENT
            rssi >= -65 -> GOOD
            rssi >= -75 -> FAIR
            rssi >= -85 -> WEAK
            else -> NONE
        }
    }
}

private fun Int.toChannelWidthLabel() = when (this) {
    0 -> "20 MHz"
    1 -> "40 MHz"
    2 -> "80 MHz"
    3 -> "160 MHz"
    4 -> "80+80 MHz"
    else -> "未知"
}