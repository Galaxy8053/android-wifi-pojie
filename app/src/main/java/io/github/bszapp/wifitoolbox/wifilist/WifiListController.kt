package io.github.bszapp.wifitoolbox.wifilist

import android.net.wifi.ScanResult
import io.github.bszapp.wifitoolbox.contract.wifilist.IWifiListController
import io.github.bszapp.wifitoolbox.contract.wifilist.ScanState
import io.github.bszapp.wifitoolbox.contract.wifilist.ScanStatus
import io.github.bszapp.wifitoolbox.services.mainservice.IMainService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WifiListController(
    private val getService: () -> IMainService?
) : IWifiListController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scanJob: Job? = null

    private val _state = MutableStateFlow(ScanState())
    override val state: StateFlow<ScanState> = _state.asStateFlow()

    override fun startScan() {
        val service = getService() ?: run {
            _state.value = ScanState(
                status = ScanStatus.ERROR,
                errorException = Exception("服务未运行，请先启动服务")
            )
            return
        }

        scanJob?.cancel()
        scanJob = scope.launch {
            try {
                service.startScan()
                _state.value = ScanState(status = ScanStatus.SCANNING)

                var latestResults: List<ScanResult> = emptyList()
                val endTime = System.currentTimeMillis() + 3_000L

                while (System.currentTimeMillis() < endTime) {
                    delay(250)
                    val results = service.getScanResults()
                    if (!results.isNullOrEmpty()) {
                        latestResults = results
                        _state.value = ScanState(
                            status = ScanStatus.SCANNING,
                            scanResults = latestResults
                        )
                    }
                }

                _state.value = ScanState(
                    status = ScanStatus.FINISH,
                    scanResults = latestResults
                )
            } catch (e: Exception) {
                _state.value = ScanState(
                    status = ScanStatus.ERROR,
                    errorException = e
                )
            }
        }
    }
}