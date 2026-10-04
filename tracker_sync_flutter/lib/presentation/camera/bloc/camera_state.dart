import 'dart:ui';
import 'package:camera/camera.dart';
import 'package:equatable/equatable.dart';

enum CameraStatus { initial, loading, ready, error, permissionDenied }

class CameraState extends Equatable {
  final CameraStatus status;
  final List<CameraDescription> availableBackCameras;
  final int selectedCameraIndex;
  final double currentZoom;
  final double minZoom;
  final double maxZoom;
  final Offset? tapFocusPosition;
  final bool isCapturing;
  final String? errorMessage;

  const CameraState({
    this.status = CameraStatus.initial,
    this.availableBackCameras = const [],
    this.selectedCameraIndex = 0,
    this.currentZoom = 1.0,
    this.minZoom = 1.0,
    this.maxZoom = 1.0,
    this.tapFocusPosition,
    this.isCapturing = false,
    this.errorMessage,
  });

  CameraState copyWith({
    CameraStatus? status,
    List<CameraDescription>? availableBackCameras,
    int? selectedCameraIndex,
    double? currentZoom,
    double? minZoom,
    double? maxZoom,
    Offset? tapFocusPosition,
    bool clearTapFocus = false,
    bool? isCapturing,
    String? errorMessage,
  }) {
    return CameraState(
      status: status ?? this.status,
      availableBackCameras: availableBackCameras ?? this.availableBackCameras,
      selectedCameraIndex: selectedCameraIndex ?? this.selectedCameraIndex,
      currentZoom: currentZoom ?? this.currentZoom,
      minZoom: minZoom ?? this.minZoom,
      maxZoom: maxZoom ?? this.maxZoom,
      tapFocusPosition: clearTapFocus ? null : (tapFocusPosition ?? this.tapFocusPosition),
      isCapturing: isCapturing ?? this.isCapturing,
      errorMessage: errorMessage ?? this.errorMessage,
    );
  }

  @override
  List<Object?> get props => [
        status,
        availableBackCameras,
        selectedCameraIndex,
        currentZoom,
        minZoom,
        maxZoom,
        tapFocusPosition,
        isCapturing,
        errorMessage,
      ];
}
