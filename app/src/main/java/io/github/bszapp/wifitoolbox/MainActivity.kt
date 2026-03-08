package io.github.bszapp.wifitoolbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import io.github.bszapp.wifitoolbox.contract.ToolboxControllerProvider
import io.github.bszapp.wifitoolbox.contract.ToolboxStatus
import io.github.bszapp.wifitoolbox.uidefault.DefaultUI
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainActivity : ComponentActivity() {

    private val controller by lazy { ToolboxControllerProvider.get() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 监听状态驱动 setContent 切换
        controller.state
            .onEach { state ->
                when (state.status) {
                    ToolboxStatus.RUNNING -> setContent {
                        DefaultUI()  // 服务跑起来了，加载对应 UI 方案
                    }
                    else -> setContent {
                        AuthUI(      // 服务未启动/出错，显示授权界面
                            state = state,
                            onShizuku = { controller.launchShizuku() },
                            onRoot = { controller.launchRoot() }
                        )
                    }
                }
            }
            .launchIn(lifecycleScope)
    }
}