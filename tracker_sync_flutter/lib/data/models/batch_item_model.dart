import '../../domain/entities/batch_item.dart';
import 'image_item_model.dart';

class BatchItemModel extends BatchItem {
  const BatchItemModel({
    required super.id,
    required super.name,
    required super.createdAt,
    super.images,
  });

  factory BatchItemModel.fromJson(
    Map<String, dynamic> json, {
    List<ImageItemModel> images = const [],
  }) {
    return BatchItemModel(
      id: json['id'] as String,
      name: json['name'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
      images: images,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'createdAt': createdAt.toIso8601String(),
    };
  }

  factory BatchItemModel.fromEntity(BatchItem entity) {
    return BatchItemModel(
      id: entity.id,
      name: entity.name,
      createdAt: entity.createdAt,
      images: entity.images.map((e) => ImageItemModel.fromEntity(e)).toList(),
    );
  }
}
