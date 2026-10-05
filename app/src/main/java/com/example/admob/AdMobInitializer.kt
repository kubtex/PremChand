package com.example.admob

import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handles thread-safe, one-time initialization of the Google Mobile Ads SDK.
 */
object AdMobInitializer {

    private val isInitialized = AtomicBoolean(false)

    /**
     * Initializes Google Mobile Ads SDK once at application startup.
     */
    fun initialize(context: Context) {
        if (isInitialized.compareAndSet(false, true)) {
            try {
                AdMobConfig.log("Init", "Initializing Google Mobile Ads SDK (USE_TEST_ADS=${AdMobConfig.USE_TEST_ADS})...")

                // Configure test devices if needed
                val requestConfiguration = RequestConfiguration.Builder()
                    .build()
                MobileAds.setRequestConfiguration(requestConfiguration)

                // Initialize Mobile Ads SDK on a background thread
                Thread {
                    try {
                        MobileAds.initialize(context.applicationContext) { status ->
                            AdMobConfig.log("Init", "MobileAds initialized successfully: ${status.adapterStatusMap.keys}")
                        }
                    } catch (e: Exception) {
                        AdMobConfig.log("Init", "Failed to initialize MobileAds in background: ${e.message}")
                    }
                }.start()
            } catch (e: Exception) {
                AdMobConfig.log("Init", "Exception during AdMob initialization: ${e.message}")
            }
        }
    }
}
