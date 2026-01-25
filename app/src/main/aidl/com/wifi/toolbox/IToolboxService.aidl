package com.wifi.toolbox;

interface IToolboxService {
    int getUid();
    void pressPowerKey();
    void setWifiEnabled(boolean enabled);
    void forgetNetwork(int netId);
}