package com.wifi.toolbox.services.pojie

import androidx.compose.runtime.snapshotFlow
import com.wifi.toolbox.ToolboxApp
import com.wifi.toolbox.services.PojieService
import com.wifi.toolbox.structs.*
import com.wifi.toolbox.utils.ShizukuUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.scan
import kotlin.math.pow

/**
 * 负责管理全部任务排队运行
 */
class PojieTaskManager(
    private val service: PojieService,
    private val scope: CoroutineScope,
    private val worker: ConnectWorker
) {

    private var mainLoopJob: Job? = null
    private var currentWorkerJob: Job? = null

    @Volatile
    private var currentWorkingSsid: String? = null
    private val handledSsids = mutableSetOf<String>()

    /**
     * 判断调度器当前是否正在工作
     * @return 是否有作业正在运行
     */
    fun isWorking(): Boolean {
        return mainLoopJob != null && mainLoopJob!!.isActive
    }

    /**
     * 取消当前正在执行的任务作业
     */
    fun cancelCurrentJob() {
        currentWorkerJob?.cancel()
        mainLoopJob?.cancel()
    }

    /**
     * 启动服务的主循环逻辑
     * @param settings 当前的破解设置
     */
    fun startLoop(settings: PojieSettings) {
        val app = service.applicationContext as ToolboxApp

        scope.launch {
            while (isActive) {
                if (settings.connectMode != 3) ShizukuUtil.executeCommandSync("am force-stop com.android.settings")
                delay(100)
            }
        }

        mainLoopJob = scope.launch {
            launch {
                snapshotFlow { app.runningPojieTasks.toList() }
                    .scan(listOf<PojieRunInfo>()) { old, new ->
                        val removed = old.map { it.ssid }.toSet() - new.map { it.ssid }.toSet()
                        removed.forEach {
                            if (!handledSsids.remove(it)) {
                                if (worker.forgetNetwork(settings, it)) service.log("忘记网络:$it")
                            }
                        }

                        val targetSsid = currentWorkingSsid
                        val isCurrentTaskRemoved =
                            targetSsid != null && new.none { it.ssid == targetSsid }
                        val isListEmpty = new.isEmpty()

                        if (isCurrentTaskRemoved || isListEmpty) {
                            currentWorkerJob?.cancel()
                        }
                        new
                    }
                    .collect()
            }

            while (isActive) {
                if (app.runningPojieTasks.isEmpty()) {
                    service.stop()
                    break
                }

                val task = findNextReadyTask(app)
                if (task == null) {
                    handleCooldown(app)
                    continue
                }

                executeTaskAttempt(app, task, settings)
            }
        }
    }

    /**
     * 处理破解任务之间的冷却等待逻辑
     * @param app 全局 Application 实例
     * @return 是否成功完成冷却等待
     */
    private suspend fun handleCooldown(app: ToolboxApp): Boolean {
        val now = System.currentTimeMillis()
        val tasks = app.runningPojieTasks
        if (tasks.isEmpty()) return false

        val nextAvailableTime = tasks.minOf {
            val waitTime = if (it.retryCount > 0) {
                (2.0.pow(it.retryCount.toDouble() - 1).toLong()) * app.pojieConfig.doublingBase
            } else 0L
            it.lastTryTime + waitTime
        }

        val waitMs = maxOf(0L, nextAvailableTime - now)
        if (waitMs > 0) {
            val cooldownJob = scope.launch {
                service.log("冷却中，等待${waitMs}ms")
                PojieNotification.update(service,"冷却中")
                delay(waitMs)
            }
            currentWorkerJob = cooldownJob
            try {
                cooldownJob.join()
            } catch (_: CancellationException) {
                return false
            } finally {
                currentWorkerJob = null
            }
        }
        return true
    }

    /**
     * 在当前任务列表中寻找下一个已过冷却时间、可立即执行的任务
     * @param app 全局 Application 实例
     * @return 匹配的任务信息
     */
    private fun findNextReadyTask(app: ToolboxApp): PojieRunInfo? {
        val now = System.currentTimeMillis()
        val config = app.pojieConfig
        return app.runningPojieTasks.filter {
            val waitTime = if (it.retryCount > 0) {
                (2.0.pow(it.retryCount.toDouble() - 1).toLong()) * config.doublingBase
            } else 0L
            now - it.lastTryTime >= waitTime
        }.minByOrNull { calculatePriority(it) }
    }

    /**
     * 根据最后尝试时间计算任务优先级
     * @param task 破解运行信息对象
     * @return 优先级数值
     */
    private fun calculatePriority(task: PojieRunInfo): Long {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - task.lastTryTime
        return -timeDiff
    }

    /**
     * 执行单次破解尝试
     * @param app 全局 Application 实例
     * @param task 当前选中的破解任务信息
     * @param settings 设置信息
     */
    private suspend fun executeTaskAttempt(
        app: ToolboxApp,
        task: PojieRunInfo,
        settings: PojieSettings
    ) {
        app.runningPojieTasks.forEach {
            app.pojieTask.edit(it.ssid) { state ->
                state.copy(textTip = if (it.ssid == task.ssid) "正在尝试..." else "排队中")
            }
        }

        currentWorkingSsid = task.ssid
        val currentPass = task.tryList.getOrNull(task.tryIndex) ?: "未知"

        app.pojieTask.edit(task.ssid) {
            it.copy(textTip = "正在尝试：$currentPass", lastTryTime = System.currentTimeMillis())
        }

        var taskResult = -1
        service.log("${worker.getLogTime()} 尝试: (${task.ssid}, $currentPass) ...", true)
        PojieNotification.update(service,"尝试: (${task.ssid}, $currentPass)")

        val workerSubJob = scope.launch {
            try {
                taskResult =
                    worker.performTaskLogic(app, SinglePojieTask(task.ssid, currentPass), settings)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                service.log("E: 任务执行出错：${e.message}")
                taskResult = SinglePojieTask.RESULT_ERROR
            }
        }
        currentWorkerJob = workerSubJob
        workerSubJob.join()

        handleAttemptResult(app, task, currentPass, taskResult, settings)

        currentWorkingSsid = null
        currentWorkerJob = null
    }

    /**
     * 统一处理单次连接尝试后的结果
     * @param app 全局 Application 实例
     * @param task 尝试的任务信息
     * @param pass 本次尝试使用的密码
     * @param result 连接任务返回的状态码
     * @param settings 设置信息
     */
    private fun handleAttemptResult(
        app: ToolboxApp, task: PojieRunInfo, pass: String, result: Int, settings: PojieSettings
    ) {
        val timeTag = worker.getLogTime()
        val isCancelled = currentWorkerJob?.isCancelled == true

        if (isCancelled) {
            app.logState.setLine("$timeTag 尝试: (${task.ssid}, $pass) 结果: 任务中断")
            app.finishedPojieTasksTip[task.ssid] = "任务中断(index=${task.tryIndex})"
        } else if (result != SinglePojieTask.RESULT_ERROR) {
            val resultStr = when (result) {
                SinglePojieTask.RESULT_SUCCESS -> "连接成功"
                SinglePojieTask.RESULT_FAILED -> "失败"
                SinglePojieTask.RESULT_TIMEOUT -> "执行超时"
                SinglePojieTask.RESULT_ERROR_TRANSIENT -> "路由器拒绝接入"
                else -> "未知错误"
            }
            app.logState.setLine("$timeTag 尝试: (${task.ssid}, $pass) 结果: $resultStr")
        } else {
            app.finishedPojieTasksTip[task.ssid] = "执行出错，请查看输出"
        }

        updateHistory(app, task.ssid)
        worker.cleanConnection(settings)

        when (result) {
            SinglePojieTask.RESULT_SUCCESS -> {
                service.log("连接成功: (${task.ssid}, $pass)")
                app.finishedPojieTasksTip[task.ssid] = "连接成功：$pass"

                app.pojieHistory.addOrUpdateHistory(
                    com.wifi.toolbox.utils.PojieHistoryItem(
                        ssid = task.ssid,
                        passwords = task.tryList,
                        progress = task.tryIndex,
                        successfulPassword = pass
                    )
                )

                handledSsids.add(task.ssid)
                app.pojieTask.stop(task.ssid)
            }

            SinglePojieTask.RESULT_FAILED -> {
                processTaskCompletion(app, task.ssid)
                app.pojieTask.edit(task.ssid) { it.copy(retryCount = 0) }
            }

            SinglePojieTask.RESULT_ERROR -> {
                app.pojieTask.stop(task.ssid)
            }

            SinglePojieTask.RESULT_TIMEOUT, SinglePojieTask.RESULT_ERROR_TRANSIENT -> {
                app.pojieTask.edit(task.ssid) { it.copy(retryCount = it.retryCount + 1) }
                getTask(app, task.ssid)?.let { updated ->
                    if (app.pojieConfig.retryCountType <= 5 && updated.retryCount > app.pojieConfig.retryCountType) {
                        app.pojieTask.edit(task.ssid) { it.copy(retryCount = 0) }
                        processTaskCompletion(app, task.ssid)
                    }
                }
            }
        }
    }

    /**
     * 处理单个密码尝试完成后的状态更新
     * @param app 全局 Application 实例
     * @param ssid WiFi 名称
     */
    private fun processTaskCompletion(app: ToolboxApp, ssid: String) {
        val task = getTask(app, ssid) ?: return
        val nextIndex = task.tryIndex + 1

        if (nextIndex >= task.tryList.size) {
            app.finishedPojieTasksTip[ssid] = "全部尝试失败(size=${task.tryList.size})"
            app.pojieTask.stop(ssid)
        } else {
            app.pojieTask.edit(ssid) {
                it.copy(tryIndex = nextIndex)
            }
        }
    }

    /**
     * 从当前运行列表中获取指定 SSID 的任务信息
     * @param app 全局 Application 实例
     * @param ssid WiFi 名称
     * @return 匹配的任务信息
     */
    private fun getTask(app: ToolboxApp, ssid: String): PojieRunInfo? {
        return app.runningPojieTasks.find { it.ssid == ssid }
    }

    /**
     * 更新历史记录
     */
    private fun updateHistory(app: ToolboxApp, ssid: String) {
        getTask(app, ssid)?.let { task ->
            app.pojieHistory.addOrUpdateHistory(
                com.wifi.toolbox.utils.PojieHistoryItem(
                    ssid = task.ssid,
                    passwords = task.tryList,
                    progress = task.tryIndex
                )
            )
        }
    }
}