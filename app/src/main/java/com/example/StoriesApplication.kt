package com.example

import android.app.Application
import com.example.admob.AdMobInitializer
import com.example.admob.AppOpenAdManager

class StoriesApplication : Application() {

    lateinit var appOpenAdManager: AppOpenAdManager
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize Google Mobile Ads SDK once at app startup
        AdMobInitializer.initialize(this)

        // Initialize App Open Ad Manager and preload initial ad
        appOpenAdManager = AppOpenAdManager.getInstance(this)
        appOpenAdManager.loadAd(this)
    }
}
