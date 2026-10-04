package com.example.trackersync.domain.model

data class OfficeLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String = "Saved Office Location",
    val timestamp: Long = System.currentTimeMillis()
)
