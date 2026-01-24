package com.wifi.toolbox;

import android.os.Bundle;

interface IRootService {
    Bundle call(String serviceName, String methodName, in Bundle args);
}