package com.phusy2001.nativedevicemonitordemo.nativecore.bluetooth

data class BleDevice(
    val name: String?,
    val address: String,
    val rssi: Int = 0,
    // false: thiết bị chỉ phát quảng bá (beacon...), không thể kết nối
    val isConnectable: Boolean = true
)