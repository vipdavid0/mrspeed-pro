package com.fdossena.speedtest.data

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptedLocalStore {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12

    private fun getStorageDirectory(): File {
        val os = System.getProperty("os.name")?.lowercase() ?: ""
        val jpackagePath = System.getProperty("jpackage.app-path") ?: ""
        val appDir = System.getProperty("app.dir") ?: ""
        val isInstalledApp = jpackagePath.isNotEmpty() || appDir.isNotEmpty() || File("C:/Program Files/MrSpeed").exists()

        return when {
            os.contains("win") -> {
                val programFilesDir = File("C:/Program Files/MrSpeed/data")
                val programDataDir = File("C:/ProgramData/MrSpeed/data")
                val appDataDir = File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "MrSpeed/data")
                val localAppDataDir = File(System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"), "MrSpeed/data")

                try {
                    if (programFilesDir.exists() || programFilesDir.mkdirs()) return programFilesDir
                } catch (_: Exception) {}

                try {
                    if (programDataDir.exists() || programDataDir.mkdirs()) return programDataDir
                } catch (_: Exception) {}

                val dir = if (isInstalledApp) appDataDir else localAppDataDir
                dir.apply { mkdirs() }
            }
            os.contains("mac") || os.contains("darwin") -> {
                File(System.getProperty("user.home"), "Library/Application Support/MrSpeed/data").apply { mkdirs() }
            }
            else -> {
                // Linux / Unix XDG_DATA_HOME standard (~/.local/share/MrSpeed/data)
                val xdgDataHome = System.getenv("XDG_DATA_HOME") ?: "${System.getProperty("user.home")}/.local/share"
                File(xdgDataHome, "MrSpeed/data").apply { mkdirs() }
            }
        }
    }

    fun getSettingsFile(): File {
        val jpackagePath = System.getProperty("jpackage.app-path") ?: ""
        val appDir = System.getProperty("app.dir") ?: ""
        val isInstalled = jpackagePath.isNotEmpty() || appDir.isNotEmpty() || File("C:/Program Files/MrSpeed").exists()
        val fileName = if (isInstalled) "mrspeed_production_settings.dat" else "mrspeed_dev_settings.dat"
        return File(getStorageDirectory(), fileName)
    }

    private fun getSecretKey(): SecretKeySpec {
        val systemInfo = (System.getenv("COMPUTERNAME") ?: System.getenv("HOSTNAME") ?: "MrSpeedHardware") +
                (System.getProperty("user.name") ?: "MrSpeedUser") +
                "MrSpeedProSecureSalt2026"
        val sha256 = MessageDigest.getInstance("SHA-256")
        val keyBytes = sha256.digest(systemInfo.toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(IV_LENGTH_BYTE)
            SecureRandom().nextBytes(iv)
            val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), parameterSpec)
            val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            return Base64.getEncoder().encodeToString(combined)
        } catch (_: Exception) {
            return ""
        }
    }

    fun decrypt(cipherTextBase64: String): String {
        if (cipherTextBase64.isEmpty()) return ""
        try {
            val combined = Base64.getDecoder().decode(cipherTextBase64)
            if (combined.size < IV_LENGTH_BYTE) return ""
            val iv = ByteArray(IV_LENGTH_BYTE)
            System.arraycopy(combined, 0, iv, 0, iv.size)
            val cipherText = ByteArray(combined.size - IV_LENGTH_BYTE)
            System.arraycopy(combined, IV_LENGTH_BYTE, cipherText, 0, cipherText.size)
            val cipher = Cipher.getInstance(ALGORITHM)
            val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), parameterSpec)
            val plainTextBytes = cipher.doFinal(cipherText)
            return String(plainTextBytes, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            return ""
        }
    }

    fun loadProps(): Properties {
        val props = Properties()
        val file = getSettingsFile()
        if (file.exists()) {
            try {
                FileInputStream(file).use { props.load(it) }
            } catch (_: Exception) {}
        }
        return props
    }

    fun saveProps(props: Properties) {
        val file = getSettingsFile()
        try {
            FileOutputStream(file).use { props.store(it, "Mr Speed PRO Encrypted Settings Data") }
        } catch (_: Exception) {}
    }
}
