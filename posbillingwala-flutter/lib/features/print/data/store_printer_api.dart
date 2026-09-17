import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';
import 'package:pos_billingwala_v2/core/network/api_client.dart';
import 'package:pos_billingwala_v2/core/network/api_response.dart';
import 'package:pos_billingwala_v2/features/print/domain/store_printer.dart';

class StorePrinterApi {
  StorePrinterApi(this.client);
  final ApiClient client;

  Future<Map<String, dynamic>> post(String path, Map<String, dynamic> fields) async {
    final response = await client.dio.post<dynamic>(
      path,
      data: fields,
      options: Options(contentType: Headers.formUrlEncodedContentType),
    );
    return asJsonMap(response.data);
  }

  Future<List<StorePrinter>> list(String userId) async {
    final data = await post(ApiEndpoints.getStorePrinterList, {'userId': userId});
    return mapJsonList(data['printerResponse'], StorePrinter.fromJson);
  }

  Future<StorePrinter> save(String userId, Map<String, dynamic> fields, {String? id}) async {
    fields['userId'] = userId;
    if (id != null) fields['id'] = id;
    final path = id == null ? ApiEndpoints.insertStorePrinter : ApiEndpoints.updateStorePrinter;
    final data = await post(path, fields);
    if (!isApiSuccess(data) || data['printer'] is! Map) {
      throw Exception(data['message']?.toString() ?? 'Unable to save printer');
    }
    return StorePrinter.fromJson(Map<String, dynamic>.from(data['printer'] as Map));
  }

  Future<void> disable(String userId, String id) async {
    final data = await post(ApiEndpoints.disableStorePrinter, {'userId': userId, 'id': id});
    if (!isApiSuccess(data)) {
      throw Exception(data['message']?.toString() ?? 'Unable to disable printer');
    }
  }

  Future<List<PrinterRouteRule>> routes(String userId) async {
    final data = await post(ApiEndpoints.getPrinterRouteList, {'userId': userId});
    return mapJsonList(data['routeResponse'], PrinterRouteRule.fromJson);
  }

  Future<void> saveRoutes(String userId, List<PrinterRouteRule> routes) async {
    final data = await post(ApiEndpoints.savePrinterRoutes, {
      'userId': userId,
      'routes': jsonEncode(routes.map((e) => e.toJson()).toList()),
    });
    if (!isApiSuccess(data)) {
      throw Exception(data['message']?.toString() ?? 'Unable to save routing');
    }
  }

  Future<Map<String, dynamic>> createJob(String userId, Map<String, dynamic> fields) {
    fields['userId'] = userId;
    return post(ApiEndpoints.createPrintJob, fields);
  }

  Future<List<Map<String, dynamic>>> claim(String userId, String deviceId) async {
    final data = await post(ApiEndpoints.claimPrintJobs, {
      'userId': userId,
      'android_device_id': deviceId,
    });
    final raw = data['printJobResponse'];
    if (raw is! List) return const [];
    return raw.whereType<Map>().map((e) => Map<String, dynamic>.from(e)).toList();
  }

  Future<void> ack(String userId, String id, String status, {String error = ''}) async {
    await post(ApiEndpoints.acknowledgePrintJob, {
      'userId': userId,
      'id': id,
      'ackStatus': status,
      'errorMessage': error,
    });
  }

  Future<List<Map<String, dynamic>>> queue(String userId) async {
    final data = await post(ApiEndpoints.getPrintJobList, {'userId': userId});
    final raw = data['printJobResponse'];
    if (raw is! List) return const [];
    return raw.whereType<Map>().map((e) => Map<String, dynamic>.from(e)).toList();
  }

  Future<void> retry(String userId, String id) async {
    await post(ApiEndpoints.retryPrintJob, {'userId': userId, 'id': id});
  }

  Future<void> heartbeat(String userId, String deviceId, String platform) {
    return post(ApiEndpoints.registerPrintHost, {
      'userId': userId,
      'android_device_id': deviceId,
      'platform': platform,
    });
  }
}
