package com.wifi.toolbox.ui.screen.pojie

/**
 * 警告：这里是屎山，有BUG也不要乱动！！
 * （看来用AI写代码不是个好主意）
 * 已知BUG：快速编辑一个脚本资源的id并重新点开查看，应用崩溃
 */

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wifi.toolbox.MyApplication
import com.wifi.toolbox.R
import com.wifi.toolbox.structs.PojieResource
import com.wifi.toolbox.ui.LocalEditorController
import com.wifi.toolbox.utils.PojieStore
import kotlinx.coroutines.*
import java.util.Locale

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
        mutableStateOf(null)
    }
    var showEditDialog by rememberSaveable { mutableStateOf(false) }

    var selectedFabOption by rememberSaveable { mutableStateOf<Int?>(null) }
    val defaultScriptContent = stringResource(id = R.string.default_script_content)

    var currentEditingId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshKey) {
        withContext(Dispatchers.IO) {
            resources = PojieStore.getAll(context)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val content = context.contentResolver.openInputStream(it)?.use { stream ->
                        stream.bufferedReader().readText()
                    } ?: return@launch

                    val fileName = it.path?.lowercase() ?: ""

                    if (fileName.lowercase(Locale.ROOT).endsWith(".js")) {
                        val newRes = PojieResource.parseScript(content)
                        PojieStore.testExists(context, newRes, null)
                        PojieStore.save(context, newRes)
                    } else if (fileName.lowercase(Locale.ROOT).endsWith(".json")) {
                        val newRes = PojieResource.parseJSON(content)
                        PojieStore.testExists(context, newRes, null)
                        PojieStore.save(context, newRes)
                    }

                    refreshKey++
                    withContext(Dispatchers.Main) {
                        onShowFabDialogChange(false)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        app.alert("导入失败", e.message.toString())
                    }
                }
            }
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
        currentEditingId = res.id

        editor.open(res.content) { newContent ->
            val newRes = PojieResource.parseScript(newContent)

            val oldId = currentEditingId ?: res.id

            PojieStore.testExists(context, newRes, oldId)

            scope.launch(Dispatchers.IO) {
                if (isNew && oldId == res.id) {
                    PojieStore.save(context, newRes, oldId)
                } else {
                    if (oldId != newRes.id) {
                        PojieStore.update(context, oldId, newRes)
                    } else {
                        PojieStore.save(context, newRes, oldId)
                    }
                }

                currentEditingId = newRes.id
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
                items(
                    items = resources
                ) { res ->
                    key(res.id) {
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
        }

        if (showDetailSheet) {
            val currentRes = selectedResource
            if (currentRes != null) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showDetailSheet = false
                        selectedResource = null
                    },
                    sheetState = sheetState,
                    contentWindowInsets = { WindowInsets(0) }
                ) {
                    ResourceDetailContent(
                        resource = currentRes,
                        onEdit = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showDetailSheet = false
                                    if (currentRes.type == 1) {
                                        openEditorForScript(currentRes, false)
                                    } else {
                                        draftState = ResourceDraft(
                                            id = currentRes.id,
                                            name = currentRes.name,
                                            description = currentRes.description,
                                            type = 0,
                                            content = currentRes.content,
                                            author = currentRes.author,
                                            version = currentRes.version,
                                            originalId = currentRes.id
                                        )
                                        showEditDialog = true
                                    }
                                }
                            }
                        },
                        onDelete = { deleteResource(currentRes.id) }
                    )
                }
            }
        }
        if (showEditDialog && draftState != null && !editor.isEditorOpen) {
            EditResourceDialog(
                draft = draftState!!,
                onDismiss = { showEditDialog = false },
                onContentEdit = { currentDraftFromUI ->
                    draftState = currentDraftFromUI
                    editor.open(currentDraftFromUI.content) { newContent ->
                        draftState = draftState?.copy(content = newContent)
                    }
                },
                onSave = { finalDraft ->
                    val newRes = PojieResource(
                        id = finalDraft.id,
                        name = finalDraft.name,
                        description = finalDraft.description,
                        type = finalDraft.type,
                        content = finalDraft.content,
                        author = finalDraft.author,
                        version = finalDraft.version
                    )
                    PojieStore.testExists(context, newRes, finalDraft.originalId)
                    scope.launch(Dispatchers.IO) {
                        try {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                importLauncher.launch(
                                    arrayOf(
                                        "application/javascript",
                                        "application/json"
                                    )
                                )
                            }
                        ) {
                            Text("导入js/json")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onShowFabDialogChange(false) }) {
                                Text("取消")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                enabled = selectedFabOption != null,
                                onClick = {
                                    onShowFabDialogChange(false)
                                    currentEditingId = null
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
                                        openEditorForScript(
                                            PojieResource.parseScript(content),
                                            true
                                        )
                                    }
                                }
                            ) {
                                Text("继续")
                            }
                        }
                    }
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
                        text = resource.name ?: "-",
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
    val app = LocalContext.current.applicationContext as MyApplication

    var id by rememberSaveable { mutableStateOf(draft.id) }
    var name by rememberSaveable { mutableStateOf(draft.name ?: "") }
    var description by rememberSaveable { mutableStateOf(draft.description ?: "") }
    var author by rememberSaveable { mutableStateOf(draft.author ?: "") }
    var version by rememberSaveable { mutableStateOf(draft.version ?: "") }

    var contentState by rememberSaveable { mutableStateOf(draft.content) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboard.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val text = stream.bufferedReader().readText()
                        contentState = text
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    val lineCount = remember(contentState) {
        if (contentState.isBlank()) 0
        else contentState.split("\n").filter { it.isNotBlank() }.size
    }

    fun emptyAsNull(s: String): String? = s.trim().ifEmpty { null }

    fun getCurrentDraft() = draft.copy(
        id = id.trim(),
        name = emptyAsNull(name),
        description = emptyAsNull(description),
        author = emptyAsNull(author),
        version = emptyAsNull(version),
        content = contentState
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        label = { Text("作者") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = version,
                        onValueChange = { version = it },
                        label = { Text("版本") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                clipboardManager.getClipEntry()?.clipData?.getItemAt(0)?.text?.let {
                                    contentState = it.toString()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentPaste, null)
                        Spacer(Modifier.width(4.dp))
                        Text("剪贴板")
                    }
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch(arrayOf("text/plain")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileOpen, null)
                        Spacer(Modifier.width(4.dp))
                        Text("导入文件")
                    }
                }

                OutlinedButton(
                    onClick = { onContentEdit(getCurrentDraft()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.EditNote, null)
                    Spacer(Modifier.width(8.dp))
                    Text("文本编辑器")
                }

                Text(
                    text = "当前包含 $lineCount 条数据",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        onSave(getCurrentDraft())
                    } catch (e: Exception) {
                        app.alert("保存失败", e.message.toString())
                    }
                },
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