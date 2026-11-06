package com.fdossena.speedtest.core.log

class Logger {
    private var log = ""

    fun getLog(): String {
        synchronized(this) {
            return log
        }
    }

    fun l(s: String) {
        synchronized(this) {
            log += System.currentTimeMillis().toString() + " " + s + "\n"
        }
    }
}
