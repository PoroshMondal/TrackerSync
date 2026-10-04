import 'package:path/path.dart';
import 'package:sqflite/sqflite.dart';
import '../../core/constants/app_constants.dart';
import '../models/batch_item_model.dart';
import '../models/image_item_model.dart';

class LocalDatabase {
  static final LocalDatabase instance = LocalDatabase._init();
  static Database? _database;

  LocalDatabase._init();

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDB('field_sync_camera.db');
    return _database!;
  }

  Future<Database> _initDB(String filePath) async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, filePath);

    return await openDatabase(
      path,
      version: 1,
      onCreate: _createDB,
    );
  }

  Future<void> _createDB(Database db, int version) async {
    await db.execute('''
      CREATE TABLE batches (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        createdAt TEXT NOT NULL
      )
    ''');

    await db.execute('''
      CREATE TABLE images (
        id TEXT PRIMARY KEY,
        batchId TEXT NOT NULL,
        filePath TEXT NOT NULL,
        status TEXT NOT NULL,
        errorMessage TEXT,
        createdAt TEXT NOT NULL,
        retryCount INTEGER NOT NULL,
        FOREIGN KEY (batchId) REFERENCES batches (id) ON DELETE CASCADE
      )
    ''');
  }

  // Batches
  Future<void> insertBatch(BatchItemModel batch) async {
    final db = await instance.database;
    await db.insert('batches', batch.toJson(),
        conflictAlgorithm: ConflictAlgorithm.replace);
  }

  Future<List<BatchItemModel>> getBatches() async {
    final db = await instance.database;
    final batchMaps = await db.query('batches', orderBy: 'createdAt DESC');
    
    List<BatchItemModel> batches = [];
    for (var bMap in batchMaps) {
      final batchId = bMap['id'] as String;
      final imageMaps = await db.query(
        'images',
        where: 'batchId = ?',
        whereArgs: [batchId],
        orderBy: 'createdAt ASC',
      );

      final images = imageMaps.map((i) => ImageItemModel.fromJson(i)).toList();
      batches.add(BatchItemModel.fromJson(bMap, images: images));
    }

    return batches;
  }

  Future<void> deleteBatch(String batchId) async {
    final db = await instance.database;
    await db.delete('images', where: 'batchId = ?', whereArgs: [batchId]);
    await db.delete('batches', where: 'id = ?', whereArgs: [batchId]);
  }

  // Images
  Future<void> insertImage(ImageItemModel image) async {
    final db = await instance.database;
    await db.insert('images', image.toJson(),
        conflictAlgorithm: ConflictAlgorithm.replace);
  }

  Future<void> updateImageStatus(
      String imageId, UploadStatus status, {String? errorMessage}) async {
    final db = await instance.database;
    final updates = <String, dynamic>{
      'status': status.name,
      'errorMessage': errorMessage,
    };
    if (status == UploadStatus.failed) {
      // Increment retryCount
      final current = await db.query('images',
          columns: ['retryCount'], where: 'id = ?', whereArgs: [imageId]);
      if (current.isNotEmpty) {
        final count = (current.first['retryCount'] as int? ?? 0) + 1;
        updates['retryCount'] = count;
      }
    }
    await db.update('images', updates, where: 'id = ?', whereArgs: [imageId]);
  }

  Future<List<ImageItemModel>> getPendingImages() async {
    final db = await instance.database;
    final maps = await db.query(
      'images',
      where: 'status = ? OR status = ?',
      whereArgs: [UploadStatus.pending.name, UploadStatus.failed.name],
    );
    return maps.map((i) => ImageItemModel.fromJson(i)).toList();
  }

  Future<void> deleteImage(String imageId) async {
    final db = await instance.database;
    await db.delete('images', where: 'id = ?', whereArgs: [imageId]);
  }
}
