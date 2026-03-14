package io.github.bszapp.wifitoolbox.services.mainservice;

import android.net.wifi.ScanResult;

interface IMainService {
    boolean isAlive();
    int getUid();
    String getUidStr();
    boolean startScan();
    List<ScanResult> getScanResults();
    void watchApp(IBinder token);
    void shutdown();
}