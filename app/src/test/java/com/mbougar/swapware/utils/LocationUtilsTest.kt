package com.mbougar.swapware.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.roundToInt

class LocationUtilsTest {

    @Test
    fun `calculateDistanceKm with same coordinates returns zero`() {
        val distance = LocationUtils.calculateDistanceKm(40.4168, -3.7038, 40.4168, -3.7038)
        assertThat(distance).isEqualTo(0.0)
    }

    @Test
    fun `calculateDistanceKm calculates known distance correctly`() {
        val lat1 = 40.4168
        val lon1 = -3.7038
        val lat2 = 41.3851
        val lon2 = 2.1734

        val distance = LocationUtils.calculateDistanceKm(lat1, lon1, lat2, lon2)

        assertThat(distance.roundToInt()).isGreaterThan(500)
        assertThat(distance.roundToInt()).isLessThan(510)
    }

    @Test
    fun `calculateDistanceKm works across the equator`() {
        val distance = LocationUtils.calculateDistanceKm(1.0, 0.0, -1.0, 0.0)
        assertThat(distance.roundToInt()).isEqualTo(222)
    }
}
