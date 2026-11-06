package com.fdossena.speedtest.ui
import android.content.Intent
import android.os.Bundle
class ShortcutDetectorAmber : MainActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        intent?.putExtra("EXTRA_LAUNCH_DETECTOR", true)
        super.onCreate(savedInstanceState)
    }
}