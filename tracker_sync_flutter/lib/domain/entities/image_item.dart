import 'package:equatable/equatable.dart';
import '../../core/constants/app_constants.dart';

class ImageItem extends Equatable {
  final String id;
  final String batchId;
  final String filePath;
  final UploadStatus status;
  final String? errorMessage;
  final DateTime createdAt;
  final int retryCount;

  const ImageItem({
    required this.id,
    required this.batchId,
    required this.filePath,
    required this.status,
    this.errorMessage,
    required this.createdAt,
    this.retryCount = 0,
  });

  ImageItem copyWith({
    String? id,
    String? batchId,
    String? filePath,
    UploadStatus? status,
    String? errorMessage,
    DateTime? createdAt,
    int? retryCount,
  }) {
    return ImageItem(
      id: id ?? this.id,
      batchId: batchId ?? this.batchId,
      filePath: filePath ?? this.filePath,
      status: status ?? this.status,
      errorMessage: errorMessage ?? this.errorMessage,
      createdAt: createdAt ?? this.createdAt,
      retryCount: retryCount ?? this.retryCount,
    );
  }

  @override
  List<Object?> get props => [
        id,
        batchId,
        filePath,
        status,
        errorMessage,
        createdAt,
        retryCount,
      ];
}
