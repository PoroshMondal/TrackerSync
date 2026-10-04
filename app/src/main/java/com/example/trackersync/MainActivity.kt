package com.example.trackersync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.trackersync.presentation.AttendanceScreen
import com.example.trackersync.presentation.AttendanceViewModel
import com.example.trackersync.presentation.camera.CameraScreen
import com.example.trackersync.ui.theme.TrackerSyncTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AttendanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrackerSyncTheme {
                var selectedTab by remember { mutableIntStateOf(0) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF0F172A),
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color.White
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.PinDrop, contentDescription = "Geo Attendance") },
                                label = { Text("Geo Attendance", fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF10B981),
                                    selectedTextColor = Color(0xFF10B981),
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray,
                                    indicatorColor = Color(0xFF10B981).copy(alpha = 0.15f)
                                )
                            )

                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Camera Sync") },
                                label = { Text("Camera & Sync", fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFFFFB300),
                                    selectedTextColor = Color(0xFFFFB300),
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray,
                                    indicatorColor = Color(0xFFFFB300).copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = Color(0xFF0F172A)
                    ) {
                        when (selectedTab) {
                            0 -> AttendanceScreen(viewModel = viewModel)
                            1 -> CameraScreen()
                        }
                    }
                }
            }
        }
    }
}
