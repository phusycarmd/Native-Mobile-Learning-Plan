package com.phusy2001.nativedevicemonitordemo.nativecore.device

import android.content.Context
import com.phusy2001.nativedevicemonitordemo.nativecore.AndroidBatteryProvider
import com.phusy2001.nativedevicemonitordemo.nativecore.AndroidNetworkProvider

class BatteryProviderImpl(context: Context) : BatteryProvider by AndroidBatteryProvider(context)
class NetworkProviderImpl(context: Context) : NetworkProvider by AndroidNetworkProvider(context)
