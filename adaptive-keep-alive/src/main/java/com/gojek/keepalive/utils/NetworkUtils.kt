package com.gojek.keepalive.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.telephony.TelephonyManager

internal const val NETWORK_TYPE_GSM = 16
internal const val DISCONNECTED = -1

internal class NetworkUtils(private val context: Context) {
    private val isLollipopOrHigher: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

    fun isConnected(): Boolean {
        val networkType = getNetworkType()
        return networkType.toInt() != DISCONNECTED
    }

    fun getNetworkType(): Short {
        val networkInfo = getNetworkInfo()
        return getNetworkType(networkInfo)
    }

    private fun getNetworkInfo(): NetworkInfo? {
        val cm =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                ?: return null
        var networkInfo = cm.activeNetworkInfo
        if (networkInfo == null) {
            networkInfo = getNetworkInfo(cm, ConnectivityManager.TYPE_MOBILE)
            if (networkInfo == null) {
                networkInfo = getNetworkInfo(cm, ConnectivityManager.TYPE_WIFI)
            }
        }
        return networkInfo
    }

    private fun getNetworkInfo(
        connectivityManager: ConnectivityManager,
        networkType: Int
    ): NetworkInfo? {
        if (!isLollipopOrHigher) {
            return connectivityManager.getNetworkInfo(networkType)
        }
        val networks = connectivityManager.allNetworks
        if (networks.isEmpty()) {
            return null
        }
        for (network in networks) {
            val networkInfo = connectivityManager.getNetworkInfo(network)
            if (networkInfo != null && networkType == networkInfo.type) {
                return networkInfo
            }
        }
        return null
    }

    private fun getNetworkType(info: NetworkInfo?): Short {
        if (info == null || !info.isConnected) {
            return DISCONNECTED.toShort()
        }
        // If device is connected via WiFi
        if (info.type == ConnectivityManager.TYPE_WIFI) {
            return ConnectivityManager.TYPE_WIFI.toShort() // return 1024 * 1024;
        }
        val networkType = info.subtype
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_LTE -> 4
            TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_HSUPA -> 3
            TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_IDEN, NETWORK_TYPE_GSM -> 2
            TelephonyManager.NETWORK_TYPE_UNKNOWN -> 0
            else -> 0
        }
    }

    fun isOnWifi(): Boolean {
        return isWifi(getNetworkType())
    }

    private fun isWifi(networkType: Short): Boolean {
        return ConnectivityManager.TYPE_WIFI == networkType.toInt()
    }

    fun getNetworkName(): String {
        return try {
            if(isOnWifi()) {
                "wifi"
            } else {
                val connectivityManager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                activeNetworkInfo!!.extraInfo
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
}