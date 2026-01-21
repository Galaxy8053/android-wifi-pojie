package com.wifi.toolbox.ui.screen

import android.app.ActivityOptions
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.wifi.toolbox.R
import com.wifi.toolbox.ToolboxApp
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ListPreferenceType
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.PreferenceCategory
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.ColorPicker
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.SuperDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onMenuClick: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as ToolboxApp
    val settings = app.settings.global // 订阅全局状态

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val hiddenApiBypassValues = listOf("不使用", "LSPass", "HiddenApiBypass")
    val darkThemeValues = listOf("跟随设备", "开启", "关闭")

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.padding(0.dp, 8.dp)) {
                        Text(text = "设置", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        ProvidePreferenceLocals {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                item {
                    PreferenceCategory(title = { Text("主题") })

                    SwitchPreference(
                        value = settings.dynamicColor,
                        onValueChange = { newValue ->
                            app.settings.update { it.copy(dynamicColor = newValue) }
                        },
                        title = { Text("动态主题色（Android12+）") },
                        summary = { Text("使用系统主题的动态颜色") },
                        icon = { Icon(Icons.Outlined.ColorLens, null) }
                    )

                    var showColorDialog by remember { mutableStateOf(false) }
                    var selectedColor by remember { mutableStateOf(Color(settings.dynamicColorSeed)) }

                    SuperDialog(
                        title = "选择颜色",
                        show = mutableStateOf(showColorDialog),
                        onDismissRequest = { showColorDialog = false }
                    ) {
                        Column {
                            ColorPicker(
                                initialColor = selectedColor,
                                onColorChanged = { selectedColor = it }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    modifier = Modifier.weight(1f),
                                    text = "取消",
                                    onClick = { showColorDialog = false }
                                )
                                TextButton(
                                    modifier = Modifier.weight(1f),
                                    text = "确认",
                                    colors = ButtonDefaults.textButtonColorsPrimary(),
                                    onClick = {
                                        showColorDialog = false
                                        app.settings.update { it.copy(dynamicColorSeed = selectedColor.toArgb()) }
                                    }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = !settings.dynamicColor,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Preference(
                            title = { Text("颜色种子") },
                            icon = { Icon(Icons.Filled.FormatColorFill, null) },
                            summary = { Text(Color(settings.dynamicColorSeed).toHexString()) },
                            onClick = {
                                selectedColor = Color(settings.dynamicColorSeed)
                                showColorDialog = true
                            }
                        )
                    }

                    ListPreference(
                        value = settings.darkTheme,
                        onValueChange = { newValue ->
                            app.settings.update { it.copy(darkTheme = newValue) }
                        },
                        values = darkThemeValues.indices.toList(),
                        valueToText = { AnnotatedString(darkThemeValues[it]) },
                        title = { Text("深色主题") },
                        summary = { Text(darkThemeValues[settings.darkTheme]) },
                        type = ListPreferenceType.DROPDOWN_MENU,
                        icon = { Icon(Icons.Filled.Brightness4, null) }
                    )

                    PreferenceCategory(title = { Text("隐藏API调用") })

                    ListPreference(
                        value = settings.hiddenApiBypass,
                        onValueChange = { newValue ->
                            app.settings.update { it.copy(hiddenApiBypass = newValue) }
                            app.ui.snackbar("重启应用生效", "重启") {
                                val restartIntent =
                                    context.packageManager.getLaunchIntentForPackage(context.packageName)
                                context.startActivity(
                                    Intent.makeRestartActivityTask(restartIntent?.component)
                                        .apply { addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION) },
                                    ActivityOptions.makeCustomAnimation(context, 0, 0).toBundle()
                                )
                                Runtime.getRuntime().exit(0)
                            }
                        },
                        values = hiddenApiBypassValues.indices.toList(),
                        valueToText = { AnnotatedString(hiddenApiBypassValues[it]) },
                        title = { Text("实现方式") },
                        summary = { Text(hiddenApiBypassValues[settings.hiddenApiBypass]) },
                        type = ListPreferenceType.DROPDOWN_MENU,
                        icon = { Icon(Icons.Filled.Api, null) }
                    )
                }
            }
        }
    }
}

fun Color.toHexString(): String = String.format("#%08X", this.toArgb())