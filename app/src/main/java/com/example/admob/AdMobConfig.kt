package com.example.admob

import android.util.Log
import com.example.BuildConfig

/**
 * Centralized AdMob Configuration for the Hindi Stories App.
 *
 * Switch [USE_TEST_ADS] to false and replace the PROD_* IDs with your
 * real AdMob Ad Unit IDs when publishing to production.
 */
object AdMobConfig {

    // =========================================================================
    // PRODUCTION SWITCH
    // =========================================================================
    /**
     * When TRUE: Uses Google's official AdMob test ad unit IDs.
     * When FALSE: Uses your production AdMob ad unit IDs defined below.
     *
     * IMPORTANT: Keep TRUE during development and testing. Set to FALSE for release builds.
     */
    const val USE_TEST_ADS = true

    // =========================================================================
    // GOOGLE OFFICIAL TEST AD UNIT IDS
    // =========================================================================
    const val TEST_APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"

    // =========================================================================
    // PRODUCTION AD UNIT IDS (Paste your real AdMob IDs here)
    // =========================================================================
    const val PROD_APP_OPEN_AD_UNIT_ID = "PASTE_REAL_APP_OPEN_ID_HERE"
    const val PROD_BANNER_AD_UNIT_ID = "PASTE_REAL_BANNER_ID_HERE"
    const val PROD_INTERSTITIAL_AD_UNIT_ID = "PASTE_REAL_INTERSTITIAL_ID_HERE"
    const val PROD_REWARDED_AD_UNIT_ID = "PASTE_REAL_REWARDED_ID_HERE"
    const val PROD_NATIVE_AD_UNIT_ID = "PASTE_REAL_NATIVE_ID_HERE"

    // =========================================================================
    // DYNAMIC RESOLVERS
    // =========================================================================
    val APP_OPEN_AD_UNIT_ID: String
        get() = if (USE_TEST_ADS) TEST_APP_OPEN_AD_UNIT_ID else PROD_APP_OPEN_AD_UNIT_ID

    val BANNER_AD_UNIT_ID: String
        get() = if (USE_TEST_ADS) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID

    val INTERSTITIAL_AD_UNIT_ID: String
        get() = if (USE_TEST_ADS) TEST_INTERSTITIAL_AD_UNIT_ID else PROD_INTERSTITIAL_AD_UNIT_ID

    val REWARDED_AD_UNIT_ID: String
        get() = if (USE_TEST_ADS) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID

    val NATIVE_AD_UNIT_ID: String
        get() = if (USE_TEST_ADS) TEST_NATIVE_AD_UNIT_ID else PROD_NATIVE_AD_UNIT_ID

    // =========================================================================
    // AD POLICIES & FREQUENCIES
    // =========================================================================
    /**
     * Number of story opens before showing an interstitial ad.
     * e.g., 3 means stories 1, 2 have no ad; story 3 shows interstitial.
     */
    const val INTERSTITIAL_FREQUENCY = 3

    /**
     * Show a native ad after every N story items in the list.
     */
    const val NATIVE_AD_INTERVAL = 6

    /**
     * Minimum interval (in milliseconds) between App Open ads to avoid spamming the user.
     * Default: 4 hours (14,400,000 ms).
     */
    const val APP_OPEN_MIN_INTERVAL_MS = 4 * 60 * 60 * 1000L

    /**
     * Centralized debug logging for AdMob events.
     */
    fun log(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d("AdMob_$tag", message)
        }
    }
}
