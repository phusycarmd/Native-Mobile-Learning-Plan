package com.phusy2001.nativedevicemonitordemo.nativecore.logging

import android.content.Context
import com.phusy2001.nativedevicemonitordemo.nativecore.device.DeviceSnapshot
import java.io.File

interface FileLogger {
    fun append(snapshot: DeviceSnapshot)
    fun append(line: String)
    fun appendRaw(line: String)
    fun appendEntry(jsonString: String)
    fun readAll(): List<DeviceSnapshot>
    fun readAllEntries(): List<String>
    fun readRawLines(): List<String>
    fun clear()
    fun getFileSizeBytes(): Long
    fun getLogCount(): Int
    fun getFile(): File
}

class FileLoggerImpl(
    private val context: Context,
    private val fileName: String = "monitor_logs.jsonl"
) : FileLogger {

    private val lock = Any()
    private val logFile: File
        get() = File(context.filesDir, fileName)

    override fun append(snapshot: DeviceSnapshot) {
        appendRaw(snapshot.toJson())
    }

    override fun append(line: String) {
        appendRaw(line)
    }

    override fun appendEntry(jsonString: String) {
        appendRaw(jsonString)
    }

    override fun appendRaw(line: String) {
        synchronized(lock) {
            val file = logFile
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                file.createNewFile()
            }
            file.appendText("$line\n", Charsets.UTF_8)
        }
    }

    override fun readAll(): List<DeviceSnapshot> {
        val lines = readRawLines()
        val result = mutableListOf<DeviceSnapshot>()
        for (line in lines) {
            val snapshot = DeviceSnapshot.fromJson(line)
            if (snapshot != null) {
                result.add(snapshot)
            }
        }
        return result
    }

    override fun readAllEntries(): List<String> = readRawLines()

    override fun readRawLines(): List<String> {
        synchronized(lock) {
            val file = logFile
            if (!file.exists()) return emptyList()
            return try {
                file.readLines(Charsets.UTF_8).filter { it.isNotBlank() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override fun clear() {
        synchronized(lock) {
            val file = logFile
            if (file.exists()) {
                file.delete()
            }
        }
    }

    override fun getFileSizeBytes(): Long {
        synchronized(lock) {
            val file = logFile
            return if (file.exists()) file.length() else 0L
        }
    }

    override fun getLogCount(): Int {
        return readRawLines().size
    }

    override fun getFile(): File = logFile
}
