package com.mbougar.swapware.utils

import kotlin.math.*

object LocationUtils {
    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Calculates the distance between two geographic coordinates.
     * @param lat1 Latitude of the first point in degrees.
     * @param lon1 Longitude of the first point in degrees.
     * @param lat2 Latitude of the second point in degrees.
     * @param lon2 Longitude of the second point in degrees.
     * @return The distance in kilometers.
     */
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val radLat1 = Math.toRadians(lat1)
        val radLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2).pow(2) + sin(dLon / 2).pow(2) * cos(radLat1) * cos(radLat2)
        val c = 2 * asin(sqrt(a))
        return EARTH_RADIUS_KM * c
    }
}