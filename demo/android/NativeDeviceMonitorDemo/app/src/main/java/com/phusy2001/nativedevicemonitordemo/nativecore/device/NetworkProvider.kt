package com.phusy2001.nativedevicemonitordemo.nativecore.device

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

data class NetworkInfo(
    val isConnected: Boolean,
    val isWifiConnected: Boolean,
    val isCellularAvailable: Boolean
)

interface NetworkProvider {
    fun isConnected(): Boolean
    fun isWifiConnected(): Boolean
    fun isCellularAvailable(): Boolean
    fun getNetworkInfo(): NetworkInfo
}

class NetworkProviderImpl(private val context: Context) : NetworkProvider {

    private val connectivityManager: ConnectivityManager? by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    override fun isConnected(): Boolean {
        val caps = getCapabilities() ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun isWifiConnected(): Boolean {
        val caps = getCapabilities() ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    override fun isCellularAvailable(): Boolean {
        val caps = getCapabilities() ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    override fun getNetworkInfo(): NetworkInfo {
        return NetworkInfo(
            isConnected = isConnected(),
            isWifiConnected = isWifiConnected(),
            isCellularAvailable = isCellularAvailable()
        )
    }

    private fun getCapabilities(): NetworkCapabilities? {
        val cm = connectivityManager ?: return null
        val activeNetwork = cm.activeNetwork ?: return null
        return cm.getNetworkCapabilities(activeNetwork)
    }
}
