package com.fdossena.speedtest.core.config

import kotlinx.serialization.Serializable

@Serializable
class TelemetryConfig(
    var telemetryLevel: String = LEVEL_DISABLED,
    var server: String? = null,
    var path: String? = null,
    var shareURL: String? = null
) {
    companion object {
        const val LEVEL_DISABLED = "disabled"
        const val LEVEL_BASIC = "basic"
        const val LEVEL_FULL = "full"
    }

    fun clone(): TelemetryConfig {
        return TelemetryConfig(telemetryLevel, server, path, shareURL)
    }
}
