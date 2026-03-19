package io.github.bszapp.wifitoolbox.uidefault.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.twotone.*
import androidx.compose.material3.*
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.bszapp.wifitoolbox.contract.wifilist.ScanStatus
import io.github.bszapp.wifitoolbox.uidefault.model.DefaultViewModel
import io.github.bszapp.wifitoolbox.uidefault.widget.PullDownScaffold
import io.github.bszapp.wifitoolbox.uidefault.widget.WifiList
import io.github.bszapp.wifitoolbox.uidefault.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(vm: DefaultViewModel = viewModel()) {
    val scanStatus by vm.wifiList.status.collectAsStateWithLifecycle()
    val isScanning = scanStatus == ScanStatus.SCANNING

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val isListAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }

    PullDownScaffold(
        pullDownContent = {
            Text(
                text = "下拉区域",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    ) { isExpanded, onToggle, _, topBarDragModifier ->

        val topBarColor by animateColorAsState(
            targetValue = if (isListAtTop) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceContainer,
            animationSpec = tween(200),
            label = "topBarColor",
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Surface(
                    color = topBarColor,
                    modifier = topBarDragModifier.graphicsLayer { clip = false },
                ) {
                    Box {
                        TopAppBar(
                            title = { Text("连接", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                TooltipBox(
                                    positionProvider = rememberTooltipPositionProvider(
                                        TooltipAnchorPosition.Below
                                    ),
                                    tooltip = { PlainTooltip { Text(if (isExpanded) "收起" else "展开") } },
                                    state = rememberTooltipState(),
                                ) {
                                    IconButton(
                                        onClick = onToggle,
                                        shapes = IconButtonDefaults.shapes()
                                    ) {
                                        Icon(
                                            painter = if (isExpanded) {
                                                painterResource(id = R.drawable.top_panel_close_24)
                                            } else {
                                                rememberVectorPainter(Icons.Rounded.Menu)
                                            },
                                            contentDescription = if (isExpanded) "收起" else "展开",
                                        )
                                    }
                                }
                            },
                            actions = {
                                TooltipBox(
                                    positionProvider = rememberTooltipPositionProvider(
                                        TooltipAnchorPosition.Below
                                    ),
                                    tooltip = { PlainTooltip { Text("刷新") } },
                                    state = rememberTooltipState(),
                                ) {
                                    IconButton(
                                        onClick = { vm.wifiList.startScan() },
                                        shapes = IconButtonDefaults.shapes(),
                                        enabled = !isScanning,
                                    ) {
                                        Icon(Icons.TwoTone.Refresh, contentDescription = null)
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = Color.Transparent,
                            ),
                        )

                        AnimatedVisibility(
                            visible = isScanning,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = 4.dp),
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(200)),
                        ) {
                            LinearWavyProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            val layoutDirection = LocalLayoutDirection.current
            val startPadding = innerPadding.calculateStartPadding(layoutDirection)
            val endPadding = innerPadding.calculateEndPadding(layoutDirection)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = startPadding, end = endPadding),
                ) {
                    WifiList(
                        modifier = Modifier,
                        vm = vm,
                        listState = listState,
                    )

                    AnimatedVisibility(
                        visible = !isListAtTop,
                        modifier = Modifier.align(Alignment.BottomEnd),
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                        ),
                    ) {
                        Box(modifier = Modifier.padding(end = 24.dp, bottom = 24.dp)) {
                            TooltipBox(
                                positionProvider = rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Above
                                ),
                                tooltip = { PlainTooltip { Text("回到顶部") } },
                                state = rememberTooltipState(),
                            ) {
                                FloatingActionButton(
                                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ) {
                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "回到顶部")
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isScanning,
                    modifier = Modifier.align(Alignment.TopCenter),
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(200)),
                ) {
                    LinearWavyProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .offset(y = (-4).dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}