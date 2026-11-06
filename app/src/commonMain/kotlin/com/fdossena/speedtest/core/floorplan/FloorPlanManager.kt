package com.fdossena.speedtest.core.floorplan

import com.fdossena.speedtest.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class FloorPlanManager {
    private val _currentProject = MutableStateFlow<FloorPlanProject?>(null)
    val currentProject: StateFlow<FloorPlanProject?> = _currentProject.asStateFlow()

    private val _activePoints = MutableStateFlow<List<CornerPoint>>(emptyList())
    val activePoints: StateFlow<List<CornerPoint>> = _activePoints.asStateFlow()

    private var currentFloorLevel = 0

    fun startNewProject(name: String) {
        _currentProject.value = FloorPlanProject(
            id = UUID.randomUUID().toString(),
            name = name,
            floors = listOf(Floor(level = 0, name = "Ground Floor", rooms = emptyList()))
        )
        currentFloorLevel = 0
        _activePoints.value = emptyList()
    }

    fun clearProject() {
        _currentProject.value = null
        currentFloorLevel = 0
        _activePoints.value = emptyList()
    }

    fun addPoint(x: Float, y: Float, z: Float) {
        _activePoints.update { it + CornerPoint(x, y, z) }
    }

    fun addPortal(x: Float, y: Float, z: Float, speed: Double) {
        val portal = SignalPortal(UUID.randomUUID().toString(), x, y, z, speed)
        _currentProject.update { project ->
            project?.let { p ->
                val updatedFloors = p.floors.map { floor ->
                    if (floor.level == currentFloorLevel) {
                        floor.copy(portals = floor.portals + portal)
                    } else {
                        floor
                    }
                }
                p.copy(floors = updatedFloors)
            }
        }
    }

    fun undoLastPoint() {
        _activePoints.update { if (it.isNotEmpty()) it.dropLast(1) else it }
    }

    fun finishRoom(roomName: String) {
        val points = _activePoints.value
        if (points.size < 3) return // Need at least a triangle

        val newRoom = Room(
            id = UUID.randomUUID().toString(),
            name = roomName,
            corners = points
        )

        _currentProject.update { project ->
            project?.let { p ->
                val updatedFloors = p.floors.map { floor ->
                    if (floor.level == currentFloorLevel) {
                        floor.copy(rooms = floor.rooms + newRoom)
                    } else {
                        floor
                    }
                }
                p.copy(floors = updatedFloors)
            }
        }
        _activePoints.value = emptyList()
    }

    fun addNewFloor(name: String) {
        _currentProject.update { project ->
            project?.let { p ->
                val nextLevel = (p.floors.maxOfOrNull { it.level } ?: 0) + 1
                currentFloorLevel = nextLevel
                p.copy(floors = p.floors + Floor(level = nextLevel, name = name, rooms = emptyList()))
            }
        }
    }

    fun switchFloor(level: Int) {
        currentFloorLevel = level
        _activePoints.value = emptyList()
    }

    fun getCurrentFloor(): Floor? {
        return _currentProject.value?.floors?.find { it.level == currentFloorLevel }
    }
}
