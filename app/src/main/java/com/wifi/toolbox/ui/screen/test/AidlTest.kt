package com.wifi.toolbox.ui.screen.test

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wifi.toolbox.R
import com.wifi.toolbox.ToolboxApp
import com.wifi.toolbox.ui.items.ActionChip
import com.wifi.toolbox.ui.items.SectionDivider
import com.wifi.toolbox.ui.items.SectionTitle
import com.wifi.toolbox.ui.screen.testAction
import com.wifi.toolbox.utils.LogState

@Composable
fun AidlTest(logState: LogState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = LocalContext.current.applicationContext as ToolboxApp

    fun checkStatus() {
        logState.addLog("服务ipc：${app.aidl.ipc}")
        app.aidl.ipc?.let { logState.addLog("uid：${it.uid}") }
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
                        ) { app.aidl.ipc!!.setWifiEnabled(true); logState.addLog(context.getString(R.string.request_sent)) }
                    }
                    ActionChip(stringResource(R.string.disable_wifi), Icons.Filled.WifiOff) {
                        testAction(
                            context,
                            logState,
                            context.getString(R.string.disable_wifi_failed)
                        ) {
                            app.aidl.ipc!!.setWifiEnabled(false); logState.addLog(
                            context.getString(
                                R.string.request_sent
                            )
                        )
                        }
                    }
                }
            }
        }
    }
}