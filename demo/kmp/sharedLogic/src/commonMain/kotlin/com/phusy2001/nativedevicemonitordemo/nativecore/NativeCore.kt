package com.phusy2001.nativedevicemonitordemo.nativecore

import com.phusy2001.nativedevicemonitordemo.nativecore.background.BackgroundMonitor
import com.phusy2001.nativedevicemonitordemo.nativecore.bluetooth.BluetoothManager
import com.phusy2001.nativedevicemonitordemo.nativecore.device.BatteryProvider
import com.phusy2001.nativedevicemonitordemo.nativecore.device.BluetoothProvider
import com.phusy2001.nativedevicemonitordemo.nativecore.device.NetworkProvider
import com.phusy2001.nativedevicemonitordemo.nativecore.logging.FileLogger
import com.phusy2001.nativedevicemonitordemo.nativecore.network.ApiClient

/**
 * Container chứa toàn bộ các Native Services độc lập của ứng dụng.
 */
interface NativeCore {
    val batteryProvider: BatteryProvider
    val networkProvider: NetworkProvider
    val bluetoothProvider: BluetoothProvider
    val fileLogger: FileLogger
    val apiClient: ApiClient
    val bluetoothManager: BluetoothManager
    val backgroundMonitor: BackgroundMonitor
}
