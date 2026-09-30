package com.jokam.tempatsambung.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Place(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val city: String = "",
    val province: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val phone: String = ""
)

@Serializable
data class Pengurus(
    val id: String = "",
    val city: String = "",
    val province: String = "",
    val phone: String = ""
)

@Serializable
data class WallpaperItem(
    val id: String = "",
    @SerialName("thumb_url") val thumbUrl: String = "",
    @SerialName("full_url") val fullUrl: String = ""
)

@Serializable
data class AdsConfig(
    val bannerAdUnitId: String? = null,
    val rewardedAdUnitId: String? = null,
    val nativeAdUnitId: String? = null,
    val isAdsEnabled: Boolean = false,
    val isBannerEnabled: Boolean = true,
    val isNativeEnabled: Boolean = true,
    val isRewardedRouteEnabled: Boolean = true,
    val isRewardedContactEnabled: Boolean = true,
    val isRewardedWallpaperEnabled: Boolean = true
)
