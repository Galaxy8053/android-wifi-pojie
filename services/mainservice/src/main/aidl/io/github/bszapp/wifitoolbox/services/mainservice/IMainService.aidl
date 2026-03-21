package io.github.bszapp.wifitoolbox.services.mainservice;

import android.net.wifi.ScanResult;

interface IMainService {
    boolean isAlive();
    int getUid();
    String getUidStr();
    int getPid();
    boolean startScan();
    List<ScanResult> getScanResults();
    byte[] getSavedWifiList();
    boolean updateWifiConfig(int networkId, in byte[] patchBytes);
    void watchApp(IBinder token);
    void shutdown();
}