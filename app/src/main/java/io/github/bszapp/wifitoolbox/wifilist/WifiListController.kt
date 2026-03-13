@file:Suppress("DEPRECATION")

package io.github.bszapp.wifitoolbox.wifilist

import android.net.wifi.ScanResult
import android.util.Log
import io.github.bszapp.wifitoolbox.contract.wifilist.IWifiListController
import io.github.bszapp.wifitoolbox.contract.wifilist.ScanState
import io.github.bszapp.wifitoolbox.contract.wifilist.ScanStatus
import io.github.bszapp.wifitoolbox.services.mainservice.IMainService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WifiListController(
    private val scope: CoroutineScope,
    private val getService: () -> IMainService?,
) : IWifiListController {

    private var scanJob: Job? = null

    private val _state = MutableStateFlow(ScanState())
    override val state: StateFlow<ScanState> = _state.asStateFlow()

    private var previousResults: Map<String, ScanResult> = emptyMap()

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
                val startTime = System.currentTimeMillis()
                Log.d(TAG, "service.startScan() 返回 $startResult，开始时间：$startTime")

                if (!startResult) {
                    // 启动失败：直接拉取缓存结果，标记完成
                    Log.w(TAG, "startScan() 返回 false，拉取缓存结果直接完成")
                    val cached = service.getScanResults()
                    diffAndLog(cached)
                    previousResults = cached.associateBy { it.BSSID }
                    _state.value = ScanState(
                        status = ScanStatus.FINISH,
                        scanResults = cached,
                        startResult = false
                    )
                    return@launch
                }

                // 启动成功：先标记 SCANNING，等待 0.5 秒让扫描开始积累结果
                _state.value = ScanState(
                    status = ScanStatus.SCANNING,
                    startResult = true,
                    scanResults = emptyList()
                )
                delay(500L)

                // 循环拉取，每 0.25 秒一次，共持续 3 秒（从 startScan 调用起计）
                // 即最多拉取约 10 次（3000 - 500 = 2500ms，每 250ms 一次）
                while (System.currentTimeMillis() - startTime < 3_000L) {
                    val results = service.getScanResults()
                    diffAndLog(results)
                    previousResults = results.associateBy { it.BSSID }
                    val elapsed = System.currentTimeMillis() - startTime
                    Log.d(TAG, "轮询拉取：结果数=${results.size}，已用时=${elapsed}ms")
                    _state.value = ScanState(
                        status = ScanStatus.SCANNING,
                        scanResults = results,
                        startResult = true
                    )
                    delay(250L)
                }

                // 3 秒结束，最后拉取一次作为最终结果
                val finalResults = service.getScanResults()
                diffAndLog(finalResults)
                previousResults = finalResults.associateBy { it.BSSID }
                Log.d(TAG, "扫描结束，最终结果数=${finalResults.size}")
                _state.value = ScanState(
                    status = ScanStatus.FINISH,
                    scanResults = finalResults,
                    startResult = true
                )

            } catch (e: CancellationException) {
                Log.d(TAG, "扫描协程被取消（用户重新发起扫描）")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "扫描过程中发生异常：${e.message}", e)
                _state.value = ScanState(
                    status = ScanStatus.ERROR_UNKNOWN,
                    errorException = e
                )
            }
        }
    }

    private fun diffAndLog(newList: List<ScanResult>) {
        if (previousResults.isEmpty()) return
        val newMap = newList.associateBy { it.BSSID }
        val appeared = newMap.keys - previousResults.keys
        val disappeared = previousResults.keys - newMap.keys
        appeared.forEach { Log.d(TAG, "新增 AP: ${newMap[it]?.SSID} [$it]") }
        disappeared.forEach { Log.d(TAG, "消失 AP: ${previousResults[it]?.SSID} [$it]") }
    }

    companion object {
        private const val TAG = "WifiListController"
    }
}