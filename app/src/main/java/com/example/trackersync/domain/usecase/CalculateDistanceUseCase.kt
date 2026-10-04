package com.example.trackersync.domain.usecase

import android.location.Location
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class CalculateDistanceUseCase {

    companion object {
        const val MAX_GEOFENCE_RADIUS_METERS = 50.0
        private const val EARTH_RADIUS_METERS = 6371000.0
    }

    /**
     * Calculates precise distance in meters between two lat/lng coordinates.
     * Uses Android's official geodetic WGS84 distance algorithm when available,
     * with Haversine formula as fallback for unit tests.
     */
    fun calculateDistanceMeters(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Double {
        return try {
            val results = FloatArray(1)
            Location.distanceBetween(startLat, startLng, endLat, endLng, results)
            results[0].toDouble()
        } catch (_: Exception) {
            // Fallback to Haversine
            haversineDistance(startLat, startLng, endLat, endLng)
        }
    }

    private fun haversineDistance(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Double {
        val dLat = Math.toRadians(endLat - startLat)
        val dLng = Math.toRadians(endLng - startLng)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(startLat)) * cos(Math.toRadians(endLat)) *
                sin(dLng / 2) * sin(dLng / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }

    /**
     * Boundary rule check: Returns true if distance is <= 50.0 meters.
     */
    fun isWithinGeofence(distanceMeters: Double): Boolean {
        return distanceMeters <= MAX_GEOFENCE_RADIUS_METERS
    }
}
