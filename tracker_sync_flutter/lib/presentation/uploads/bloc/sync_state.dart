import 'package:equatable/equatable.dart';

enum SyncEngineStatus { idle, syncing, completed, offline, error }

class SyncState extends Equatable {
  final SyncEngineStatus status;
  final bool isConnected;
  final String? lastSyncMessage;
  final DateTime? lastSyncTime;

  const SyncState({
    this.status = SyncEngineStatus.idle,
    this.isConnected = true,
    this.lastSyncMessage,
    this.lastSyncTime,
  });

  SyncState copyWith({
    SyncEngineStatus? status,
    bool? isConnected,
    String? lastSyncMessage,
    DateTime? lastSyncTime,
  }) {
    return SyncState(
      status: status ?? this.status,
      isConnected: isConnected ?? this.isConnected,
      lastSyncMessage: lastSyncMessage ?? this.lastSyncMessage,
      lastSyncTime: lastSyncTime ?? this.lastSyncTime,
    );
  }

  @override
  List<Object?> get props => [
        status,
        isConnected,
        lastSyncMessage,
        lastSyncTime,
      ];
}
