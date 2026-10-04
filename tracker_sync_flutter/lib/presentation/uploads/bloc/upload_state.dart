import 'package:equatable/equatable.dart';
import '../../../domain/entities/batch_item.dart';

enum UploadUIStatus { initial, loading, loaded, error }

class UploadState extends Equatable {
  final UploadUIStatus status;
  final List<BatchItem> batches;
  final String? activeBatchId;
  final bool isMockFailMode;
  final String? errorMessage;
  final String? infoMessage;

  const UploadState({
    this.status = UploadUIStatus.initial,
    this.batches = const [],
    this.activeBatchId,
    this.isMockFailMode = false,
    this.errorMessage,
    this.infoMessage,
  });

  BatchItem? get activeBatch {
    if (activeBatchId == null || batches.isEmpty) return null;
    try {
      return batches.firstWhere((b) => b.id == activeBatchId);
    } catch (_) {
      return batches.first;
    }
  }

  int get totalPendingImages {
    int count = 0;
    for (var b in batches) {
      count += b.pendingCount;
    }
    return count;
  }

  UploadState copyWith({
    UploadUIStatus? status,
    List<BatchItem>? batches,
    String? activeBatchId,
    bool? isMockFailMode,
    String? errorMessage,
    String? infoMessage,
    bool clearInfo = false,
  }) {
    return UploadState(
      status: status ?? this.status,
      batches: batches ?? this.batches,
      activeBatchId: activeBatchId ?? this.activeBatchId,
      isMockFailMode: isMockFailMode ?? this.isMockFailMode,
      errorMessage: errorMessage ?? this.errorMessage,
      infoMessage: clearInfo ? null : (infoMessage ?? this.infoMessage),
    );
  }

  @override
  List<Object?> get props => [
        status,
        batches,
        activeBatchId,
        isMockFailMode,
        errorMessage,
        infoMessage,
      ];
}
