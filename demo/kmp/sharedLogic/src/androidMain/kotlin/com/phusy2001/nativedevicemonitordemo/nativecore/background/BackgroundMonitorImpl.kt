package com.phusy2001.nativedevicemonitordemo.nativecore.background

import android.content.Context
import com.phusy2001.nativedevicemonitordemo.nativecore.AndroidBackgroundMonitor
import com.phusy2001.nativedevicemonitordemo.nativecore.AndroidBatteryProvider
import com.phusy2001.nativedevicemonitordemo.nativecore.AndroidBluetoothProvider
import com.phusy2001.nativedevicemonitordemo.nativecore.AndroidFileLogger
import com.phusy2001.nativedevicemonitordemo.nativecore.AndroidNetworkProvider
import com.phusy2001.nativedevicemonitordemo.nativecore.logging.FileLogger

class BackgroundMonitorImpl(
    context: Context,
    val fileLogger: FileLogger = AndroidFileLogger(context)
) : BackgroundMonitor by AndroidBackgroundMonitor(
    batteryProvider = AndroidBatteryProvider(context),
    networkProvider = AndroidNetworkProvider(context),
    bluetoothProvider = AndroidBluetoothProvider(context),
    fileLogger = fileLogger
)
