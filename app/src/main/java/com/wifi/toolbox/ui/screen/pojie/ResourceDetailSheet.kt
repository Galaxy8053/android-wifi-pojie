package com.wifi.toolbox.ui.screen.pojie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wifi.toolbox.structs.PojieResource
import com.wifi.toolbox.ui.items.TagItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceDetailSheet(
    isVisible: Boolean,
    resource: PojieResource?,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onEdit: (PojieResource) -> Unit,
    onDelete: (String) -> Unit
) {
    var lastResource by remember { mutableStateOf<PojieResource?>(null) }
    if (resource != null) lastResource = resource
    val scope = rememberCoroutineScope()

    if (isVisible || sheetState.isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            contentWindowInsets = { WindowInsets(0) }
        ) {
            lastResource?.let { res ->
                ResourceDetailContent(
                    resource = res,
                    onEdit = { onEdit(res) },
                    onDelete = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                onDismiss()
                                onDelete(res.id)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ResourceDetailContent(
    resource: PojieResource,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = "资源详情", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Column(Modifier
                .weight(1f)
                .padding(vertical = 4.dp)) {
                Text(
                    text = "名称",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = resource.name ?: "-",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f, false)
                    )
                    if (resource.type == 1) {
                        TagItem(text = "脚本", modifier = Modifier.padding(4.dp))
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                DetailItem("ID", resource.id)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Column(Modifier.weight(1f)) {
                DetailItem("作者", resource.author)
            }
            Column(Modifier.weight(1f)) {
                DetailItem("版本", resource.version)
            }
        }

        Spacer(Modifier.height(12.dp))

        DetailItem("描述", resource.description)

        Spacer(modifier = Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (resource.isBuiltin) {
                1 -> {
                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        enabled = false
                    ) {
                        Icon(Icons.Default.Lock, null)
                        Spacer(Modifier.width(8.dp))
                        Text("内置资源")
                    }
                    Button(onClick = onEdit, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Edit, null)
                        Spacer(Modifier.width(8.dp))
                        Text("覆写资源")
                    }
                }

                2 -> {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.SettingsBackupRestore, null)
                        Spacer(Modifier.width(8.dp))
                        Text("还原内置")
                    }
                    Button(onClick = onEdit, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Edit, null)
                        Spacer(Modifier.width(8.dp))
                        Text("编辑覆写")
                    }
                }

                else -> {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, null)
                        Spacer(Modifier.width(8.dp))
                        Text("删除")
                    }
                    Button(onClick = onEdit, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Edit, null)
                        Spacer(Modifier.width(8.dp))
                        Text("编辑")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun DetailItem(label: String, value: String?) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = value ?: "-", style = MaterialTheme.typography.bodyLarge)
    }
}