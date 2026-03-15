@file:Suppress("DEPRECATION")

package io.github.bszapp.wifitoolbox.uidefault.widget

import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.bszapp.wifitoolbox.contract.wifilist.ScanStatus
import io.github.bszapp.wifitoolbox.uidefault.DefaultViewModel
import io.github.bszapp.wifitoolbox.uidefault.component.ActionButtonConfig
import io.github.bszapp.wifitoolbox.uidefault.component.ActionButtonGroupWithMenu
import io.github.bszapp.wifitoolbox.uidefault.component.MenuGroupConfig
import io.github.bszapp.wifitoolbox.uidefault.component.MenuItemConfig
import io.github.bszapp.wifitoolbox.uidefault.component.WifiIcon

private enum class ListUiState { LOADING, EMPTY, CONTENT }

/**
 * 逻辑 Wi-Fi 分组：同 SSID 的多个 [ScanResult] 合并为一组。
 * 隐藏网络（SSID 为空）统一归为一组。
 */
data class MergedWifiGroup(
    val networks: List<ScanResult>
) {
    val strongest: ScanResult get() = networks.first()
    val weakest: ScanResult get() = networks.last()
    val isMerged: Boolean get() = networks.size > 1

    val displaySsid: String
        get() = strongest.SSID
            ?.takeIf { it.isNotEmpty() }
            ?: "<隐藏的网络>"

    val signalDisplay: String
        get() = if (isMerged) "${strongest.level}dBm ~ ${weakest.level}dBm"
        else "${strongest.level}dBm"
}

// ── 顶层 Composable ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WifiList(
    modifier: Modifier = Modifier,
    vm: DefaultViewModel = viewModel(),
    listState: LazyListState = rememberLazyListState()
) {
    val scanResults by vm.wifiList.results.collectAsStateWithLifecycle()
    val scanStatus by vm.wifiList.status.collectAsStateWithLifecycle()
    val isScanning = scanStatus == ScanStatus.SCANNING
    var selectedGroup by rememberSaveable { mutableStateOf<MergedWifiGroup?>(null) }

    // 每次 scanResults 变化时重建分组
    val groups = remember(scanResults) { buildGroups(scanResults) }

    // 决定当前应显示哪种状态
    val uiState: ListUiState = when {
        isScanning && groups.isEmpty() -> ListUiState.LOADING
        !isScanning && groups.isEmpty() -> ListUiState.EMPTY
        else -> ListUiState.CONTENT
    }

    Column(modifier = modifier.fillMaxSize()) {

        AnimatedContent(
            targetState = uiState,
            label = "WifiListContent",
            modifier = Modifier.fillMaxSize()
        ) { state ->
            when (state) {

                // ── 加载中：居中转圈 ──────────────────────────────────────
                ListUiState.LOADING -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ContainedLoadingIndicator(Modifier.size(60.dp))
                    }
                }

                // ── 空列表：图标 + 提示文字 ───────────────────────────────
                ListUiState.EMPTY -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Inbox,
                                contentDescription = null,
                                modifier = Modifier.size(96.dp),
                                tint = MaterialTheme.colorScheme.outlineVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "空空如也",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── 有内容：卡片列表 ─────────────────────────────────────
                ListUiState.CONTENT -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            items = groups,
                            key = { it.strongest.BSSID ?: it.hashCode() }
                        ) { group ->
                            WifiGroupCard(
                                group = group,
                                modifier = Modifier.animateItem(),
                                onClick = { selectedGroup = group }
                            )
                        }
                    }
                }
            }
        }
    }

    // 详情底部弹窗
    selectedGroup?.let { group ->
        WifiDetailSheet(group = group, onDismiss = { selectedGroup = null })
    }
}

// ── 分组辅助函数 ───────────────────────────────────────────────────────────────

private fun buildGroups(results: List<ScanResult>): List<MergedWifiGroup> {
    val visible = results.filter { !it.SSID.isNullOrEmpty() }
    val hidden = results.filter { it.SSID.isNullOrEmpty() }

    val mergedVisible = visible
        .groupBy { it.SSID!! }
        .values
        .map { group -> MergedWifiGroup(group.sortedByDescending { it.level }) }

    val mergedHidden = if (hidden.isNotEmpty()) {
        listOf(MergedWifiGroup(hidden.sortedByDescending { it.level }))
    } else emptyList()

    return (mergedVisible + mergedHidden).sortedByDescending { it.strongest.level }
}

// ── 单个 Wi-Fi 卡片 ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WifiGroupCard(
    group: MergedWifiGroup,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val levelIndex = if (group.strongest.level == 0) 0
    else WifiManager.calculateSignalLevel(group.strongest.level, 5)
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            WifiIcon(
                modifier = Modifier.size(28.dp),
                level = levelIndex
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = group.displaySsid,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (group.isMerged) {
                        Spacer(Modifier.width(4.dp))
                        MergedBadge(count = group.networks.size)
                    }
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = group.strongest.BSSID,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = group.signalDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(12.dp))

            var favoriteChecked by remember { mutableStateOf(false) }

            val menuGroups = listOf(
                MenuGroupConfig(
                    icon = null,
                    title = null,
                    items = listOf(
                        MenuItemConfig(
                            title = "新窗口打开",
                            icon = Icons.Outlined.OpenInNew,
                            onCheckedChange = { /* TODO */ },
                        ),
                    ),
                ),
                MenuGroupConfig(
                    icon = Icons.Outlined.Tune,
                    title = "选项",
                    items = listOf(
                        MenuItemConfig(
                            title = "分享",
                            icon = Icons.Outlined.Share,
                            checkedIcon = Icons.Filled.Share,
                            onCheckedChange = { /* TODO */ },
                        ),
                        MenuItemConfig(
                            title = "收藏",
                            icon = Icons.Outlined.FavoriteBorder,
                            checkedIcon = Icons.Filled.Favorite,
                            checked = favoriteChecked,
                            onCheckedChange = { favoriteChecked = it },
                        ),
                    ),
                ),
            )

            ActionButtonGroupWithMenu(
                buttonConfig = ActionButtonConfig(
                    icon = Icons.Outlined.Link,
                    text = "连接",
                    onClick = { /* TODO */ },
                ),
                menuGroups = menuGroups,
            )

        }
    }
}

// ── 合并角标 ──────────────────────────────────────────────────────────────────

@Composable
private fun MergedBadge(count: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = "合并网络",
                modifier = Modifier.size(11.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ── 详情底部弹窗 ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WifiDetailSheet(
    group: MergedWifiGroup,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = group.displaySsid,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${group.networks.size} 个接入点",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            HorizontalDivider()

            group.networks.forEachIndexed { index, ap ->
                ApDetailRow(ap = ap)
                if (index < group.networks.lastIndex) {
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}

// ── 单条 AP 详情行 ─────────────────────────────────────────────────────────────

@Composable
private fun ApDetailRow(ap: ScanResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ap.BSSID ?: "<Unknown>",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "${ap.level} dBm",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = signalColor(ap.level)
            )
        }
        Text(
            text = "Freq: ${ap.frequency} MHz | Caps: ${ap.capabilities}",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

// ── 工具函数 ──────────────────────────────────────────────────────────────────

@Composable
private fun signalColor(level: Int): Color = when {
    level >= -50 -> MaterialTheme.colorScheme.primary   // 优秀
    level >= -70 -> MaterialTheme.colorScheme.tertiary  // 一般
    else -> MaterialTheme.colorScheme.error     // 差
}