package com.gojek.networktracker.model

import android.net.NetworkInfo

data class NetworkState(
    val isConnected: Boolean,
    val isValidated: Boolean,
    val isMetered: Boolean,
    val isNotRoaming: Boolean,
    val netInfo: NetworkInfo?
)
