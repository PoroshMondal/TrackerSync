package com.example.trackersync.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed class LocationState {
    data class Success(val location: Location) : LocationState()
    data class Error(val message: String) : LocationState()
    object PermissionDenied : LocationState()
    object LocationDisabled : LocationState()
}

class LocationTracker(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun hasLocationPermission(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted || coarseLocationGranted
    }

    fun isGpsEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @android.annotation.SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationState {
        if (!hasLocationPermission()) {
            return LocationState.PermissionDenied
        }

        if (!isGpsEnabled()) {
            return LocationState.LocationDisabled
        }

        return suspendCancellableCoroutine { continuation ->
            // Always request a fresh, high-accuracy location fix to avoid stale cached lastLocation
            requestFreshLocation { freshLocation ->
                if (freshLocation != null) {
                    continuation.resume(LocationState.Success(freshLocation))
                } else {
                    // Fallback to lastLocation if fresh request times out
                    try {
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                continuation.resume(LocationState.Success(lastLoc))
                            } else {
                                continuation.resume(LocationState.Error("Unable to retrieve location. Please check GPS connection."))
                            }
                        }.addOnFailureListener { e ->
                            continuation.resume(LocationState.Error(e.localizedMessage ?: "Failed to get location"))
                        }
                    } catch (e: Exception) {
                        continuation.resume(LocationState.Error("Location service error: ${e.message}"))
                    }
                }
            }
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun requestFreshLocation(onLocation: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            onLocation(null)
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 2000
        ).setMaxUpdates(1).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                val bestLoc = result.lastLocation
                onLocation(bestLoc)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        } catch (e: Exception) {
            onLocation(null)
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun getLocationUpdates(intervalMs: Long = 3000): Flow<Location> = callbackFlow {
        if (!hasLocationPermission() || !isGpsEnabled()) {
            close()
            return@callbackFlow
        }

        // Set minUpdateDistanceMeters to 1.5 meters to prevent idle GPS jitter/drift when sitting still
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, intervalMs
        )
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setMinUpdateDistanceMeters(1.5f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    // Ignore low accuracy fixes (> 30 meters accuracy radius)
                    if (!location.hasAccuracy() || location.accuracy <= 35.0f) {
                        trySend(location)
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
}
