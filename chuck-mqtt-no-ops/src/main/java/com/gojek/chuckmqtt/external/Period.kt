package com.gojek.chuckmqtt.external

enum class Period {
        /** Retain data for the last hour. */
        ONE_HOUR,

        /** Retain data for the last day. */
        ONE_DAY,

        /** Retain data for the last week. */
        ONE_WEEK,

        /** Retain data forever. */
        FOREVER
    }