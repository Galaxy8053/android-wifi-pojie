package com.wifi.toolbox.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wifi.toolbox.R
import com.wifi.toolbox.ui.items.*
import com.wifi.toolbox.ui.screen.test.*
import com.wifi.toolbox.utils.LogState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(onMenuClick: () -> Unit) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.shizuku),
        "AIDL服务",
        stringResource(R.string.system_api),
        stringResource(R.string.terminal_command)
    )
    val logState = rememberLogState()
    var logCardExpanded by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column(
                    modifier = Modifier.padding(0.dp, 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.lab),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }, navigationIcon = {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu, contentDescription = null
                    )
                }
            })
        }) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            })
                    }
                }

                when (selectedTabIndex) {
                    0 -> ShizukuTest(logState = logState, modifier = Modifier.fillMaxSize())
                    1 -> AidlTest(logState = logState, modifier = Modifier.fillMaxSize())
                    2 -> ApiTest(logState = logState, modifier = Modifier.fillMaxSize())
                    3 -> ShellTest(logState = logState, modifier = Modifier.fillMaxSize())
                }
            }

            FoldCard(
                title = stringResource(R.string.run_output),
                icon = Icons.Rounded.Terminal,
                expanded = logCardExpanded,
                onExpandedChange = { logCardExpanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                LogView(
                    logState = logState,
                    modifier = Modifier
                        .padding(8.dp)
                        .then(if (logCardExpanded) Modifier.fillMaxHeight(0.5f) else Modifier)
                )
            }
        }
    }
}

inline fun testAction(
    context: Context,
    logState: LogState,
    errorPrefix: String,
    action: () -> Unit
) {
    try {
        action()
    } catch (e: Exception) {
        logState.addLog(context.getString(R.string.error_string, errorPrefix))
        logState.addLog(e.stackTraceToString())
    }
}

@Preview
@Composable
fun TestScreenPreview() {
    TestScreen(onMenuClick = {})
}