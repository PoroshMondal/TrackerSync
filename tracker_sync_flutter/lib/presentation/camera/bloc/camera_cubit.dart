import 'dart:async';
import 'dart:io';
import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:path_provider/path_provider.dart';
import 'package:permission_handler/permission_handler.dart';
import 'camera_state.dart';

class CameraCubit extends Cubit<CameraState> {
  CameraController? controller;
  Timer? _focusTimer;

  CameraCubit() : super(const CameraState());

  Future<void> initialize() async {
    emit(state.copyWith(status: CameraStatus.loading, errorMessage: null));

    final status = await Permission.camera.request();
    if (status.isDenied || status.isPermanentlyDenied) {
      emit(state.copyWith(
        status: CameraStatus.permissionDenied,
        errorMessage: 'Camera permission was denied.',
      ));
      return;
    }

    try {
      final cameras = await availableCameras();
      final backCameras = cameras
          .where((c) => c.lensDirection == CameraLensDirection.back)
          .toList();

      if (backCameras.isEmpty) {
        // Fallback to any camera if no back camera detected
        if (cameras.isNotEmpty) {
          backCameras.add(cameras.first);
        } else {
          emit(state.copyWith(
            status: CameraStatus.error,
            errorMessage: 'No camera hardware found on device.',
          ));
          return;
        }
      }

      emit(state.copyWith(
        availableBackCameras: backCameras,
        selectedCameraIndex: 0,
      ));

      await _initController(backCameras.first);
    } catch (e) {
      emit(state.copyWith(
        status: CameraStatus.error,
        errorMessage: 'Failed to initialize camera: ${e.toString()}',
      ));
    }
  }

  Future<void> _initController(CameraDescription description) async {
    await controller?.dispose();

    controller = CameraController(
      description,
      ResolutionPreset.high,
      enableAudio: false,
    );

    try {
      await controller!.initialize();

      final minZ = await controller!.getMinZoomLevel();
      final maxZ = await controller!.getMaxZoomLevel();

      emit(state.copyWith(
        status: CameraStatus.ready,
        minZoom: minZ,
        maxZoom: maxZ,
        currentZoom: minZ,
      ));
    } catch (e) {
      emit(state.copyWith(
        status: CameraStatus.error,
        errorMessage: 'Camera init error: ${e.toString()}',
      ));
    }
  }

  Future<void> switchBackCamera() async {
    if (state.availableBackCameras.length <= 1) return;

    final nextIndex = (state.selectedCameraIndex + 1) % state.availableBackCameras.length;
    emit(state.copyWith(
      status: CameraStatus.loading,
      selectedCameraIndex: nextIndex,
    ));

    await _initController(state.availableBackCameras[nextIndex]);
  }

  Future<void> setZoom(double value) async {
    if (controller == null || !controller!.value.isInitialized) return;

    final clampedZoom = value.clamp(state.minZoom, state.maxZoom);
    try {
      await controller!.setZoomLevel(clampedZoom);
      emit(state.copyWith(currentZoom: clampedZoom));
    } catch (e) {
      // Ignore unsupported zoom error gracefully
    }
  }

  Future<void> tapToFocus(Offset tapPosition, Size screenSize) async {
    if (controller == null || !controller!.value.isInitialized) return;

    // Trigger visual indicator in state
    emit(state.copyWith(tapFocusPosition: tapPosition));

    _focusTimer?.cancel();
    _focusTimer = Timer(const Duration(milliseconds: 1500), () {
      emit(state.copyWith(clearTapFocus: true));
    });

    try {
      // Convert screen coordinate to relative point (0.0 to 1.0)
      final relativePoint = Offset(
        (tapPosition.dx / screenSize.width).clamp(0.0, 1.0),
        (tapPosition.dy / screenSize.height).clamp(0.0, 1.0),
      );

      await controller!.setFocusPoint(relativePoint);
      await controller!.setExposurePoint(relativePoint);
      await controller!.setFocusMode(FocusMode.auto);
    } catch (_) {
      // Handle unsupported focus mode gracefully without crash
    }
  }

  Future<String?> capturePhoto() async {
    if (controller == null || !controller!.value.isInitialized) return null;

    try {
      emit(state.copyWith(isCapturing: true));

      final xFile = await controller!.takePicture();

      // Save to app documents directory
      final appDir = await getApplicationDocumentsDirectory();
      final fileName = 'IMG_${DateTime.now().millisecondsSinceEpoch}.jpg';
      final savedFile = File('${appDir.path}/$fileName');

      await File(xFile.path).copy(savedFile.path);

      emit(state.copyWith(isCapturing: false));
      return savedFile.path;
    } catch (e) {
      emit(state.copyWith(
        isCapturing: false,
        errorMessage: 'Capture error: ${e.toString()}',
      ));
      return null;
    }
  }

  @override
  Future<void> close() {
    _focusTimer?.cancel();
    controller?.dispose();
    return super.close();
  }
}
