package com.wifi.toolbox.ui.screen.pojie

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wifi.toolbox.structs.PojieResource
import com.wifi.toolbox.ui.items.pojie.PojieResourceItem
import com.wifi.toolbox.utils.PojieStore
import com.wifi.toolbox.utils.ResourcesState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesPage(
    showFabDialog: Boolean,
    onShowFabDialogChange: (Boolean) -> Unit
) {
    val state = ResourcesState.rememberState(onShowFabDialogChange)
    val sheetState = rememberModalBottomSheetState()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> state.handleImport(uri) }

    LaunchedEffect(state.refreshKey) {
        state.loadResources()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                state.selectedFabOption = null
                onShowFabDialogChange(true)
            }) {
                Icon(Icons.Filled.Add, null)
            }
        },
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        if (state.resources.isEmpty()) {
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
                    items = state.resources,
                    key = { it.id }
                ) { res ->
                    PojieResourceItem(
                        modifier = Modifier
                            .animateItem()
                            .clickable {
                                state.selectedResource = res
                                state.showDetailSheet = true
                            },
                        res = res
                    )
                }
            }
        }

        ResourceDetailSheet(
            isVisible = state.showDetailSheet,
            resource = state.selectedResource,
            sheetState = sheetState,
            onDismiss = {
                state.showDetailSheet = false
                state.selectedResource = null
            },
            onEdit = { currentRes ->
                state.scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        state.showDetailSheet = false
                        if (currentRes.type == 1) {
                            state.openEditorForScript(currentRes, false)
                        } else {
                            state.draftState = currentRes
                            state.originalIdForEdit = currentRes.id
                            state.showEditDialog = true
                        }
                    }
                }
            },
            onDelete = { state.deleteResource(it) }
        )

        val currentDraft = state.draftState
        if (state.showEditDialog && currentDraft != null && !state.editor.isEditorOpen) {
            EditResourceDialog(
                draft = currentDraft,
                isNew = state.originalIdForEdit == null,
                onDismiss = { state.showEditDialog = false },
                onContentEdit = { updatedDraft ->
                    state.draftState = updatedDraft
                    state.editor.open(updatedDraft.content) { newContent ->
                        state.draftState = state.draftState?.copy(content = newContent)
                    }
                },
                onSave = { finalDraft ->
                    val originalId = state.originalIdForEdit
                    PojieStore.testExists(state.context, finalDraft, originalId)
                    state.scope.launch(Dispatchers.IO) {
                        try {
                            if (originalId != null) {
                                PojieStore.update(
                                    state.context,
                                    originalId,
                                    finalDraft
                                )
                            } else {
                                PojieStore.save(state.context, finalDraft)
                            }
                            state.refreshKey++
                            withContext(Dispatchers.Main) {
                                state.showEditDialog = false
                            }
                        } catch (e: Exception) {
                            state.app.alert("保存失败", e.message.toString())
                        }
                    }
                }
            )
        }

        AddResourceDialog(
            isVisible = showFabDialog,
            selectedOption = state.selectedFabOption,
            onOptionSelect = { state.selectedFabOption = it },
            onDismiss = { onShowFabDialogChange(false) },
            onImport = {
                importLauncher.launch(
                    arrayOf(
                        "application/javascript",
                        "application/json",
                        "text/plain"
                    )
                )
            },
            onContinue = {
                onShowFabDialogChange(false)
                state.currentEditingId = null
                if (state.selectedFabOption == 0) {
                    state.originalIdForEdit = null
                    state.draftState = PojieResource(
                        id = PojieStore.randomID(),
                        name = "",
                        description = "",
                        type = 0,
                        content = "",
                        author = "",
                        version = ""
                    )
                    state.showEditDialog = true
                } else {
                    val tempId = PojieStore.randomID()
                    val content = state.defaultScriptContent.replace("{ID}", tempId)
                    state.openEditorForScript(PojieResource.parseScript(content), true)
                }
            }
        )
    }
}