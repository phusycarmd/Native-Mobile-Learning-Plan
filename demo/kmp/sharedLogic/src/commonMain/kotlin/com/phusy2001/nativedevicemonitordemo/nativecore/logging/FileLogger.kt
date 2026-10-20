package com.phusy2001.nativedevicemonitordemo.nativecore.logging

import com.phusy2001.nativedevicemonitordemo.nativecore.device.DeviceSnapshot

interface FileLogger {
    fun append(snapshot: DeviceSnapshot)
    fun appendRaw(line: String)
    fun readAll(): List<DeviceSnapshot>
    fun readRawLines(): List<String>
    fun clear()
    fun getFileSizeBytes(): Long
    fun getLogCount(): Int
}
