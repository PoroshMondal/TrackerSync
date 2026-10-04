import 'package:uuid/uuid.dart';
import '../../core/constants/app_constants.dart';
import '../../domain/entities/batch_item.dart';
import '../../domain/entities/image_item.dart';
import '../../domain/repositories/upload_repository.dart';
import '../datasources/local_database.dart';
import '../datasources/mock_upload_api.dart';
import '../models/batch_item_model.dart';
import '../models/image_item_model.dart';

class UploadRepositoryImpl implements UploadRepository {
  final LocalDatabase db;
  final MockUploadApi mockApi;
  final Uuid _uuid = const Uuid();

  UploadRepositoryImpl({
    required this.db,
    required this.mockApi,
  });

  @override
  bool get isMockFailScenario => mockApi.shouldSimulateFailure;

  @override
  void setMockFailScenario(bool shouldFail) {
    mockApi.shouldSimulateFailure = shouldFail;
  }

  @override
  Future<List<BatchItem>> getAllBatches() async {
    return await db.getBatches();
  }

  @override
  Future<BatchItem> createBatch(String name) async {
    final batch = BatchItemModel(
      id: _uuid.v4(),
      name: name,
      createdAt: DateTime.now(),
      images: const [],
    );
    await db.insertBatch(batch);
    return batch;
  }

  @override
  Future<ImageItem> addImageToBatch(String batchId, String filePath) async {
    final image = ImageItemModel(
      id: _uuid.v4(),
      batchId: batchId,
      filePath: filePath,
      status: UploadStatus.pending,
      createdAt: DateTime.now(),
      retryCount: 0,
    );
    await db.insertImage(image);
    return image;
  }

  @override
  Future<void> updateImageStatus(String imageId, String status,
      {String? errorMessage}) async {
    final uploadStatus = UploadStatus.values.firstWhere(
      (e) => e.name == status,
      orElse: () => UploadStatus.pending,
    );
    await db.updateImageStatus(imageId, uploadStatus,
        errorMessage: errorMessage);
  }

  @override
  Future<void> deleteImage(String imageId) async {
    await db.deleteImage(imageId);
  }

  @override
  Future<void> deleteBatch(String batchId) async {
    await db.deleteBatch(batchId);
  }

  @override
  Future<void> uploadSingleImage(ImageItem item) async {
    // 1. Mark status as uploading
    await db.updateImageStatus(item.id, UploadStatus.uploading);

    try {
      // 2. Call API
      await mockApi.uploadImage(
        imageId: item.id,
        batchId: item.batchId,
        filePath: item.filePath,
      );

      // 3. On success, mark uploaded
      await db.updateImageStatus(item.id, UploadStatus.uploaded);
    } catch (e) {
      // 4. On failure, mark failed with error message
      await db.updateImageStatus(
        item.id,
        UploadStatus.failed,
        errorMessage: e.toString().replaceAll('Exception: ', ''),
      );
      rethrow;
    }
  }

  @override
  Future<void> syncPendingQueue() async {
    final pendingImages = await db.getPendingImages();
    for (final item in pendingImages) {
      if (item.status == UploadStatus.uploaded) continue;
      try {
        await uploadSingleImage(item);
      } catch (_) {
        // Continue processing remaining items in queue
      }
    }
  }
}
