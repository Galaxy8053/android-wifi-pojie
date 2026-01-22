package com.wifi.toolbox.ui.screen.test

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wifi.toolbox.utils.ShizukuUtil
import com.wifi.toolbox.ui.items.*
import com.wifi.toolbox.utils.LogState
import com.wifi.toolbox.utils.ShizukuUtil.REQUEST_PERMISSION_CODE
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.*

inline fun shizukuAction(logState: LogState, errorPrefix: String, action: () -> Unit) {
    try {
        action()
    } catch (e: Exception) {
        logState.addLog("E: $errorPrefix")
        logState.addLog(e.stackTraceToString())
    }
}

@Composable
fun ShizukuTest(logState: LogState, modifier: Modifier = Modifier) {
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ShizukuProvider.enableMultiProcessSupport(true)
        }
    }

    val requestPermissionResultListener = remember(logState) {
        Shizuku.OnRequestPermissionResultListener { code, grantResult ->
            if (code == REQUEST_PERMISSION_CODE) {
                val granted = grantResult == PackageManager.PERMISSION_GRANTED
                logState.addLog("权限申请结果：${if (granted) "同意" else "拒绝"}")
            }
        }
    }

    DisposableEffect(Unit) {
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
        onDispose { Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener) }
    }

    fun checkStatus() {
        logState.addLog("--- 检查Shizuku状态 ---")
        try {
            logState.addLog("服务已启动: ${Shizuku.pingBinder()}")
            val granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            logState.addLog("已授权: $granted")
            if (granted) {
                try {
                    logState.addLog("UID: ${Shizuku.getUid()}")
                } catch (_: IllegalStateException) {
                    logState.addLog("E: 获取UID失败")
                }
            }
        } catch (e: IllegalStateException) {
            logState.addLog("E: 检查状态失败: ${e.message}")
        }
    }

    fun requestPermission() {
        logState.addLog("--- 申请权限 ---")
        try {
            if (Shizuku.isPreV11()) {
                logState.addLog("E: 不支持Shizuku pre-v11")
            } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                logState.addLog("权限已经授予，无需重复申请")
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                logState.addLog("当前已被始终拒绝")
            } else {
                Shizuku.requestPermission(REQUEST_PERMISSION_CODE)
                logState.addLog("申请已发送")
            }
        } catch (e: IllegalStateException) {
            logState.addLog("E: 申请权限失败: ${e.message}")
        }
    }

    LazyColumn {
        item {
            Column(modifier = modifier
                .fillMaxSize()
                .padding(16.dp)) {
                SectionTitle(title = "权限", icon = Icons.Default.Key)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionChip(
                        text = "检查状态",
                        icon = Icons.Filled.Search,
                        onClick = { checkStatus() })
                    ActionChip(
                        text = "申请权限",
                        icon = Icons.Filled.Security,
                        onClick = { requestPermission() })
                }

                SectionDivider()

                SectionTitle(title = "设备控制", icon = Icons.Default.Devices)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionChip("打开wifi", Icons.Filled.Wifi) {
                        shizukuAction(
                            logState,
                            "打开wifi失败"
                        ) { ShizukuUtil.setWifiEnabled(true); logState.addLog("请求已发送") }
                    }
                    ActionChip("关闭wifi", Icons.Filled.WifiOff) {
                        shizukuAction(
                            logState,
                            "关闭wifi失败"
                        ) { ShizukuUtil.setWifiEnabled(false); logState.addLog("请求已发送") }
                    }
                    ActionChip("扫描wifi", Icons.Filled.Radar) {
                        scope.launch {
                            shizukuAction(logState, "扫描wifi失败") {
                                if (ShizukuUtil.startWifiScan()) logState.addLog("请求已发送，3秒后获取结果") else logState.addLog(
                                    "W: 请求发送失败"
                                )
                                delay(3000)
                                val result = ShizukuUtil.getWifiScanResults()
                                logState.addLog("=== 扫描结果 ===")
                                result.forEach {
                                    logState.addLog(
                                        String.format(
                                            "名称: %-16s 信号强度: %-8s 支持的协议: %s",
                                            it.ssid,
                                            it.level,
                                            it.capabilities
                                        )
                                    )
                                }
                                logState.addLog("===============")
                            }
                        }
                    }
                    ActionChip("获取已保存wifi", Icons.Outlined.Dns) {
                        shizukuAction(logState, "获取失败") {
                            val result = ShizukuUtil.getSavedWifiList()
                            logState.addLog("=== 已保存的wifi列表 ===")
                            result.forEach {
                                @Suppress("DEPRECATION") logState.addLog(
                                    String.format(
                                        "ID: %-4s 名称: %-16s 密码: %s",
                                        it.networkId,
                                        it.SSID.removeSurrounding("\""),
                                        it.preSharedKey?.removeSurrounding("\"") ?: ""
                                    )
                                )
                            }
                            logState.addLog("===============")
                        }
                    }
                    ActionChip("断开wifi", Icons.Filled.WifiOff) {
                        shizukuAction(
                            logState,
                            "断开失败"
                        ) { ShizukuUtil.disconnectWifi(); logState.addLog("请求已发送") }
                    }
                    ActionChip("锁屏", Icons.Filled.Lock) {
                        shizukuAction(
                            logState,
                            "锁屏失败"
                        ) { ShizukuUtil.lookScreen(); logState.addLog("请求已发送") }
                    }
                    ActionChip("调整音量最大", Icons.AutoMirrored.Filled.VolumeUp) {
                        shizukuAction(
                            logState,
                            "调整音量失败"
                        ) { ShizukuUtil.setMediaVolumeMax(); logState.addLog("请求已发送") }
                    }
                }

                SectionDivider()

                SectionTitle(title = "连接wifi", icon = Icons.Default.InsertLink)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("名称") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("密码") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            shizukuAction(logState, "执行Shell命令失败") {
                                val command = "cmd wifi connect-network $name wpa2 $password"
                                logState.addLog("执行Shell命令：$command")
                                logState.addLog(
                                    "请求已发送（响应：${
                                        ShizukuUtil.executeCommandSync(
                                            command
                                        )
                                    }）"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                    ) { Text("命令行（cmd wifi connect-network）") }
                    Button(
                        onClick = {
                            shizukuAction(
                                logState,
                                "连接wifi失败"
                            ) {
                                ShizukuUtil.connectToWifi(
                                    name,
                                    password
                                ); logState.addLog("请求已发送")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                    ) { Text("系统隐藏API（IWifiManager）") }
                }
            }
        }
    }
}