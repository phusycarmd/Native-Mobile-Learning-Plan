package com.phusy2001.nativedevicemonitordemo.nativecore.device

import java.time.Instant
import java.time.format.DateTimeFormatter

interface DeviceSnapshotProvider {
    fun takeSnapshot(): DeviceSnapshot
}

class DeviceSnapshotProviderImpl(
    private val batteryProvider: BatteryProvider,
    private val networkProvider: NetworkProvider,
    private val bluetoothProvider: BluetoothProvider
) : DeviceSnapshotProvider {

    override fun takeSnapshot(): DeviceSnapshot {
        return DeviceSnapshot(
            timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            battery = batteryProvider.getBatteryLevel(),
            wifiConnected = networkProvider.isWifiConnected(),
            cellularAvailable = networkProvider.isCellularAvailable(),
            bluetoothEnabled = bluetoothProvider.isBluetoothEnabled(),
            bluetoothConnected = bluetoothProvider.isBluetoothConnected()
        )
    }
}
