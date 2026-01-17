package com.wifi.toolbox.ui.screen.pojie

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wifi.toolbox.ui.LocalEditorController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesPage(
    showFabDialog: Boolean,
    onShowFabDialogChange: (Boolean) -> Unit
) {
    val editor = LocalEditorController.current
    val scope = rememberCoroutineScope()

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
                    TextButton(onClick = {

                        editor.open("hello world"/*file.readText()*/) { newContent ->
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {

                                //file.writeText(newContent)
                            }
                        }
                        onShowFabDialogChange(false)
                    }) {
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