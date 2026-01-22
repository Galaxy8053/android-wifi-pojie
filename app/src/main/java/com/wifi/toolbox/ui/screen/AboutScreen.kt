package com.wifi.toolbox.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withContext
import com.wifi.toolbox.BuildConfig
import com.wifi.toolbox.R
import com.wifi.toolbox.ui.items.TagItem
import com.wifi.toolbox.ui.pages.LicensePage
import kotlinx.parcelize.Parcelize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onMenuClick: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val isPreview = LocalInspectionMode.current
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var showLicenseDialog by rememberSaveable { mutableStateOf(false) }
    var changelogText by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var showCreditsDialog by rememberSaveable { mutableStateOf(false) }

    val creditProjects = remember {
        listOf(
            CreditProject(
                "Hail",
                "参考完成设置页的布局\n借鉴了使用Shizuku访问隐藏API的原理",
                "https://github.com/aistra0528/Hail",
                "GPL-3.0 license"
            ),
            CreditProject(
                "WiFiList",
                "借鉴了通过Shizuku在Android11+设备上免root获取已保存wifi的明文密码的实现方式",
                "https://github.com/zacharee/WiFiList",
            ),
            CreditProject(
                "Native Detector",
                "借鉴了可折叠卡片的布局样式并以此为基础稍作修改",
                "https://github.com/reveny/Android-Native-Root-Detector",
                "MIT License"
            ),
            CreditProject(
                "幻影WIFI",
                "借鉴了使用应用API来连接wifi以及监听wifi连接状态的实现方式\n密码字典破解的脚本类型密码由高级字典启发而来"
            ),
            CreditProject(
                "HyperCeiler",
                "v2版本之后的关于页面参考了HyperCeiler的布局样式",
                "https://github.com/ReChronoRain/HyperCeiler"
            ),
            CreditProject(
                "LSPosed",
                "密码字典破解的页面参考了LSPosed的导航栏样式",
                "https://github.com/JingMatrix/LSPosed",
            ),
            CreditProject(
                "清浊",
                "参考了UI架构及侧边导航栏布局",
                "https://www.dircleaner.com/"
            ),
            CreditProject(
                "Easter Eggs",
                "借鉴了折叠块的小箭头以及点按区域设计",
                "https://github.com/hushenghao/AndroidEasterEggs",
                "Apache License 2.0"
            )
        )
    }

    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(showDialog) {
        if (showDialog && changelogText.isEmpty()) {
            changelogText = try {
                context.assets.open("CHANGELOG.md").bufferedReader().use { it.readText() }
            } catch (_: Exception) {
                "此版本未包含CHANGELOG.md"
            }
        }
    }

    val libs = remember {
        runCatching {
            Libs.Builder()
                .withContext(context)
                .build()
        }.getOrElse {
            Libs.Builder().withJson("{}").build()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Image(
            painter = painterResource(id = R.drawable.about_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                AnimatedVisibility(
                    visible = visible,
                    enter = if (isPreview) fadeIn(tween(0)) else fadeIn() + slideInVertically(
                        initialOffsetY = { 40 })
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val drawable = remember(R.mipmap.ic_launcher) {
                            context.packageManager.getDrawable(
                                context.packageName,
                                R.mipmap.ic_launcher,
                                context.applicationInfo
                            )
                        }
                        Surface(
                            modifier = Modifier.size(110.dp),
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shadowElevation = 12.dp
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(model = drawable),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            if (BuildConfig.DEBUG) {
                                TagItem("DEBUG")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedVisibility(
                        visible = visible,
                        enter = if (isPreview) fadeIn(tween(0)) else fadeIn(
                            animationSpec = tween(
                                800,
                                0
                            )
                        ) +
                                slideInVertically(animationSpec = tween(800, 0)) { 100 }
                    ) {
                        SettingsGroup {
                            ClickableInfoItem("更新日志", "查看最近更新的改动内容") {
                                showDialog = true
                            }
                            SettingsDivider()
                            ClickableInfoItem("开源地址", "在GitHub上查看项目源码") {
                                uriHandler.openUri("https://github.com/bszapp/android-wifi-pojie")
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = visible,
                        enter = if (isPreview) fadeIn(tween(0)) else fadeIn(
                            animationSpec = tween(
                                800,
                                200
                            )
                        ) +
                                slideInVertically(animationSpec = tween(800, 200)) { 100 }
                    ) {
                        SettingsGroup {
                            ClickableInfoItem(
                                "鸣谢项目",
                                "感谢为本项目提供灵感与技术支持的开源项目"
                            ) { showCreditsDialog = true }
                            SettingsDivider()
                            ClickableInfoItem(
                                "三方开源许可",
                                "查看第三方库授权协议"
                            ) { showLicenseDialog = true }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(32.dp))

                val buildYear = remember {
                    BuildConfig.BUILD_DATE.substring(0, 4)
                }

                Text(
                    text = "© 2025-$buildYear WiFi Toolbox · Apache-2.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )

            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) { Text("知道了") }
                },
                title = { Text("更新日志", fontWeight = FontWeight.Bold) },
                text = {
                    Box(
                        modifier = Modifier
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(text = changelogText, style = MaterialTheme.typography.bodyMedium)
                    }
                },
                shape = RoundedCornerShape(28.dp)
            )
        }

        if (showCreditsDialog) {
            AlertDialog(
                onDismissRequest = { showCreditsDialog = false },
                confirmButton = {
                    TextButton(onClick = { showCreditsDialog = false }) {
                        Text("关闭")
                    }
                },
                title = {
                    Text("鸣谢项目", fontWeight = FontWeight.Bold)
                },
                text = {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        items(creditProjects) { project ->
                            CreditProjectCard(project, uriHandler)
                        }

                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        }

                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(Modifier.width(6.dp))

                                Text(
                                    "专业名词解释",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                "• 参考：指看着界面直接仿写\n" +
                                        "• 借鉴：指 CtrlCV 代码然后洗稿",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(28.dp)
            )


        }

        BackHandler(enabled = showLicenseDialog) {
            showLicenseDialog = false
        }

        AnimatedVisibility(
            visible = showLicenseDialog,
            enter = if (isPreview) fadeIn(tween(0)) else slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                LicensePage(libs = libs, onBack = { showLicenseDialog = false })
            }
        }
    }
}

@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            content = content
        )
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
fun ClickableInfoItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            painter = rememberVectorPainter(Icons.AutoMirrored.Filled.KeyboardArrowRight),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Parcelize
data class CreditProject(
    val name: String,
    val description: String,
    val link: String = "",
    val license: String = ""
) : android.os.Parcelable

@Composable
fun CreditProjectCard(
    project: CreditProject,
    uriHandler: UriHandler
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        onClick = { expanded = !expanded }
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(14.dp)
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(2.dp)
                        )
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            project.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        project.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
                if (project.link.isNotBlank() || project.license.isNotBlank()) {
                    val angle by animateFloatAsState(if (expanded) 180f else 0f)
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(angle)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    if (project.link.isNotBlank()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        ProjectDetailItem(
                            icon = Icons.Default.Link,
                            text = project.link,
                            onClick = { uriHandler.openUri(project.link) }
                        )
                    }
                    if (project.license.isNotBlank()) {
                        ProjectDetailItem(
                            icon = Icons.Default.Gavel,
                            text = project.license
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectDetailItem(
    icon: ImageVector,
    text: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick ?: {})
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showSystemUi = true)
@Composable
fun AboutScreenPreview() {
    MaterialTheme {
        AboutScreen(onMenuClick = {})
    }
}