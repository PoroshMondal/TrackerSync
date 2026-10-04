package com.example.trackersync.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackersync.data.local.PreferencesManager
import com.example.trackersync.data.location.LocationState
import com.example.trackersync.data.location.LocationTracker
import com.example.trackersync.domain.model.AttendanceRecord
import com.example.trackersync.domain.model.OfficeLocation
import com.example.trackersync.domain.usecase.CalculateDistanceUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsManager = PreferencesManager(application)
    private val locationTracker = LocationTracker(application)
    private val distanceUseCase = CalculateDistanceUseCase()

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    private var locationUpdatesJob: Job? = null

    init {
        loadSavedData()
        checkPermissionsAndStartUpdates()
    }

    fun loadSavedData() {
        val savedOffice = prefsManager.getOfficeLocation()
        val savedHistory = prefsManager.getAttendanceHistory()
        _uiState.update { state ->
            state.copy(
                officeLocation = savedOffice,
                attendanceHistory = savedHistory
            )
        }
        recalculateDistance()
    }

    fun checkPermissionsAndStartUpdates() {
        val hasPerm = locationTracker.hasLocationPermission()
        val isGpsOn = locationTracker.isGpsEnabled()
        _uiState.update { it.copy(hasPermission = hasPerm, isGpsEnabled = isGpsOn) }

        if (hasPerm && isGpsOn) {
            refreshLocation()
            startRealTimeLocationUpdates()
        }
    }

    private fun startRealTimeLocationUpdates() {
        locationUpdatesJob?.cancel()
        locationUpdatesJob = viewModelScope.launch {
            locationTracker.getLocationUpdates().collect { location ->
                val coords = CurrentCoordinates(location.latitude, location.longitude)
                _uiState.update { it.copy(currentLocation = coords, isLoadingLocation = false) }
                recalculateDistance()
            }
        }
    }

    fun setOfficeLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSettingOffice = true, errorMessage = null, successMessage = null) }

            when (val result = locationTracker.getCurrentLocation()) {
                is LocationState.Success -> {
                    val lat = result.location.latitude
                    val lng = result.location.longitude
                    val formattedLat = String.format(java.util.Locale.US, "%.4f", lat)
                    val formattedLng = String.format(java.util.Locale.US, "%.4f", lng)
                    val newOffice = OfficeLocation(
                        latitude = lat,
                        longitude = lng,
                        address = "Office ($formattedLat, $formattedLng)",
                        timestamp = System.currentTimeMillis()
                    )
                    prefsManager.saveOfficeLocation(newOffice)
                    _uiState.update { state ->
                        state.copy(
                            officeLocation = newOffice,
                            currentLocation = CurrentCoordinates(lat, lng),
                            isSettingOffice = false,
                            successMessage = "Office location saved successfully! ($formattedLat, $formattedLng)"
                        )
                    }
                    recalculateDistance()
                }
                is LocationState.PermissionDenied -> {
                    _uiState.update {
                        it.copy(
                            isSettingOffice = false,
                            hasPermission = false,
                            errorMessage = "Location permission is required to set office location."
                        )
                    }
                }
                is LocationState.LocationDisabled -> {
                    _uiState.update {
                        it.copy(
                            isSettingOffice = false,
                            isGpsEnabled = false,
                            errorMessage = "Location services/GPS are disabled. Please enable GPS."
                        )
                    }
                }
                is LocationState.Error -> {
                    _uiState.update {
                        it.copy(
                            isSettingOffice = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun markAttendance() {
        viewModelScope.launch {
            val currentState = _uiState.value

            val office = currentState.officeLocation
            if (office == null) {
                _uiState.update {
                    it.copy(errorMessage = "Cannot mark attendance: No office location has been saved yet.")
                }
                return@launch
            }

            _uiState.update { it.copy(isMarkingAttendance = true, errorMessage = null, successMessage = null) }

            when (val result = locationTracker.getCurrentLocation()) {
                is LocationState.Success -> {
                    val userLat = result.location.latitude
                    val userLng = result.location.longitude

                    val distance = distanceUseCase.calculateDistanceMeters(
                        startLat = userLat,
                        startLng = userLng,
                        endLat = office.latitude,
                        endLng = office.longitude
                    )

                    val isWithin = distanceUseCase.isWithinGeofence(distance)

                    if (isWithin) {
                        val record = AttendanceRecord(
                            id = UUID.randomUUID().toString(),
                            timestamp = System.currentTimeMillis(),
                            userLatitude = userLat,
                            userLongitude = userLng,
                            officeLatitude = office.latitude,
                            officeLongitude = office.longitude,
                            distanceMeters = distance,
                            isWithinGeofence = true,
                            note = "Verified GPS Check-in"
                        )

                        prefsManager.saveAttendanceRecord(record)
                        val updatedHistory = prefsManager.getAttendanceHistory()

                        val distStr = String.format(java.util.Locale.US, "%.1f", distance)
                        _uiState.update { state ->
                            state.copy(
                                isMarkingAttendance = false,
                                attendanceHistory = updatedHistory,
                                successMessage = "Attendance marked successfully! (${distStr}m away from office)"
                            )
                        }
                    } else {
                        val distStr = String.format(java.util.Locale.US, "%.1f", distance)
                        _uiState.update {
                            it.copy(
                                isMarkingAttendance = false,
                                errorMessage = "Attendance blocked: You are ${distStr}m away from office (must be within 50m)."
                            )
                        }
                    }
                }
                is LocationState.PermissionDenied -> {
                    _uiState.update {
                        it.copy(
                            isMarkingAttendance = false,
                            hasPermission = false,
                            errorMessage = "Location permission denied. Cannot mark attendance."
                        )
                    }
                }
                is LocationState.LocationDisabled -> {
                    _uiState.update {
                        it.copy(
                            isMarkingAttendance = false,
                            isGpsEnabled = false,
                            errorMessage = "Location services/GPS disabled. Please enable GPS."
                        )
                    }
                }
                is LocationState.Error -> {
                    _uiState.update {
                        it.copy(
                            isMarkingAttendance = false,
                            errorMessage = "Location retrieval failure: ${result.message}"
                        )
                    }
                }
            }
        }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLocation = true, errorMessage = null) }
            when (val result = locationTracker.getCurrentLocation()) {
                is LocationState.Success -> {
                    val coords = CurrentCoordinates(result.location.latitude, result.location.longitude)
                    _uiState.update { it.copy(currentLocation = coords, isLoadingLocation = false) }
                    recalculateDistance()
                }
                else -> {
                    _uiState.update { it.copy(isLoadingLocation = false) }
                }
            }
        }
    }

    fun clearOfficeLocation() {
        prefsManager.clearOfficeLocation()
        _uiState.update { state ->
            state.copy(
                officeLocation = null,
                distanceMeters = null,
                isWithinGeofence = false,
                successMessage = "Office location cleared."
            )
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private fun recalculateDistance() {
        val state = _uiState.value
        val office = state.officeLocation ?: run {
            _uiState.update { it.copy(distanceMeters = null, isWithinGeofence = false) }
            return
        }

        val current = state.currentLocation ?: run {
            _uiState.update { it.copy(distanceMeters = null, isWithinGeofence = false) }
            return
        }

        val distance = distanceUseCase.calculateDistanceMeters(
            startLat = current.latitude,
            startLng = current.longitude,
            endLat = office.latitude,
            endLng = office.longitude
        )

        val isWithin = distanceUseCase.isWithinGeofence(distance)

        _uiState.update {
            it.copy(
                distanceMeters = distance,
                isWithinGeofence = isWithin
            )
        }
    }
}
