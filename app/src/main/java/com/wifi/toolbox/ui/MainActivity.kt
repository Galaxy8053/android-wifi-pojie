package com.wifi.toolbox.ui

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.lifecycle.ViewModel
import com.wifi.toolbox.BuildConfig
import com.wifi.toolbox.ToolboxApp
import com.wifi.toolbox.ui.items.AppNav
import com.wifi.toolbox.ui.theme.AppTheme
import com.wifi.toolbox.ui.theme.defaultColorSeed
import com.wifi.toolbox.utils.ApiUtil
import com.wifi.toolbox.utils.ShizukuUtil
import io.github.rosemoe.sora.widget.CodeEditor
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.eclipse.tm4e.core.registry.IThemeSource


class EditorViewModel : ViewModel() {
    var editorInstance: CodeEditor? = null
    var isEditorOpen by mutableStateOf(false)
    var editorInitialContent by mutableStateOf("")
    var originalContent by mutableStateOf("")
    var editorLanguage by mutableStateOf("text")
    var errorMessage by mutableStateOf<String?>(null)
    var onSaveAction by mutableStateOf<(String) -> Unit>({})
    var showExitConfirmDialog by mutableStateOf(false)

    fun handleBackPress(hideKeyboard: () -> Unit) {
        val currentText = editorInstance?.text.toString()
        if (currentText != originalContent) {
            showExitConfirmDialog = true
        } else {
            closeAndRelease(hideKeyboard)
        }
    }

    fun closeAndRelease(hideKeyboard: () -> Unit) {
        hideKeyboard() // 先隐藏键盘
        isEditorOpen = false
        editorInstance = null
        showExitConfirmDialog = false
    }
}

class EditorController(private val vm: EditorViewModel) {
    val isEditorOpen get() = vm.isEditorOpen
    fun open(content: String, language: String = "text", onSave: (String) -> Unit) {
        vm.editorInitialContent = content
        vm.originalContent = content
        vm.editorLanguage = language
        vm.onSaveAction = onSave
        vm.isEditorOpen = true
    }
}

val LocalEditorController = staticCompositionLocalOf<EditorController> { error("No Controller") }

class MainActivity : ComponentActivity() {
    private var pendingNavigation = mutableStateOf<String?>(null)
    private val editorViewModel: EditorViewModel by viewModels()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("target")?.let { pendingNavigation.value = it }
    }

    private fun handleIntent(intent: Intent) {
        intent.getStringExtra("target")?.let { pendingNavigation.value = it }
    }

    val handleSave: (String, () -> Unit) -> Unit = { text, onSuccess ->
        try {
            editorViewModel.onSaveAction(text)
            editorViewModel.originalContent = text
            onSuccess()
        } catch (e: Exception) {
            editorViewModel.errorMessage = e.message ?: e.toString()
        }
    }

    private fun handleCrashRecovery(intent: Intent?) {
        val isRecovery = intent?.getBooleanExtra("CRASH_RECOVERY", false) ?: false
        if (isRecovery) {
            val msg = intent.getStringExtra("ERROR_MESSAGE") ?: "未知错误"
            val stack = intent.getStringExtra("ERROR_STACK") ?: ""

            val app = application as ToolboxApp

            app.alert(
                title = "崩溃啦",
                text = "刚才发生了不可逆的错误，应用已自动重启\n$msg\n你可以将此截图发给开发者，以及描述刚才做了什么（但能不能修复就是另一回事了）"
            )

            if (BuildConfig.DEBUG) {
                println("Crash Recovery StackTrace:\n$stack")
            }
            intent.removeExtra("CRASH_RECOVERY")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleCrashRecovery(intent)
        handleIntent(intent)

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false //小米设备导航栏修复

        ShizukuUtil.initialize(applicationContext)
        val sharedPreferences = getSharedPreferences("settings_global", MODE_PRIVATE)

        setContent {
            val app = LocalContext.current.applicationContext as ToolboxApp
            val snackbarHostState = remember { SnackbarHostState() }
            val editorController = remember { EditorController(editorViewModel) }
            val keyboardController = LocalSoftwareKeyboardController.current

            LaunchedEffect(Unit) {
                app.snackbarState.collect { data ->
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        data.message,
                        data.actionLabel,
                        duration = SnackbarDuration.Short
                    ).let {
                        if (it == SnackbarResult.ActionPerformed) data.onActionClick?.invoke()
                    }
                }
            }

            var dynamicColor by remember {
                mutableStateOf(
                    sharedPreferences.getBoolean(
                        "dynamic_color",
                        true
                    )
                )
            }
            var dynamicColorSeed by remember {
                mutableIntStateOf(
                    sharedPreferences.getInt(
                        "dynamic_color_seed",
                        defaultColorSeed.toArgb()
                    )
                )
            }
            var darkThemeSetting by remember {
                mutableIntStateOf(
                    sharedPreferences.getInt(
                        "dark_theme",
                        0
                    )
                )
            }
            var hiddenApiBypassIndex by remember {
                mutableIntStateOf(
                    sharedPreferences.getInt(
                        "hidden_api_bypass",
                        1
                    )
                )
            }

            val useDarkTheme = when (darkThemeSetting) {
                1 -> true
                2 -> false
                else -> isSystemInDarkTheme()
            }

            MiuixTheme(ThemeController(isDark = useDarkTheme)) {
                CompositionLocalProvider(LocalEditorController provides editorController) {
                    AppTheme(
                        darkTheme = useDarkTheme,
                        dynamicColor = dynamicColor,
                        dynamicColorSeed = Color(dynamicColorSeed)
                    ) {
                        val alertDialogData by app.alertDialogState.collectAsState(initial = null)
                        val showDialog = remember { mutableStateOf(false) }
                        LaunchedEffect(alertDialogData) {
                            if (alertDialogData != null) showDialog.value = true
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            Scaffold(modifier = Modifier.fillMaxSize()) {
                                AppNav(
                                    dynamicColor = dynamicColor,
                                    onDynamicColorChange = {
                                        dynamicColor = it; sharedPreferences.edit {
                                        putBoolean(
                                            "dynamic_color",
                                            it
                                        )
                                    }
                                    },
                                    dynamicColorSeed = dynamicColorSeed,
                                    onDynamicColorSeedChange = {
                                        dynamicColorSeed = it; sharedPreferences.edit {
                                        putInt(
                                            "dynamic_color_seed",
                                            it
                                        )
                                    }
                                    },
                                    darkThemeSetting = darkThemeSetting,
                                    onDarkThemeSettingChange = {
                                        darkThemeSetting =
                                            it; sharedPreferences.edit { putInt("dark_theme", it) }
                                    },
                                    hiddenApiBypass = hiddenApiBypassIndex,
                                    onHiddenApiBypassChange = {
                                        hiddenApiBypassIndex = it; sharedPreferences.edit {
                                        putInt(
                                            "hidden_api_bypass",
                                            it
                                        )
                                    }
                                        app.ui.snackbar("重启应用生效", "重启") {
                                            val restartIntent =
                                                packageManager.getLaunchIntentForPackage(packageName)
                                            startActivity(
                                                Intent.makeRestartActivityTask(
                                                    restartIntent?.component
                                                )
                                                    .apply { addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION) },
                                                ActivityOptions.makeCustomAnimation(
                                                    this@MainActivity,
                                                    0,
                                                    0
                                                ).toBundle()
                                            )
                                            Runtime.getRuntime().exit(0)
                                        }
                                    },
                                    pendingNavigation = pendingNavigation
                                )
                            }

                            AnimatedVisibility(
                                visible = editorViewModel.isEditorOpen,
                                enter = slideInHorizontally(initialOffsetX = { it }),
                                exit = slideOutHorizontally(targetOffsetX = { it })
                            ) {

                                BackHandler(enabled = editorViewModel.isEditorOpen) {
                                    editorViewModel.handleBackPress { keyboardController?.hide() }
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .imePadding(), // 关键：自动为键盘留出空间
                                    color = MaterialTheme.colorScheme.background
                                ) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        TopAppBar(
                                            title = { Text("Code Editor") },
                                            navigationIcon = {
                                                IconButton(onClick = {
                                                    editorViewModel.handleBackPress { keyboardController?.hide() }
                                                    //TODO:到底是哪里的BUG咋返回按钮点了键盘关不上
                                                }) {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.ArrowBack,
                                                        null
                                                    )
                                                }
                                            },
                                            actions = {
                                                TextButton(onClick = {
                                                    val text =
                                                        editorViewModel.editorInstance?.text.toString()
                                                    handleSave(text) {
                                                        app.ui.snackbar("已保存", null) {}
                                                    }
                                                }) { Text("保存") }
                                            }
                                        )
                                        AndroidView(
                                            modifier = Modifier.fillMaxSize(),
                                            factory = { context ->
                                                editorViewModel.editorInstance?.let { existingView ->
                                                    (existingView.parent as? ViewGroup)?.removeView(existingView)
                                                    existingView
                                                } ?: CodeEditor(context).apply {
                                                    layoutParams = ViewGroup.LayoutParams(-1, -1)
                                                    typefaceText = Typeface.MONOSPACE
                                                    nonPrintablePaintingFlags = 28
                                                    setText(editorViewModel.editorInitialContent)

                                                    val fileProvider = FileProviderRegistry.getInstance()
                                                    fileProvider.addFileProvider(AssetsFileResolver(context.assets))
                                                    val grammarRegistry = GrammarRegistry.getInstance()
                                                    grammarRegistry.loadGrammars("textmate/languages.json")

                                                    val themeRegistry = ThemeRegistry.getInstance()
                                                    val themeName = if (useDarkTheme) "darcula" else "quietlight"
                                                    val themePath = "textmate/$themeName.json"

                                                    try {
                                                        if (themeRegistry.findThemeByFileName(themeName) == null) {
                                                            themeRegistry.loadTheme(
                                                                ThemeModel(
                                                                    IThemeSource.fromInputStream(
                                                                        fileProvider.tryGetInputStream(themePath)!!,
                                                                        themePath,
                                                                        null
                                                                    ),
                                                                    themeName
                                                                ).apply { isDark = useDarkTheme }
                                                            )
                                                        }
                                                        themeRegistry.setTheme(themeName)
                                                        colorScheme = TextMateColorScheme.create(themeRegistry)
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }

                                                    if (editorViewModel.editorLanguage == "js") {
                                                        setEditorLanguage(TextMateLanguage.create("source.js", true))
                                                    } else {
                                                        setEditorLanguage(null)
                                                    }

                                                    editorViewModel.editorInstance = this
                                                }
                                            },
                                            update = { view ->
                                                val themeRegistry = ThemeRegistry.getInstance()
                                                val themeName = if (useDarkTheme) "darcula" else "quietlight"
                                                val existingTheme = themeRegistry.findThemeByFileName(themeName)

                                                if (existingTheme != null) {
                                                    if (themeRegistry.currentThemeModel != existingTheme) {
                                                        themeRegistry.setTheme(themeName)
                                                        view.colorScheme = TextMateColorScheme.create(themeRegistry)
                                                    }
                                                } else {
                                                    try {
                                                        val fileProvider = FileProviderRegistry.getInstance()
                                                        val themePath = "textmate/$themeName.json"
                                                        themeRegistry.loadTheme(
                                                            ThemeModel(
                                                                IThemeSource.fromInputStream(
                                                                    fileProvider.tryGetInputStream(themePath)!!,
                                                                    themePath,
                                                                    null
                                                                ),
                                                                themeName
                                                            ).apply { isDark = useDarkTheme }
                                                        )
                                                        view.colorScheme = TextMateColorScheme.create(themeRegistry)
                                                    } catch (e: Exception) { }
                                                }
                                                view.invalidate()
                                            }
                                        )
                                    }
                                }
                            }

                            if (editorViewModel.showExitConfirmDialog) {
                                AlertDialog(
                                    onDismissRequest = {
                                        editorViewModel.showExitConfirmDialog = false
                                    },
                                    title = { Text("提示") },
                                    text = { Text("文档已修改，是否保存？") },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            val text =
                                                editorViewModel.editorInstance?.text.toString()
                                            handleSave(text) {
                                                editorViewModel.closeAndRelease { keyboardController?.hide() }
                                            }
                                        }) { Text("保存") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = {
                                            editorViewModel.closeAndRelease { keyboardController?.hide() }
                                        }) { Text("不保存") }
                                    }
                                )
                            }

                            if (alertDialogData != null) {
                                SuperDialog(
                                    title = alertDialogData?.title ?: "",
                                    summary = alertDialogData?.text ?: "",
                                    show = showDialog,
                                    onDismissRequest = { app.ui.dismissAlert() }) {
                                    TextButton(
                                        text = "确定",
                                        onClick = { app.ui.dismissAlert() },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            SnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp)
                            )

                            if (BuildConfig.DEBUG) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawIntoCanvas { canvas ->
                                        canvas.nativeCanvas.save()
                                        canvas.nativeCanvas.translate(drawContext.size.width, 0f)
                                        canvas.nativeCanvas.rotate(45f)
                                        val paint = android.graphics.Paint()
                                            .apply { color = "#77FF0000".toColorInt() }
                                        canvas.nativeCanvas.drawRect(-200f, 90f, 200f, 170f, paint)
                                        val textPaint = android.graphics.Paint().apply {
                                            color = android.graphics.Color.WHITE
                                            textSize = 32f
                                            textAlign = android.graphics.Paint.Align.CENTER
                                            isFakeBoldText = true
                                        }
                                        canvas.nativeCanvas.drawText("DEBUG", 0f, 130f, textPaint)
                                        textPaint.textSize = 24f
                                        textPaint.isFakeBoldText = false
                                        canvas.nativeCanvas.drawText(
                                            "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})_${BuildConfig.BUILD_COUNT}",
                                            0f,
                                            160f,
                                            textPaint
                                        )
                                        canvas.nativeCanvas.restore()
                                    }
                                }
                            }

                            if (editorViewModel.errorMessage != null) {
                                AlertDialog(
                                    onDismissRequest = { editorViewModel.errorMessage = null },
                                    title = { Text("保存失败") },
                                    text = { Text(editorViewModel.errorMessage ?: "") },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            editorViewModel.errorMessage = null
                                            editorViewModel.showExitConfirmDialog = false
                                        }) { Text("确定") }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    var pendingLocationCallback: (() -> Unit)? = null

    val locationLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        Log.d("MainActivity", "返回")
        if (result.resultCode == RESULT_OK) {
            if (ApiUtil.isLocationEnabled(this)) {
                pendingLocationCallback?.invoke()
                pendingLocationCallback = null
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "返回页面")
        if (pendingLocationCallback != null && ApiUtil.isLocationEnabled(this)) {
            pendingLocationCallback?.invoke()
            pendingLocationCallback = null
        }
    }

    var pendingPermissionCallback: (() -> Unit)? = null

    val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            pendingPermissionCallback?.invoke()
            pendingPermissionCallback = null
        }
    }
}
