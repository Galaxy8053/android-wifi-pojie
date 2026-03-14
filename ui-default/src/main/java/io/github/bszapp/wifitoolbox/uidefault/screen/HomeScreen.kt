package io.github.bszapp.wifitoolbox.uidefault.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.MenuOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.twotone.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.bszapp.wifitoolbox.contract.wifilist.ScanStatus
import io.github.bszapp.wifitoolbox.uidefault.DefaultViewModel
import io.github.bszapp.wifitoolbox.uidefault.widget.WifiList
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ── 可调参数 ──────────────────────────────────────────────────────────────────

private val MaxSheetOffset = 200.dp
private val MaxCornerRadius = 28.dp
private const val OpenVelocityThreshold = 1200f
private const val CloseVelocityThreshold = 800f
private const val PositionSnapThreshold = 0.5f

// ─────────────────────────────────────────────────────────────────────────────

private fun shouldOpen(velocityY: Float, offset: Float, maxOffset: Float): Boolean =
    when {
        velocityY > OpenVelocityThreshold -> true
        velocityY < -CloseVelocityThreshold -> false
        velocityY < 0f -> false
        else -> offset > maxOffset * PositionSnapThreshold
    }

// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: DefaultViewModel = viewModel()) {
    val scanStatus by vm.wifiList.status.collectAsStateWithLifecycle()
    val isScanning = scanStatus == ScanStatus.SCANNING

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val maxOffsetPx = with(density) { (MaxSheetOffset + navBarHeight).toPx() }

    // Column 固定高度 = MaxSheetOffset + navBarHeight
    val columnHeightDp = MaxSheetOffset + navBarHeight
    val columnHeightPx = with(density) { columnHeightDp.toPx() }

    val isExpanded = rememberSaveable { mutableStateOf(false) }

    val rawOffset = remember { mutableFloatStateOf(0f) }
    val snapAnim = remember { Animatable(0f) }
    val isSnapping = remember { mutableStateOf(false) }

    LaunchedEffect(maxOffsetPx) {
        val target = if (isExpanded.value) maxOffsetPx else 0f
        rawOffset.floatValue = target
        snapAnim.snapTo(target)
    }

    val displayOffset by remember {
        derivedStateOf { if (isSnapping.value) snapAnim.value else rawOffset.floatValue }
    }
    val progress by remember {
        derivedStateOf { (displayOffset / maxOffsetPx).coerceIn(0f, 1f) }
    }
    val isListAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
        }
    }

    fun snap(targetOpen: Boolean) {
        if (isSnapping.value) return
        isExpanded.value = targetOpen
        isSnapping.value = true
        scope.launch {
            val startVal = rawOffset.floatValue
            val target = if (targetOpen) maxOffsetPx else 0f
            snapAnim.snapTo(startVal)
            snapAnim.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            )
            rawOffset.floatValue = snapAnim.value
            isSnapping.value = false
        }
    }

    val nestedScrollConnection = remember(maxOffsetPx) {
        object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isSnapping.value || available.y >= 0f || rawOffset.floatValue <= 0f)
                    return Offset.Zero
                val prev = rawOffset.floatValue
                rawOffset.floatValue = (prev + available.y).coerceAtLeast(0f)
                return Offset(0f, rawOffset.floatValue - prev)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (isSnapping.value || available.y <= 0f) return Offset.Zero
                val effectiveY = if (source == NestedScrollSource.SideEffect) 0f else available.y
                val prev = rawOffset.floatValue
                rawOffset.floatValue = (prev + effectiveY).coerceAtMost(maxOffsetPx)
                return Offset(0f, rawOffset.floatValue - prev)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val offset = rawOffset.floatValue
                if (offset < 2f) return Velocity.Zero
                snap(shouldOpen(available.y, offset, maxOffsetPx))
                return available
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val offset = rawOffset.floatValue
                if (offset >= 2f) snap(shouldOpen(consumed.y, offset, maxOffsetPx))
                return super.onPostFling(consumed, available)
            }
        }
    }

    val topBarColor by animateColorAsState(
        targetValue = if (isListAtTop) MaterialTheme.colorScheme.surface
        else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(200),
        label = "topBarColor",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {

        // ── 下拉区域 ──────────────────────────────────────────────────────────
        // 总高固定；中心 = displayOffset / 2 → top = displayOffset/2 - columnHeight/2
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(columnHeightDp)
                .offset {
                    IntOffset(
                        x = 0,
                        y = (displayOffset / 2f - columnHeightPx / 2f).roundToInt(),
                    )
                },
        ) {
            // 状态栏占位
            Spacer(modifier = Modifier.height(statusBarHeight))

            // 剩余空间填满的圆角矩形（无外边距）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "下拉区域",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ── 主内容面板 ────────────────────────────────────────────────────────
        val shape = RoundedCornerShape(
            topStart = MaxCornerRadius * progress,
            topEnd = MaxCornerRadius * progress,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, displayOffset.roundToInt()) }
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .nestedScroll(nestedScrollConnection),
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    Surface(
                        color = topBarColor,
                        modifier = Modifier.pointerInput(Unit) {
                            val tracker = VelocityTracker()
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                tracker.resetTracking()
                                tracker.addPointerInputChange(down)

                                drag(down.id) { change ->
                                    if (!isSnapping.value) {
                                        change.consume()
                                        tracker.addPointerInputChange(change)
                                        val delta = change.position.y - change.previousPosition.y
                                        rawOffset.floatValue =
                                            (rawOffset.floatValue + delta).coerceIn(0f, maxOffsetPx)
                                    }
                                }

                                val vel = tracker.calculateVelocity()
                                snap(shouldOpen(vel.y, rawOffset.floatValue, maxOffsetPx))
                            }
                        },
                    ) {
                        TopAppBar(
                            title = { Text("连接", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { snap(!isExpanded.value) }) {
                                    Icon(
                                        imageVector = if (isExpanded.value)
                                            Icons.AutoMirrored.TwoTone.MenuOpen
                                        else
                                            Icons.TwoTone.Menu,
                                        contentDescription = if (isExpanded.value) "收起" else "展开",
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { vm.wifiList.startScan() },
                                    enabled = !isScanning,
                                ) {
                                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = Color.Transparent,
                            ),
                        )
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                            end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                        ),
                ) {
                    WifiList(vm, listState)

                    AnimatedVisibility(
                        visible = isScanning,
                        modifier = Modifier.align(Alignment.TopCenter),
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(200)),
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    if (!isListAtTop) {
                        FloatingActionButton(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 24.dp, bottom = 24.dp),
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
    }
}