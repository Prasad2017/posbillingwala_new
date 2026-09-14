import 'package:dio/dio.dart';
import 'package:pos_billingwala_v2/core/network/api_client.dart';
import 'package:pos_billingwala_v2/core/network/api_response.dart';
import 'package:pos_billingwala_v2/features/auth/data/device_identity_service.dart';
import 'package:pos_billingwala_v2/features/auth/domain/login_response.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';

class AuthApi {
  AuthApi(this.client);

  final ApiClient client;

  Future<LoginResponse> loginCheck({
    required String licenceKey,
    required String deviceId,
  }) {
    return postForm(
      ApiEndpoints.login,
      {
        'app_licence_key': licenceKey,
        ...DeviceApiFields.asForm(
          deviceId: deviceId,
          includeAndroidIdAlias: false,
          includeDeviceIdAlias: false,
          includeDeviceName: false,
        ),
      },
    );
  }

  Future<LoginResponse> updateLicenceKey({
    required String licenceKey,
    required String deviceId,
    required String deviceName,
  }) {
    return postForm(
      ApiEndpoints.updateAndroidKey,
      {
        'app_licence_key': licenceKey,
        ...DeviceApiFields.asForm(
          deviceId: deviceId,
          deviceName: deviceName,
          includeDeviceIdAlias: false,
        ),
      },
    );
  }

  Future<LoginResponse> checkLicenceExpire({
    required String userId,
    required String deviceId,
    required String deviceName,
  }) {
    return postForm(
      ApiEndpoints.checkLicenceExpire,
      {
        'userId': userId,
        ...DeviceApiFields.asForm(
          deviceId: deviceId,
          deviceName: deviceName,
          includeAndroidIdAlias: false,
          includeDeviceIdAlias: false,
        ),
      },
    );
  }

  Future<LoginResponse> loginMpin({
    required String mpin,
    required String licenceKey,
    required String deviceId,
    required String deviceName,
  }) {
    return postForm(
      ApiEndpoints.loginMpin,
      {
        'mpin': mpin,
        'app_licence_key': licenceKey,
        ...DeviceApiFields.asForm(
          deviceId: deviceId,
          deviceName: deviceName,
          includeDeviceIdAlias: false,
        ),
      },
    );
  }

  Future<LoginResponse> updateMpin({
    required String mpin,
    required String licenceKey,
    required String deviceId,
    required String deviceName,
  }) {
    return postForm(
      ApiEndpoints.updateMpin,
      {
        'mpin': mpin,
        'app_licence_key': licenceKey,
        ...DeviceApiFields.asForm(
          deviceId: deviceId,
          deviceName: deviceName,
          includeDeviceIdAlias: false,
        ),
      },
    );
  }

  Future<bool> serverLogout({required String licenceKey}) async {
    final response = await client.dio.get<dynamic>(
      ApiEndpoints.logOut,
      queryParameters: {'licenceKey': licenceKey},
      options: Options(responseType: ResponseType.json),
    );
    return isApiSuccess(asJsonMap(response.data));
  }

  /* Silent refresh — same fields as WithTable `AuthTokenRefresh`. */
  Future<({String token, String? expiresAt})?> refreshAuthToken({
    required String licenceKey,
    required String deviceId,
  }) async {
    final response = await client.dio.post<dynamic>(
      ApiEndpoints.refreshAuthToken,
      data: {
        'app_licence_key': licenceKey,
        'android_device_id': deviceId,
      },
      options: Options(contentType: Headers.formUrlEncodedContentType),
    );
    final data = asJsonMap(response.data);
    if (!isApiSuccess(data)) return null;
    final token = (data['authToken'] ?? '').toString().trim();
    if (token.isEmpty) return null;
    final expires = (data['tokenExpiresAt'] ?? '').toString();
    return (token: token, expiresAt: expires.isEmpty ? null : expires);
  }

  Future<TrialRegisterResponse> registerTrial({
    required String name,
    required String contactNumber,
    required String address,
    required String shopName,
  }) async {
    final response = await client.dio.post<Map<String, dynamic>>(
      ApiEndpoints.registerTrial,
      data: {
        'name': name,
        'contact_number': contactNumber,
        'address': address,
        'shopName': shopName,
      },
      options: Options(contentType: Headers.formUrlEncodedContentType),
    );
    return TrialRegisterResponse.fromJson(response.data ?? const {});
  }

  Future<LoginResponse> postForm(
    String path,
    Map<String, dynamic> fields,
  ) async {
    final response = await client.dio.post<dynamic>(
      path,
      data: fields,
      options: Options(contentType: Headers.formUrlEncodedContentType),
    );
    final data = response.data;
    if (data is! Map) {
      throw Exception('Unexpected response from $path');
    }
    return LoginResponse.fromJson(asJsonMap(data));
  }
}
