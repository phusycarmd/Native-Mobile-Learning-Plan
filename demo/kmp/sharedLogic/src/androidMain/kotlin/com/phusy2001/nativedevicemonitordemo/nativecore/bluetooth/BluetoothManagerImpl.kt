package com.phusy2001.nativedevicemonitordemo.nativecore.bluetooth

import android.content.Context
import com.phusy2001.nativedevicemonitordemo.nativecore.AndroidBluetoothManager

class BluetoothManagerImpl(context: Context) : BluetoothManager by AndroidBluetoothManager(context)
