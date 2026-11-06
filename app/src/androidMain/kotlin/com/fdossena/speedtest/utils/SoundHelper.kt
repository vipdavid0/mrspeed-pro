package com.fdossena.speedtest.utils

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.mr.speed.test.R

class SoundHelper(private val context: Context) {
    fun playCompletionSound() {
        try {
            val mediaPlayer = MediaPlayer.create(context, R.raw.notification)
            mediaPlayer.setOnCompletionListener { mp ->
                mp.reset()
                mp.release()
            }
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun triggerHapticFeedback() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
