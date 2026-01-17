package com.wifi.toolbox.ui.screen.pojie

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wifi.toolbox.MyApplication
import com.wifi.toolbox.R
import com.wifi.toolbox.structs.PojieResource
import com.wifi.toolbox.ui.LocalEditorController
import com.wifi.toolbox.utils.PojieStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ResourceDraft(
    val id: String,
    val name: String?,
    val description: String?,
    val type: Int,
    val content: String,
    val author: String?,
    val version: String?,
    val originalId: String? = null
)

val ResourceDraftSaver = Saver<ResourceDraft?, Any>(
    save = { draft ->
        if (draft == null) "" else mapOf(
            "id" to draft.id,
            "name" to draft.name,
            "description" to draft.description,
            "type" to draft.type,
            "content" to draft.content,
            "author" to draft.author,
            "version" to draft.version,
            "originalId" to draft.originalId
        )
    },
    restore = { savedValue ->
        if (savedValue is Map<*, *>) {
            ResourceDraft(
                id = savedValue["id"] as String,
                name = savedValue["name"] as? String,
                description = savedValue["description"] as? String,
                type = savedValue["type"] as Int,
                content = savedValue["content"] as String,
                author = savedValue["author"] as? String,
                version = savedValue["version"] as? String,
                originalId = savedValue["originalId"] as? String
            )
        } else null
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesPage(
    showFabDialog: Boolean,
    onShowFabDialogChange: (Boolean) -> Unit
) {
    val app = LocalContext.current.applicationContext as MyApplication
    val context = LocalContext.current
    val editor = LocalEditorController.current
    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState()

    var resources by remember { mutableStateOf<List<PojieResource>>(emptyList()) }
    var refreshKey by remember { mutableIntStateOf(0) }

    var selectedResource by remember { mutableStateOf<PojieResource?>(null) }
    var showDetailSheet by rememberSaveable { mutableStateOf(false) }

    var draftState by rememberSaveable(stateSaver = ResourceDraftSaver) {
        mutableStateOf<ResourceDraft?>(null)
    }
    var showEditDialog by rememberSaveable { mutableStateOf(false) }

    var selectedFabOption by rememberSaveable { mutableStateOf<Int?>(null) }
    val defaultScriptContent = stringResource(id = R.string.default_script_content)

    LaunchedEffect(refreshKey) {
        withContext(Dispatchers.IO) {
            resources = PojieStore.getAll(context)
        }
    }

    fun deleteResource(id: String) {
        scope.launch(Dispatchers.IO) {
            PojieStore.delete(context, id)
            refreshKey++
            withContext(Dispatchers.Main) {
                showDetailSheet = false
                selectedResource = null
            }
        }
    }

    fun openEditorForScript(res: PojieResource, isNew: Boolean) {
        editor.open(res.content) { newContent ->
            val newRes = PojieResource.parseScript(newContent)
            PojieStore.testExists(context, newRes, res.id)
            scope.launch(Dispatchers.IO) {
                if (!isNew && newRes.id != res.id) {
                    PojieStore.update(context, res.id, newRes)
                } else {
                    PojieStore.save(context, newRes, res.id)
                }
                refreshKey++
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                selectedFabOption = null
                onShowFabDialogChange(true)
            }) {
                Icon(Icons.Filled.Add, "添加资源")
            }
        },
        contentWindowInsets = WindowInsets()
    ) { paddingValues ->
        if (resources.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "¯\\_(ツ)_/¯\n暂无资源，点击 + 号添加",
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                items(resources) { res ->
                    ListItem(
                        headlineContent = { Text(res.name ?: res.id) },
                        supportingContent = {
                            Text(
                                text = res.description ?: "无描述",
                                maxLines = 2,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            Icon(
                                if (res.type == 1) Icons.Default.Code else Icons.Default.Description,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.clickable {
                            selectedResource = res
                            showDetailSheet = true
                        }
                    )
                    HorizontalDivider()
                }
            }
        }

        if (showDetailSheet && selectedResource != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    showDetailSheet = false
                    selectedResource = null // 建议置空，防止重组时读取旧数据
                },
                sheetState = sheetState, // 显式绑定状态
                contentWindowInsets = { WindowInsets(0) } // 消除可能的内边距计算干扰
            ) {
                ResourceDetailContent(
                    resource = selectedResource!!,
                    onEdit = {
                        // 先关闭 Sheet 再打开 Dialog 或编辑器，避免窗口层级冲突
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showDetailSheet = false
                                if (selectedResource!!.type == 1) {
                                    openEditorForScript(selectedResource!!, false)
                                } else {
                                    draftState = ResourceDraft(
                                        id = selectedResource!!.id,
                                        name = selectedResource!!.name,
                                        description = selectedResource!!.description,
                                        type = 0,
                                        content = selectedResource!!.content,
                                        author = selectedResource!!.author,
                                        version = selectedResource!!.version,
                                        originalId = selectedResource!!.id
                                    )
                                    showEditDialog = true
                                }
                            }
                        }
                    },
                    onDelete = { deleteResource(selectedResource!!.id) }
                )
            }
        }

        if (showEditDialog && draftState != null && !editor.isEditorOpen) {
            EditResourceDialog(
                draft = draftState!!,
                onDismiss = { showEditDialog = false },
                onContentEdit = { currentDraftFromUI ->
                    // 1. 先把 Dialog 里的最新输入保存到 draftState
                    draftState = currentDraftFromUI
                    // 2. 再打开编辑器
                    editor.open(currentDraftFromUI.content) { newContent ->
                        // 3. 编辑器返回后更新 content，此时其他字段已经是刚才同步过的了
                        draftState = draftState?.copy(content = newContent)
                    }
                },
                onSave = { finalDraft ->
                    scope.launch(Dispatchers.IO) {
                        try {
                            val newRes = PojieResource(
                                id = finalDraft.id,
                                name = finalDraft.name,
                                description = finalDraft.description,
                                type = finalDraft.type,
                                content = finalDraft.content,
                                author = finalDraft.author,
                                version = finalDraft.version
                            )
                            if (finalDraft.originalId != null) {
                                PojieStore.update(context, finalDraft.originalId, newRes)
                            } else {
                                PojieStore.save(context, newRes)
                            }
                            refreshKey++
                            withContext(Dispatchers.Main) {
                                showEditDialog = false
                            }
                        } catch (e: Exception) {
                            app.alert("保存失败", e.message.toString())
                        }
                    }
                }
            )
        }

        if (showFabDialog) {
            AlertDialog(
                onDismissRequest = { onShowFabDialogChange(false) },
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                title = { Text("添加资源") },
                text = {
                    Column(Modifier.selectableGroup()) {
                        ResourceOptionItem(
                            icon = Icons.Default.Description,
                            title = "普通",
                            description = "通用格式，一行一个密码",
                            selected = selectedFabOption == 0,
                            onClick = { selectedFabOption = 0 }
                        )
                        ResourceOptionItem(
                            icon = Icons.Default.Code,
                            title = "脚本",
                            description = "使用JS脚本动态生成密码本",
                            selected = selectedFabOption == 1,
                            onClick = { selectedFabOption = 1 }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        enabled = selectedFabOption != null,
                        onClick = {
                            onShowFabDialogChange(false)
                            if (selectedFabOption == 0) {
                                draftState = ResourceDraft(
                                    id = PojieStore.randomID(),
                                    name = "",
                                    description = "",
                                    type = 0,
                                    content = "",
                                    author = "",
                                    version = "",
                                    originalId = null
                                )
                                showEditDialog = true
                            } else {
                                val tempId = PojieStore.randomID()
                                val content = defaultScriptContent.replace("{ID}", tempId)
                                editor.open(content) { newContent ->
                                    val newRes = PojieResource.parseScript(newContent)
                                    PojieStore.testExists(context, newRes, null)
                                    scope.launch(Dispatchers.IO) {
                                        PojieStore.save(context, newRes)
                                        refreshKey++
                                    }
                                }
                            }
                        }
                    ) {
                        Text("继续")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onShowFabDialogChange(false) }) { Text("取消") }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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

        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "名称",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = resource.name ?: "未命名",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f, false)
                    )
                    if (resource.type == 1) {
                        Spacer(Modifier.width(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "脚本",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                DetailItem("ID", resource.id)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth()) {
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
            Button(onClick = onEdit, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Edit, null)
                Spacer(Modifier.width(8.dp))
                Text("编辑")
            }
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, null)
                Spacer(Modifier.width(8.dp))
                Text("删除")
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

@Composable
fun EditResourceDialog(
    draft: ResourceDraft,
    onDismiss: () -> Unit,
    onContentEdit: (ResourceDraft) -> Unit,
    onSave: (ResourceDraft) -> Unit
) {
    var id by rememberSaveable { mutableStateOf(draft.id) }
    var name by rememberSaveable { mutableStateOf(draft.name ?: "") }
    var description by rememberSaveable { mutableStateOf(draft.description ?: "") }
    var author by rememberSaveable { mutableStateOf(draft.author ?: "") }
    var version by rememberSaveable { mutableStateOf(draft.version ?: "") }

    fun emptyAsNull(s: String): String? = s.trim().ifEmpty { null }

    fun getCurrentDraft() = draft.copy(
        id = id.trim(),
        name = emptyAsNull(name),
        description = emptyAsNull(description),
        author = emptyAsNull(author),
        version = emptyAsNull(version)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.originalId == null) "新建资源" else "编辑资源") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it },
                    label = { Text("ID (必填)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("作者") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = version,
                    onValueChange = { version = it },
                    label = { Text("版本") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { onContentEdit(getCurrentDraft()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.EditNote, null)
                    Spacer(Modifier.width(8.dp))
                    Text("编辑内容 (当前长度: ${draft.content.length})")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(getCurrentDraft()) },
                enabled = id.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun ResourceOptionItem(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = description, style = MaterialTheme.typography.bodySmall)
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}