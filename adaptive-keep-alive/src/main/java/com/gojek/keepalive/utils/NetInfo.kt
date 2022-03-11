package com.gojek.keepalive.utils

import android.net.NetworkInfo
import android.text.TextUtils

internal class NetInfo(info: NetworkInfo, networkType: Short, isWifi: Boolean) {
    private val info: NetworkInfo?
    private val isAvailable: Boolean
    private val isRoaming: Boolean
    val networkType: Short
    private val ssid: String?
    private val isWifi: Boolean
    override fun hashCode(): Int {
        var hashCode = networkType.toInt()
        hashCode += 31 * if (isWifi) 1231 else 1237
        hashCode += 31 * (ssid?.hashCode() ?: 0)
        return hashCode
    }

    override fun equals(other: Any?): Boolean {
        if (null == other) {
            return false
        }
        if (this === other) {
            return true
        }
        return if (other !is NetInfo) {
            false
        } else equals(other)
    }

    private fun equals(info: NetInfo): Boolean {
        if (networkType != info.networkType) {
            return false
        }
        if (!isWifi || !info.isWifi) {
            return true
        }
        if (TextUtils.isEmpty(ssid) && TextUtils.isEmpty(info.ssid)) // both the ssids values are empty
        {
            return true
        }
        return if (ssid.isNullOrEmpty() || info.ssid.isNullOrEmpty()) {
            false
        } else ssid == info.ssid
        // return true if ssids are equal false otherwise
    }

    override fun toString(): String {
        return if (info == null) {
            ""
        } else "[" + "type: " + info.typeName + "[" + info.subtypeName +
                "], state: " + info.state + "/" + info.detailedState +
                ", reason: " + (if (info.reason == null) "(unspecified)" else info.reason) +
                ", extra: " + (ssid ?: "(none)") +
                ", roaming: " + isRoaming +
                ", failover: " + info.isFailover +
                ", isAvailable: " + isAvailable +
                "]"
    }

    init {
        this.info = info
        isAvailable = info.isAvailable
        isRoaming = info.isRoaming
        this.networkType = networkType
        this.isWifi = isWifi
        ssid = info.extraInfo
    }
}