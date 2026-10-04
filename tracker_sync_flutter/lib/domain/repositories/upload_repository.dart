import '../entities/batch_item.dart';
import '../entities/image_item.dart';

abstract class UploadRepository {
  Future<List<BatchItem>> getAllBatches();
  Future<BatchItem> createBatch(String name);
  Future<ImageItem> addImageToBatch(String batchId, String filePath);
  Future<void> updateImageStatus(String imageId, String status, {String? errorMessage});
  Future<void> deleteImage(String imageId);
  Future<void> deleteBatch(String batchId);
  Future<void> uploadSingleImage(ImageItem item);
  Future<void> syncPendingQueue();
  void setMockFailScenario(bool shouldFail);
  bool get isMockFailScenario;
}
