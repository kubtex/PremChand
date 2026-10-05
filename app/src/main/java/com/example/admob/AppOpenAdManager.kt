package com.example.admob

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages App Open Ad loading, display, and frequency capping.
 * AdMob best practices:
 * - Preloads ads so they are ready for launch.
 * - Expires ads after 4 hours.
 * - Suppresses ads while user is actively reading a story or navigating internally.
 * - Provides sensible cooldown between App Open ads.
 */
class AppOpenAdManager private constructor(private val application: Application) :
    Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Long = 0L
    private var lastAdShownTimestamp: Long = 0L

    private var currentActivity: Activity? = null

    /**
     * Set to true when user is on the Story Reading screen.
     * Prevents any App Open ad from disrupting the reading experience.
     */
    var isReadingStory: Boolean = false

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    /**
     * Checks if an ad is currently loaded and not expired (valid for 4 hours).
     */
    private fun isAdAvailable(): Boolean {
        val fourHoursMs = 3600000L * 4
        return appOpenAd != null && (Date().time - loadTime < fourHoursMs)
    }

    /**
     * Request an App Open Ad if not already loading or available.
     */
    fun loadAd(context: Context) {
        if (isLoadingAd || isAdAvailable()) {
            return
        }

        isLoadingAd = true
        AdMobConfig.log("AppOpen", "Requesting App Open Ad: ${AdMobConfig.APP_OPEN_AD_UNIT_ID}")

        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            AdMobConfig.APP_OPEN_AD_UNIT_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    AdMobConfig.log("AppOpen", "App Open Ad loaded successfully")
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    AdMobConfig.log("AppOpen", "App Open Ad failed to load: ${loadAdError.message}")
                    isLoadingAd = false
                    appOpenAd = null
                }
            }
        )
    }

    /**
     * Shows the App Open ad if available and eligible.
     * Respects reading mode, frequency limits, and ad availability.
     */
    fun showAdIfAvailable(
        activity: Activity,
        onAdDismissedOrSkipped: () -> Unit = {}
    ) {
        // Rule 1: Never interrupt the user while reading a story
        if (isReadingStory) {
            AdMobConfig.log("AppOpen", "User is actively reading a story; suppressing App Open ad.")
            onAdDismissedOrSkipped()
            return
        }

        // Rule 2: Do not show if another ad is already being shown
        if (isShowingAd) {
            AdMobConfig.log("AppOpen", "An ad is already being shown; skipping.")
            onAdDismissedOrSkipped()
            return
        }

        // Rule 3: Frequency capping between App Open ads
        val now = System.currentTimeMillis()
        if (now - lastAdShownTimestamp < AdMobConfig.APP_OPEN_MIN_INTERVAL_MS) {
            AdMobConfig.log("AppOpen", "Cooldown active; skipping App Open ad.")
            onAdDismissedOrSkipped()
            return
        }

        // Rule 4: Check if ad is available
        if (!isAdAvailable()) {
            AdMobConfig.log("AppOpen", "App Open ad is not ready; preloading next ad.")
            loadAd(activity)
            onAdDismissedOrSkipped()
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                AdMobConfig.log("AppOpen", "App Open ad dismissed by user")
                appOpenAd = null
                isShowingAd = false
                lastAdShownTimestamp = System.currentTimeMillis()
                loadAd(activity)
                onAdDismissedOrSkipped()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                AdMobConfig.log("AppOpen", "App Open ad failed to show: ${adError.message}")
                appOpenAd = null
                isShowingAd = false
                loadAd(activity)
                onAdDismissedOrSkipped()
            }

            override fun onAdShowedFullScreenContent() {
                AdMobConfig.log("AppOpen", "App Open ad is showing full screen")
                isShowingAd = true
            }
        }

        appOpenAd?.show(activity)
    }

    // =========================================================================
    // Activity Lifecycle Callbacks
    // =========================================================================
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }
    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }

    companion object {
        @Volatile
        private var instance: AppOpenAdManager? = null

        fun getInstance(application: Application): AppOpenAdManager {
            return instance ?: synchronized(this) {
                instance ?: AppOpenAdManager(application).also { instance = it }
            }
        }
    }
}
