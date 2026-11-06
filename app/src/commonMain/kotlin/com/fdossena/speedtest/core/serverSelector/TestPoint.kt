package com.fdossena.speedtest.core.serverSelector

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class TestPoint(
    val id: Int = -1,
    val name: String,
    val server: String,
    val dlURL: String,
    val ulURL: String,
    val pingURL: String,
    val getIpURL: String,
    val sponsorName: String? = null,
    val sponsorURL: String? = null
) {
    @Transient
    var ping: Float = -1f
}
