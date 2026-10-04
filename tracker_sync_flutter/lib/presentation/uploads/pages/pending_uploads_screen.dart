import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../bloc/sync_cubit.dart';
import '../bloc/sync_state.dart';
import '../bloc/upload_cubit.dart';
import '../bloc/upload_state.dart';
import '../widgets/batch_card_widget.dart';

class PendingUploadsScreen extends StatelessWidget {
  const PendingUploadsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Pending Uploads Queue', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            Text('Offline-First Resilient Sync Engine', style: TextStyle(fontSize: 12, color: Colors.grey)),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.add_a_photo_outlined),
            tooltip: 'New Batch',
            onPressed: () {
              context.read<UploadCubit>().createNewBatch();
            },
          ),
        ],
      ),
      body: Column(
        children: [
          // Sync Engine Status Banner
          BlocBuilder<SyncCubit, SyncState>(
            builder: (context, syncState) {
              return Container(
                color: syncState.isConnected
                    ? (syncState.status == SyncEngineStatus.syncing
                        ? Colors.blue.shade900
                        : Colors.green.shade900)
                    : Colors.red.shade900,
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                child: Row(
                  children: [
                    Icon(
                      syncState.isConnected
                          ? (syncState.status == SyncEngineStatus.syncing
                              ? Icons.sync
                              : Icons.wifi)
                          : Icons.wifi_off,
                      color: Colors.white,
                      size: 20,
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            syncState.isConnected
                                ? (syncState.status == SyncEngineStatus.syncing
                                    ? 'Auto-Syncing Pending Queue...'
                                    : 'Online • Sync Engine Ready')
                                : 'Offline • Images Saved Safely in Local Storage',
                            style: const TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.bold,
                              fontSize: 13,
                            ),
                          ),
                          if (syncState.lastSyncMessage != null)
                            Text(
                              syncState.lastSyncMessage!,
                              style: const TextStyle(color: Colors.white70, fontSize: 11),
                            ),
                        ],
                      ),
                    ),
                    if (syncState.status == SyncEngineStatus.syncing)
                      const SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    else
                      IconButton(
                        icon: const Icon(Icons.refresh, color: Colors.white, size: 20),
                        onPressed: () {
                          context.read<SyncCubit>().triggerSyncProcess();
                        },
                      ),
                  ],
                ),
              );
            },
          ),

          // Evaluator API Control Bar
          BlocBuilder<UploadCubit, UploadState>(
            builder: (context, uploadState) {
              return Card(
                margin: const EdgeInsets.all(12),
                color: Colors.grey.shade900,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                  side: const BorderSide(color: Colors.amberAccent, width: 1),
                ),
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  child: Row(
                    children: [
                      const Icon(Icons.science, color: Colors.amberAccent, size: 22),
                      const SizedBox(width: 12),
                      const Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Mock API Failure Simulation',
                              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                            ),
                            Text(
                              'Toggle to test upload errors vs success retries',
                              style: TextStyle(color: Colors.grey, fontSize: 11),
                            ),
                          ],
                        ),
                      ),
                      Switch(
                        value: uploadState.isMockFailMode,
                        activeColor: Colors.redAccent,
                        onChanged: (value) {
                          context.read<UploadCubit>().toggleMockFailMode(value);
                        },
                      ),
                    ],
                  ),
                ),
              );
            },
          ),

          // Batches Queue List
          Expanded(
            child: BlocBuilder<UploadCubit, UploadState>(
              builder: (context, state) {
                if (state.status == UploadUIStatus.loading) {
                  return const Center(child: CircularProgressIndicator());
                }

                if (state.batches.isEmpty) {
                  return const Center(
                    child: Text(
                      'No batches available. Capture photos in camera to begin.',
                      style: TextStyle(color: Colors.grey),
                    ),
                  );
                }

                return ListView.builder(
                  itemCount: state.batches.length,
                  itemBuilder: (context, index) {
                    final batch = state.batches[index];
                    final isSelected = batch.id == state.activeBatchId;

                    return BatchCardWidget(
                      batch: batch,
                      isSelected: isSelected,
                      onSelect: () {
                        context.read<UploadCubit>().selectActiveBatch(batch.id);
                      },
                      onDeleteImage: (imgId) {
                        context.read<UploadCubit>().deleteImage(imgId);
                      },
                      onDeleteBatch: (batchId) {
                        context.read<UploadCubit>().deleteBatch(batchId);
                      },
                    );
                  },
                );
              },
            ),
          ),
        ],
      ),
      bottomNavigationBar: Container(
        padding: const EdgeInsets.all(16),
        color: const Color(0xFF1E1E1E),
        child: SafeArea(
          child: ElevatedButton.icon(
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.amberAccent,
              foregroundColor: Colors.black,
              minimumSize: const Size.fromHeight(50),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            ),
            icon: const Icon(Icons.cloud_upload),
            label: const Text(
              'Sync Pending Queue Now',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
            onPressed: () {
              context.read<SyncCubit>().triggerSyncProcess();
            },
          ),
        ),
      ),
    );
  }
}
