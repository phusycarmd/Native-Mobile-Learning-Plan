package com.phusy2001.nativedevicemonitordemo.nativecore.bluetooth

data class BleDevice(
    val name: String?,
    val address: String,
    val rssi: Int = 0
)