import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../domain/entities/batch_item.dart';
import '../../../domain/repositories/upload_repository.dart';
import 'upload_state.dart';

class UploadCubit extends Cubit<UploadState> {
  final UploadRepository repository;

  UploadCubit({required this.repository}) : super(const UploadState());

  Future<void> loadBatches() async {
    emit(state.copyWith(status: UploadUIStatus.loading));
    try {
      var batches = await repository.getAllBatches();

      // If no batch exists, create default Batch #1
      if (batches.isEmpty) {
        final defaultBatch = await repository.createBatch('Batch #1');
        batches = [defaultBatch];
      }

      final activeId = state.activeBatchId ?? batches.first.id;
      final mockFail = repository.isMockFailScenario;

      emit(state.copyWith(
        status: UploadUIStatus.loaded,
        batches: batches,
        activeBatchId: activeId,
        isMockFailMode: mockFail,
      ));
    } catch (e) {
      emit(state.copyWith(
        status: UploadUIStatus.error,
        errorMessage: 'Failed to load queue: ${e.toString()}',
      ));
    }
  }

  Future<void> createNewBatch([String? customName]) async {
    try {
      final name = customName ?? 'Batch #${state.batches.length + 1}';
      final newBatch = await repository.createBatch(name);
      await loadBatches();
      emit(state.copyWith(
        activeBatchId: newBatch.id,
        infoMessage: 'Created new $name',
      ));
    } catch (e) {
      emit(state.copyWith(errorMessage: 'Failed to create batch: $e'));
    }
  }

  void selectActiveBatch(String batchId) {
    emit(state.copyWith(activeBatchId: batchId));
  }

  Future<void> addPhotoToQueue(String filePath) async {
    try {
      var batchId = state.activeBatchId;
      if (batchId == null || state.batches.isEmpty) {
        final newBatch = await repository.createBatch('Batch #1');
        batchId = newBatch.id;
      }

      await repository.addImageToBatch(batchId, filePath);
      await loadBatches();
      emit(state.copyWith(infoMessage: 'Photo added to Pending Uploads queue'));
    } catch (e) {
      emit(state.copyWith(errorMessage: 'Failed to queue image: $e'));
    }
  }

  void toggleMockFailMode(bool shouldFail) {
    repository.setMockFailScenario(shouldFail);
    emit(state.copyWith(isMockFailMode: shouldFail));
  }

  Future<void> deleteImage(String imageId) async {
    try {
      await repository.deleteImage(imageId);
      await loadBatches();
    } catch (e) {
      emit(state.copyWith(errorMessage: 'Delete error: $e'));
    }
  }

  Future<void> deleteBatch(String batchId) async {
    try {
      await repository.deleteBatch(batchId);
      await loadBatches();
    } catch (e) {
      emit(state.copyWith(errorMessage: 'Delete batch error: $e'));
    }
  }

  void clearInfoMessage() {
    emit(state.copyWith(clearInfo: true));
  }
}
