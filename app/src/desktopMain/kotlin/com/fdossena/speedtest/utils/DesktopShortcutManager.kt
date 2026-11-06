package com.fdossena.speedtest.utils

import java.awt.*
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import javax.imageio.ImageIO

object DesktopShortcutManager {

    private val desktopResDir = File("E:/My-Speed-Test-App/app/src/desktopMain/resources").apply { if (!exists()) mkdirs() }
    private val webImagesDir = File("E:/My-Speed-Test-App/web/images").apply { if (!exists()) mkdirs() }

    data class AppExecTarget(
        val targetPath: String,
        val appArguments: String,
        val suiteArguments: String
    )

    fun getAppExecutionTarget(): AppExecTarget {
        // 1. Check jpackage app path (Installer / Installed EXE)
        val jpackagePath = System.getProperty("jpackage.app-path")
        if (!jpackagePath.isNullOrBlank() && File(jpackagePath).exists()) {
            return AppExecTarget(
                targetPath = File(jpackagePath).absolutePath,
                appArguments = "",
                suiteArguments = "--suite"
            )
        }

        // 2. Check current ProcessHandle command (running compiled EXE)
        try {
            val processCmd = ProcessHandle.current().info().command().orElse(null)
            if (!processCmd.isNullOrBlank() && processCmd.endsWith(".exe", ignoreCase = true) && File(processCmd).exists()) {
                val exeFile = File(processCmd)
                if (!exeFile.name.startsWith("java", ignoreCase = true)) {
                    return AppExecTarget(
                        targetPath = exeFile.absolutePath,
                        appArguments = "",
                        suiteArguments = "--suite"
                    )
                }
            }
        } catch (_: Exception) {}

        // 3. Fallback for Development (JVM / JAR)
        val javaExe = System.getProperty("java.home") + "\\bin\\javaw.exe"
        val jarPath = File(System.getProperty("java.class.path")).absolutePath
        return AppExecTarget(
            targetPath = javaExe,
            appArguments = "-jar \"$jarPath\"",
            suiteArguments = "-jar \"$jarPath\" --suite"
        )
    }

    fun getExecutableStartCommand(): String {
        val exec = getAppExecutionTarget()
        val silentArg = if (exec.appArguments.isBlank()) "--silent" else "${exec.appArguments} --silent"
        return "\"${exec.targetPath}\" $silentArg"
    }

    fun createOrUpdateShortcuts(isLightMode: Boolean) {
        val os = System.getProperty("os.name")?.lowercase() ?: ""

        val appIconFile = generateAppIcon(isLightMode)
        val suiteIconFile = generateSuiteIcon(isLightMode)

        // Pre-generate all color variants so files exist on disk for installer & theme switches
        generateAppIcon(true)
        generateAppIcon(false)
        generateSuiteIcon(true)
        generateSuiteIcon(false)
        generateWixSkinBitmaps()

        val exec = getAppExecutionTarget()

        if (os.contains("win")) {
            try {
                val targetPath = exec.targetPath.replace("\\", "\\\\")
                val appArgs = exec.appArguments.replace("\"", "`\"")
                val suiteArgs = exec.suiteArguments.replace("\"", "`\"")

                val psScript = """
                    ${'$'}WshShell = New-Object -ComObject WScript.Shell
                    ${'$'}DesktopPath = [System.Environment]::GetFolderPath('Desktop')
                    ${'$'}StartMenuPath = Join-Path ([System.Environment]::GetFolderPath('StartMenu')) 'Programs'
                    ${'$'}StartMenuGroupPath = Join-Path ${'$'}StartMenuPath 'Mr Speed PRO'

                    if (-not (Test-Path ${'$'}StartMenuPath)) { New-Item -ItemType Directory -Path ${'$'}StartMenuPath -Force }
                    if (-not (Test-Path ${'$'}StartMenuGroupPath)) { New-Item -ItemType Directory -Path ${'$'}StartMenuGroupPath -Force }

                    # 1. Mr Speed Desktop Shortcuts (Desktop, Start Menu Top Level & Program Group)
                    ${'$'}AppDesktop = ${'$'}WshShell.CreateShortcut("${'$'}DesktopPath\Mr Speed Desktop.lnk")
                    ${'$'}AppDesktop.TargetPath = "$targetPath"
                    ${'$'}AppDesktop.Arguments = "$appArgs"
                    ${'$'}AppDesktop.IconLocation = "${appIconFile.absolutePath.replace("\\", "\\\\")}"
                    ${'$'}AppDesktop.Save()

                    ${'$'}AppStart1 = ${'$'}WshShell.CreateShortcut("${'$'}StartMenuPath\Mr Speed Desktop.lnk")
                    ${'$'}AppStart1.TargetPath = "$targetPath"
                    ${'$'}AppStart1.Arguments = "$appArgs"
                    ${'$'}AppStart1.IconLocation = "${appIconFile.absolutePath.replace("\\", "\\\\")}"
                    ${'$'}AppStart1.Save()

                    ${'$'}AppStart2 = ${'$'}WshShell.CreateShortcut("${'$'}StartMenuGroupPath\Mr Speed Desktop.lnk")
                    ${'$'}AppStart2.TargetPath = "$targetPath"
                    ${'$'}AppStart2.Arguments = "$appArgs"
                    ${'$'}AppStart2.IconLocation = "${appIconFile.absolutePath.replace("\\", "\\\\")}"
                    ${'$'}AppStart2.Save()

                    # 2. Mr Pro Network Tools Shortcuts (Desktop, Start Menu Top Level & Program Group)
                    ${'$'}SuiteDesktop = ${'$'}WshShell.CreateShortcut("${'$'}DesktopPath\Mr Pro Network Tools.lnk")
                    ${'$'}SuiteDesktop.TargetPath = "$targetPath"
                    ${'$'}SuiteDesktop.Arguments = "$suiteArgs"
                    ${'$'}SuiteDesktop.IconLocation = "${suiteIconFile.absolutePath.replace("\\", "\\\\")}"
                    ${'$'}SuiteDesktop.Save()

                    ${'$'}SuiteStart1 = ${'$'}WshShell.CreateShortcut("${'$'}StartMenuPath\Mr Pro Network Tools.lnk")
                    ${'$'}SuiteStart1.TargetPath = "$targetPath"
                    ${'$'}SuiteStart1.Arguments = "$suiteArgs"
                    ${'$'}SuiteStart1.IconLocation = "${suiteIconFile.absolutePath.replace("\\", "\\\\")}"
                    ${'$'}SuiteStart1.Save()

                    ${'$'}SuiteStart2 = ${'$'}WshShell.CreateShortcut("${'$'}StartMenuGroupPath\Mr Pro Network Tools.lnk")
                    ${'$'}SuiteStart2.TargetPath = "$targetPath"
                    ${'$'}SuiteStart2.Arguments = "$suiteArgs"
                    ${'$'}SuiteStart2.IconLocation = "${suiteIconFile.absolutePath.replace("\\", "\\\\")}"
                    ${'$'}SuiteStart2.Save()

                    # Force Windows Shell Icon Cache & Start Menu Refresh
                    ${'$'}code = @'
                    [DllImport("shell32.dll", CharSet = CharSet.Auto, SetLastError = true)]
                    public static extern void SHChangeNotify(int wEventId, uint uFlags, IntPtr dwItem1, IntPtr dwItem2);
'@
                    ${'$'}type = Add-Type -MemberDefinition ${'$'}code -Name "Win32SHChangeNotify" -Namespace "Win32" -PassThru
                    ${'$'}type::SHChangeNotify(0x08000000, 0, [IntPtr]::Zero, [IntPtr]::Zero)
                """.trimIndent()

                val tempPsFile = File.createTempFile("create_shortcuts", ".ps1")
                tempPsFile.writeText(psScript)

                val process = Runtime.getRuntime().exec(arrayOf("powershell.exe", "-ExecutionPolicy", "Bypass", "-File", tempPsFile.absolutePath))
                process.inputStream.close()
                process.errorStream.close()
                process.waitFor()
                tempPsFile.delete()

                println("[DesktopShortcutManager] Created/Updated Windows Desktop & Start Menu shortcuts for target: ${exec.targetPath} (LightMode=$isLightMode).")
            } catch (e: Exception) {
                println("[DesktopShortcutManager] Windows shortcut error: ${e.message}")
            }
        } else if (os.contains("linux") || os.contains("unix")) {
            try {
                val homeDir = System.getProperty("user.home") ?: return
                val appsDir = File(homeDir, ".local/share/applications").apply { if (!exists()) mkdirs() }
                val desktopDir = File(homeDir, "Desktop").apply { if (!exists()) mkdirs() }

                val pngAppIcon = File(desktopResDir, if (isLightMode) "favicon_amber.png" else "favicon_cyan.png").absolutePath
                val pngSuiteIcon = File(desktopResDir, if (isLightMode) "suite_amber.png" else "suite_cyan.png").absolutePath

                val mainAppContent = """
                    [Desktop Entry]
                    Name=Mr Speed Desktop
                    Comment=Mr Speed PRO Broadband Speedtest & Telemetry Diagnostics
                    Exec=${exec.targetPath} ${exec.appArguments} %u
                    Icon=$pngAppIcon
                    Terminal=false
                    Type=Application
                    Categories=Network;Utility;
                    Keywords=speedtest;network;diagnostics;internet;
                """.trimIndent()

                val proToolsContent = """
                    [Desktop Entry]
                    Name=Mr Pro Network Tools
                    Comment=Mr Speed PRO Suite & Network Diagnostics Tools
                    Exec=${exec.targetPath} ${exec.suiteArguments} %u
                    Icon=$pngSuiteIcon
                    Terminal=false
                    Type=Application
                    Categories=Network;Utility;
                    Keywords=speedtest;network;protools;suite;
                """.trimIndent()

                File(appsDir, "mrspeed-desktop.desktop").writeText(mainAppContent)
                File(appsDir, "mrspeed-protools.desktop").writeText(proToolsContent)

                val deskMain = File(desktopDir, "mrspeed-desktop.desktop")
                deskMain.writeText(mainAppContent)
                deskMain.setExecutable(true)

                val deskSuite = File(desktopDir, "mrspeed-protools.desktop")
                deskSuite.writeText(proToolsContent)
                deskSuite.setExecutable(true)

                println("[DesktopShortcutManager] Created/Updated Linux Desktop & App Menu shortcuts.")
            } catch (e: Exception) {
                println("[DesktopShortcutManager] Linux shortcut error: ${e.message}")
            }
        } else if (os.contains("mac") || os.contains("darwin")) {
            try {
                val homeDir = System.getProperty("user.home") ?: return
                val binDir = File(homeDir, ".local/bin").apply { if (!exists()) mkdirs() }

                val mainAppCmd = "#!/bin/bash\nexec \"${exec.targetPath}\" ${exec.appArguments} \"\$@\"\n"
                val suiteCmd = "#!/bin/bash\nexec \"${exec.targetPath}\" ${exec.suiteArguments} \"\$@\"\n"

                val binMain = File(binDir, "mrspeed")
                binMain.writeText(mainAppCmd)
                binMain.setExecutable(true)

                val binSuite = File(binDir, "mrspeed-protools")
                binSuite.writeText(suiteCmd)
                binSuite.setExecutable(true)

                println("[DesktopShortcutManager] Created/Updated macOS CLI launchers in ~/.local/bin/.")
            } catch (e: Exception) {
                println("[DesktopShortcutManager] macOS shortcut error: ${e.message}")
            }
        }
    }

    private fun generateAppIcon(isLightMode: Boolean): File {
        val fileName = if (isLightMode) "favicon_amber.ico" else "favicon_cyan.ico"
        val pngFileName = if (isLightMode) "favicon_amber.png" else "favicon_cyan.png"

        val targetFile1 = File(desktopResDir, fileName)
        val targetFile2 = File(webImagesDir, fileName)
        val pngFile1 = File(desktopResDir, pngFileName)
        val pngFile2 = File(webImagesDir, pngFileName)

        val img = renderAppVectorLogo(128, isLightMode)
        try { ImageIO.write(img, "png", pngFile1); ImageIO.write(img, "png", pngFile2) } catch (_: Exception) {}
        try { writePngAsIco(img, targetFile1); writePngAsIco(img, targetFile2) } catch (_: Exception) {}

        return if (targetFile1.exists() && targetFile1.length() > 0) targetFile1 else pngFile1
    }

    private fun generateSuiteIcon(isLightMode: Boolean): File {
        val fileName = if (isLightMode) "suite_amber.ico" else "suite_cyan.ico"
        val pngFileName = if (isLightMode) "suite_amber.png" else "suite_cyan.png"

        val targetFile1 = File(desktopResDir, fileName)
        val targetFile2 = File(webImagesDir, fileName)
        val pngFile1 = File(desktopResDir, pngFileName)
        val pngFile2 = File(webImagesDir, pngFileName)

        val img = renderSuiteVectorIcon(128, isLightMode)
        try { ImageIO.write(img, "png", pngFile1); ImageIO.write(img, "png", pngFile2) } catch (_: Exception) {}
        try { writePngAsIco(img, targetFile1); writePngAsIco(img, targetFile2) } catch (_: Exception) {}

        return if (targetFile1.exists() && targetFile1.length() > 0) targetFile1 else pngFile1
    }

    fun generateWixSkinBitmaps() {
        val bannerFile = File(desktopResDir, "banner.bmp")
        val dialogFile = File(desktopResDir, "dialog.bmp")

        val bannerImg = BufferedImage(493, 58, BufferedImage.TYPE_INT_RGB)
        val gBanner = bannerImg.createGraphics()
        gBanner.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        gBanner.color = java.awt.Color(0x08, 0x08, 0x0E)
        gBanner.fillRect(0, 0, 493, 58)
        gBanner.color = java.awt.Color(0x00, 0xE5, 0xFF)
        gBanner.fillRect(0, 56, 493, 2)
        val logoBanner = renderAppVectorLogo(48, false)
        gBanner.drawImage(logoBanner, 430, 5, null)
        gBanner.dispose()

        val dialogImg = BufferedImage(493, 312, BufferedImage.TYPE_INT_RGB)
        val gDialog = dialogImg.createGraphics()
        gDialog.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        gDialog.color = java.awt.Color(0x08, 0x08, 0x0E)
        gDialog.fillRect(0, 0, 493, 312)
        val logoDialog = renderAppVectorLogo(96, false)
        gDialog.drawImage(logoDialog, 20, 20, null)
        gDialog.dispose()

        try { ImageIO.write(bannerImg, "bmp", bannerFile) } catch (_: Exception) {}
        try { ImageIO.write(dialogImg, "bmp", dialogFile) } catch (_: Exception) {}
    }

    private fun writePngAsIco(image: BufferedImage, icoFile: File) {
        val pngBaos = ByteArrayOutputStream()
        ImageIO.write(image, "png", pngBaos)
        val pngBytes = pngBaos.toByteArray()

        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        // ICONDIR Header (6 bytes)
        dos.writeByte(0) // Reserved (0)
        dos.writeByte(0)
        dos.writeByte(1) // Type: 1 = ICO
        dos.writeByte(0)
        dos.writeByte(1) // Image count: 1
        dos.writeByte(0)

        // ICONDIRENTRY (16 bytes)
        val w = if (image.width >= 256) 0 else image.width
        val h = if (image.height >= 256) 0 else image.height
        dos.writeByte(w) // Width
        dos.writeByte(h) // Height
        dos.writeByte(0) // Color count (0 = >256 colors)
        dos.writeByte(0) // Reserved (0)
        dos.writeByte(1) // Color planes (1)
        dos.writeByte(0)
        dos.writeByte(32) // Bit count (32 bits per pixel ARGB)
        dos.writeByte(0)

        // Image size in bytes (4 bytes Little-Endian)
        val pngSize = pngBytes.size
        dos.writeByte(pngSize and 0xFF)
        dos.writeByte((pngSize shr 8) and 0xFF)
        dos.writeByte((pngSize shr 16) and 0xFF)
        dos.writeByte((pngSize shr 24) and 0xFF)

        // Image offset from start of file = 22 bytes (4 bytes Little-Endian)
        val offset = 22
        dos.writeByte(offset and 0xFF)
        dos.writeByte((offset shr 8) and 0xFF)
        dos.writeByte((offset shr 16) and 0xFF)
        dos.writeByte((offset shr 24) and 0xFF)

        // Write PNG raw payload bytes
        dos.write(pngBytes)
        dos.flush()

        icoFile.writeBytes(baos.toByteArray())
    }

    private fun renderAppVectorLogo(size: Int, isLightMode: Boolean): BufferedImage {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

        val scale = size / 108f
        val primaryColor = if (isLightMode) Color(255, 159, 10) else Color(0, 229, 255)
        val indigoColor = Color(94, 92, 230)

        // Outer circle
        g.color = if (isLightMode) Color(240, 242, 248) else Color(8, 8, 14)
        g.fillOval(0, 0, size, size)

        // Glass Ring
        g.color = if (isLightMode) Color(255, 159, 10, 60) else Color(255, 255, 255, 40)
        g.stroke = BasicStroke(1.5f * scale)
        val ringOffset = (10 * scale).toInt()
        val ringSize = (88 * scale).toInt()
        g.drawOval(ringOffset, ringOffset, ringSize, ringSize)

        // Speed lines
        g.paint = GradientPaint(11f * scale, 54f * scale, Color(0, 0, 0, 0), 43f * scale, 54f * scale, primaryColor)
        g.stroke = BasicStroke(3.5f * scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

        val l1 = Path2D.Float().apply { moveTo(11f * scale, 43.2f * scale); lineTo(37.8f * scale, 43.2f * scale) }
        val l2 = Path2D.Float().apply { moveTo(11f * scale, 54f * scale); lineTo(32.4f * scale, 54f * scale) }
        val l3 = Path2D.Float().apply { moveTo(11f * scale, 64.8f * scale); lineTo(27f * scale, 64.8f * scale) }
        g.draw(l1); g.draw(l2); g.draw(l3)

        // Lightning bolt path
        g.paint = GradientPaint(43.2f * scale, 32.4f * scale, primaryColor, 75.6f * scale, 75.6f * scale, indigoColor)
        g.stroke = BasicStroke(4.5f * scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val bolt = Path2D.Float().apply {
            moveTo(48.6f * scale, 32.4f * scale)
            lineTo(81f * scale, 48.6f * scale)
            lineTo(43.2f * scale, 59.4f * scale)
            lineTo(70.2f * scale, 81f * scale)
        }
        g.draw(bolt)

        g.dispose()
        return image
    }

    private fun renderSuiteVectorIcon(size: Int, isLightMode: Boolean): BufferedImage {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

        val scale = size / 108f
        val primaryColor = if (isLightMode) Color(255, 159, 10) else Color(0, 229, 255)
        val indigoColor = Color(94, 92, 230)

        // Background Circle
        g.color = if (isLightMode) Color(240, 242, 248) else Color(8, 8, 14)
        g.fillOval(0, 0, size, size)

        // Network Mesh Polygon Lines
        g.color = Color(94, 92, 230, 180)
        g.stroke = BasicStroke(2f * scale)
        val hex = Path2D.Float().apply {
            moveTo(54f * scale, 30f * scale)
            lineTo(80f * scale, 45f * scale)
            lineTo(80f * scale, 63f * scale)
            lineTo(54f * scale, 78f * scale)
            lineTo(28f * scale, 63f * scale)
            lineTo(28f * scale, 45f * scale)
            closePath()
        }
        g.draw(hex)

        // Connection Lines
        g.color = primaryColor
        g.stroke = BasicStroke(3f * scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

        val lines = listOf(
            Pair(54f to 54f, 54f to 15f),
            Pair(54f to 54f, 90f to 34f),
            Pair(54f to 54f, 90f to 74f),
            Pair(54f to 54f, 54f to 93f),
            Pair(54f to 54f, 18f to 74f),
            Pair(54f to 54f, 18f to 34f)
        )

        lines.forEach { (start, end) ->
            val p = Path2D.Float().apply {
                moveTo(start.first * scale, start.second * scale)
                lineTo(end.first * scale, end.second * scale)
            }
            g.draw(p)
        }

        // Satellite Nodes
        val nodeCoords = listOf(
            54f to 15f, 90f to 34f, 90f to 74f,
            54f to 93f, 18f to 74f, 18f to 34f
        )
        val nodeR = (5f * scale).toInt()
        nodeCoords.forEach { (nx, ny) ->
            g.color = primaryColor
            g.fillOval((nx * scale - nodeR).toInt(), (ny * scale - nodeR).toInt(), nodeR * 2, nodeR * 2)
        }

        // Central Master Hub
        val hubR = (9f * scale).toInt()
        g.color = primaryColor
        g.fillOval((54f * scale - hubR).toInt(), (54f * scale - hubR).toInt(), hubR * 2, hubR * 2)
        g.color = indigoColor
        g.stroke = BasicStroke(2f * scale)
        g.drawOval((54f * scale - hubR).toInt(), (54f * scale - hubR).toInt(), hubR * 2, hubR * 2)

        g.dispose()
        return image
    }
}
