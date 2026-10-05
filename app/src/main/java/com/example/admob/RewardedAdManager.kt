package com.example.admob

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages Rewarded Ad loading and playback unlocking.
 *
 * AdMob Best Practices:
 * - Grants the reward ONLY via [onUserEarnedRewardListener].
 * - Precludes multiple simultaneous audio player initializations.
 * - Gracefully falls back if no ad is loaded or if offline.
 */
object RewardedAdManager {

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false
    private val isShowingAd = AtomicBoolean(false)

    /**
     * Preloads a Rewarded Ad in the background.
     */
    fun loadAd(context: Context) {
        if (isLoading || rewardedAd != null) {
            return
        }

        isLoading = true
        AdMobConfig.log("Rewarded", "Requesting Rewarded Ad: ${AdMobConfig.REWARDED_AD_UNIT_ID}")

        val request = AdRequest.Builder().build()
        RewardedAd.load(
            context.applicationContext,
            AdMobConfig.REWARDED_AD_UNIT_ID,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    AdMobConfig.log("Rewarded", "Rewarded Ad loaded successfully")
                    rewardedAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdMobConfig.log("Rewarded", "Rewarded Ad failed to load: ${error.message}")
                    rewardedAd = null
                    isLoading = false
                }
            }
        )
    }

    /**
     * Checks if a Rewarded Ad is currently available.
     */
    fun isAdAvailable(): Boolean = (rewardedAd != null)

    /**
     * Displays the Rewarded Ad to the user.
     *
     * @param activity Hosting Activity.
     * @param onRewardEarned Invoked ONLY when the user completed watching the ad and earned the reward.
     * @param onDismissedWithoutReward Invoked if the user closed/skipped the ad early without earning reward.
     * @param onAdUnavailable Invoked if no ad is ready to show.
     */
    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onDismissedWithoutReward: () -> Unit,
        onAdUnavailable: () -> Unit
    ) {
        val currentAd = rewardedAd

        if (currentAd == null) {
            AdMobConfig.log("Rewarded", "Rewarded ad is not available, triggering fallback.")
            loadAd(activity)
            onAdUnavailable()
            return
        }

        if (!isShowingAd.compareAndSet(false, true)) {
            AdMobConfig.log("Rewarded", "Ad is already showing; ignoring duplicate request.")
            return
        }

        var rewardGranted = false

        currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                AdMobConfig.log("Rewarded", "Rewarded ad dismissed. Reward earned: $rewardGranted")
                rewardedAd = null
                isShowingAd.set(false)
                loadAd(activity) // Preload next rewarded ad

                if (rewardGranted) {
                    onRewardEarned()
                } else {
                    onDismissedWithoutReward()
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                AdMobConfig.log("Rewarded", "Rewarded ad failed to show: ${adError.message}")
                rewardedAd = null
                isShowingAd.set(false)
                loadAd(activity)
                onAdUnavailable()
            }

            override fun onAdShowedFullScreenContent() {
                AdMobConfig.log("Rewarded", "Rewarded ad is displaying on screen")
            }
        }

        currentAd.show(activity) { rewardItem ->
            AdMobConfig.log("Rewarded", "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            rewardGranted = true
        }
    }
}
