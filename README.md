# TrackerSync — Smart Attendance & Camera Sync Platform

[![Android Build](https://img.shields.io/badge/Android-Native_Kotlin-green.svg)](https://developer.android.com/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![Flutter Build](https://img.shields.io/badge/Flutter-3.47.0-blue.svg)](https://flutter.dev/)
[![BLoC Pattern](https://img.shields.io/badge/State-BLoC%2FCubit-purple.svg)](https://bloclibrary.dev/)

**TrackerSync** (also referred to as FieldSync) is a high-reliability production-grade platform designed for field workers and enterprise personnel. The project solves two primary operational challenges:
1. **Geo-fenced field attendance logging** on Native Android.
2. **Offline-first resilient camera photo queueing and background sync** on Flutter.

---

## Technical Tasks Overview

The project is structured into two standalone modules:

### TASK 1 — Native Android Geo-Fenced Attendance System
* **Tech Stack**: Native Android, Kotlin 2.2, Jetpack Compose, Kotlin Flow, Coroutines, Material 3.
* **Architecture**: Clean Layered Architecture (`data`, `domain`, `presentation`).
* **Key Features**:
  * **Set Office Location**: Fetches high-accuracy GPS coordinates via `FusedLocationProviderClient`, saves office latitude & longitude locally via `SharedPreferences`. Persists across app restarts.
  * **Real-time Distance Monitoring**: Streams live location updates via Kotlin `Flow`, continuously calculating distance in meters between user position and office.
  * **Geofence Boundary Rule**:
    * `<= 50.0m`: Attendance check-in allowed.
    * `> 50.0m`: Attendance check-in blocked with descriptive warning.
    * Inclusive boundary rule: Distance = 50.0m is allowed.
  * **Offline Persistence**: Office location and full attendance check-in history are stored locally and survive application restarts.

### TASK 2 — Flutter Advanced Camera & Sync Engine
* **Tech Stack**: Flutter 3.47, Dart 3.12, `flutter_bloc` (BLoC/Cubit), `sqflite` (SQLite), `camera`, `connectivity_plus`, `permission_handler`.
* **Architecture**: Clean Layered Architecture (`core`, `data`, `domain`, `presentation`).
* **Key Features**:
  * **Camera Hardware Integration**: Functional preview, graceful permission handling, and automatic detection of available back cameras with lens switching support.
  * **Multi-Modal Zoom**: Pinch-to-zoom gestures on camera preview, zoom slider, and quick preset buttons (`0.5x`, `1x`, `2x`, `3x`). Zoom values are safely clamped to device hardware bounds (`minZoomLevel` and `maxZoomLevel`).
  * **Tap-to-Focus**: Interactive touch focus with coordinate normalization, focus/exposure mode configuration, and an animated focus indicator ring.
  * **Batch & Photo Queue Management**: Captures photos into structured batches with unique UUIDs. Stores local file path, upload status (`pending`, `uploading`, `uploaded`, `failed`), retry counters, and error logs in local SQLite database (`sqflite`).
  * **Resilient Sync Engine**: Background worker that monitors network connectivity via `connectivity_plus`. When internet is restored, pending uploads auto-sync automatically without requiring manual user intervention.
  * **Offline Resilience**: Offline or failed uploads are never lost or deleted. They remain safely in local storage and in the "Pending Uploads" queue with retry counters.
  * **Mock API & Evaluator Simulation**: Includes `MockUploadApi` supporting success and failure scenarios, with an in-app toggle so evaluators can simulate server 503 / network timeouts and observe automatic retry behavior.

---

## Project Structure

```
TrackerSync2/
├── app/                                  # TASK 1: Native Android Geo-Fenced Attendance System
│   └── src/
│       ├── main/java/com/example/trackersync/
│       │   ├── data/
│       │   │   ├── local/PreferencesManager.kt       # Local persistent storage for office & logs
│       │   │   └── location/LocationTracker.kt      # FusedLocationProvider wrapper & Flow updates
│       │   ├── domain/
│       │   │   ├── model/OfficeLocation.kt
│       │   │   ├── model/AttendanceRecord.kt
│       │   │   └── usecase/CalculateDistanceUseCase.kt  # Haversine formula & 50m geofence rules
│       │   ├── presentation/
│       │   │   ├── AttendanceUiState.kt
│       │   │   ├── AttendanceViewModel.kt            # StateFlow & business logic
│       │   │   └── AttendanceScreen.kt               # Jetpack Compose UI
│       │   └── MainActivity.kt
│       └── test/java/com/example/trackersync/
│           └── CalculateDistanceUseCaseTest.kt       # Unit tests (25m, 49m, 50m, 51m, 120m)
│
└── tracker_sync_flutter/                 # TASK 2: Flutter Advanced Camera & Sync Engine
    ├── lib/
    │   ├── core/
    │   │   ├── constants/app_constants.dart
    │   │   ├── error/failures.dart
    │   │   ├── network/network_info.dart          # Connectivity listener wrapper
    │   │   └── theme/app_theme.dart
    │   ├── data/
    │   │   ├── datasources/local_database.dart   # sqflite SQLite persistence
    │   │   ├── datasources/mock_upload_api.dart   # Mock API (Success/Failure modes) & Real API spec
    │   │   ├── models/batch_item_model.dart
    │   │   ├── models/image_item_model.dart
    │   │   └── repositories/upload_repository_impl.dart
    │   ├── domain/
    │   │   ├── entities/batch_item.dart
    │   │   ├── entities/image_item.dart
    │   │   └── repositories/upload_repository.dart
    │   └── presentation/
    │       ├── camera/
    │       │   ├── bloc/camera_cubit.dart        # Camera init, back camera switch, zoom, focus
    │       │   ├── pages/camera_preview_screen.dart
    │       │   └── widgets/zoom_controls.dart
    │       └── uploads/
    │           ├── bloc/upload_cubit.dart        # Batch & Queue state
    │           ├── bloc/sync_cubit.dart          # Auto-sync engine on network reconnect
    │           ├── pages/pending_uploads_screen.dart
    │           └── widgets/batch_card_widget.dart
    └── test/
        └── sync_queue_test.dart                 # Queue creation, retry, status transition tests
```

---

## Requirements Verification Checklist

### Task 1 — Native Android
- [x] Set Office Location button retrieves GPS coordinates and saves locally.
- [x] Office location persists across app restarts.
- [x] Real-time distance calculation updates as user moves.
- [x] Mark Attendance button disabled when distance > 50m.
- [x] Mark Attendance allowed when distance <= 50m.
- [x] Boundary rule handles 50.0m inclusive.
- [x] Handles location permissions denied / permanently denied gracefully without crashing.
- [x] Handles GPS disabled state with user-friendly error banners.
- [x] Unit tests cover distance calculation, 25m, 49m, 50m, 51m, 120m, and missing office scenarios.

### Task 2 — Flutter
- [x] Functional camera preview with permission checks.
- [x] Pinch zoom, zoom slider, and 0.5x, 1x, 2x, 3x preset buttons safely clamped to camera range.
- [x] Multi-back camera support with lens switcher.
- [x] Tap-to-focus with relative coordinate translation and animated focus indicator.
- [x] Capture multiple photos into batches with unique UUIDs.
- [x] Pending Uploads queue section displaying batches, images, and upload status tags (`pending`, `uploading`, `failed`, `uploaded`).
- [x] Local persistence using SQLite (`sqflite`) ensuring queue survives app restarts.
- [x] Resilient background sync engine listening to network connectivity.
- [x] Offline behavior: Failed/offline images are never deleted or lost.
- [x] Automatic retry when internet connection is restored.
- [x] Mock API supporting success and failure scenarios with in-app toggle switch.
- [x] Clean architecture with `flutter_bloc` (CameraCubit, UploadCubit, SyncCubit).
- [x] Unit tests covering queue creation, status transitions, mock API success/failure, and retries.

---

## How to Run

### Task 1 — Native Android Geo-Fenced Attendance System
1. Open the root directory in Android Studio.
2. Ensure Android SDK 34+ / Build Tools are installed.
3. Run unit tests:
   ```bash
   ./gradlew app:test
   ```
4. Build Debug APK:
   ```bash
   ./gradlew app:assembleDebug
   ```
   *APK location: `app/build/outputs/apk/debug/app-debug.apk`*

### Task 2 — Flutter Advanced Camera & Sync Engine
1. Navigate to the Flutter module folder:
   ```bash
   cd tracker_sync_flutter
   ```
2. Get dependencies:
   ```bash
   flutter pub get
   ```
3. Run unit tests:
   ```bash
   flutter test
   ```
4. Run Flutter application on device / emulator:
   ```bash
   flutter run
   ```
5. Build Flutter Debug APK:
   ```bash
   flutter build apk --debug
   ```
   *APK location: `tracker_sync_flutter/build/app/outputs/flutter-apk/app-debug.apk`*

---

## Generative AI Usage Disclosure

In compliance with technical assessment guidelines, generative AI tools were utilized during the project implementation as follows:

* **Tools Used**: OpenAI GPT-4o / Claude 3.5 Sonnet / IDE Assistant.
* **Areas of AI Assistance**:
  1. **Architecture Planning**: Assisting in mapping clean architecture layers for both Kotlin/Compose and Flutter BLoC modules.
  2. **Camera Hardware API Research**: Verifying CameraX / Flutter camera package bounds-clamping logic for variable camera zoom ranges.
  3. **Edge Case Checklist Generation**: Ensuring edge case scenarios (missing location, GPS disabled, network timeout, camera switch) were covered.
  4. **Unit Test Boilerplate**: Generating test case structures for Haversine distance calculations and queue status transitions.
* **Essential Prompts Used**:
  - *"How to compute WGS84 / Haversine distance in Kotlin and restrict Jetpack Compose button state when distance is under 50.0 meters?"*
  - *"What is the best clean architecture pattern for Flutter BLoC/Cubit managing an offline-first upload queue with SQLite and connectivity listeners?"*
  - *"How to clamp camera zoom ratio between hardware minZoom and maxZoom in Flutter camera package without throwing state errors?"*
* **Human Review & Modifications**: All AI-assisted code was manually reviewed, refined, re-structured to comply with SOLID principles, and validated against build pipelines (`gradlew test` and `flutter test`).

---

## Deliverables & Submission Links

* **APK Download Link (Google Drive / Release)**: `[Insert Your Google Drive APK Link Here]`
* **GitHub Repository**: `https://github.com/your-username/TrackerSync`

---

## License & Submission

This project is created for the **Senior App Developer Technical Assessment** for **TrackerSync / FieldSync**.

