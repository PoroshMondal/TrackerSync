import 'dart:async';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../core/network/network_info.dart';
import '../../../domain/repositories/upload_repository.dart';
import 'sync_state.dart';
import 'upload_cubit.dart';

class SyncCubit extends Cubit<SyncState> {
  final UploadRepository repository;
  final NetworkInfo networkInfo;
  final UploadCubit uploadCubit;
  StreamSubscription<bool>? _networkSubscription;

  SyncCubit({
    required this.repository,
    required this.networkInfo,
    required this.uploadCubit,
  }) : super(const SyncState()) {
    _initNetworkMonitoring();
  }

  Future<void> _initNetworkMonitoring() async {
    final connected = await networkInfo.isConnected;
    emit(state.copyWith(
      isConnected: connected,
      status: connected ? SyncEngineStatus.idle : SyncEngineStatus.offline,
    ));

    _networkSubscription = networkInfo.onConnectivityChanged.listen((connected) {
      emit(state.copyWith(
        isConnected: connected,
        status: connected ? SyncEngineStatus.idle : SyncEngineStatus.offline,
      ));

      if (connected) {
        // Automatic retry & upload sync when connection restored
        triggerSyncProcess();
      }
    });
  }

  Future<void> triggerSyncProcess() async {
    if (state.status == SyncEngineStatus.syncing) return;

    final connected = await networkInfo.isConnected;
    if (!connected) {
      emit(state.copyWith(
        status: SyncEngineStatus.offline,
        lastSyncMessage: 'Offline: Connectivity unavailable.',
      ));
      return;
    }

    emit(state.copyWith(
      status: SyncEngineStatus.syncing,
      lastSyncMessage: 'Sync engine processing pending uploads...',
    ));

    try {
      await repository.syncPendingQueue();
      await uploadCubit.loadBatches();

      emit(state.copyWith(
        status: SyncEngineStatus.completed,
        lastSyncMessage: 'Sync completed successfully.',
        lastSyncTime: DateTime.now(),
      ));
    } catch (e) {
      await uploadCubit.loadBatches();
      emit(state.copyWith(
        status: SyncEngineStatus.error,
        lastSyncMessage: 'Sync encountered failures: ${e.toString()}',
      ));
    }
  }

  @override
  Future<void> close() {
    _networkSubscription?.cancel();
    return super.close();
  }
}
