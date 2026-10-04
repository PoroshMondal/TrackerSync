import 'dart:io';
import 'package:flutter/material.dart';
import '../../../core/constants/app_constants.dart';
import '../../../domain/entities/batch_item.dart';
import '../../../domain/entities/image_item.dart';

class BatchCardWidget extends StatelessWidget {
  final BatchItem batch;
  final bool isSelected;
  final VoidCallback onSelect;
  final Function(String) onDeleteImage;
  final Function(String) onDeleteBatch;

  const BatchCardWidget({
    super.key,
    required this.batch,
    required this.isSelected,
    required this.onSelect,
    required this.onDeleteImage,
    required this.onDeleteBatch,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(
          color: isSelected ? Colors.amberAccent : Colors.transparent,
          width: 2,
        ),
      ),
      child: ExpansionTile(
        initiallyExpanded: isSelected,
        title: Row(
          children: [
            Text(
              batch.name,
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
            const SizedBox(width: 8),
            _buildBatchBadge(),
          ],
        ),
        subtitle: Text(
          'Total: ${batch.images.length} images | ${batch.pendingCount} pending',
          style: TextStyle(color: Colors.grey.shade400, fontSize: 12),
        ),
        trailing: IconButton(
          icon: const Icon(Icons.delete_outline, color: Colors.redAccent),
          onPressed: () => onDeleteBatch(batch.id),
        ),
        children: [
          if (batch.images.isEmpty)
            const Padding(
              padding: EdgeInsets.all(16.0),
              child: Text(
                'No images in this batch yet. Use camera to capture.',
                style: TextStyle(color: Colors.grey),
              ),
            )
          else
            ListView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: batch.images.length,
              itemBuilder: (context, index) {
                final img = batch.images[index];
                return _ImageTile(
                  image: img,
                  onDelete: () => onDeleteImage(img.id),
                );
              },
            ),
        ],
      ),
    );
  }

  Widget _buildBatchBadge() {
    if (batch.isFullyUploaded) {
      return Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
        decoration: BoxDecoration(
          color: Colors.green.withOpacity(0.2),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: Colors.green),
        ),
        child: const Text(
          'All Uploaded',
          style: TextStyle(color: Colors.green, fontSize: 10, fontWeight: FontWeight.bold),
        ),
      );
    }
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: Colors.orange.withOpacity(0.2),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.orange),
      ),
      child: Text(
        '${batch.pendingCount} Pending',
        style: const TextStyle(color: Colors.orange, fontSize: 10, fontWeight: FontWeight.bold),
      ),
    );
  }
}

class _ImageTile extends StatelessWidget {
  final ImageItem image;
  final VoidCallback onDelete;

  const _ImageTile({required this.image, required this.onDelete});

  @override
  Widget build(BuildContext context) {
    final file = File(image.filePath);

    return ListTile(
      leading: ClipRRect(
        borderRadius: BorderRadius.circular(8),
        child: file.existsSync()
            ? Image.file(file, width: 50, height: 50, fit: BoxFit.cover)
            : Container(
                width: 50,
                height: 50,
                color: Colors.grey,
                child: const Icon(Icons.broken_image),
              ),
      ),
      title: Text(
        image.filePath.split('/').last,
        style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500),
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
      ),
      subtitle: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              _statusIcon(image.status),
              const SizedBox(width: 4),
              Text(
                image.status.displayName,
                style: TextStyle(
                  color: _statusColor(image.status),
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                ),
              ),
              if (image.retryCount > 0)
                Text(
                  ' (${image.retryCount} retries)',
                  style: const TextStyle(color: Colors.grey, fontSize: 10),
                ),
            ],
          ),
          if (image.errorMessage != null && image.status == UploadStatus.failed)
            Text(
              image.errorMessage!,
              style: const TextStyle(color: Colors.redAccent, fontSize: 11),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
        ],
      ),
      trailing: IconButton(
        icon: const Icon(Icons.close, size: 18, color: Colors.grey),
        onPressed: onDelete,
      ),
    );
  }

  Widget _statusIcon(UploadStatus status) {
    switch (status) {
      case UploadStatus.pending:
        return const Icon(Icons.schedule, size: 14, color: Colors.orange);
      case UploadStatus.uploading:
        return const SizedBox(
          width: 12,
          height: 12,
          child: CircularProgressIndicator(strokeWidth: 2, color: Colors.blue),
        );
      case UploadStatus.uploaded:
        return const Icon(Icons.check_circle, size: 14, color: Colors.green);
      case UploadStatus.failed:
        return const Icon(Icons.error_outline, size: 14, color: Colors.redAccent);
    }
  }

  Color _statusColor(UploadStatus status) {
    switch (status) {
      case UploadStatus.pending:
        return Colors.orange;
      case UploadStatus.uploading:
        return Colors.blue;
      case UploadStatus.uploaded:
        return Colors.green;
      case UploadStatus.failed:
        return Colors.redAccent;
    }
  }
}
