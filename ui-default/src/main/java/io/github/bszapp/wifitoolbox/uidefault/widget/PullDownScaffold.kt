package io.github.bszapp.wifitoolbox.uidefault.widget

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
private val MaxCornerRadius = 36.dp
private const val OpenVelocityThreshold = 1200f
private const val CloseVelocityThreshold = 800f
private const val PositionSnapThreshold = 0.5f

private fun shouldOpen(velocityY: Float, offset: Float, maxOffset: Float): Boolean =
    when {
        velocityY > OpenVelocityThreshold -> true
        velocityY < -CloseVelocityThreshold -> false
        velocityY < 0f -> false
        else -> offset > maxOffset * PositionSnapThreshold
    }

/**
 * 支持顶部下拉展开区域的脚手架。
 *
 * @param pullDownContent 下拉展开区域内容，会被包裹在圆角矩形卡片中。
 * @param maxSheetOffset  下拉展开区域高度
 * @param content         主内容区域。通过 isExpanded 和 onToggle 与展开状态交互。
 */
@Composable
fun PullDownScaffold(
    modifier: Modifier = Modifier,
    pullDownContent: @Composable () -> Unit,
    maxSheetOffset: Dp = 200.dp,
    content: @Composable (
        isExpanded: Boolean,
        onToggle: () -> Unit,
        nestedScrollConnection: NestedScrollConnection,
        topBarDragModifier: Modifier,
    ) -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val maxOffsetPx = with(density) { (maxSheetOffset + navBarHeight).toPx() }
    val columnHeightDp = maxSheetOffset + navBarHeight
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

    fun snap(targetOpen: Boolean) {
        if (isSnapping.value) return
        isExpanded.value = targetOpen
        isSnapping.value = true
        scope.launch {
            snapAnim.snapTo(rawOffset.floatValue)
            snapAnim.animateTo(
                targetValue = if (targetOpen) maxOffsetPx else 0f,
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
                if (rawOffset.floatValue == 0f) isExpanded.value = false
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
                if (rawOffset.floatValue == maxOffsetPx) isExpanded.value = true
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

    // 标题栏拖拽手势
    val topBarDragModifier = Modifier.pointerInput(Unit) {
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
            snap(shouldOpen(tracker.calculateVelocity().y, rawOffset.floatValue, maxOffsetPx))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        // 下拉区域
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
            Spacer(modifier = Modifier.height(statusBarHeight))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                contentAlignment = Alignment.Center,
            ) {
                pullDownContent()
            }
        }

        // 主内容
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, displayOffset.roundToInt()) }
                .clip(
                    RoundedCornerShape(
                        topStart = MaxCornerRadius * progress,
                        topEnd = MaxCornerRadius * progress,
                    )
                )
                .background(MaterialTheme.colorScheme.surface)
                .nestedScroll(nestedScrollConnection),
        ) {
            content(
                isExpanded.value,
                { snap(!isExpanded.value) },
                nestedScrollConnection,
                topBarDragModifier,
            )
        }
    }
}