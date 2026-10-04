import 'dart:async';
import 'dart:io';

abstract class UploadApi {
  Future<bool> uploadImage({
    required String imageId,
    required String batchId,
    required String filePath,
  });
}

class MockUploadApi implements UploadApi {
  bool shouldSimulateFailure = false;
  double failureRate = 0.0; // 0.0 to 1.0

  MockUploadApi({this.shouldSimulateFailure = false});

  @override
  Future<bool> uploadImage({
    required String imageId,
    required String batchId,
    required String filePath,
  }) async {
    // Simulate realistic network delay (800ms - 1500ms)
    await Future.delayed(const Duration(milliseconds: 1000));

    // Check if mock failure toggle is enabled
    if (shouldSimulateFailure) {
      throw Exception('Mock API Error: Simulated Server 503 / Network Timeout');
    }

    // Verify local image file exists
    final file = File(filePath);
    if (!await file.exists()) {
      throw Exception('Local File Error: Image file no longer exists at $filePath');
    }

    // Success response
    return true;
  }
}

/* 
===================================================================
PRODUCTION REAL API IMPLEMENTATION (PLACEHOLDER FOR REAL BACKEND)
===================================================================

import 'package:http/http.dart' as http;

class RealUploadApi implements UploadApi {
  final String baseUrl;
  final http.Client client;

  RealUploadApi({required this.baseUrl, required this.client});

  @override
  Future<bool> uploadImage({
    required String imageId,
    required String batchId,
    required String filePath,
  }) async {
    final uri = Uri.parse('$baseUrl/api/v1/batches/$batchId/images');
    final request = http.MultipartRequest('POST', uri);
    
    request.fields['imageId'] = imageId;
    request.fields['batchId'] = batchId;
    request.files.add(await http.MultipartFile.fromPath('image', filePath));

    final streamedResponse = await request.send().timeout(const Duration(seconds: 30));
    final response = await http.Response.fromStream(streamedResponse);

    if (response.statusCode == 200 || response.statusCode == 201) {
      return true;
    } else {
      throw Exception('HTTP ${response.statusCode}: ${response.body}');
    }
  }
}
*/
