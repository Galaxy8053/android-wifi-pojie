package com.wifi.toolbox.ui.items

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wifi.toolbox.BuildConfig
import com.wifi.toolbox.R
import com.wifi.toolbox.ui.screen.AboutScreen
import com.wifi.toolbox.ui.screen.HomeScreen
import com.wifi.toolbox.ui.screen.ManageScreen
import com.wifi.toolbox.ui.screen.PojieScreen
import com.wifi.toolbox.ui.screen.SettingsScreen
import com.wifi.toolbox.ui.screen.TestScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Scaffold

private val DrawerWidth = 310.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav(pendingNavigation: MutableState<String?>) {
    val view = LocalView.current
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    var currentTip by remember {
        mutableStateOf(readRandomTipFromAssets(context))
    }
    var tipPrepared by remember { mutableStateOf(false) }
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(pendingNavigation.value) {
        pendingNavigation.value?.let {
            navController.navigate(it) {
                popUpTo("Home") { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            pendingNavigation.value = null
        }
    }

    val density = LocalDensity.current
    val drawerWidthPx = with(density) { DrawerWidth.toPx() }

    LaunchedEffect(drawerState) {
        snapshotFlow { drawerState.currentOffset }
            .collect { offset ->
                if (offset.isNaN()) return@collect

                val progress = ((drawerWidthPx + offset) / drawerWidthPx).coerceIn(0f, 1f)

                if (progress < 0.01f) {
                    if (tipPrepared) {
                        tipPrepared = false
                    }
                }

                if (progress > 0.01f && !tipPrepared) {
                    currentTip = readRandomTipFromAssets(context)
                    tipPrepared = true
                }
            }
    }
    BackHandler(enabled = currentRoute != "Home") {
        navController.navigate("Home") {
            popUpTo("Home") { inclusive = true }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState, drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(DrawerWidth)
                    .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                NavContent(currentTip, currentRoute, view, scope, drawerState, navController)
            }
        }) {
        Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
            NavHost(
                navController = navController,
                startDestination = "Home",
                modifier = Modifier.padding(padding)
            ) {
                composable("Home") { HomeScreen { scope.launch { drawerState.open() } } }
                composable("Settings") { SettingsScreen { scope.launch { drawerState.open() } } }
                composable("Pojie") { PojieScreen { scope.launch { drawerState.open() } } }
                composable("Viewer") { ManageScreen { scope.launch { drawerState.open() } } }
                composable("Test") { TestScreen { scope.launch { drawerState.open() } } }
                composable("About") { AboutScreen { scope.launch { drawerState.open() } } }
            }
        }
    }
}

@Composable
private fun DrawerSection(
    title: String, icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, bottom = 12.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun DrawerDivider(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}


@Composable
private fun DrawerItem(
    label: String, route: String, icon: ImageVector, currentRoute: String?, onClick: () -> Unit
) {
    val selected = currentRoute == route
    val background by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surface
    )
    val iconColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant
    )
    val elevation by animateDpAsState(if (selected) 2.dp else 0.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp)),
        tonalElevation = elevation,
        color = background,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = iconColor)
            Spacer(Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = iconColor
            )
        }
    }
}

fun readRandomTipFromAssets(context: Context): String {
    return try {
        val tips = context.assets.open("apptip/tip.txt").bufferedReader().readLines()
            .filter { it.isNotBlank() }

        if (tips.isNotEmpty()) {
            tips.random()
        } else {
            ""
        }
    } catch (_: Exception) {
        ""
    }
}

@Composable
fun NavContent(
    currentTip: String,
    currentRoute: String?,
    view: View,
    scope: CoroutineScope,
    drawerState: DrawerState,
    navController: NavHostController
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(2.dp))
            TagItem(BuildConfig.VERSION_NAME)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (currentTip.isNotBlank()) "Tip: $currentTip" else "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        DrawerSection("主页", Icons.Rounded.Home)
        DrawerItem(
            label = "主页",
            route = "Home",
            icon = Icons.Outlined.Home,
            currentRoute = currentRoute,
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                scope.launch { drawerState.close() }
                if (currentRoute != "Home") {
                    navController.navigate("Home") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            })

        DrawerDivider()
        DrawerSection("工具箱", Icons.Rounded.Science)

        DrawerItem("密码字典破解", "Pojie", Icons.Outlined.LockOpen, currentRoute) {
            scope.launch { drawerState.close() }
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            if (currentRoute != "Pojie") navController.navigate("Pojie")
        }

        DrawerItem("WiFi 管理器", "Viewer", Icons.Filled.Dns, currentRoute) {
            scope.launch { drawerState.close() }
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            if (currentRoute != "Viewer") navController.navigate("Viewer")
        }

        DrawerItem("实验室", "Test", Icons.Outlined.Science, currentRoute) {
            scope.launch { drawerState.close() }
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            if (currentRoute != "Test") navController.navigate("Test")
        }

        DrawerDivider()
        DrawerSection("选项", Icons.Rounded.Settings)

        DrawerItem("设置", "Settings", Icons.Outlined.Settings, currentRoute) {
            scope.launch { drawerState.close() }
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            if (currentRoute != "Settings") navController.navigate("Settings")
        }

        DrawerItem("关于", "About", Icons.Filled.Info, currentRoute) {
            scope.launch { drawerState.close() }
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            if (currentRoute != "About") navController.navigate("About")
        }
    }
}