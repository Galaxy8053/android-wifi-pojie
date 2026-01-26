package com.wifi.toolbox.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wifi.toolbox.R
import com.wifi.toolbox.ui.items.*
import com.wifi.toolbox.ui.screen.test.*
import com.wifi.toolbox.utils.LogState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(onMenuClick: () -> Unit) {
    val tabs = listOf(
        stringResource(R.string.shizuku),
        "AIDL服务",
        "应用API",
        stringResource(R.string.terminal_command)
    )

    val scope = rememberCoroutineScope()
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(
        initialPage = selectedTabIndex,
        pageCount = { tabs.size }
    )

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            selectedTabIndex = pagerState.currentPage
        }
    }

    val logState = rememberLogState()
    var logCardExpanded by rememberSaveable { mutableStateOf(true) }

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
                PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = title,
                                    color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            })
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    userScrollEnabled = true
                ) { page ->
                    val pageModifier = Modifier.fillMaxWidth()
                    when (page) {
                        0 -> ShizukuTest(logState = logState, modifier = pageModifier)
                        1 -> AidlTest(logState = logState, modifier = pageModifier)
                        2 -> ApiTest(logState = logState, modifier = pageModifier)
                        3 -> ShellTest(logState = logState, modifier = pageModifier)
                    }
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