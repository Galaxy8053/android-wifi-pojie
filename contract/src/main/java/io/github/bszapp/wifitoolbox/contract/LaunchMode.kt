package io.github.bszapp.wifitoolbox.contract

enum class LaunchMode {
    SHIZUKU,           // 直连 Shizuku Binder，暂未实现
    SHIZUKU_TERMINAL,  // 通过 Shizuku 起 sh 子进程，再绑服务（当前实现）
    ROOT               // 通过 su 起子进程，再绑服务
}