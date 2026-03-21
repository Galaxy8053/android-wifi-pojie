package io.github.bszapp.wifitoolbox.services.mainservice;

import android.net.wifi.ScanResult;
import io.github.bszapp.wifitoolbox.services.mainservice.IMainServiceCallback;

interface IMainService {
    boolean isAlive();
    int getUid();
    String getUidStr();
    int getPid();
    boolean startScan();
    List<ScanResult> getScanResults();
    byte[] getSavedWifiList();
    boolean isWifiEnabled();
    void setWifiEnabled(boolean enabled);
    boolean updateWifiConfig(int networkId, in byte[] patchBytes);
    void watchApp(IBinder token);
    void shutdown();
    void registerCallback(IMainServiceCallback cb);
    void unregisterCallback(IMainServiceCallback cb);
}