@file:Suppress("DEPRECATION")

package io.github.bszapp.wifitoolbox.uidefault.widget

import android.net.wifi.ScanResult
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.bszapp.wifitoolbox.contract.wifilist.ScanStatus
import io.github.bszapp.wifitoolbox.uidefault.model.DefaultViewModel
import io.github.bszapp.wifitoolbox.uidefault.component.ActionButtonConfig
import io.github.bszapp.wifitoolbox.uidefault.component.ActionButtonGroupWithMenu
import io.github.bszapp.wifitoolbox.uidefault.component.MenuGroupConfig
import io.github.bszapp.wifitoolbox.uidefault.component.MenuItemConfig
import io.github.bszapp.wifitoolbox.uidefault.component.TagItem
import io.github.bszapp.wifitoolbox.uidefault.component.TagStyle
import io.github.bszapp.wifitoolbox.uidefault.component.WifiIcon
import io.github.bszapp.wifitoolbox.uidefault.model.MergedWifiGroup
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiEnterpriseConfig
import android.os.Build
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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
    var selectedGroup by rememberSaveable { mutableStateOf<MergedWifiGroup?>(null) }

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
                .verticalScroll(rememberScrollState())
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

            if (group.savedWifiList.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "已保存的配置 (${group.savedWifiList.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                group.savedWifiList.forEachIndexed { index, config ->
                    SavedWifiConfigRow(config = config)
                    if (index < group.savedWifiList.lastIndex) {
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun SavedWifiConfigRow(config: WifiConfiguration) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 标题行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = config.SSID?.trim('"') ?: "<未知>",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "ID: ${config.networkId}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 密码
        if (!config.preSharedKey.isNullOrEmpty()) {
            SavedInfoRow("密码", config.preSharedKey!!.trim('"'))
        }

        // WEP 密钥
        config.wepKeys?.forEachIndexed { i, key ->
            if (!key.isNullOrEmpty()) {
                SavedInfoRow("WEP Key $i", key)
            }
        }

        // BSSID
        if (!config.BSSID.isNullOrEmpty()) {
            SavedInfoRow("BSSID", config.BSSID!!)
        }

        // 安全类型 / KeyMgmt
        val keyMgmtBits = buildList {
            if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE)) add("NONE")
            if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)) add("WPA_PSK")
            if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP)) add("WPA_EAP")
            if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X)) add("IEEE8021X")
            if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA2_PSK)) add("WPA2_PSK")
            if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.SAE)) add("SAE(WPA3)")
            if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.OWE)) add("OWE")
            if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.SUITE_B_192)) add("SUITE_B_192")
        }
        if (keyMgmtBits.isNotEmpty()) {
            SavedInfoRow("认证方式", keyMgmtBits.joinToString(" / "))
        }

        // 协议
        val protocols = buildList {
            if (config.allowedProtocols.get(WifiConfiguration.Protocol.WPA)) add("WPA")
            if (config.allowedProtocols.get(WifiConfiguration.Protocol.RSN)) add("RSN(WPA2/3)")
        }
        if (protocols.isNotEmpty()) {
            SavedInfoRow("协议", protocols.joinToString(" / "))
        }

        // 成对加密
        val pairwise = buildList {
            if (config.allowedPairwiseCiphers.get(WifiConfiguration.PairwiseCipher.TKIP)) add("TKIP")
            if (config.allowedPairwiseCiphers.get(WifiConfiguration.PairwiseCipher.CCMP)) add("CCMP(AES)")
            if (config.allowedPairwiseCiphers.get(WifiConfiguration.PairwiseCipher.GCMP_256)) add("GCMP-256")
        }
        if (pairwise.isNotEmpty()) {
            SavedInfoRow("单播加密", pairwise.joinToString(" / "))
        }

        // 组播加密
        val group = buildList {
            if (config.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.TKIP)) add("TKIP")
            if (config.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.CCMP)) add("CCMP")
            if (config.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.WEP40)) add("WEP40")
            if (config.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.WEP104)) add("WEP104")
            if (config.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.GCMP_256)) add("GCMP-256")
        }
        if (group.isNotEmpty()) {
            SavedInfoRow("组播加密", group.joinToString(" / "))
        }

        // 隐藏网络
        SavedInfoRow("隐藏网络", if (config.hiddenSSID) "是" else "否")

        // MAC 随机化
        val macMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (config.macRandomizationSetting) {
                WifiConfiguration.RANDOMIZATION_NONE -> "关闭 (使用出厂MAC)"
                WifiConfiguration.RANDOMIZATION_PERSISTENT -> "持久随机"
                WifiConfiguration.RANDOMIZATION_NON_PERSISTENT -> "每次随机"
                WifiConfiguration.RANDOMIZATION_AUTO -> "自动"
                else -> "未知(${config.macRandomizationSetting})"
            }
        } else {
            TODO("VERSION.SDK_INT < TIRAMISU")
        }
        SavedInfoRow("MAC 随机化", macMode)

        // 随机 MAC 地址
        SavedInfoRow("随机 MAC", config.randomizedMacAddress.toString())
    }
}

@Composable
private fun SavedInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.38f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.62f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
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