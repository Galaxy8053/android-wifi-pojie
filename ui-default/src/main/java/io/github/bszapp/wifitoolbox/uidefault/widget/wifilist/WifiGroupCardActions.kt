package io.github.bszapp.wifitoolbox.uidefault.widget.wifilist

import android.net.wifi.WifiConfiguration
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import io.github.bszapp.wifitoolbox.uidefault.R
import io.github.bszapp.wifitoolbox.uidefault.component.ActionButtonConfig
import io.github.bszapp.wifitoolbox.uidefault.component.ActionButtonGroupWithMenu
import io.github.bszapp.wifitoolbox.uidefault.component.MenuGroupConfig
import io.github.bszapp.wifitoolbox.uidefault.component.MenuItemConfig
import io.github.bszapp.wifitoolbox.uidefault.model.MergedWifiGroup
@Composable
fun WifiGroupCardActions(
    group: MergedWifiGroup,
    onConnect: () -> Unit = {},
    onOpenDetail: () -> Unit = {},
    onConnectWithConfig: (WifiConfiguration) -> Unit = {},
) {
    val supportsAutoJoin = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    val menuGroups = buildList {
        add(
            MenuGroupConfig(
                icon = null,
                title = null,
                items = listOf(
                    MenuItemConfig(
                        title = "详细信息",
                        icon = Icons.Outlined.Info,
                        onCheckedChange = { onOpenDetail() },
                    )
                )
            )
        )

        group.savedWifiList.forEach { config ->
            // 启用配置：status 为 ENABLED 或 CURRENT 均属于启用状态
            val isEnabled = config.status == WifiConfiguration.Status.ENABLED
                    || config.status == WifiConfiguration.Status.CURRENT

            // 自动连接：API 30+ 才有 allowAutojoin 字段
            val isAutoJoin = if (supportsAutoJoin) {
                config.getAllowAutojoin()
            } else false

            add(
                MenuGroupConfig(
                    title = "已保存的配置#${config.networkId}",
                    items = buildList {
                        add(MenuItemConfig(
                            title = "使用此配置连接",
                            icon = Icons.Outlined.PlayArrow,
                            onCheckedChange = { onConnectWithConfig(config) },
                        ))
                        add(MenuItemConfig(
                            title = "管理此配置",
                            icon = Icons.Outlined.EditNote,
                            onCheckedChange = { /* TODO */ },
                        ))
                        if (supportsAutoJoin) {
                            add(MenuItemConfig(
                                title = "自动连接",
                                icon = Icons.Outlined.AutoFixHigh,
                                checked = isAutoJoin,
                                onCheckedChange = {},
                            ))
                        }
                        add(MenuItemConfig(
                            title = "启用配置",
                            icon = ImageVector.vectorResource(R.drawable.enable_24),
                            checked = isEnabled,
                            onCheckedChange = {},
                        ))
                    }
                )
            )
        }
    }

    ActionButtonGroupWithMenu(
        buttonConfig = ActionButtonConfig(
            icon = Icons.Outlined.Link,
            text = "连接",
            onClick = onConnect,
        ),
        menuGroups = menuGroups,
    )
}

// 工具函数，放在文件顶部或单独 util 文件
@RequiresApi(Build.VERSION_CODES.R)
private fun WifiConfiguration.getAllowAutojoin(): Boolean = try {
    WifiConfiguration::class.java
        .getField("allowAutojoin")
        .getBoolean(this)
} catch (_: Exception) {
    true // 读取失败时保守默认为 true
}