package com.fdossena.speedtest.ui.heatmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.fdossena.speedtest.data.Floor

@Composable
fun GeneratedFloorPlanView(
    floor: Floor,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.fillMaxSize()) {
        if (floor.rooms.isEmpty()) return@Canvas

        // Find bounds of all rooms to normalize coordinates
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = Float.MIN_VALUE

        floor.rooms.forEach { room ->
            room.corners.forEach { corner ->
                if (corner.x < minX) minX = corner.x
                if (corner.x > maxX) maxX = corner.x
                if (corner.z < minZ) minZ = corner.z
                if (corner.z > maxZ) maxZ = corner.z
            }
        }

        if (minX == Float.MAX_VALUE) return@Canvas // No corners found

        val pad = 40f
        val width = size.width - (pad * 2)
        val height = size.height - (pad * 2)

        val rangeX = if (maxX - minX == 0f) 1f else maxX - minX
        val rangeZ = if (maxZ - minZ == 0f) 1f else maxZ - minZ

        val scaleX = width / rangeX
        val scaleZ = height / rangeZ
        val scale = minOf(scaleX, scaleZ)

        floor.rooms.forEach { room ->
            if (room.corners.size < 2) return@forEach

            val path = Path()
            room.corners.forEachIndexed { index, corner ->
                // Map x,z 3D coords to x,y 2D screen canvas coords
                val px = pad + (corner.x - minX) * scale
                val py = pad + (corner.z - minZ) * scale

                if (index == 0) {
                    path.moveTo(px, py)
                } else {
                    path.lineTo(px, py)
                }
            }
            path.close()

            // Draw filled room background
            drawPath(
                path = path,
                color = primaryColor.copy(alpha = 0.15f)
            )

            // Draw room walls outline
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 3f)
            )
        }

        // Draw AR signal portals/heatmap dots onto the floor plan
        floor.portals.forEach { portal ->
            val px = pad + (portal.x - minX) * scale
            val py = pad + (portal.z - minZ) * scale
            
            val dotColor = when {
                portal.speedMbps >= 100.0 -> Color.Green
                portal.speedMbps >= 30.0 -> Color.Yellow
                else -> Color.Red
            }

            // Outer glowing ring
            drawCircle(
                color = dotColor.copy(alpha = 0.3f),
                radius = 16f,
                center = Offset(px, py)
            )
            // Solid center signal node point
            drawCircle(
                color = dotColor,
                radius = 8f,
                center = Offset(px, py)
            )
        }
    }
}
