package com.wifi.toolbox.ui.screen.pojie

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wifi.toolbox.ToolboxApp
import com.wifi.toolbox.utils.PojieHistoryItem

@Composable
fun HistoryPage() {
    val context = LocalContext.current.applicationContext as ToolboxApp
    val historyList by context.pojieHistory.historyFlow.collectAsState()
    var pendingDeleteItem by remember { mutableStateOf<PojieHistoryItem?>(null) }

    if (historyList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "¯\\_(ツ)_/¯\n暂无历史记录",
                textAlign = TextAlign.Center,
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(historyList) { index, item ->
                ListItem(
                    modifier = Modifier.clickable { /* 这里可以扩展点击恢复任务 */ },
                    headlineContent = {
                        Text(item.ssid, style = MaterialTheme.typography.titleMedium)
                    },
                    supportingContent = {
                        Column {
                            Text("进度: ${item.progress} / ${item.passwords.size}")
                            item.successfulPassword?.let { pwd ->
                                Text(
                                    "成功密码: $pwd",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = { pendingDeleteItem = item }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "删除",
                            )
                        }
                    }
                )
                if (index < historyList.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }

    pendingDeleteItem?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDeleteItem = null },
            title = { Text("确认删除") },
            text = { Text("是否删除 ${item.ssid} 的记录？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        context.pojieHistory.deleteHistory(item.ssid)
                        pendingDeleteItem = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteItem = null }) {
                    Text("取消")
                }
            }
        )
    }
}