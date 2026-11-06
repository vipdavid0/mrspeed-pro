package com.fdossena.speedtest.data

data class TrackedDomain(
    val domainName: String,
    val registrar: String = "Unknown",
    val registrationDate: String = "Unknown",
    val expirationDate: String = "Unknown",
    val lastChecked: Long = System.currentTimeMillis(),
    val daysUntilExpiry: Int = -1
)
