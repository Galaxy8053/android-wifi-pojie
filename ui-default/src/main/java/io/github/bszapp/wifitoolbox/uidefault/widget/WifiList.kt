@file:Suppress("DEPRECATION")

package io.github.bszapp.wifitoolbox.uidefault.widget

import android.net.wifi.WifiManager
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.bszapp.wifitoolbox.contract.wifilist.ScanStatus
import io.github.bszapp.wifitoolbox.uidefault.component.TagItem
import io.github.bszapp.wifitoolbox.uidefault.component.TagStyle
import io.github.bszapp.wifitoolbox.uidefault.component.WifiIcon
import io.github.bszapp.wifitoolbox.uidefault.model.DefaultViewModel
import io.github.bszapp.wifitoolbox.uidefault.model.MergedWifiGroup
import io.github.bszapp.wifitoolbox.uidefault.widget.wifilist.WifiDetailSheet
import io.github.bszapp.wifitoolbox.uidefault.widget.wifilist.WifiGroupCardActions

private enum class ListUiState { LOADING, EMPTY, CONTENT }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WifiList(
    modifier: Modifier = Modifier,
    vm: DefaultViewModel = viewModel(),
    listState: LazyListState = rememberLazyListState()
) {
    val scanResults by vm.wifiList.results.collectAsStateWithLifecycle()
    val savedWifiList by vm.wifiList.savedWifiList.collectAsStateWithLifecycle()
    val scanStatus by vm.wifiList.status.collectAsStateWithLifecycle()
    val isScanning = scanStatus == ScanStatus.SCANNING
    var selectedIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    // 每次 scanResults 变化时重建分组
    val groups = remember(scanResults, savedWifiList) {
        MergedWifiGroup.buildFrom(scanResults, savedWifiList)
    }

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
                        itemsIndexed(
                            items = groups,
                            key = { _, item -> item.strongest.BSSID ?: item.hashCode() }
                        ) { index, group ->
                            WifiGroupCard(
                                group = group,
                                modifier = Modifier.animateItem(),
                                onClick = { selectedIndex = index }
                            )
                        }
                    }
                }
            }
        }
    }

    // 详情底部弹窗
    selectedIndex?.let { index ->
        groups.getOrNull(index)?.let { group ->
            WifiDetailSheet(group = group, onDismiss = { selectedIndex = null })
        }
    }
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
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
                        overflow = TextOverflow.Visible,
                        softWrap = true,
                    )

                    val hasTags = group.networks.size > 1 || group.savedWifiList.isNotEmpty()
                    if (hasTags) {
                        Spacer(Modifier.width(4.dp))
                        if (group.networks.size > 1) {
                            TagItem(
                                text = group.networks.size.toString(),
                                icon = Icons.Default.Layers,
                                style = TagStyle.Tertiary
                            )
                        }
                        if (group.savedWifiList.isNotEmpty()) {
                            TagItem(
                                text = "已保存",
                                style = TagStyle.Primary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = group.signalDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(12.dp))
            WifiGroupCardActions(
                group = group,
                onConnect = { /* TODO */ },
                onOpenDetail = { onClick() },   // 或者单独触发详情 sheet
                onConnectWithConfig = { /* TODO */ },
            )

        }
    }
}
