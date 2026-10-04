package com.example.trackersync.presentation.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import kotlin.coroutines.resume

private val PrimaryEmerald = Color(0xFF10B981)
private val DarkSlateBg = Color(0xFF0F172A)
private val CardSlateBg = Color(0xFF1E293B)
private val AccentAmber = Color(0xFFFFB300)

data class CapturedPhoto(
    val id: String,
    val file: File,
    val timestamp: Long,
    var status: String = "Pending", // Pending, Uploaded, Failed
    var errorMsg: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = modifier.fillMaxSize().background(DarkSlateBg),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Camera Permission Required", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("TrackerSync requires camera access to capture photos for sync queue.", color = Color.Gray)
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                ) {
                    Text("Grant Camera Access", color = Color.White)
                }
            }
        }
        return
    }

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraInfo by remember { mutableStateOf<CameraInfo?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    var currentZoom by remember { mutableFloatStateOf(1.0f) }
    var tapFocusPoint by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var isMockFailureMode by remember { mutableStateOf(false) }

    val queueList = remember { mutableStateListOf<CapturedPhoto>() }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // CameraX Live Preview
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val newZoom = (currentZoom * zoom).clamp(1.0f, 5.0f)
                        currentZoom = newZoom
                        cameraControl?.setZoomRatio(newZoom)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        tapFocusPoint = Pair(offset.x, offset.y)
                        val factory = SurfaceOrientedMeteringPointFactory(size.width.toFloat(), size.height.toFloat())
                        val point = factory.createPoint(offset.x, offset.y)
                        val action = FocusMeteringAction.Builder(point).build()
                        cameraControl?.startFocusAndMetering(action)

                        coroutineScope.launch {
                            delay(1500)
                            tapFocusPoint = null
                        }
                    }
                },
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val mainExecutor = ContextCompat.getMainExecutor(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner, cameraSelector, preview, imageCapture
                        )
                        cameraControl = camera.cameraControl
                        cameraInfo = camera.cameraInfo
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, mainExecutor)

                previewView
            }
        )

        // Tap-to-Focus Ring Overlay
        tapFocusPoint?.let { (x, y) ->
            Box(
                modifier = Modifier
                    .offset((x - 30).dp, (y - 30).dp)
                    .size(60.dp)
                    .border(2.dp, Color.Yellow, CircleShape)
            )
        }

        // Top Overlay Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Camera, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TrackerSync Cam", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Pending Queue Drawer Button with Badge
            IconButton(
                onClick = { showQueueSheet = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                BadgedBox(
                    badge = {
                        val pendingCount = queueList.count { it.status != "Uploaded" }
                        if (pendingCount > 0) {
                            Badge(containerColor = Color.Yellow, contentColor = Color.Black) {
                                Text("$pendingCount")
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = "Queue", tint = Color.White)
                }
            }
        }

        // Zoom Preset Chips Bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 110.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(1.0f, 2.0f, 3.0f).forEach { zoomVal ->
                FilterChip(
                    selected = (currentZoom - zoomVal).let { it * it } < 0.1f,
                    onClick = {
                        currentZoom = zoomVal
                        cameraControl?.setZoomRatio(zoomVal)
                    },
                    label = { Text("${zoomVal.toInt()}x", color = Color.White, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.Yellow,
                        selectedLabelColor = Color.Black,
                        containerColor = Color.Black.copy(alpha = 0.6f)
                    )
                )
            }
        }

        // Bottom Capture Control Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            IconButton(
                onClick = {
                    val capture = imageCapture ?: return@IconButton
                    isCapturing = true
                    val outputDirectory = context.filesDir
                    val photoFile = File(outputDirectory, "IMG_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    capture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                isCapturing = false
                                val photo = CapturedPhoto(
                                    id = UUID.randomUUID().toString(),
                                    file = photoFile,
                                    timestamp = System.currentTimeMillis(),
                                    status = if (isMockFailureMode) "Failed" else "Uploaded",
                                    errorMsg = if (isMockFailureMode) "Mock API Error: 503 Server Timeout" else null
                                )
                                queueList.add(0, photo)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                isCapturing = false
                            }
                        }
                    )
                },
                enabled = !isCapturing,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(if (isCapturing) Color.Gray else Color.Yellow)
                    .border(4.dp, Color.White, CircleShape)
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(28.dp))
                } else {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Capture", tint = Color.Black, modifier = Modifier.size(32.dp))
                }
            }
        }
    }

    // Pending Uploads Queue Bottom Sheet
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = CardSlateBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pending Uploads Queue", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(onClick = { showQueueSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                    }
                }

                // Mock Failure Toggle Bar
                Surface(
                    color = DarkSlateBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Mock API Failure Mode", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text("Toggle to test failure retries", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isMockFailureMode,
                            onCheckedChange = { isMockFailureMode = it }
                        )
                    }
                }

                if (queueList.isEmpty()) {
                    Text("No photos captured in queue yet.", color = Color.Gray, modifier = Modifier.padding(vertical = 24.dp))
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(queueList) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (item.status == "Uploaded") Icons.Default.CheckCircle else Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (item.status == "Uploaded") PrimaryEmerald else Color.Yellow
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.file.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                        Text("Status: ${item.status}", color = if (item.status == "Uploaded") PrimaryEmerald else Color.Yellow, fontSize = 12.sp)
                                        item.errorMsg?.let {
                                            Text(it, color = Color.Red, fontSize = 11.sp)
                                        }
                                    }
                                    IconButton(onClick = { queueList.remove(item) }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Float.clamp(min: Float, max: Float): Float {
    return if (this < min) min else if (this > max) max else this
}
