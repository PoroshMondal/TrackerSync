import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../uploads/bloc/sync_cubit.dart';
import '../../uploads/bloc/sync_state.dart';
import '../../uploads/bloc/upload_cubit.dart';
import '../../uploads/bloc/upload_state.dart';
import '../../uploads/pages/pending_uploads_screen.dart';
import '../bloc/camera_cubit.dart';
import '../bloc/camera_state.dart';
import '../widgets/focus_indicator.dart';
import '../widgets/zoom_controls.dart';

class CameraPreviewScreen extends StatefulWidget {
  const CameraPreviewScreen({super.key});

  @override
  State<CameraPreviewScreen> createState() => _CameraPreviewScreenState();
}

class _CameraPreviewScreenState extends State<CameraPreviewScreen> {
  double _baseZoom = 1.0;

  @override
  void initState() {
    super.initState();
    context.read<CameraCubit>().initialize();
    context.read<UploadCubit>().loadBatches();
  }

  @override
  Widget build(BuildContext context) {
    final topPadding = MediaQuery.of(context).padding.top;
    final bottomPadding = MediaQuery.of(context).padding.bottom;

    return Scaffold(
      backgroundColor: Colors.black,
      body: BlocConsumer<CameraCubit, CameraState>(
        listener: (context, state) {
          if (state.errorMessage != null) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text(state.errorMessage!),
                backgroundColor: Colors.redAccent,
              ),
            );
          }
        },
        builder: (context, cameraState) {
          if (cameraState.status == CameraStatus.loading ||
              cameraState.status == CameraStatus.initial) {
            return const Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  CircularProgressIndicator(color: Colors.amberAccent),
                  SizedBox(height: 16),
                  Text('Initializing Camera Hardware...', style: TextStyle(color: Colors.white)),
                ],
              ),
            );
          }

          if (cameraState.status == CameraStatus.permissionDenied) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(24.0),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Icon(Icons.camera_alt_outlined, size: 64, color: Colors.amberAccent),
                    const SizedBox(height: 16),
                    const Text(
                      'Camera Permission Required',
                      style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Colors.white),
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      'TrackerSync requires camera access to capture field photos for queue synchronization.',
                      textAlign: TextAlign.center,
                      style: TextStyle(color: Colors.grey),
                    ),
                    const SizedBox(height: 24),
                    ElevatedButton(
                      style: ElevatedButton.styleFrom(backgroundColor: Colors.amberAccent),
                      onPressed: () {
                        context.read<CameraCubit>().initialize();
                      },
                      child: const Text('Grant Camera Access', style: TextStyle(color: Colors.black)),
                    ),
                  ],
                ),
              ),
            );
          }

          if (cameraState.status == CameraStatus.error) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.error_outline, size: 48, color: Colors.redAccent),
                  const SizedBox(height: 16),
                  Text(
                    cameraState.errorMessage ?? 'Camera Unavailable',
                    style: const TextStyle(color: Colors.white),
                  ),
                  const SizedBox(height: 16),
                  ElevatedButton(
                    onPressed: () => context.read<CameraCubit>().initialize(),
                    child: const Text('Retry Camera Init'),
                  )
                ],
              ),
            );
          }

          final cubit = context.read<CameraCubit>();
          final controller = cubit.controller;

          return Stack(
            children: [
              // Fullscreen Camera Preview
              if (controller != null && controller.value.isInitialized)
                Positioned.fill(
                  child: GestureDetector(
                    onScaleStart: (_) {
                      _baseZoom = cameraState.currentZoom;
                    },
                    onScaleUpdate: (details) {
                      cubit.setZoom(_baseZoom * details.scale);
                    },
                    onTapUp: (details) {
                      final renderBox = context.findRenderObject() as RenderBox?;
                      if (renderBox != null) {
                        cubit.tapToFocus(
                          details.localPosition,
                          renderBox.size,
                        );
                      }
                    },
                    child: CameraPreview(controller),
                  ),
                ),

              // Tap-Focus Visual Ring
              if (cameraState.tapFocusPosition != null)
                FocusIndicator(position: cameraState.tapFocusPosition!),

              // Top Safe Area Polished Header Bar
              Positioned(
                top: topPadding + 24,
                left: 16,
                right: 16,
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                  decoration: BoxDecoration(
                    color: Colors.black.withOpacity(0.70),
                    borderRadius: BorderRadius.circular(30),
                    border: Border.all(color: Colors.white12, width: 1),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.3),
                        blurRadius: 10,
                        offset: const Offset(0, 4),
                      )
                    ],
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      // Camera Selector or Lens Badge
                      if (cameraState.availableBackCameras.length > 1)
                        InkWell(
                          onTap: () => cubit.switchBackCamera(),
                          borderRadius: BorderRadius.circular(20),
                          child: Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                            child: Row(
                              children: [
                                const Icon(Icons.cameraswitch, color: Colors.amberAccent, size: 20),
                                const SizedBox(width: 6),
                                Text(
                                  'Lens ${cameraState.selectedCameraIndex + 1}/${cameraState.availableBackCameras.length}',
                                  style: const TextStyle(
                                    color: Colors.white,
                                    fontSize: 12,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        )
                      else
                        const Row(
                          children: [
                            Icon(Icons.camera_alt, color: Colors.amberAccent, size: 18),
                            SizedBox(width: 6),
                            Text(
                              'TrackerSync Cam',
                              style: TextStyle(
                                color: Colors.white,
                                fontSize: 13,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),

                      // Connectivity Indicator Status Pill
                      BlocBuilder<SyncCubit, SyncState>(
                        builder: (context, syncState) {
                          final isOnline = syncState.isConnected;
                          return Container(
                            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                            decoration: BoxDecoration(
                              color: isOnline
                                  ? Colors.green.withOpacity(0.2)
                                  : Colors.red.withOpacity(0.2),
                              borderRadius: BorderRadius.circular(20),
                              border: Border.all(
                                color: isOnline ? Colors.greenAccent : Colors.redAccent,
                                width: 1,
                              ),
                            ),
                            child: Row(
                              children: [
                                Icon(
                                  isOnline ? Icons.wifi : Icons.wifi_off,
                                  color: isOnline ? Colors.greenAccent : Colors.redAccent,
                                  size: 13,
                                ),
                                const SizedBox(width: 4),
                                Text(
                                  isOnline ? 'ONLINE' : 'OFFLINE',
                                  style: TextStyle(
                                    color: isOnline ? Colors.greenAccent : Colors.redAccent,
                                    fontSize: 10,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              ],
                            ),
                          );
                        },
                      ),

                      // Pending Uploads Queue Drawer Button
                      BlocBuilder<UploadCubit, UploadState>(
                        builder: (context, uploadState) {
                          final count = uploadState.totalPendingImages;
                          return InkWell(
                            onTap: () {
                              Navigator.of(context).push(
                                MaterialPageRoute(
                                  builder: (_) => const PendingUploadsScreen(),
                                ),
                              );
                            },
                            borderRadius: BorderRadius.circular(20),
                            child: Badge(
                              label: Text('$count'),
                              isLabelVisible: count > 0,
                              backgroundColor: Colors.amberAccent,
                              textColor: Colors.black,
                              child: Container(
                                padding: const EdgeInsets.all(6),
                                decoration: const BoxDecoration(
                                  color: Colors.white10,
                                  shape: BoxShape.circle,
                                ),
                                child: const Icon(
                                  Icons.cloud_upload_outlined,
                                  color: Colors.white,
                                  size: 20,
                                ),
                              ),
                            ),
                          );
                        },
                      ),
                    ],
                  ),
                ),
              ),

              // Zoom Controls Overlay (Above Bottom Control Bar)
              Positioned(
                bottom: bottomPadding + 110,
                left: 0,
                right: 0,
                child: ZoomControls(
                  currentZoom: cameraState.currentZoom,
                  minZoom: cameraState.minZoom,
                  maxZoom: cameraState.maxZoom,
                  onZoomChanged: (zoom) => cubit.setZoom(zoom),
                ),
              ),

              // Bottom Safe Area Capture Control Bar
              Positioned(
                bottom: bottomPadding + 16,
                left: 16,
                right: 16,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    // Active Batch Selector Pill
                    BlocBuilder<UploadCubit, UploadState>(
                      builder: (context, uploadState) {
                        final activeBatch = uploadState.activeBatch;
                        return Container(
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                          decoration: BoxDecoration(
                            color: Colors.black87,
                            borderRadius: BorderRadius.circular(24),
                            border: Border.all(color: Colors.white24),
                          ),
                          child: PopupMenuButton<String>(
                            color: Colors.grey.shade900,
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                const Icon(Icons.folder_open, color: Colors.amberAccent, size: 18),
                                const SizedBox(width: 8),
                                Text(
                                  activeBatch != null ? activeBatch.name : 'Select Batch',
                                  style: const TextStyle(
                                    color: Colors.white,
                                    fontWeight: FontWeight.bold,
                                    fontSize: 13,
                                  ),
                                ),
                                const SizedBox(width: 4),
                                const Icon(Icons.arrow_drop_down, color: Colors.white),
                              ],
                            ),
                            onSelected: (batchId) {
                              if (batchId == 'NEW') {
                                context.read<UploadCubit>().createNewBatch();
                              } else {
                                context.read<UploadCubit>().selectActiveBatch(batchId);
                              }
                            },
                            itemBuilder: (context) {
                              final items = uploadState.batches.map((b) {
                                return PopupMenuItem<String>(
                                  value: b.id,
                                  child: Row(
                                    children: [
                                      const Icon(Icons.folder, color: Colors.amberAccent, size: 18),
                                      const SizedBox(width: 8),
                                      Text('${b.name} (${b.images.length} imgs)'),
                                    ],
                                  ),
                                );
                              }).toList();

                              items.add(
                                const PopupMenuItem<String>(
                                  value: 'NEW',
                                  child: Row(
                                    children: [
                                      Icon(Icons.create_new_folder, color: Colors.greenAccent, size: 18),
                                      SizedBox(width: 8),
                                      Text('+ Create New Batch', style: TextStyle(color: Colors.greenAccent)),
                                    ],
                                  ),
                                ),
                              );
                              return items;
                            },
                          ),
                        );
                      },
                    ),

                    const SizedBox(height: 12),

                    // Shutter Capture Button
                    GestureDetector(
                      onTap: cameraState.isCapturing
                          ? null
                          : () async {
                              final path = await cubit.capturePhoto();
                              if (path != null && context.mounted) {
                                context.read<UploadCubit>().addPhotoToQueue(path);
                              }
                            },
                      child: Container(
                        width: 72,
                        height: 72,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          border: Border.all(color: Colors.white, width: 4),
                          color: cameraState.isCapturing ? Colors.grey : Colors.amberAccent,
                          boxShadow: [
                            BoxShadow(
                              color: Colors.amberAccent.withOpacity(0.4),
                              blurRadius: 16,
                              spreadRadius: 2,
                            )
                          ],
                        ),
                        child: cameraState.isCapturing
                            ? const Center(
                                child: SizedBox(
                                  width: 24,
                                  height: 24,
                                  child: CircularProgressIndicator(strokeWidth: 2, color: Colors.black),
                                ),
                              )
                            : const Icon(Icons.camera_alt, color: Colors.black, size: 32),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}
