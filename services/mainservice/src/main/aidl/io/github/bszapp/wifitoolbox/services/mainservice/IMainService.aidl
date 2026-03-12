package io.github.bszapp.wifitoolbox.services.mainservice;

import android.net.wifi.ScanResult;

interface IMainService {
    boolean isAlive();
    int getUid();
    boolean startScan();
    List<ScanResult> getScanResults();
}