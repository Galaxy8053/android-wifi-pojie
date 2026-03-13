package io.github.bszapp.wifitoolbox.uidefault.component

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

private val CornerRadius = 16.dp
private val ConnectionRadius = 4.dp

data class SplicedItemData(
    val key: Any?,
    val visible: Boolean,
    val content: @Composable () -> Unit
)

class SplicedGroupScope {
    val items = mutableListOf<SplicedItemData>()

    fun item(
        key: Any? = null,
        visible: Boolean = true,
        content: @Composable () -> Unit
    ) {
        items.add(SplicedItemData(key ?: items.size, visible, content))
    }
}

@Composable
fun SplicedListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(clickableModifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingContent != null) {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                leadingContent()
            }
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = headline,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(16.dp))
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                trailingContent()
            }
        }
    }
}

@Composable
private fun Box(modifier: Modifier, contentAlignment: Alignment, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(modifier = modifier, contentAlignment = contentAlignment) {
        content()
    }
}

@Composable
fun SplicedColumnGroup(
    modifier: Modifier = Modifier,
    title: String = "",
    content: SplicedGroupScope.() -> Unit
) {
    val scope = SplicedGroupScope().apply(content)
    val allItems = scope.items

    if (allItems.isEmpty()) return

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        }

        Column(verticalArrangement = Arrangement.Top) {
            val firstVisibleIndex = allItems.indexOfFirst { it.visible }
            val lastVisibleIndex = allItems.indexOfLast { it.visible }
            val isAtLeastTiramisu = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            val sharedStiffness = Spring.StiffnessMediumLow

            allItems.forEachIndexed { index, itemData ->
                key(itemData.key) {
                    val zIndex = if (itemData.visible) 0f else 1f

                    AnimatedVisibility(
                        visible = itemData.visible,
                        modifier = Modifier.zIndex(zIndex),
                        enter = expandVertically(
                            animationSpec = spring(stiffness = sharedStiffness),
                            expandFrom = Alignment.Top
                        ) + fadeIn(animationSpec = spring(stiffness = sharedStiffness)),
                        exit = shrinkVertically(
                            animationSpec = spring(stiffness = sharedStiffness),
                            shrinkTowards = Alignment.Top
                        ) + fadeOut(animationSpec = spring(stiffness = sharedStiffness))
                    ) {
                        val isFirst = index == firstVisibleIndex
                        val isLast = index == lastVisibleIndex

                        val targetTopRadius: Dp = if (isFirst) CornerRadius else ConnectionRadius
                        val targetBottomRadius: Dp = if (isLast) CornerRadius else ConnectionRadius
                        val targetTopPadding: Dp = if (isFirst) 0.dp else 1.dp
                        val targetBottomPadding: Dp = if (isLast) 0.dp else 1.dp

                        val currentTopRadius = if (isAtLeastTiramisu)
                            animateDpAsState(targetTopRadius, spring(stiffness = sharedStiffness)).value
                        else targetTopRadius

                        val currentBottomRadius = if (isAtLeastTiramisu)
                            animateDpAsState(targetBottomRadius, spring(stiffness = sharedStiffness)).value
                        else targetBottomRadius

                        val currentTopPadding = if (isAtLeastTiramisu)
                            animateDpAsState(targetTopPadding, spring(stiffness = sharedStiffness)).value
                        else targetTopPadding

                        val currentBottomPadding = if (isAtLeastTiramisu)
                            animateDpAsState(targetBottomPadding, spring(stiffness = sharedStiffness)).value
                        else targetBottomPadding

                        val shape = RoundedCornerShape(
                            topStart = currentTopRadius,
                            topEnd = currentTopRadius,
                            bottomStart = currentBottomRadius,
                            bottomEnd = currentBottomRadius
                        )

                        Column(
                            modifier = Modifier
                                .padding(top = currentTopPadding, bottom = currentBottomPadding)
                                .graphicsLayer {
                                    this.shape = shape
                                    this.clip = true
                                }
                                .background(MaterialTheme.colorScheme.surfaceBright)
                        ) {
                            itemData.content()
                        }
                    }
                }
            }
        }
    }
}