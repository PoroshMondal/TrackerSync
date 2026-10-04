package com.example.trackersync.domain.model

data class AttendanceRecord(
    val id: String,
    val timestamp: Long,
    val userLatitude: Double,
    val userLongitude: Double,
    val officeLatitude: Double,
    val officeLongitude: Double,
    val distanceMeters: Double,
    val isWithinGeofence: Boolean,
    val note: String = "Geo-fenced check-in"
)
