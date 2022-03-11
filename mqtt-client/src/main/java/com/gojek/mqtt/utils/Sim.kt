package com.gojek.mqtt.utils

import org.json.JSONException
import org.json.JSONObject

internal class Sim {
    var imei: String? = null
    var phoneNumber: String? = null
    var networkOperator: String? = null
    var roaming = 0
    var countryISO: String? = null
    var slotIndex = 0
    var carrierName: String? = null
    fun toJSONString(): String {
        val jsonObj = JSONObject()
        try {
            jsonObj.put("imei", imei)
            jsonObj.put("phone_number", phoneNumber)
            jsonObj.put("operator", networkOperator)
            jsonObj.put("roaming", if (roaming == 1) true else false)
            jsonObj.put("countryISO", countryISO)
            jsonObj.put("slotIndex", slotIndex)
        } catch (e: JSONException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        return jsonObj.toString()
    }

    fun isRoaming(): Int {
        return roaming
    }

}