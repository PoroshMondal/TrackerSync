package com.example.trackersync

import com.example.trackersync.domain.usecase.CalculateDistanceUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalculateDistanceUseCaseTest {

    private lateinit var useCase: CalculateDistanceUseCase

    @Before
    fun setUp() {
        useCase = CalculateDistanceUseCase()
    }

    @Test
    fun `calculateDistanceMeters returns zero for identical coordinates`() {
        val lat = 37.7749
        val lng = -122.4194

        val distance = useCase.calculateDistanceMeters(lat, lng, lat, lng)

        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun `isWithinGeofence returns true for 25 meters`() {
        val isWithin = useCase.isWithinGeofence(25.0)
        assertTrue("25m should be within geofence", isWithin)
    }

    @Test
    fun `isWithinGeofence returns true for 49 meters`() {
        val isWithin = useCase.isWithinGeofence(49.0)
        assertTrue("49m should be within geofence", isWithin)
    }

    @Test
    fun `isWithinGeofence returns true for exactly 50 meters boundary condition`() {
        val isWithin = useCase.isWithinGeofence(50.0)
        assertTrue("Exactly 50m boundary condition should be within geofence", isWithin)
    }

    @Test
    fun `isWithinGeofence returns false for 51 meters`() {
        val isWithin = useCase.isWithinGeofence(51.0)
        assertFalse("51m should be outside geofence", isWithin)
    }

    @Test
    fun `isWithinGeofence returns false for 120 meters`() {
        val isWithin = useCase.isWithinGeofence(120.0)
        assertFalse("120m should be outside geofence", isWithin)
    }

    @Test
    fun `calculateDistanceMeters correctly calculates distance for real coordinates`() {
        // Known distance between Googleplex (37.4220, -122.0841) and nearby spot (37.4224, -122.0841)
        val lat1 = 37.4220
        val lng1 = -122.0841
        val lat2 = 37.4224
        val lng2 = -122.0841

        val distance = useCase.calculateDistanceMeters(lat1, lng1, lat2, lng2)

        // Expect approximately ~44.4 meters
        assertTrue("Distance should be around 44.4 meters", distance in 40.0..50.0)
        assertTrue("44.4m should be within 50m geofence", useCase.isWithinGeofence(distance))
    }
}
