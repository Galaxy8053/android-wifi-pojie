package io.github.bszapp.wifitoolbox.contract

import io.github.bszapp.wifitoolbox.contract.startup.IStartupController
import io.github.bszapp.wifitoolbox.contract.wifilist.IWifiListController

interface IAppController {
    val startup: IStartupController
    val wifiList: IWifiListController
}