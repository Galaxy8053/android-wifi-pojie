package com.wifi.toolbox.ui.items.pojie

import android.net.wifi.WifiManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wifi.toolbox.R
import com.wifi.toolbox.ToolboxApp
import com.wifi.toolbox.structs.*
import com.wifi.toolbox.ui.items.WifiIcon

@Composable
fun WifiPojieItem(
    modifier: Modifier,
    wifi: WifiInfo,
    runningInfo: PojieRunInfo?,
    finishedInfo: String?,
    onStartClick: ((String) -> Unit) = {},
    onStopClick: ((String) -> Unit) = {},
) {
    val stableInfo = remember { mutableStateOf<PojieRunInfo?>(null) }
    val stableFinishedInfo = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(runningInfo) { if (runningInfo != null) stableInfo.value = runningInfo }
    LaunchedEffect(finishedInfo) {
        if (finishedInfo != null) stableFinishedInfo.value = finishedInfo
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {}) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            @Suppress("DEPRECATION")
            val levelIndex =
                if (wifi.level == 0) 0 else WifiManager.calculateSignalLevel(wifi.level, 5)
            WifiIcon(modifier = Modifier.size(28.dp), level = levelIndex)
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                WifiItemMainHeader(
                    wifi,
                    finishedInfo,
                    stableFinishedInfo.value,
                    runningInfo,
                    onStartClick,
                    onStopClick
                )
                WifiItemRunningProgress(runningInfo, stableInfo.value)
            }
        }
    }
}

@Composable
private fun WifiItemMainHeader(
    wifi: WifiInfo,
    finishedInfo: String?,
    stableFinishedInfo: String?,
    runningInfo: PojieRunInfo?,
    onStartClick: (String) -> Unit,
    onStopClick: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = wifi.ssid, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(
                text = if (wifi.level == 0) stringResource(R.string.unknown) else stringResource(
                    R.string.dbm_string,
                    wifi.level
                ),
                style = MaterialTheme.typography.bodySmall
            )
            AnimatedVisibility(
                visible = finishedInfo != null,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Text(
                    text = stableFinishedInfo ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        val isRunning = runningInfo != null
        Button(
            onClick = { if (isRunning) onStopClick(wifi.ssid) else onStartClick(wifi.ssid) },
            colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) Color(0xFFFF4444) else MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                tint = if (isRunning) Color.White else MaterialTheme.colorScheme.onPrimary,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = if (isRunning) stringResource(R.string.btn_stop) else stringResource(R.string.btn_start),
                color = if (isRunning) Color.White else MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun WifiItemRunningProgress(runningInfo: PojieRunInfo?, info: PojieRunInfo?) {
    AnimatedVisibility(
        visible = runningInfo != null,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        info?.let {
            val total = it.tryList.size
            val current = it.tryIndex
            val progress = if (total > 0) current.toFloat() / total else 0f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f))
                    .padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.RocketLaunch,
                        null,
                        Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.status_running),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight(600),
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (total == 0) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.1f)
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (total > 0) stringResource(
                            R.string.progress_format,
                            current,
                            total,
                            progress * 100
                        )
                        else stringResource(R.string.progress_unknown),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    val speed = calculateAverageSpeed(it)
                    Text(
                        text = if (speed != null) {
                            stringResource(R.string.run_speed_minute, speed.toInt())
                        } else {
                            "-/m"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Text(
                    text = it.textTip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}

fun calculateAverageSpeed(task: PojieRunInfo): Double? {
    val costs = task.costList
    if (costs.size < 5) return null

    val avgMs = costs.average()
    if (avgMs <= 0) return null

    return 60000.0 / avgMs
}