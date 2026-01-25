package com.wifi.toolbox.ui.screen.test

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wifi.toolbox.R
import com.wifi.toolbox.utils.ShizukuUtil
import com.wifi.toolbox.ui.items.*
import com.wifi.toolbox.ui.screen.testAction
import com.wifi.toolbox.utils.LogState
import com.wifi.toolbox.utils.ShizukuUtil.REQUEST_PERMISSION_CODE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.*

@Composable
fun ShizukuTest(logState: LogState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
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
                logState.addLog(
                    context.getString(
                        R.string.Permission_result,
                        if (granted) context.getString(R.string.granted)
                        else context.getString(R.string.refuse)
                    )
                )
            }
        }
    }

    DisposableEffect(Unit) {
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
        onDispose { Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener) }
    }

    fun checkStatus() {
        logState.addLog(context.getString(R.string.check_shizuku_status_tip))
        try {
            logState.addLog(context.getString(R.string.service_running, Shizuku.pingBinder()))
            val granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            logState.addLog(context.getString(R.string.is_granted_string, granted))
            if (granted) {
                try {
                    logState.addLog(context.getString(R.string.uid_string, Shizuku.getUid()))
                } catch (_: IllegalStateException) {
                    logState.addLog(context.getString(R.string.get_uid_failed))
                }
            }
        } catch (e: IllegalStateException) {
            logState.addLog(context.getString(R.string.check_status_failed, e.message))
        }
    }

    fun requestPermission() {
        logState.addLog(context.getString(R.string.apply_permission_tip))
        try {
            if (Shizuku.isPreV11()) {
                logState.addLog(context.getString(R.string.shizuku_pre_v11_not_supported))
            } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                logState.addLog(context.getString(R.string.permission_already_granted_tip))
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                logState.addLog(context.getString(R.string.permission_always_refuse))
            } else {
                Shizuku.requestPermission(REQUEST_PERMISSION_CODE)
                logState.addLog(context.getString(R.string.request_sent))
            }
        } catch (e: IllegalStateException) {
            logState.addLog(context.getString(R.string.apply_permission_failed_tip, e.message))
        }
    }

    LazyColumn {
        item {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                SectionTitle(title = stringResource(R.string.permission), icon = Icons.Default.Key)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionChip(
                        text = stringResource(R.string.check_status),
                        icon = Icons.Filled.Search,
                        onClick = { checkStatus() })
                    ActionChip(
                        text = stringResource(R.string.apply_permission),
                        icon = Icons.Filled.Security,
                        onClick = { requestPermission() })
                }

                SectionDivider()

                SectionTitle(
                    title = stringResource(R.string.device_control),
                    icon = Icons.Default.Devices
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionChip(stringResource(R.string.enable_wifi), Icons.Filled.Wifi) {
                        testAction(
                            context,
                            logState,
                            context.getString(R.string.enable_wifi_failed)
                        ) { ShizukuUtil.setWifiEnabled(true); logState.addLog(context.getString(R.string.request_sent)) }
                    }
                    ActionChip(stringResource(R.string.disable_wifi), Icons.Filled.WifiOff) {
                        testAction(
                            context,
                            logState,
                            context.getString(R.string.disable_wifi_failed)
                        ) { ShizukuUtil.setWifiEnabled(false); logState.addLog(context.getString(R.string.request_sent)) }
                    }
                    ActionChip(stringResource(R.string.scan_wifi), Icons.Filled.Radar) {
                        scope.launch {
                            testAction(
                                context, logState, context.getString(R.string.scan_wifi_failed)
                            ) {
                                if (ShizukuUtil.startWifiScan()) logState.addLog(context.getString(R.string.start_scan_success_later))
                                else logState.addLog(context.getString(R.string.start_scan_failed_warning))
                                delay(3000)
                                val result = ShizukuUtil.getWifiScanResults()
                                logState.addLog(context.getString(R.string.scan_result_head))
                                result.forEach {
                                    logState.addLog(
                                        context.getString(R.string.scan_result_item, it.ssid, it.level, it.capabilities)
                                    )
                                    logState.addLog(it.bssid)
                                }
                                logState.addLog(context.getString(R.string.command_end))
                            }
                        }
                    }
                    ActionChip(stringResource(R.string.get_saved_wifi), Icons.Outlined.Dns) {
                        testAction(
                            context, logState, context.getString(R.string.get_failed)
                        ) {
                            val result = ShizukuUtil.getSavedWifiList()
                            logState.addLog(context.getString(R.string.saved_wifi_list_head))
                            result.forEach {
                                @Suppress("DEPRECATION") logState.addLog(
                                    context.getString(
                                        R.string.saved_wifi_item_with_psk,
                                        it.networkId.toString(),
                                        it.SSID.removeSurrounding("\""),
                                        it.preSharedKey?.removeSurrounding("\"") ?: ""
                                    )
                                )
                            }
                            logState.addLog(context.getString(R.string.command_end))
                        }
                    }
                    ActionChip(stringResource(R.string.disconnect_wifi), Icons.Filled.WifiOff) {
                        testAction(
                            context,
                            logState,
                            context.getString(R.string.disconnect_failed)
                        ) { ShizukuUtil.disconnectWifi(); logState.addLog(context.getString(R.string.request_sent)) }
                    }
                    ActionChip(stringResource(R.string.lock_screen), Icons.Filled.Lock) {
                        testAction(
                            context,
                            logState,
                            context.getString(R.string.lock_screen_failed)
                        ) { ShizukuUtil.lookScreen(); logState.addLog(context.getString(R.string.request_sent)) }
                    }
                }

                SectionDivider()

                SectionTitle(title = stringResource(R.string.connect_wifi), icon = Icons.Default.InsertLink)
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
                            label = { Text(stringResource(R.string.ssid)) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.password)) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            testAction(
                                context, logState, context.getString(R.string.execute_shell_failed)
                            ) {
                                val command = "cmd wifi connect-network $name wpa2 $password"
                                logState.addLog(context.getString(R.string.run_command_string, command))
                                logState.addLog(
                                    context.getString(R.string.request_sent_with_response, ShizukuUtil.executeCommandSync(command))
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                    ) { Text(stringResource(R.string.btn_cmd_connect)) }
                    Button(
                        onClick = {
                            testAction(
                                context,
                                logState,
                                context.getString(R.string.connect_wifi_failed_general)
                            ) {
                                ShizukuUtil.connectToWifi(name, password)
                                logState.addLog(context.getString(R.string.request_sent))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                    ) { Text(stringResource(R.string.btn_hidden_api_connect)) }
                }
            }
        }
    }
}