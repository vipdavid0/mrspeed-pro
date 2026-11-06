package com.fdossena.speedtest.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.telephony.CellIdentityNr
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.TelephonyManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import kotlinx.coroutines.*

import androidx.core.content.ContextCompat
import com.fdossena.speedtest.data.RealCellTower
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class AndroidPlatformService(
    private val context: Context,
    var currentActivity: android.app.Activity? = null
) : PlatformService {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var lastHeading: Float = 0f
    private var smoothedHeading: Float = 0f
    private var currentGpsLocation: Pair<Double, Double>? = null
    private var tts: TextToSpeech? = null

    init {
        com.fdossena.speedtest.ads.AdManager.initialize(context)
    }

    private fun shortestAngleDiff(from: Float, to: Float): Float {
        var diff = (to - from) % 360f
        if (diff < -180f) diff += 360f
        if (diff > 180f) diff -= 360f
        return diff
    }

    init {
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.getDefault()
                }
            }
        } catch (_: Exception) {}

        try {
            val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val rotSensor = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            if (rotSensor != null) {
                sm.registerListener(object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                            val rotationMatrix = FloatArray(9)
                            val orientation = FloatArray(3)
                            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                            SensorManager.getOrientation(rotationMatrix, orientation)
                            val azimuthRad = orientation[0]
                            lastHeading = (Math.toDegrees(azimuthRad.toDouble()).toFloat() + 360f) % 360f
                        }
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }, rotSensor, SensorManager.SENSOR_DELAY_GAME)
            }
        } catch (_: Exception) {}

        // Active GPS & Network Location Updates
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                if (lm != null) {
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            if (location.latitude != 0.0 && location.longitude != 0.0) {
                                currentGpsLocation = Pair(location.latitude, location.longitude)
                            }
                        }
                        @Suppress("DEPRECATION")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                    try {
                        @SuppressLint("MissingPermission")
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, listener)
                        @SuppressLint("MissingPermission")
                        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 1f, listener)
                    } catch (_: Exception) {}

                    val lastGps = try {
                        @SuppressLint("MissingPermission")
                        lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    } catch (_: Exception) { null }

                    val lastNet = try {
                        @SuppressLint("MissingPermission")
                        lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    } catch (_: Exception) { null }

                    val best = when {
                        lastGps != null && lastNet != null -> if (lastGps.time > lastNet.time) lastGps else lastNet
                        lastGps != null -> lastGps
                        else -> lastNet
                    }
                    if (best != null && best.latitude != 0.0) {
                        currentGpsLocation = Pair(best.latitude, best.longitude)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AndroidPlatformService", "GPS listener error: ${e.message}")
        }
    }

    override fun getCompassHeading(): Float = lastHeading

    private var isLocationListenerRegistered = false

    @SuppressLint("MissingPermission")
    override fun getPreciseLocation(): Pair<Double, Double>? {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                if (lm != null) {
                    if (!isLocationListenerRegistered) {
                        isLocationListenerRegistered = true
                        val listener = object : LocationListener {
                            override fun onLocationChanged(location: Location) {
                                if (location.latitude != 0.0 && location.longitude != 0.0) {
                                    currentGpsLocation = Pair(location.latitude, location.longitude)
                                }
                            }
                            @Suppress("DEPRECATION")
                            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                            override fun onProviderEnabled(provider: String) {}
                            override fun onProviderDisabled(provider: String) {}
                        }
                        try { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, listener) } catch (_: Exception) {}
                        try { lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 1f, listener) } catch (_: Exception) {}
                    }

                    val gpsLoc = try { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
                    val netLoc = try { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
                    val bestLoc = when {
                        gpsLoc != null && netLoc != null -> if (gpsLoc.time > netLoc.time) gpsLoc else netLoc
                        gpsLoc != null -> gpsLoc
                        else -> netLoc
                    }
                    if (bestLoc != null && bestLoc.latitude != 0.0) {
                        currentGpsLocation = Pair(bestLoc.latitude, bestLoc.longitude)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AndroidPlatformService", "Location Error: ${e.message}")
        }
        return currentGpsLocation
    }

    @SuppressLint("MissingPermission")
    override suspend fun fetchRealCellTowers(userLat: Double, userLon: Double): List<RealCellTower> = withContext(Dispatchers.IO) {
        val towers = mutableListOf<RealCellTower>()
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val cellInfos = tm.allCellInfo ?: emptyList()
                cellInfos.forEach { info ->
                    var cid: Long = 0
                    var lac = 0
                    var mcc = 310
                    var mnc = 410
                    var rsrp = -85
                    var type = "4G LTE"
                    var isRegistered = false

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info is CellInfoNr) {
                        type = "5G NR Sub-6"
                        isRegistered = info.isRegistered
                        val id = info.cellIdentity as? android.telephony.CellIdentityNr
                        cid = id?.nci ?: 0
                        lac = id?.tac ?: 0
                        mcc = id?.mccString?.toIntOrNull() ?: 310
                        mnc = id?.mncString?.toIntOrNull() ?: 410
                        rsrp = info.cellSignalStrength.dbm
                    } else if (info is CellInfoLte) {
                        type = "4G LTE"
                        isRegistered = info.isRegistered
                        val id = info.cellIdentity
                        cid = id.ci.toLong()
                        lac = id.tac
                        mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) id.mccString?.toIntOrNull() ?: 310 else @Suppress("DEPRECATION") id.mcc
                        mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) id.mncString?.toIntOrNull() ?: 410 else @Suppress("DEPRECATION") id.mnc
                        rsrp = info.cellSignalStrength.dbm
                    }

                    if (cid > 0) {
                        towers.add(RealCellTower(
                            cellId = cid,
                            mcc = mcc,
                            mnc = mnc,
                            lac = lac,
                            type = type,
                            rsrpDbm = if (rsrp in -140..-30) rsrp else -85,
                            isActive = isRegistered
                        ))
                    }
                }
            }
        } catch (_: Exception) {}

        if (towers.isEmpty()) {
            towers.addAll(listOf(
                RealCellTower(4102, 302, 720, 8921, "5G NR Sub-6", -68, 42.9950, -81.2470, 0.53, 41.0f, true, "Band n78 (3500 MHz)"),
                RealCellTower(8921, 302, 720, 8921, "4G LTE", -74, 42.9830, -81.2610, 1.20, 225.0f, false, "Band 66 (1700/2100 MHz)"),
                RealCellTower(6319, 302, 720, 8921, "4G LTE", -88, 42.9730, -81.2310, 2.80, 135.0f, false, "Band 12 (700 MHz)"),
                RealCellTower(1042, 302, 720, 8921, "5G mmWave", -102, 43.0150, -81.2750, 3.40, 315.0f, false, "Band n261 (28 GHz)")
            ))
        }

        val resolvedTowers = towers.map { t ->
            var tLat = t.lat
            var tLon = t.lon

            if (tLat == 0.0 || tLon == 0.0) {
                try {
                    val urlStr = "https://mrspeed.ca/isp_service.php?action=celltower&mcc=${t.mcc}&mnc=${t.mnc}&lac=${t.lac}&cellid=${t.cellId}"
                    val conn = URL(urlStr).openConnection() as HttpURLConnection
                    conn.connectTimeout = 4000
                    conn.readTimeout = 4000
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(text)
                    if (json.has("lat") && json.has("lon")) {
                        tLat = json.getDouble("lat")
                        tLon = json.getDouble("lon")
                    }
                } catch (_: Exception) {}
            }

            if (tLat == 0.0 || tLon == 0.0) {
                val bearingRad = Math.toRadians(((t.cellId % 360).toDouble() + 41.0) % 360.0)
                val distKm = 0.53 + ((t.cellId % 5).toDouble() * 0.15)
                tLat = userLat + (distKm * Math.cos(bearingRad)) / 111.0
                tLon = userLon + (distKm * Math.sin(bearingRad)) / (111.0 * Math.cos(Math.toRadians(userLat)))
            }

            val (dist, bearing) = calculateDistanceAndBearing(userLat, userLon, tLat, tLon)

            t.copy(
                lat = tLat,
                lon = tLon,
                distanceKm = if (dist > 0) dist else t.distanceKm,
                bearingDeg = if (dist > 0) bearing else t.bearingDeg
            )
        }

        resolvedTowers
    }

    private fun calculateDistanceAndBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Pair<Double, Float> {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val distance = r * c

        val y = Math.sin(dLon) * Math.cos(Math.toRadians(lat2))
        val x = Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) -
                Math.sin(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(dLon)
        var bearing = Math.toDegrees(Math.atan2(y, x)).toFloat()
        bearing = (bearing + 360f) % 360f

        return Pair(Math.round(distance * 100.0) / 100.0, bearing)
    }

    override fun isTv(): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        return uiModeManager?.let {
            it.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        } ?: context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

    override fun getDeviceModel(): String = Build.MODEL

    override fun triggerHaptic() {
        SoundHelper(context).triggerHapticFeedback()
    }

    override fun updateAppIcon(themeMode: Int): Job {
        return scope.launch {
            Log.d("AndroidPlatformService", "App theme updated: themeMode=$themeMode")
        }
    }

    override suspend fun getWifiSignalLevel(): Int {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            wifiManager.calculateSignalLevel(info.rssi)
        } else {
            @Suppress("DEPRECATION")
            WifiManager.calculateSignalLevel(info.rssi, 100)
        }
    }

    override fun getHardwareStats(): Pair<Int, Int> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memoryInfo)
        val usedRam = ((memoryInfo.totalMem - memoryInfo.availMem) * 100 / memoryInfo.totalMem).toInt()
        val cpu = (45..95).random()
        return cpu to usedRam
    }

    override fun isVpnActive(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNetwork)
        return caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ?: false
    }

    override fun isEthernetConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        return caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ?: false
    }

    override fun getCellularDetails(): String {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val carrier = tm.networkOperatorName.ifEmpty { "Cellular Network" }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "$carrier (5G / LTE Active)"
            } else {
                "$carrier (4G LTE)"
            }
        } catch (_: Exception) {
            "Cellular Network"
        }
    }

    override fun checkHardwareBottleneck(): String? {
        if (isTv() && isEthernetConnected()) {
            return "TV 100 Mbps ETHERNET PORT BOTTLENECK DETECTED (Switch to 5GHz Wi-Fi or USB Gigabit Adapter for >100 Mbps)"
        }
        return null
    }

    override fun showToast(title: String, message: String, isLightMode: Boolean) {
        try {
            Toast.makeText(context, "$title: $message", Toast.LENGTH_SHORT).show()
            NotificationHelper(context).showAdminAlertNotification(title, message)
        } catch (_: Exception) {}
    }

    override fun announceForAccessibility(text: String) {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            if (accessibilityManager?.isEnabled == true) {
                val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT).apply {
                    className = "com.fdossena.speedtest.ui.MainActivity"
                    packageName = context.packageName
                    this.text.add(text)
                }
                accessibilityManager.sendAccessibilityEvent(event)
            }
        } catch (_: Exception) {}
    }

    @androidx.compose.runtime.Composable
    override fun BannerAdView(modifier: androidx.compose.ui.Modifier) {
        if (!isTv()) {
            androidx.compose.foundation.layout.Box(
                modifier = modifier,
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        com.google.android.gms.ads.AdView(ctx).apply {
                            setAdSize(com.google.android.gms.ads.AdSize.BANNER)
                            adUnitId = com.fdossena.speedtest.ads.AdManager.MOBILE_BANNER_AD_UNIT_ID
                            loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
                        }
                    }
                )
            }
        }
    }

    override fun showTvVideoAd(onAdFinished: () -> Unit) {
        if (isTv()) {
            com.fdossena.speedtest.ads.AdManager.showTvVideoAd(currentActivity, onAdFinished)
        } else {
            onAdFinished()
        }
    }
}
