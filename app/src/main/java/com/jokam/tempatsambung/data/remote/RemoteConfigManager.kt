package com.jokam.tempatsambung.data.remote

import android.util.Log
import com.jokam.tempatsambung.BuildConfig
import com.jokam.tempatsambung.data.model.AdsConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object RemoteConfigManager {
    private const val TAG = "RemoteConfigManager"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    // Default configuration (Debug: fallback to test IDs; Release: no ads)
    private val defaultConfig = if (BuildConfig.DEBUG) {
        AdsConfig(
            bannerAdUnitId = BuildConfig.TEST_BANNER_AD_UNIT_ID.ifEmpty { "ca-app-pub-3940256099942544/6300978111" },
            rewardedAdUnitId = BuildConfig.TEST_REWARDED_AD_UNIT_ID.ifEmpty { "ca-app-pub-3940256099942544/5224354917" },
            nativeAdUnitId = BuildConfig.TEST_NATIVE_AD_UNIT_ID.ifEmpty { "ca-app-pub-3940256099942544/2247696110" },
            isAdsEnabled = false,
            isBannerEnabled = true,
            isNativeEnabled = true,
            isRewardedRouteEnabled = true,
            isRewardedContactEnabled = true,
            isRewardedWallpaperEnabled = true
        )
    } else {
        AdsConfig(
            isAdsEnabled = false
        )
    }

    private val _adsConfig = MutableStateFlow(defaultConfig)
    val adsConfig: StateFlow<AdsConfig> = _adsConfig.asStateFlow()

    private var hasFetched = false

    suspend fun fetchAdsConfig() {
        if (hasFetched) return
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(RemoteConstants.ADS_CONFIG_URL)
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val parsed = json.decodeFromString<AdsConfig>(body)
                            _adsConfig.value = parsed
                            hasFetched = true
                            Log.d(TAG, "AdsConfig fetched successfully: isAdsEnabled=${parsed.isAdsEnabled}")
                        }
                    } else {
                        Log.w(TAG, "Failed to fetch remote config: HTTP ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching remote config, using fallback", e)
            }
        }
    }
}
