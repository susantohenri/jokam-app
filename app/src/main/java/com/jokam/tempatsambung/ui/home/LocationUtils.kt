package com.jokam.tempatsambung.ui.home

import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object LocationUtils {
    private const val EARTH_RADIUS_METERS = 6371000.0

    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    fun formatDistance(meters: Double): String {
        return if (meters < 1000.0) {
            "${meters.toInt()} m"
        } else {
            val km = meters / 1000.0
            String.format(Locale.getDefault(), "%.1f km", km)
        }
    }
}
