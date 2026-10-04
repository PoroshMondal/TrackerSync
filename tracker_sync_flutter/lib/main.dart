import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'core/network/network_info.dart';
import 'core/theme/app_theme.dart';
import 'data/datasources/local_database.dart';
import 'data/datasources/mock_upload_api.dart';
import 'data/repositories/upload_repository_impl.dart';
import 'presentation/camera/bloc/camera_cubit.dart';
import 'presentation/camera/pages/camera_preview_screen.dart';
import 'presentation/uploads/bloc/sync_cubit.dart';
import 'presentation/uploads/bloc/upload_cubit.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final localDb = LocalDatabase.instance;
  final mockApi = MockUploadApi(shouldSimulateFailure: false);
  final connectivity = Connectivity();
  final networkInfo = NetworkInfoImpl(connectivity);

  final repository = UploadRepositoryImpl(
    db: localDb,
    mockApi: mockApi,
  );

  runApp(TrackerSyncFlutterApp(
    repository: repository,
    networkInfo: networkInfo,
  ));
}

class TrackerSyncFlutterApp extends StatelessWidget {
  final UploadRepositoryImpl repository;
  final NetworkInfo networkInfo;

  const TrackerSyncFlutterApp({
    super.key,
    required this.repository,
    required this.networkInfo,
  });

  @override
  Widget build(BuildContext context) {
    return MultiBlocProvider(
      providers: [
        BlocProvider<CameraCubit>(
          create: (_) => CameraCubit(),
        ),
        BlocProvider<UploadCubit>(
          create: (_) => UploadCubit(repository: repository),
        ),
        BlocProvider<SyncCubit>(
          create: (context) => SyncCubit(
            repository: repository,
            networkInfo: networkInfo,
            uploadCubit: context.read<UploadCubit>(),
          ),
        ),
      ],
      child: MaterialApp(
        title: 'TrackerSync Camera & Sync',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.darkTheme,
        home: const CameraPreviewScreen(),
      ),
    );
  }
}
