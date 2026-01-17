package com.wifi.toolbox.ui.screen.pojie

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesPage(
    showFabDialog: Boolean,
    onShowFabDialogChange: (Boolean) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { onShowFabDialogChange(true) }) {
                Icon(Icons.Filled.Add, "添加资源")
            }
        },
        contentWindowInsets = WindowInsets()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text("啥都木有")
        }

        if (showFabDialog) {
            AlertDialog(
                onDismissRequest = { onShowFabDialogChange(false) },
                title = { Text("添加资源") },
                text = {

                },
                confirmButton = {
                    TextButton(onClick = { onShowFabDialogChange(false) }) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onShowFabDialogChange(false) }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}