package com.gojek.keepalive.utils

import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.telephony.TelephonyManager

internal const val NETWORK_TYPE_GSM = 16
internal const val DISCONNECTED = -1

internal class NetworkUtils {
    internal fun getNetworkType(info: NetworkInfo?): Short {
        if (info == null || !info.isConnected) {
            return DISCONNECTED.toShort()
        }
        if (info.type == ConnectivityManager.TYPE_WIFI) {
            return ConnectivityManager.TYPE_WIFI.toShort()
        }
        return when (info.subtype) {
            TelephonyManager.NETWORK_TYPE_LTE -> 4
            TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_HSUPA -> 3
            TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_IDEN, NETWORK_TYPE_GSM -> 2
            TelephonyManager.NETWORK_TYPE_UNKNOWN -> 0
            else -> 0
        }
    }

    fun getNetworkName(info: NetworkInfo?): String {
        return try {
            if (isWifi(getNetworkType(info))) {
                "wifi"
            } else {
                info!!.extraInfo
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun isWifi(networkType: Short): Boolean {
        return ConnectivityManager.TYPE_WIFI == networkType.toInt()
    }
}