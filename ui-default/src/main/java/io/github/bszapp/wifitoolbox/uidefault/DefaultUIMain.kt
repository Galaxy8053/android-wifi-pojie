package io.github.bszapp.wifitoolbox.uidefault

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DefaultUI(viewModel: DefaultViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 这里只放服务运行中的正式功能 UI
            Text(
                text = state.message,
                style = MaterialTheme.typography.titleMedium
            )

            // 未来在这里扩展 Wi-Fi 功能等
        }
    }
}