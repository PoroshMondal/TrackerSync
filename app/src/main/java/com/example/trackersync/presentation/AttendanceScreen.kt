package com.example.trackersync.presentation

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trackersync.domain.model.AttendanceRecord
import java.text.SimpleDateFormat
import java.util.*

// Custom Modern Premium Color Palette
private val PrimaryEmerald = Color(0xFF10B981)
private val EmeraldDark = Color(0xFF047857)
private val EmeraldLight = Color(0xFFD1FAE5)
private val PrimaryCyan = Color(0xFF0EA5E9)
private val ErrorRose = Color(0xFFF43F5E)
private val ErrorRoseBg = Color(0xFFFFE4E6)
private val DarkSlateBg = Color(0xFF0F172A)
private val CardSlateBg = Color(0xFF1E293B)
private val BorderSlate = Color(0xFF334155)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.checkPermissionsAndStartUpdates()
    }

    LaunchedEffect(Unit) {
        viewModel.loadSavedData()
        viewModel.checkPermissionsAndStartUpdates()
    }

    Scaffold(
        containerColor = DarkSlateBg,
        topBar = {
            ModernHeader(
                isLoadingLocation = state.isLoadingLocation,
                hasPermission = state.hasPermission,
                isGpsEnabled = state.isGpsEnabled,
                onRefresh = { viewModel.refreshLocation() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Notifications
                item {
                    AnimatedVisibility(
                        visible = state.errorMessage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        state.errorMessage?.let { msg ->
                            ModernErrorCard(message = msg, onDismiss = { viewModel.dismissMessage() })
                        }
                    }

                    AnimatedVisibility(
                        visible = state.successMessage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        state.successMessage?.let { msg ->
                            ModernSuccessCard(message = msg, onDismiss = { viewModel.dismissMessage() })
                        }
                    }
                }

                // Permission or GPS Warning Banner
                if (!state.hasPermission || !state.isGpsEnabled) {
                    item {
                        ModernWarningCard(
                            hasPermission = state.hasPermission,
                            onRequestPermission = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            onOpenSettings = {
                                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                // Section 1: Office Location Setup
                item {
                    ModernOfficeCard(
                        officeLocation = state.officeLocation,
                        isSettingOffice = state.isSettingOffice,
                        onSetOfficeLocation = {
                            if (!state.hasPermission) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else {
                                viewModel.setOfficeLocation()
                            }
                        },
                        onClearOfficeLocation = { viewModel.clearOfficeLocation() }
                    )
                }

                // Section 2: Distance & Geofence Hero Card
                item {
                    ModernDistanceHeroCard(
                        officeLocation = state.officeLocation,
                        distanceMeters = state.distanceMeters,
                        isWithinGeofence = state.isWithinGeofence,
                        currentLocation = state.currentLocation
                    )
                }

                // Section 3: Mark Attendance Action Button
                item {
                    ModernMarkAttendanceSection(
                        isWithinGeofence = state.isWithinGeofence,
                        officeLocation = state.officeLocation,
                        isMarkingAttendance = state.isMarkingAttendance,
                        distanceMeters = state.distanceMeters,
                        onMarkAttendance = { viewModel.markAttendance() }
                    )
                }

                // Section 4: History Header & List
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PrimaryCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Attendance Check-in History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                if (state.attendanceHistory.isEmpty()) {
                    item {
                        ModernEmptyHistoryCard()
                    }
                } else {
                    items(state.attendanceHistory) { record ->
                        ModernAttendanceRecordItem(record = record)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernHeader(
    isLoadingLocation: Boolean,
    hasPermission: Boolean,
    isGpsEnabled: Boolean,
    onRefresh: () -> Unit
) {
    Surface(
        color = CardSlateBg,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryCyan, PrimaryEmerald)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "TrackerSync",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pulsing dot
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "alpha"
                        )

                        val statusColor = if (hasPermission && isGpsEnabled) PrimaryEmerald else ErrorRose

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = alpha))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasPermission && isGpsEnabled) "GPS Active" else "GPS Offline",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.LightGray
                        )
                    }
                }
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DarkSlateBg)
            ) {
                if (isLoadingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryCyan
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh GPS",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernOfficeCard(
    officeLocation: com.example.trackersync.domain.model.OfficeLocation?,
    isSettingOffice: Boolean,
    onSetOfficeLocation: () -> Unit,
    onClearOfficeLocation: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSlateBg),
        border = BorderStroke(1.dp, BorderSlate),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.BusinessCenter,
                            contentDescription = null,
                            tint = PrimaryCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Office Coordinates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (officeLocation != null) "Saved in Local Storage" else "Not Configured Yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (officeLocation != null) PrimaryEmerald else ErrorRose
                        )
                    }
                }

                if (officeLocation != null) {
                    IconButton(
                        onClick = onClearOfficeLocation,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Clear Office Location",
                            tint = ErrorRose
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (officeLocation != null) {
                Surface(
                    color = DarkSlateBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderSlate),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = PrimaryEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = officeLocation.address,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Lat: ${String.format(Locale.US, "%.6f", officeLocation.latitude)} | Lng: ${String.format(Locale.US, "%.6f", officeLocation.longitude)}",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            } else {
                Surface(
                    color = DarkSlateBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderSlate),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Set office location first to enable geofenced check-in.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSetOfficeLocation,
                enabled = !isSettingOffice,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryCyan,
                    contentColor = Color.White
                )
            ) {
                if (isSettingOffice) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Fetching High Accuracy GPS...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (officeLocation == null) "Set Current Location as Office" else "Update Saved Office Location",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernDistanceHeroCard(
    officeLocation: com.example.trackersync.domain.model.OfficeLocation?,
    distanceMeters: Double?,
    isWithinGeofence: Boolean,
    currentLocation: CurrentCoordinates?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSlateBg),
        border = BorderStroke(
            1.dp,
            if (officeLocation != null && isWithinGeofence) PrimaryEmerald.copy(alpha = 0.6f)
            else if (officeLocation != null) ErrorRose.copy(alpha = 0.6f)
            else BorderSlate
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Real-Time Geofence Radius",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Surface(
                    color = DarkSlateBg,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, BorderSlate)
                ) {
                    Text(
                        text = "50m Limit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (officeLocation == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Office Location Required",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Please configure your office location above to start distance tracking.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            } else if (distanceMeters == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryCyan, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Acquiring live GPS position...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            } else {
                val formattedDistance = String.format(Locale.US, "%.1f", distanceMeters)

                // Large Hero Metric Box
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(
                            if (isWithinGeofence) PrimaryEmerald.copy(alpha = 0.12f)
                            else ErrorRose.copy(alpha = 0.12f)
                        )
                        .border(
                            3.dp,
                            if (isWithinGeofence) PrimaryEmerald else ErrorRose,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formattedDistance,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isWithinGeofence) PrimaryEmerald else ErrorRose
                        )
                        Text(
                            text = "METERS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Distance to Office: ${formattedDistance}m",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Modern Status Pill
                Surface(
                    color = if (isWithinGeofence) EmeraldLight else ErrorRoseBg,
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isWithinGeofence) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (isWithinGeofence) EmeraldDark else ErrorRose,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isWithinGeofence)
                                "Within 50m • Attendance Allowed"
                            else
                                "Outside 50m • Attendance Blocked",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isWithinGeofence) EmeraldDark else ErrorRose
                        )
                    }
                }
            }

            if (currentLocation != null && officeLocation != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderSlate)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = null,
                        tint = PrimaryCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Current GPS: ${String.format(Locale.US, "%.5f", currentLocation.latitude)}, ${String.format(Locale.US, "%.5f", currentLocation.longitude)}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernMarkAttendanceSection(
    isWithinGeofence: Boolean,
    officeLocation: com.example.trackersync.domain.model.OfficeLocation?,
    isMarkingAttendance: Boolean,
    distanceMeters: Double?,
    onMarkAttendance: () -> Unit
) {
    val buttonEnabled = isWithinGeofence && officeLocation != null && !isMarkingAttendance

    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onMarkAttendance,
            enabled = buttonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(if (buttonEnabled) 8.dp else 0.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryEmerald,
                contentColor = Color.White,
                disabledContainerColor = CardSlateBg,
                disabledContentColor = Color.Gray
            )
        ) {
            if (isMarkingAttendance) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Verifying Geofence Check-in...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.HowToReg, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "Mark Attendance Now",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (officeLocation == null) {
            Text(
                text = "⚠️ Attendance disabled: Office location is not set.",
                fontSize = 12.sp,
                color = ErrorRose,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        } else if (!isWithinGeofence && distanceMeters != null) {
            Text(
                text = "⚠️ Attendance disabled: You must be within 50m of office (currently ${String.format(Locale.US, "%.1f", distanceMeters)}m away).",
                fontSize = 12.sp,
                color = ErrorRose,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        } else if (isWithinGeofence) {
            Text(
                text = "✓ Location verified within 50m radius. Ready to check in.",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryEmerald,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun ModernErrorCard(message: String, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ErrorRoseBg),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, ErrorRose),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Error, contentDescription = null, tint = ErrorRose)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = ErrorRose,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = ErrorRose)
            }
        }
    }
}

@Composable
private fun ModernSuccessCard(message: String, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = EmeraldLight),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, PrimaryEmerald),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldDark)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = EmeraldDark,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = EmeraldDark)
            }
        }
    }
}

@Composable
private fun ModernWarningCard(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSlateBg),
        border = BorderStroke(1.dp, ErrorRose),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRose)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (!hasPermission) "Location Permission Required" else "GPS Services Disabled",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (!hasPermission)
                    "TrackerSync requires location access to verify geofenced attendance."
                else
                    "Your device GPS is turned off. Please turn on location services in settings.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = { if (!hasPermission) onRequestPermission() else onOpenSettings() },
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRose, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (!hasPermission) "Grant Permission" else "Enable GPS Settings", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ModernEmptyHistoryCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSlateBg),
        border = BorderStroke(1.dp, BorderSlate),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No attendance check-ins yet.",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Marked attendance logs will be saved locally and listed here.",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ModernAttendanceRecordItem(record: AttendanceRecord) {
    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy • HH:mm:ss", Locale.US) }
    val formattedTime = remember(record.timestamp) { dateFormat.format(Date(record.timestamp)) }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardSlateBg),
        border = BorderStroke(1.dp, BorderSlate),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (record.isWithinGeofence) PrimaryEmerald.copy(alpha = 0.15f)
                        else ErrorRose.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (record.isWithinGeofence) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (record.isWithinGeofence) PrimaryEmerald else ErrorRose,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formattedTime,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Distance: ${String.format(Locale.US, "%.1f", record.distanceMeters)}m",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${record.note}",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Lat: ${String.format(Locale.US, "%.4f", record.userLatitude)}, Lng: ${String.format(Locale.US, "%.4f", record.userLongitude)}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray
                )
            }
        }
    }
}
