package io.github.bszapp.wifitoolbox.uidefault.widget

import android.net.wifi.ScanResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.bszapp.wifitoolbox.uidefault.DefaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiList(
    vm: DefaultViewModel = viewModel(),
    listState: LazyListState = rememberLazyListState()
) {
    val scanResults by vm.wifiList.results.collectAsStateWithLifecycle()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = scanResults.sortedByDescending { it.level },
            key = { it.BSSID ?: it.hashCode() }
        ) { ap ->
            WifiTextItem(ap = ap, modifier = Modifier.animateItem())
        }
    }
}

@Composable
private fun WifiTextItem(ap: ScanResult, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = ap.SSID?.ifEmpty { "<Hidden Network>" } ?: "<Unknown>",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "BSSID: ${ap.BSSID}\nRSSI: ${ap.level} dBm | Freq: ${ap.frequency}MHz\nCaps: ${ap.capabilities}",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp), thickness = 0.5.dp)
    }
}