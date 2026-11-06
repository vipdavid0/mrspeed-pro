package com.fdossena.speedtest.data

import kotlinx.serialization.Serializable

@Serializable
data class ISPRecord(
    val name: String,
    val avgSpeed: Double,
    val reliability: Double
)

@Serializable
data class GamingScore(
    val gameName: String,
    val avgPing: Double,
    val avgJitter: Double,
    val grade: String,
    val status: String,
    val description: String
)
