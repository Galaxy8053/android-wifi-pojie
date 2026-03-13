@file:Suppress("DEPRECATION")

package io.github.bszapp.wifitoolbox.wifilist

import android.util.Log
import io.github.bszapp.wifitoolbox.contract.wifilist.IWifiListController
import io.github.bszapp.wifitoolbox.contract.wifilist.ScanState
import io.github.bszapp.wifitoolbox.contract.wifilist.ScanStatus
import io.github.bszapp.wifitoolbox.services.mainservice.IMainService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class WifiListController(
    private val scope: CoroutineScope,
    private val getService: () -> IMainService?,
    private val scanResultsAvailable: MutableSharedFlow<Boolean>
) : IWifiListController {

    private var scanJob: Job? = null

    private val _state = MutableStateFlow(ScanState())
    override val state: StateFlow<ScanState> = _state.asStateFlow()

    override fun startScan() {
        val service = getService() ?: run {
            Log.e(TAG, "startScan() 失败：服务未运行（getService() 返回 null）")
            _state.value = ScanState(
                status = ScanStatus.ERROR_UNKNOWN,
                errorException = Exception("服务未运行")
            )
            return
        }

        Log.d(TAG, "获取到服务实例，准备启动扫描协程")
        scanJob?.cancel()
        scanJob = scope.launch(Dispatchers.IO) {
            try {
                val startResult = service.startScan()
                Log.d(TAG, "service.startScan() 返回 $startResult")
                _state.value = ScanState(status = ScanStatus.SCANNING, startResult = startResult)

                if (startResult) {
                    // 等待系统广播通知扫描结果已就绪，最长等待 30 秒
                    val received = withTimeoutOrNull(30_000L) {
                        scanResultsAvailable.first()
                    }
                    if (received == null) {
                        Log.w(TAG, "等待扫描结果广播超时（30s），强制读取当前结果")
                    } else {
                        Log.d(TAG, "收到广播，结果是否更新：$received")
                    }
                }

                val results = service.getScanResults()
                Log.d(TAG, "扫描完成，最终结果数量：${results.size}")
                _state.value = ScanState(
                    status = ScanStatus.FINISH,
                    scanResults = results,
                    startResult = startResult
                )
            } catch (e: Exception) {
                Log.e(TAG, "扫描过程中发生异常：${e.message}", e)
                _state.value = ScanState(
                    status = ScanStatus.ERROR_UNKNOWN,
                    errorException = e
                )
            }
        }
    }

    companion object {
        private const val TAG = "WifiListController"
    }
}