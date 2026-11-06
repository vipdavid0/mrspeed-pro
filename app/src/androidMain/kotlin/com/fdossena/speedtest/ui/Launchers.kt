package com.fdossena.speedtest.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Multiple real activity classes to ensure the Android launcher (especially Pixel)
 * treats them as distinct entities with separate icons and labels.
 */

// These were moved to individual files to resolve redeclaration issues while maintaining the original behavior.
/*
class ShortcutSpeedCyan : ComponentActivity() { ... }
class ShortcutSpeedAmber : ComponentActivity() { ... }
class ShortcutDetectorCyan : ComponentActivity() { ... }
class ShortcutDetectorAmber : ComponentActivity() { ... }
*/

class DetectorActivity {
    companion object {
        const val EXTRA_LAUNCH_DETECTOR = "EXTRA_LAUNCH_DETECTOR"
    }
}

class SuiteActivity {
    companion object {
        const val EXTRA_LAUNCH_SUITE = "EXTRA_LAUNCH_SUITE"
    }
}
