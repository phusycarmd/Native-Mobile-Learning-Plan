package com.phusy2001.nativedevicemonitordemo.nativecore.device

import android.bluetooth.BluetoothAdapter
import android.content.Context
import com.phusy2001.nativedevicemonitordemo.nativecore.bluetooth.BluetoothManager
import com.phusy2001.nativedevicemonitordemo.nativecore.bluetooth.ConnectionState

data class BluetoothInfo(
    val isEnabled: Boolean,
    val isConnected: Boolean
)

interface BluetoothProvider {
    fun isBluetoothEnabled(): Boolean
    fun isBluetoothConnected(): Boolean
    fun getBluetoothInfo(): BluetoothInfo
}

class BluetoothProviderImpl(
    private val context: Context,
    private val bluetoothManager: BluetoothManager? = null
) : BluetoothProvider {

    private val adapter: BluetoothAdapter? by lazy {
        val sysManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        sysManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    }

    override fun isBluetoothEnabled(): Boolean {
        return bluetoothManager?.isEnabled() ?: (adapter?.isEnabled == true)
    }

    override fun isBluetoothConnected(): Boolean {
        return bluetoothManager?.getConnectionState() == ConnectionState.CONNECTED
    }

    override fun getBluetoothInfo(): BluetoothInfo {
        return BluetoothInfo(
            isEnabled = isBluetoothEnabled(),
            isConnected = isBluetoothConnected()
        )
    }
}
