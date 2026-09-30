package com.jokam.tempatsambung.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.jokam.tempatsambung.data.remote.RemoteConfigManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdsManager(
    private val context: Context,
    private val consentManager: ConsentManager
) {
    private val tag = "AdsManager"

    private var currentRewardedAd: RewardedAd? = null
    private var isRewardedAdLoading = false

    private val _nativeAds = MutableStateFlow<List<NativeAd>>(emptyList())
    val nativeAds: StateFlow<List<NativeAd>> = _nativeAds.asStateFlow()

    fun preloadRewardedAd() {
        val config = RemoteConfigManager.adsConfig.value
        if (!config.isAdsEnabled || !consentManager.canRequestAds()) {
            return
        }
        val adUnitId = config.rewardedAdUnitId
        if (adUnitId.isNullOrEmpty() || currentRewardedAd != null || isRewardedAdLoading) {
            return
        }

        isRewardedAdLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(rewardedAd: RewardedAd) {
                Log.d(tag, "Rewarded ad loaded successfully.")
                currentRewardedAd = rewardedAd
                isRewardedAdLoading = false
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.w(tag, "Rewarded ad failed to load: ${loadAdError.message}")
                currentRewardedAd = null
                isRewardedAdLoading = false
            }
        })
    }

    /**
     * Shows the rewarded ad if available and runs [onRewardEarned] upon reward completion.
     * If the ad is not loaded or fails, directly calls [onRewardEarned] so user is never stuck.
     */
    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit) {
        val ad = currentRewardedAd
        if (ad == null) {
            Log.d(tag, "No rewarded ad available, proceeding with action directly.")
            onRewardEarned()
            preloadRewardedAd()
            return
        }

        var rewardEarned = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(tag, "Rewarded ad dismissed.")
                currentRewardedAd = null
                preloadRewardedAd()
                if (rewardEarned) {
                    onRewardEarned()
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(tag, "Rewarded ad failed to show: ${adError.message}")
                currentRewardedAd = null
                preloadRewardedAd()
                // Never leave user stuck
                onRewardEarned()
            }
        }

        ad.show(activity) { _ ->
            Log.d(tag, "User earned reward!")
            rewardEarned = true
        }
    }

    fun loadNativeAds(maxCount: Int = 3) {
        val config = RemoteConfigManager.adsConfig.value
        if (!config.isAdsEnabled || !config.isNativeEnabled || !consentManager.canRequestAds()) {
            return
        }
        val adUnitId = config.nativeAdUnitId
        if (adUnitId.isNullOrEmpty()) {
            return
        }

        destroyNativeAds()

        val loadedAds = mutableListOf<NativeAd>()
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                loadedAds.add(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdLoaded() {
                    _nativeAds.value = ArrayList(loadedAds)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(tag, "Native ad failed to load: ${error.message}")
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setRequestMultipleImages(false)
                    .build()
            )
            .build()

        adLoader.loadAds(AdRequest.Builder().build(), maxCount)
    }

    fun destroyNativeAds() {
        _nativeAds.value.forEach { it.destroy() }
        _nativeAds.value = emptyList()
    }
}
