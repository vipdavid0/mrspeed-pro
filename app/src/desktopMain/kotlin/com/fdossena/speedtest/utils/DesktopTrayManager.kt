package com.fdossena.speedtest.utils

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import javax.swing.JWindow
import javax.swing.JPanel
import javax.swing.JLabel
import javax.swing.Timer
import javax.swing.border.EmptyBorder

object DesktopTrayManager {
    private var trayIcon: TrayIcon? = null
    private var currentMode: Int = 0 // 0: Download, 1: Upload, 2: Ping, 3: Logo
    private var isLight: Boolean = false

    fun initializeTray(
        onRunQuickTest: () -> Unit,
        onOpenDashboard: () -> Unit,
        initialMode: Int = 0,
        isLightMode: Boolean = false
    ) {
        if (!SystemTray.isSupported()) return
        if (trayIcon != null) return // Already initialized

        try {
            val systemTray = SystemTray.getSystemTray()
            currentMode = initialMode
            isLight = isLightMode
            val initialImage = createMetricImage("--", currentMode, isLightMode)

            val popup = PopupMenu()

            val testItem = MenuItem("Run Quick Speed Test")
            testItem.addActionListener { onRunQuickTest() }

            val dashboardItem = MenuItem("Open Dashboard")
            dashboardItem.addActionListener { onOpenDashboard() }

            val exitItem = MenuItem("Exit")
            exitItem.addActionListener { System.exit(0) }

            popup.add(dashboardItem)
            popup.add(testItem)
            popup.addSeparator()
            popup.add(exitItem)

            trayIcon = TrayIcon(initialImage, "Mr Speed PRO | Desktop (Click to test)", popup)
            trayIcon?.isImageAutoSize = true

            // Single left-click on tray icon immediately triggers Quick Speed Test in silent toast mode!
            trayIcon?.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.button == MouseEvent.BUTTON1) {
                        onRunQuickTest()
                    }
                }
            })

            systemTray.add(trayIcon)
            println("[DesktopTray] System tray icon initialized and active.")
        } catch (e: Exception) {
            println("[DesktopTray] System tray error: ${e.message}")
        }
    }

    fun setMode(mode: Int) {
        currentMode = mode
    }

    fun getLogoImage(isLightMode: Boolean = isLight): BufferedImage {
        return createMetricImage("LOGO", 3, isLightMode)
    }

    fun updateMetric(dlSpeed: Double, ulSpeed: Double, ping: Double, mode: Int = currentMode, isLightMode: Boolean = isLight) {
        currentMode = mode
        isLight = isLightMode

        val text: String = when (mode) {
            0 -> if (dlSpeed > 0) "${dlSpeed.toInt()}" else "--"
            1 -> if (ulSpeed > 0) "${ulSpeed.toInt()}" else "--"
            2 -> if (ping > 0) "${ping.toInt()}" else "--"
            else -> "LOGO"
        }

        trayIcon?.image = createMetricImage(text, mode, isLightMode)
        trayIcon?.toolTip = "Mr Speed PRO | Desktop: ${dlSpeed.toInt()} Mbps ↓ / ${ulSpeed.toInt()} Mbps ↑ / ${ping.toInt()} ms (Click to test)"
    }

    fun showToastNotification(title: String, message: String, isLightMode: Boolean = isLight) {
        // Suppressed: Handled exclusively by Compose Floating Toast Overlay in Main.kt
    }

    private fun createMetricImage(text: String, mode: Int, isLightMode: Boolean): BufferedImage {
        val size = 32 // 32x32 High-DPI canvas
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)

        val primaryColor = if (isLightMode) Color(255, 159, 10) else Color(0, 229, 255)

        if (mode == 3 || text == "LOGO") {
            // High-precision vector rendering of favicon_cyan (Dark Theme) & favicon_amber (Light Theme)
            val scale = size / 108f

            // Background Circle
            val bgColor = if (isLightMode) Color(240, 242, 248) else Color(8, 8, 14)
            g.color = bgColor
            g.fillOval(0, 0, size, size)

            val boltColor1 = if (isLightMode) Color(255, 159, 10) else Color(0, 229, 255) // Amber vs Cyan
            val boltColor2 = Color(94, 92, 230) // Indigo

            // Outer glass ring
            g.color = if (isLightMode) Color(255, 159, 10, 50) else Color(255, 255, 255, 35)
            g.stroke = BasicStroke(1f * scale)
            val ringOffset = (10 * scale).toInt()
            val ringSize = (88 * scale).toInt()
            g.drawOval(ringOffset, ringOffset, ringSize, ringSize)

            // Speed Lines on left
            val linePaint = GradientPaint(
                11f * scale, 54f * scale, if (isLightMode) Color(255, 159, 10, 0) else Color(0, 229, 255, 0),
                43f * scale, 54f * scale, if (isLightMode) Color(255, 159, 10, 180) else Color(0, 229, 255, 180)
            )
            g.paint = linePaint
            g.stroke = BasicStroke(3f * scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

            val line1 = Path2D.Float()
            line1.moveTo((11f * scale).toDouble(), (43.2f * scale).toDouble())
            line1.lineTo((37.8f * scale).toDouble(), (43.2f * scale).toDouble())
            g.draw(line1)

            val line2 = Path2D.Float()
            line2.moveTo((11f * scale).toDouble(), (54f * scale).toDouble())
            line2.lineTo((32.4f * scale).toDouble(), (54f * scale).toDouble())
            g.draw(line2)

            val line3 = Path2D.Float()
            line3.moveTo((11f * scale).toDouble(), (64.8f * scale).toDouble())
            line3.lineTo((27f * scale).toDouble(), (64.8f * scale).toDouble())
            g.draw(line3)

            // Lightning Bolt Path: M48.6 32.4 L81 48.6 L43.2 59.4 L70.2 81
            val boltGrad = GradientPaint(
                43.2f * scale, 32.4f * scale, boltColor1,
                75.6f * scale, 75.6f * scale, boltColor2
            )
            g.paint = boltGrad
            g.stroke = BasicStroke(4.5f * scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

            val boltPath = Path2D.Float()
            boltPath.moveTo((48.6f * scale).toDouble(), (32.4f * scale).toDouble())
            boltPath.lineTo((81f * scale).toDouble(), (48.6f * scale).toDouble())
            boltPath.lineTo((43.2f * scale).toDouble(), (59.4f * scale).toDouble())
            boltPath.lineTo((70.2f * scale).toDouble(), (81f * scale).toDouble())
            g.draw(boltPath)
        } else {
            // Determine Neon Font Color
            val fontColor = when (mode) {
                0 -> primaryColor // Download: Amber in Light Theme, Cyan in Dark Theme
                1 -> if (isLightMode) Color(140, 80, 220) else Color(176, 102, 255) // Upload: Purple
                else -> if (isLightMode) Color(0, 180, 220) else Color(255, 159, 10) // Ping
            }

            val bgColor = if (isLightMode) Color(240, 242, 248, 240) else Color(8, 8, 14, 230)
            g.color = bgColor
            g.fillRoundRect(0, 0, size, size, 8, 8)
            g.color = fontColor
            g.drawRoundRect(0, 0, size - 1, size - 1, 8, 8)

            // Dynamically calculate maximum possible font size that fills canvas as large as possible
            var fontSize = 22
            var font = Font("Segoe UI", Font.BOLD, fontSize)
            g.font = font
            var fm = g.fontMetrics

            while ((fm.stringWidth(text) > (size - 3) || fm.height > (size - 3)) && fontSize > 8) {
                fontSize--
                font = Font("Segoe UI", Font.BOLD, fontSize)
                g.font = font
                fm = g.fontMetrics
            }

            // Center text perfectly
            val x = (size - fm.stringWidth(text)) / 2
            val y = ((size - fm.height) / 2) + fm.ascent

            g.color = fontColor
            g.drawString(text, x.coerceAtLeast(1), y)
        }

        g.dispose()
        return image
    }
}
