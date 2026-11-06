package com.fdossena.speedtest.data

data class CornerPoint(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long = System.currentTimeMillis(),
)

data class Room(
    val id: String,
    val name: String,
    val corners: List<CornerPoint>
)

data class SignalPortal(
    val id: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val speedMbps: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class Floor(
    val level: Int,
    val name: String,
    val rooms: List<Room>,
    val portals: List<SignalPortal> = emptyList()
)

data class FloorPlanProject(
    val id: String,
    val name: String,
    val floors: List<Floor>,
    val createdAt: Long = System.currentTimeMillis()
)
