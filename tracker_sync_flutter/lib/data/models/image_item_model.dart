import '../../core/constants/app_constants.dart';
import '../../domain/entities/image_item.dart';

class ImageItemModel extends ImageItem {
  const ImageItemModel({
    required super.id,
    required super.batchId,
    required super.filePath,
    required super.status,
    super.errorMessage,
    required super.createdAt,
    super.retryCount,
  });

  factory ImageItemModel.fromJson(Map<String, dynamic> json) {
    return ImageItemModel(
      id: json['id'] as String,
      batchId: json['batchId'] as String,
      filePath: json['filePath'] as String,
      status: UploadStatus.values.firstWhere(
        (e) => e.name == json['status'],
        orElse: () => UploadStatus.pending,
      ),
      errorMessage: json['errorMessage'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
      retryCount: json['retryCount'] as int? ?? 0,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'batchId': batchId,
      'filePath': filePath,
      'status': status.name,
      'errorMessage': errorMessage,
      'createdAt': createdAt.toIso8601String(),
      'retryCount': retryCount,
    };
  }

  factory ImageItemModel.fromEntity(ImageItem entity) {
    return ImageItemModel(
      id: entity.id,
      batchId: entity.batchId,
      filePath: entity.filePath,
      status: entity.status,
      errorMessage: entity.errorMessage,
      createdAt: entity.createdAt,
      retryCount: entity.retryCount,
    );
  }
}
