import 'package:equatable/equatable.dart';
import '../../core/constants/app_constants.dart';
import 'image_item.dart';

class BatchItem extends Equatable {
  final String id;
  final String name;
  final DateTime createdAt;
  final List<ImageItem> images;

  const BatchItem({
    required this.id,
    required this.name,
    required this.createdAt,
    this.images = const [],
  });

  BatchItem copyWith({
    String? id,
    String? name,
    DateTime? createdAt,
    List<ImageItem>? images,
  }) {
    return BatchItem(
      id: id ?? this.id,
      name: name ?? this.name,
      createdAt: createdAt ?? this.createdAt,
      images: images ?? this.images,
    );
  }

  int get pendingCount =>
      images.where((img) => img.status != UploadStatus.uploaded).length;

  bool get isFullyUploaded =>
      images.isNotEmpty && images.every((img) => img.status == UploadStatus.uploaded);

  @override
  List<Object?> get props => [id, name, createdAt, images];
}
