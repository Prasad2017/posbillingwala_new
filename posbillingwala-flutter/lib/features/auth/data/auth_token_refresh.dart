import 'package:dio/dio.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';
import 'package:pos_billingwala_v2/core/network/api_response.dart';
import 'package:pos_billingwala_v2/features/auth/data/device_identity_service.dart';
import 'package:pos_billingwala_v2/features/auth/data/session_store.dart';
import 'package:pos_billingwala_v2/features/auth/domain/session_keys.dart';
import 'package:shared_preferences/shared_preferences.dart';

/* Silent POS token refresh for licence-bound devices (mirrors WithTable */
/* `AuthTokenRefresh`). Uses licence key + device id — no MPIN prompt. */
class AuthTokenRefresh {
  AuthTokenRefresh._();

  static DateTime? lastAttempt;
  static Future<bool>? inFlight;

  /* Returns true when a fresh Bearer token was saved. */
  static Future<bool> tryRefresh({
    void Function(String? token)? onTokenSaved,
  }) {
    return synchronized(() async {
      final now = DateTime.now();
      if (lastAttempt != null &&
          now.difference(lastAttempt!) < const Duration(seconds: 5)) {
        final prefs = await SharedPreferences.getInstance();
        final existing = prefs.getString(SessionKeys.authToken);
        return existing != null && existing.isNotEmpty;
      }
      lastAttempt = now;

      final prefs = await SharedPreferences.getInstance();
      final licenceKey = (prefs.getString(SessionKeys.licenceKey) ?? '').trim();
      if (licenceKey.isEmpty) return false;

      final device = await DeviceIdentityService().resolve();
      final deviceId = device.deviceId.trim();
      if (deviceId.isEmpty) return false;

      final oldToken = prefs.getString(SessionKeys.authToken);

      try {
        /* Bare client — no auth interceptor — avoids refresh recursion. */
        final dio = Dio(
          BaseOptions(
            baseUrl: ApiConstants.baseUrl,
            connectTimeout: const Duration(seconds: 30),
            receiveTimeout: const Duration(seconds: 30),
            headers: {
              'Accept': 'application/json',
              if (oldToken != null && oldToken.isNotEmpty)
                'Authorization': 'Bearer $oldToken',
            },
            validateStatus: (status) =>
                status != null && status >= 200 && status < 500,
          ),
        );

        final response = await dio.post<dynamic>(
          ApiEndpoints.refreshAuthToken,
          data: {
            'app_licence_key': licenceKey,
            'android_device_id': deviceId,
          },
          options: Options(contentType: Headers.formUrlEncodedContentType),
        );

        final data = asJsonMap(response.data);
        if (!isApiSuccess(data)) return false;

        final token = (data['authToken'] ?? '').toString().trim();
        if (token.isEmpty) return false;
        final expires = (data['tokenExpiresAt'] ?? '').toString();

        await SessionStore().saveAuthToken(token, expiresAt: expires);
        onTokenSaved?.call(token);
        return true;
      } catch (_) {
        return false;
      }
    });
  }

  static Future<bool> synchronized(Future<bool> Function() action) {
    if (inFlight != null) return inFlight!;
    final future = action().whenComplete(() => inFlight = null);
    inFlight = future;
    return future;
  }
}
