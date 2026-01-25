package com.wifi.toolbox.ui.screen

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wifi.toolbox.ToolboxApp
import com.wifi.toolbox.ui.LocalNavTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

data class NetworkState(
    val wifiSsid: String = "未连接",
    val ipList: List<String> = emptyList(),
    val isWifiConnected: Boolean = false
)

@Composable
fun HomeScreen(onMenuClick: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ToolboxApp
    val navTarget = LocalNavTarget.current
    val scrollState = rememberScrollState()

    val isDark = MaterialTheme.colorScheme.background.run {
        val luminance = (red * 0.2126f + green * 0.7152f + blue * 0.0722f)
        luminance < 0.5f
    }

    val networkState by produceState(initialValue = NetworkState()) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        suspend fun update() {
            val newState = withContext(Dispatchers.IO) {
                val wifiManager =
                    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val activeNetwork = cm.activeNetwork
                val caps = cm.getNetworkCapabilities(activeNetwork)

                var ssid = "未连接"
                var isWifi = false

                if (caps != null) {
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        isWifi = true
                        val info = wifiManager.connectionInfo
                        val rawSsid = info.ssid
                        ssid = if (rawSsid == "<unknown ssid>") "WiFi 已连接" else rawSsid.trim('"')
                    } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        ssid = "移动数据网络"
                    }
                }
                NetworkState(ssid, getAllIpAddresses(), isWifi)
            }
            value = newState
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                launch { update() }
            }

            override fun onLost(network: Network) {
                launch { update() }
            }

            override fun onCapabilitiesChanged(network: Network, c: NetworkCapabilities) {
                launch { update() }
            }

            override fun onLinkPropertiesChanged(network: Network, l: LinkProperties) {
                launch { update() }
            }
        }

        update()
        cm.registerDefaultNetworkCallback(callback)

        awaitDispose {
            cm.unregisterNetworkCallback(callback)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HomeTopBar(onMenuClick) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            AnimatedVisibility(
                visible = true,
                enter = slideInVertically() + fadeIn()
            ) {
                InfoCard(networkState, isDark)
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                val isAidlAlive = app.aidl.ipc != null
                AnimatedVisibility(
                    visible = isAidlAlive,
                    enter = expandVertically(tween(300)) + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    AidlStatusCard(isDark)
                }

                Text(
                    text = "快捷操作",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HomeCardItem(
                            modifier = Modifier.weight(1f),
                            title = "密码字典破解",
                            icon = Icons.Rounded.VpnKey,
                            baseColor = Color(0xFFFFD8E4),
                            darkColor = Color(0xFF5E2A38),
                            contentColor = if (isDark) Color(0xFFFFD8E4) else Color(0xFF6E2838),
                            isDark = isDark,
                            onClick = { navTarget.value = "Pojie" }
                        )
                        HomeCardItem(
                            modifier = Modifier.weight(1f),
                            title = "wifi管理器",
                            icon = Icons.Rounded.Dns,
                            baseColor = Color(0xFFFDE495),
                            darkColor = Color(0xFF4A3E15),
                            contentColor = if (isDark) Color(0xFFFDE495) else Color(0xFF5C4912),
                            isDark = isDark,
                            onClick = { navTarget.value = "Viewer" }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HomeCardItem(
                            modifier = Modifier.weight(1f),
                            title = "实验室",
                            icon = Icons.Rounded.Science,
                            baseColor = Color(0xFFF2D9FA),
                            darkColor = Color(0xFF3E1C4A),
                            contentColor = if (isDark) Color(0xFFF2D9FA) else Color(0xFF4A148C),
                            isDark = isDark,
                            isHorizontal = true,
                            onClick = { navTarget.value = "Test" }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HomeCardItem(
                            modifier = Modifier.weight(1f),
                            title = "设置",
                            icon = Icons.Rounded.Settings,
                            baseColor = Color(0xFFFFE0C8),
                            darkColor = Color(0xFF4A2C20),
                            contentColor = if (isDark) Color(0xFFFFE0C8) else Color(0xFF5D4037),
                            isDark = isDark,
                            isHorizontal = true,
                            onClick = { navTarget.value = "Settings" }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun HomeTopBar(onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Rounded.Menu, contentDescription = "Menu")
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = "工具箱",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun InfoCard(state: NetworkState, isDark: Boolean) {
    val containerColor = if (isDark) {
        Color(0xFF284893).copy(alpha = 0.15f)
    } else {
        Color(0xFFE3EAFC)
    }

    val textColor = if (isDark) Color(0xFFE3EAFC) else Color(0xFF1A1A1A)
    val subTextColor = if (isDark) Color(0xFFAAAAAA) else Color(0xFF555555)
    val iconTint = if (isDark) Color(0xFF6495ED) else Color(0xFF284893)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (state.isWifiConnected) Icons.Rounded.Wifi else Icons.Rounded.SignalCellularAlt,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "网络状态",
                    style = MaterialTheme.typography.labelLarge,
                    color = iconTint,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = state.wifiSsid,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(
                color = if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFCBD5E1),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (state.ipList.isEmpty()) {
                    Text(
                        "无活跃 IP 地址",
                        style = MaterialTheme.typography.bodyMedium,
                        color = subTextColor
                    )
                } else {
                    state.ipList.forEach { ip ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Lan,
                                contentDescription = null,
                                tint = subTextColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = ip,
                                style = MaterialTheme.typography.bodyMedium,
                                color = subTextColor,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AidlStatusCard(isDark: Boolean) {
    val bgColor = if (isDark) Color(0xFF004D40).copy(alpha = 0.3f) else Color(0xFFE0F2F1)
    val titleColor = if (isDark) Color(0xFF80CBC4) else Color(0xFF00695C)
    val iconColor = if (isDark) Color(0xFF4DB6AC) else Color(0xFF00897B)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "AIDL 服务",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                Text(
                    text = "后台服务正在运行中",
                    style = MaterialTheme.typography.bodySmall,
                    color = titleColor.copy(alpha = 0.8f)
                )
            }
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun HomeCardItem(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    baseColor: Color,
    darkColor: Color,
    contentColor: Color,
    isDark: Boolean,
    isHorizontal: Boolean = false,
    onClick: () -> Unit
) {
    val finalBgColor = if (isDark) darkColor.copy(alpha = 0.4f) else baseColor

    Surface(
        onClick = onClick,
        color = finalBgColor,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.height(if (isHorizontal) 72.dp else 110.dp)
    ) {
        if (isHorizontal) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )
                }
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
            }
        }
    }
}

fun getAllIpAddresses(): List<String> {
    val ipList = mutableListOf<String>()
    try {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (intf in interfaces) {
            if (!intf.isUp) continue

            val addrs = Collections.list(intf.inetAddresses)
            for (addr in addrs) {
                val hostAddr = addr.hostAddress ?: continue

                if (addr is Inet4Address) {
                    ipList.add("$hostAddr (${intf.displayName ?: intf.name})")
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return listOf("获取失败: ${e.message}")
    }
    return ipList.sortedBy {
        when {
            it.startsWith("192") -> 0
            it.startsWith("10.") -> 1
            it.startsWith("172") -> 2
            it.startsWith("127") -> 9
            else -> 5
        }
    }
}