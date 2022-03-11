package com.gojek.courier.logging

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
class NoOpLogger: ILogger {
    override fun v(tag: String, msg: String) {
        
    }

    override fun v(tag: String, msg: String, tr: Throwable) {
        
    }

    override fun d(tag: String, msg: String) {
        
    }

    override fun d(tag: String, msg: String, tr: Throwable) {
        
    }

    override fun i(tag: String, msg: String) {
        
    }

    override fun i(tag: String, msg: String, tr: Throwable) {
        
    }

    override fun w(tag: String, msg: String) {
        
    }

    override fun w(tag: String, msg: String, tr: Throwable) {
        
    }

    override fun w(tag: String, tr: Throwable) {
        
    }

    override fun e(tag: String, msg: String) {
        
    }

    override fun e(tag: String, msg: String, tr: Throwable) {
        
    }
}