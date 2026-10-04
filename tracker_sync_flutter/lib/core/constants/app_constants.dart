enum UploadStatus {
  pending,
  uploading,
  uploaded,
  failed,
}

extension UploadStatusX on UploadStatus {
  String get displayName {
    switch (this) {
      case UploadStatus.pending:
        return 'Pending';
      case UploadStatus.uploading:
        return 'Uploading...';
      case UploadStatus.uploaded:
        return 'Uploaded';
      case UploadStatus.failed:
        return 'Failed';
    }
  }
}
