package com.fdossena.speedtest.core.config

import kotlinx.serialization.Serializable

@Serializable
class SpeedtestConfig(
    var dl_ckSize: Int = 100,
    var ul_ckSize: Int = 20,
    var dl_parallelStreams: Int = 3,
    var ul_parallelStreams: Int = 3,
    var dl_streamDelay: Int = 300,
    var ul_streamDelay: Int = 300,
    var dl_graceTime: Double = 1.5,
    var ul_graceTime: Double = 1.5,
    var dl_connectTimeout: Int = 5000,
    var dl_soTimeout: Int = 10000,
    var ul_connectTimeout: Int = 5000,
    var ul_soTimeout: Int = 10000,
    var ping_connectTimeout: Int = 2000,
    var ping_soTimeout: Int = 5000,
    var dl_recvBuffer: Int = -1,
    var dl_sendBuffer: Int = -1,
    var ul_recvBuffer: Int = -1,
    var ul_sendBuffer: Int = 16384,
    var ping_recvBuffer: Int = -1,
    var ping_sendBuffer: Int = -1,
    var errorHandlingMode: String = ONERROR_ATTEMPT_RESTART,
    var time_dl_max: Int = 15,
    var time_ul_max: Int = 15,
    var time_auto: Boolean = true,
    var count_ping: Int = 10,
    var telemetry_extra: String = "",
    var overheadCompensationFactor: Double = 1.06,
    var getIP_isp: Boolean = true,
    var getIP_distance: String = DISTANCE_KM,
    var useMebibits: Boolean = false,
    var test_order: String = "IP_P_D_U"
) {
    companion object {
        const val ONERROR_FAIL = "fail"
        const val ONERROR_ATTEMPT_RESTART = "attempt-restart"
        const val ONERROR_MUST_RESTART = "must-restart"
        const val DISTANCE_NO = "no"
        const val DISTANCE_MILES = "mi"
        const val DISTANCE_KM = "km"
    }

    fun clone(): SpeedtestConfig {
        return SpeedtestConfig(
            dl_ckSize, ul_ckSize, dl_parallelStreams, ul_parallelStreams, dl_streamDelay, ul_streamDelay,
            dl_graceTime, ul_graceTime, dl_connectTimeout, dl_soTimeout, ul_connectTimeout, ul_soTimeout,
            ping_connectTimeout, ping_soTimeout, dl_recvBuffer, dl_sendBuffer, ul_recvBuffer, ul_sendBuffer,
            ping_recvBuffer, ping_sendBuffer, errorHandlingMode, time_dl_max, time_ul_max, time_auto,
            count_ping, telemetry_extra, overheadCompensationFactor, getIP_isp, getIP_distance, useMebibits, test_order
        )
    }
}
