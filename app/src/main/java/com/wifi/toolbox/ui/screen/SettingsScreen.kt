package com.wifi.toolbox.ui.screen

import android.app.ActivityOptions
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Language
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
import androidx.core.os.LocaleListCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onMenuClick: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as ToolboxApp
    val settings = app.settings.global

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val hiddenApiBypassValues = listOf(
        stringResource(R.string.no_use),
        stringResource(R.string.lspass), stringResource(R.string.hiddenapibypass)
    )
    val darkThemeValues = listOf(
        stringResource(R.string.follow_system),
        stringResource(R.string.always_on), stringResource(R.string.always_off)
    )

    val languageValues = listOf(
        stringResource(R.string.follow_system),
        "中文（简体）",
        "中文（繁體）",
        "文言（華夏）",
        "English"
    )

    val providerComponent = remember {
        android.content.ComponentName(context, "com.wifi.toolbox.services.AppFileProvider")
    }

    var isProviderEnabled by remember {
        mutableStateOf(
            context.packageManager.getComponentEnabledSetting(providerComponent) ==
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.padding(0.dp, 8.dp)) {
                        Text(
                            text = stringResource(R.string.settings),
                            style = MaterialTheme.typography.titleLarge
                        )
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
                    PreferenceCategory(title = { Text(stringResource(R.string.theme)) })

                    SwitchPreference(
                        value = settings.dynamicColor,
                        onValueChange = { newValue ->
                            app.settings.update { it.copy(dynamicColor = newValue) }
                        },
                        title = { Text(stringResource(R.string.dynamic_color)) },
                        summary = { Text(stringResource(R.string.dynamic_color_tip)) },
                        icon = { Icon(Icons.Outlined.ColorLens, null) }
                    )

                    var showColorDialog by remember { mutableStateOf(false) }
                    var selectedColor by remember { mutableStateOf(Color(settings.dynamicColorSeed)) }

                    SuperDialog(
                        title = stringResource(R.string.choose_color),
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
                                    text = stringResource(R.string.btn_cancel),
                                    onClick = { showColorDialog = false }
                                )
                                TextButton(
                                    modifier = Modifier.weight(1f),
                                    text = stringResource(R.string.btn_confirm),
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
                            title = { Text(stringResource(R.string.color_seed)) },
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
                        title = { Text(stringResource(R.string.dark_theme)) },
                        summary = { Text(darkThemeValues[settings.darkTheme]) },
                        type = ListPreferenceType.DROPDOWN_MENU,
                        icon = { Icon(Icons.Filled.Brightness4, null) }
                    )

                    PreferenceCategory(title = { Text(stringResource(R.string.use_hidden_api)) })

                    ListPreference(
                        value = settings.hiddenApiBypass,
                        onValueChange = { newValue ->
                            app.settings.update { it.copy(hiddenApiBypass = newValue) }
                            app.ui.snackbar(
                                context.getString(R.string.need_restart_app),
                                context.getString(R.string.restart)
                            ) {
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
                        title = { Text(stringResource(R.string.hidden_api_method)) },
                        summary = { Text(hiddenApiBypassValues[settings.hiddenApiBypass]) },
                        type = ListPreferenceType.DROPDOWN_MENU,
                        icon = { Icon(Icons.Filled.Api, null) }
                    )

                    PreferenceCategory(title = { Text(stringResource(R.string.debug)) })

                    SwitchPreference(
                        value = isProviderEnabled,
                        onValueChange = { newValue ->
                            val setting = if (newValue) {
                                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            } else {
                                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                            }

                            context.packageManager.setComponentEnabledSetting(
                                providerComponent,
                                setting,
                                android.content.pm.PackageManager.DONT_KILL_APP
                            )

                            isProviderEnabled = newValue
                        },
                        title = { Text(stringResource(R.string.show_file_in_system)) },
                        summary = { Text(stringResource(R.string.show_file_in_system_tip)) },
                        icon = { Icon(Icons.Default.FolderOpen, null) }
                    )

                    PreferenceCategory(title = { Text(stringResource(R.string.language_settings)) })

                    ListPreference(
                        value = settings.language,
                        onValueChange = { index ->
                            app.settings.update { it.copy(language = index) }

                            val appLocale: LocaleListCompat = when (index) {
                                1 -> LocaleListCompat.forLanguageTags("zh-CN")
                                2 -> LocaleListCompat.forLanguageTags("zh-TW")
                                3 -> LocaleListCompat.forLanguageTags("lzh-CN")
                                4 -> LocaleListCompat.forLanguageTags("en")
                                else -> LocaleListCompat.getEmptyLocaleList()
                            }
                            AppCompatDelegate.setApplicationLocales(appLocale)
                        },
                        values = languageValues.indices.toList(),
                        valueToText = { AnnotatedString(languageValues[it]) },
                        title = { Text(stringResource(R.string.language)) },
                        summary = { Text(languageValues[settings.language]) },
                        type = ListPreferenceType.DROPDOWN_MENU,
                        icon = { Icon(Icons.Default.Language, null) }
                    )
                }
            }
        }
    }
}

fun Color.toHexString(): String = String.format("#%08X", this.toArgb())