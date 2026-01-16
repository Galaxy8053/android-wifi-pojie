package com.wifi.toolbox.ui.screen.pojie

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wifi.toolbox.structs.PojieResource
import com.wifi.toolbox.ui.items.CodeEditor
import com.wifi.toolbox.utils.PojieStore

@Composable
fun ResourcesPage(refreshTrigger: Int, onStartEdit: (String?) -> Unit) {
    val context = LocalContext.current
    var list by remember { mutableStateOf(emptyList<PojieResource>()) }

    fun refresh() {
        list = PojieStore.getAll(context)
    }

    LaunchedEffect(Unit, refreshTrigger) { refresh() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { onStartEdit(null) }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("啥都木有")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list, key = { it.id }) { item ->
                    ResourceItem(
                        item = item,
                        onEdit = { onStartEdit(item.id) },
                        onDelete = {
                            PojieStore.delete(context, item.id)
                            refresh()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ResourceItem(item: PojieResource, onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick = { onEdit() },
            onLongClick = { expanded = true }
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium)
            Text("ID: ${item.id} | 类型: ${if (item.type == 1) "脚本" else "普通"}", style = MaterialTheme.typography.bodySmall)
            if (item.description.isNotEmpty()) {
                Text(item.description, style = MaterialTheme.typography.bodyMedium)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("编辑") }, onClick = { expanded = false; onEdit() })
                DropdownMenuItem(text = { Text("删除") }, onClick = { expanded = false; onDelete() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceEditPage(
    targetId: String?,
    onBack: () -> Unit,
    vm: PojieViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    LaunchedEffect(targetId) {
        vm.initData(context, targetId)
    }

    BackHandler {
        vm.reset()
        onBack()
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r ->
            vm.contentValue = TextFieldValue(r.readText())
        } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (targetId == null) "添加资源" else "编辑资源") },
                navigationIcon = {
                    IconButton(onClick = {
                        vm.reset()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(
                        modifier = Modifier.padding(end = 8.dp),
                        onClick = {
                            val idRegex = Regex("^[a-zA-Z][a-zA-Z0-9._-]+\$")
                            if (!vm.id.matches(idRegex)) {
                                Toast.makeText(context, "ID格式错误", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            if (targetId != vm.id && PojieStore.exists(context, vm.id)) {
                                Toast.makeText(context, "ID已存在", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            if (targetId != null && targetId != vm.id) PojieStore.delete(context, targetId)
                            PojieStore.save(context, PojieResource(vm.id, vm.name, vm.desc, vm.type, vm.contentValue.text))
                            vm.reset()
                            onBack()
                        }
                    ) {
                        Text("保存", style = MaterialTheme.typography.titleMedium)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            OutlinedTextField(value = vm.name, onValueChange = { vm.name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = vm.id, onValueChange = { vm.id = it }, label = { Text("ID") }, modifier = Modifier.fillMaxWidth())

            Row(Modifier.selectableGroup().padding(vertical = 4.dp)) {
                listOf("普通" to 0, "脚本" to 1).forEach { (label, value) ->
                    Row(
                        Modifier.height(48.dp).selectable(selected = (vm.type == value), onClick = { vm.type = value }, role = Role.RadioButton).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (vm.type == value), onClick = null)
                        Text(label, Modifier.padding(start = 8.dp))
                    }
                }
            }

            OutlinedTextField(value = vm.desc, onValueChange = { vm.desc = it }, label = { Text("描述") }, modifier = Modifier.fillMaxWidth())

            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { filePicker.launch("*/*") }) { Text("从文件导入") }
                Button(onClick = { clipboard.getText()?.let { vm.contentValue = TextFieldValue(it.text) } }) { Text("从剪贴板导入") }
            }

            CodeEditor(
                modifier = Modifier.fillMaxWidth().weight(1f),
                scrollState = scrollState,
                content = vm.contentValue,
                onValueChange = { vm.contentValue = it }
            )
        }
    }
}