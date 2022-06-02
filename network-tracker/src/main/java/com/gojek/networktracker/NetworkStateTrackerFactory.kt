package com.gojek.networktracker

import android.content.Context
import com.gojek.courier.logging.ILogger
import com.gojek.courier.logging.NoOpLogger

class NetworkStateTrackerFactory private constructor() {
    companion object {
        fun create(
            context: Context,
            logger: ILogger = NoOpLogger()
        ): NetworkStateTracker {
            return NetworkStateTrackerImpl(context.applicationContext, logger)
        }
    }
}
