import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';
import 'package:pos_billingwala_v2/core/network/api_client.dart';
import 'package:pos_billingwala_v2/core/network/api_response.dart';
import 'package:pos_billingwala_v2/features/staff/domain/staff_user.dart';

class StaffApi {
  StaffApi(this.client);

  final ApiClient client;

  Future<Map<String, dynamic>> post(
    String path,
    Map<String, dynamic> fields,
  ) async {
    final response = await client.dio.post<dynamic>(
      path,
      data: fields,
      options: Options(contentType: Headers.formUrlEncodedContentType),
    );
    return asJsonMap(response.data);
  }

  Future<
      ({
        StaffUser staff,
        String permissionVersion,
        String sessionId,
        bool coldLogin,
        Map<String, dynamic> raw,
      })> login({
    String? userId,
    required String mobileNumber,
    required String pin,
    required String deviceId,
    String? deviceName,
  }) async {
    final fields = <String, dynamic>{
      'mobileNumber': mobileNumber,
      'appLoginPin': pin,
      'android_device_id': deviceId,
      if (userId != null && userId.isNotEmpty) 'userId': userId,
      if (deviceName != null && deviceName.isNotEmpty)
        'android_device_name': deviceName,
    };
    final data = await post(ApiEndpoints.staffLogin, fields);
    if (!isApiSuccess(data) || data['staff'] is! Map) {
      throw Exception(data['message']?.toString() ?? 'Staff login failed');
    }
    return (
      staff: StaffUser.fromJson(Map<String, dynamic>.from(data['staff'] as Map)),
      permissionVersion: data['permissionVersion']?.toString() ?? '1',
      sessionId: data['staffSessionId']?.toString() ?? '',
      coldLogin: data['coldLogin']?.toString() == '1' ||
          (userId == null || userId.isEmpty),
      raw: data,
    );
  }

  Future<List<StaffUser>> list(String userId) async {
    final data = await post(ApiEndpoints.getStaffList, {'userId': userId});
    return mapJsonList(data['staffResponse'], StaffUser.fromJson);
  }

  Future<StaffUser> get(String userId, String id) async {
    final data = await post(ApiEndpoints.getStaff, {'userId': userId, 'id': id});
    if (data['staff'] is! Map) {
      throw Exception(data['message']?.toString() ?? 'User not found');
    }
    return StaffUser.fromJson(Map<String, dynamic>.from(data['staff'] as Map));
  }

  Future<Map<String, dynamic>> roleDefaults(String userId, String role) {
    return post(ApiEndpoints.getRoleDefaults, {'userId': userId, 'role': role});
  }

  Future<StaffUser> create({
    required String userId,
    required String name,
    required String mobileNumber,
    required String role,
    required String pin,
    required String confirmPin,
    String address = '',
    Map<String, String> overrides = const {},
  }) async {
    final data = await post(ApiEndpoints.insertStaff, {
      'userId': userId,
      'name': name,
      'mobileNumber': mobileNumber,
      'role': role,
      'appLoginPin': pin,
      'confirmPin': confirmPin,
      'address': address,
      'permissionOverrides': jsonEncode(overrides),
    });
    if (!isApiSuccess(data) || data['staff'] is! Map) {
      throw Exception(data['message']?.toString() ?? 'Unable to save user');
    }
    return StaffUser.fromJson(Map<String, dynamic>.from(data['staff'] as Map));
  }

  Future<StaffUser> update({
    required String userId,
    required String id,
    required String name,
    required String mobileNumber,
    String address = '',
    String status = 'ACTIVE',
    Map<String, String>? overrides,
  }) async {
    final fields = <String, dynamic>{
      'userId': userId,
      'id': id,
      'name': name,
      'mobileNumber': mobileNumber,
      'address': address,
      'status': status,
    };
    if (overrides != null) {
      fields['permissionOverrides'] = jsonEncode(overrides);
    }
    final data = await post(ApiEndpoints.updateStaff, fields);
    if (!isApiSuccess(data) || data['staff'] is! Map) {
      throw Exception(data['message']?.toString() ?? 'Unable to update user');
    }
    return StaffUser.fromJson(Map<String, dynamic>.from(data['staff'] as Map));
  }

  Future<void> deactivate(String userId, String id) async {
    final data = await post(ApiEndpoints.deactivateStaff, {
      'userId': userId,
      'id': id,
    });
    if (!isApiSuccess(data)) {
      throw Exception(data['message']?.toString() ?? 'Unable to deactivate');
    }
  }

  Future<void> changeRole(String userId, String id, String role) async {
    final data = await post(ApiEndpoints.changeStaffRole, {
      'userId': userId,
      'id': id,
      'role': role,
    });
    if (!isApiSuccess(data)) {
      throw Exception(data['message']?.toString() ?? 'Unable to change role');
    }
  }

  Future<void> resetPin({
    required String userId,
    required String id,
    required String pin,
    required String confirmPin,
  }) async {
    final data = await post(ApiEndpoints.resetStaffPin, {
      'userId': userId,
      'id': id,
      'appLoginPin': pin,
      'confirmPin': confirmPin,
    });
    if (!isApiSuccess(data)) {
      throw Exception(data['message']?.toString() ?? 'Unable to reset PIN');
    }
  }

  Future<StaffUser> effective(String userId, String staffId) async {
    final data = await post(ApiEndpoints.getEffectivePermissions, {
      'userId': userId,
      'staffId': staffId,
    });
    if (data['staff'] is Map) {
      return StaffUser.fromJson(Map<String, dynamic>.from(data['staff'] as Map));
    }
    throw Exception(data['message']?.toString() ?? 'Unable to load permissions');
  }
}
