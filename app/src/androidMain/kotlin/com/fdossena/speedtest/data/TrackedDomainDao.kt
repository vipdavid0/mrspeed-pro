package com.fdossena.speedtest.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedDomainDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(domain: TrackedDomainEntity)

    @Query("SELECT * FROM tracked_domains ORDER BY domainName ASC")
    fun getAllTrackedDomains(): Flow<List<TrackedDomainEntity>>

    @Delete
    suspend fun delete(domain: TrackedDomainEntity)

    @Query("SELECT * FROM tracked_domains WHERE domainName = :name")
    suspend fun getDomain(name: String): TrackedDomainEntity?
}
