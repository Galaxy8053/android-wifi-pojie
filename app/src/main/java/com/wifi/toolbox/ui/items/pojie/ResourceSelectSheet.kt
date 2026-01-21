package com.wifi.toolbox.ui.items.pojie

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wifi.toolbox.structs.WifiInfo
import com.wifi.toolbox.ui.items.LogView
import com.wifi.toolbox.utils.LogState
import com.wifi.toolbox.utils.PojieStore
import com.wifi.toolbox.utils.ResourcesRunner

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
fun ResourceSelectSheet(
    show: Boolean,
    wifiInfo: WifiInfo,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    if (!show) return

    val context = LocalContext.current

    val logState = remember { LogState() }
    var showLog by remember { mutableStateOf(false) }

    val allResources = remember { PojieStore.getAll(context) }
    var selectedIds by remember { mutableStateOf(listOf<String>()) }

    var isRunning by remember { mutableStateOf(false) }
    var resultList by remember { mutableStateOf(listOf<String>()) }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true // 强制跳过半展开状态，直接全屏显示
    )
    val sortedList = remember(selectedIds) {
        val selected = selectedIds.mapNotNull { id -> allResources.find { it.id == id } }
        val unselected = allResources.filter { it.id !in selectedIds }
        selected + unselected
    }

    // 监听选中项变化并自动执行
    LaunchedEffect(selectedIds) {
        if (selectedIds.isEmpty()) {
            resultList = emptyList()
            return@LaunchedEffect
        }
        logState.clear()

        isRunning = true
        val selectedResources = selectedIds.mapNotNull { id -> allResources.find { it.id == id } }

        val results = ResourcesRunner.run(
            context = context,
            resources = selectedResources,
            wifiInfo = wifiInfo,
            onProgress = { _, _ -> },
            onLog = { logState.addLog(it) }
        )
        resultList = results
        isRunning = false
    }


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,

        ) {
        Text(
            text = "选择密码本",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
        ) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(sortedList, key = { it.id }) { res ->
                    val isSelected = res.id in selectedIds
                    PojieResourceItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .clickable(enabled = !isRunning) {
                                selectedIds = if (isSelected) {
                                    selectedIds - res.id
                                } else {
                                    selectedIds + res.id
                                }
                            },
                        res = res,
                        checkbox = isSelected
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = { showLog = !showLog }
                ) {
                    Text(if (showLog) "收起日志" else "展开日志")
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Button(
                        enabled = !isRunning && resultList.isNotEmpty(),
                        onClick = {
                            onConfirm(resultList)
                            onDismiss()
                        }
                    ) {
                        Text(if (isRunning) "运行中..." else "完成 (${resultList.size})")
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = showLog,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            LogView(
                logState = logState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            )
        }
    }
}