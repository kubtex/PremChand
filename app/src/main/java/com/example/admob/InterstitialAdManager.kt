package com.example.admob

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Manages Interstitial Ad loading, display, and frequency capping.
 *
 * AdMob Best Practices:
 * - Never blocks the user from opening or reading a story.
 * - Shows an interstitial only after every [AdMobConfig.INTERSTITIAL_FREQUENCY] story transitions.
 * - Always preloads the next interstitial in the background.
 */
object InterstitialAdManager {

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var storyTransitionCount = 0

    /**
     * Preloads an Interstitial Ad in the background.
     */
    fun loadAd(context: Context) {
        if (isLoading || interstitialAd != null) {
            return
        }

        isLoading = true
        AdMobConfig.log("Interstitial", "Requesting Interstitial Ad: ${AdMobConfig.INTERSTITIAL_AD_UNIT_ID}")

        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext,
            AdMobConfig.INTERSTITIAL_AD_UNIT_ID,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    AdMobConfig.log("Interstitial", "Interstitial Ad loaded successfully")
                    interstitialAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdMobConfig.log("Interstitial", "Interstitial Ad failed to load: ${error.message}")
                    interstitialAd = null
                    isLoading = false
                }
            }
        )
    }

    /**
     * Called when the user clicks to open/read a story.
     * Checks if the frequency threshold is met.
     * If met and ad is ready, shows the interstitial; otherwise immediately proceeds.
     *
     * @param activity Foreground activity to host the ad.
     * @param onProceed Callback to navigate to the story screen. Always called.
     */
    fun onStoryOpen(activity: Activity, onProceed: () -> Unit) {
        storyTransitionCount++
        AdMobConfig.log(
            "Interstitial",
            "Story opened (Count=$storyTransitionCount, Frequency=${AdMobConfig.INTERSTITIAL_FREQUENCY})"
        )

        val shouldShowAd = (storyTransitionCount % AdMobConfig.INTERSTITIAL_FREQUENCY == 0)
        val currentAd = interstitialAd

        if (shouldShowAd && currentAd != null) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    AdMobConfig.log("Interstitial", "Interstitial ad dismissed by user")
                    interstitialAd = null
                    loadAd(activity)
                    onProceed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    AdMobConfig.log("Interstitial", "Interstitial ad failed to show: ${adError.message}")
                    interstitialAd = null
                    loadAd(activity)
                    onProceed()
                }

                override fun onAdShowedFullScreenContent() {
                    AdMobConfig.log("Interstitial", "Interstitial ad showing full screen")
                }
            }

            currentAd.show(activity)
        } else {
            // Not time for ad, or ad is not yet ready: proceed immediately
            if (currentAd == null && !isLoading) {
                loadAd(activity)
            }
            onProceed()
        }
    }
}
