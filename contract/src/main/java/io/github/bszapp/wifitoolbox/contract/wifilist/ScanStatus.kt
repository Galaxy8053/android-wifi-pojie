package io.github.bszapp.wifitoolbox.contract.wifilist

enum class ScanStatus {
    IDLE,
    SCANNING,  // 扫描进行中（3秒内）
    FINISH,    // 扫描完成（3秒后）
    ERROR
}