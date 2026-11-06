package com.fdossena.speedtest.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_domains")
data class TrackedDomainEntity(
    @PrimaryKey
    val domainName: String,
    val registrar: String = "Unknown",
    val registrationDate: String = "Unknown",
    val expirationDate: String = "Unknown",
    val lastChecked: Long = System.currentTimeMillis(),
    val daysUntilExpiry: Int = -1
)

fun TrackedDomainEntity.toDomain() = TrackedDomain(
    domainName = domainName,
    registrar = registrar,
    registrationDate = registrationDate,
    expirationDate = expirationDate,
    lastChecked = lastChecked,
    daysUntilExpiry = daysUntilExpiry
)

fun TrackedDomain.toEntity() = TrackedDomainEntity(
    domainName = domainName,
    registrar = registrar,
    registrationDate = registrationDate,
    expirationDate = expirationDate,
    lastChecked = lastChecked,
    daysUntilExpiry = daysUntilExpiry
)
