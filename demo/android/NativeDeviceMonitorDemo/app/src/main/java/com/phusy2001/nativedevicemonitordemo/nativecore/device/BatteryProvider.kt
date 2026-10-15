package com.phusy2001.nativedevicemonitordemo.nativecore.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

data class BatteryInfo(
    val level: Int,
    val isCharging: Boolean
)

interface BatteryProvider {
    fun getBatteryLevel(): Int
    fun isCharging(): Boolean
    fun getBatteryInfo(): BatteryInfo
}

class BatteryProviderImpl(private val context: Context) : BatteryProvider {

    override fun getBatteryLevel(): Int {
        val status = getBatteryIntent()
        val level = status?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = status?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) {
            (level * 100 / scale.toFloat()).toInt().coerceIn(0, 100)
        } else {
            100 // fallback nếu emulator không báo pin
        }
    }

    override fun isCharging(): Boolean {
        val status = getBatteryIntent()
        val statusVal = status?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return statusVal == BatteryManager.BATTERY_STATUS_CHARGING ||
                statusVal == BatteryManager.BATTERY_STATUS_FULL
    }

    override fun getBatteryInfo(): BatteryInfo {
        return BatteryInfo(
            level = getBatteryLevel(),
            isCharging = isCharging()
        )
    }

    private fun getBatteryIntent(): Intent? {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        return context.registerReceiver(null, filter)
    }
}
