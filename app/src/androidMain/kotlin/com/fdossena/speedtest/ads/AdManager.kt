package com.fdossena.speedtest.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {

    private const val TAG = "AdManager"

    // Production AdMob Ad Unit IDs from AdMob Console
    const val MOBILE_BANNER_AD_UNIT_ID = "ca-app-pub-7038623471624224/3381203117"
    const val TV_VIDEO_AD_UNIT_ID = "ca-app-pub-7038623471624224/5001406544"

    private var rewardedVideoAd: RewardedAd? = null
    private var isLoadingAd = false
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            MobileAds.initialize(context) {
                isInitialized = true
                Log.d(TAG, "AdMob initialized successfully.")
                preloadTvVideoAd(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "AdMob initialization error: ${e.message}")
        }
    }

    fun preloadTvVideoAd(context: Context) {
        if (rewardedVideoAd != null || isLoadingAd) return
        isLoadingAd = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, TV_VIDEO_AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedVideoAd = ad
                isLoadingAd = false
                Log.d(TAG, "TV Video Ad loaded successfully.")
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                rewardedVideoAd = null
                isLoadingAd = false
                Log.w(TAG, "TV Video Ad failed to load (e.g. blocked by AdGuard or network issue): ${adError.message}")
            }
        })
    }

    /**
     * Shows TV Mode Video Ad AFTER speed test completes, but BEFORE test results are displayed.
     * Guarantees calling [onAdFinished] when ad ends, is closed, or if blocked by AdGuard/network failure.
     */
    fun showTvVideoAd(activity: Activity?, onAdFinished: () -> Unit) {
        val ad = rewardedVideoAd
        if (ad != null && activity != null && !activity.isFinishing && !activity.isDestroyed) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedVideoAd = null
                    Log.d(TAG, "TV Video Ad dismissed. Returning to test results.")
                    activity.runOnUiThread { onAdFinished() }
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedVideoAd = null
                    Log.w(TAG, "TV Video Ad failed to show: ${error.message}. Proceeding to test results.")
                    activity.runOnUiThread { onAdFinished() }
                }
            }
            activity.runOnUiThread {
                ad.show(activity) {
                    Log.d(TAG, "User finished watching TV Video Ad.")
                }
            }
        } else {
            // Ad not ready or blocked by AdGuard -> Seamlessly proceed to test results
            Log.i(TAG, "TV Video Ad not available or blocked by AdGuard. Showing results directly.")
            activity?.let { preloadTvVideoAd(it.applicationContext) }
            onAdFinished()
        }
    }
}
