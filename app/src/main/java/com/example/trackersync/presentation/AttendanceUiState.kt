package com.example.trackersync.presentation

import com.example.trackersync.domain.model.AttendanceRecord
import com.example.trackersync.domain.model.OfficeLocation

data class CurrentCoordinates(
    val latitude: Double,
    val longitude: Double
)

data class AttendanceUiState(
    val officeLocation: OfficeLocation? = null,
    val currentLocation: CurrentCoordinates? = null,
    val distanceMeters: Double? = null,
    val isWithinGeofence: Boolean = false,
    val isLoadingLocation: Boolean = false,
    val isSettingOffice: Boolean = false,
    val isMarkingAttendance: Boolean = false,
    val hasPermission: Boolean = false,
    val isGpsEnabled: Boolean = true,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val attendanceHistory: List<AttendanceRecord> = emptyList()
)
