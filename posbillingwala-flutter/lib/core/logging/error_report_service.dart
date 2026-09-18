import 'dart:async';
import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';
import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
import 'package:pos_billingwala_v2/core/logging/file_log_store.dart';
import 'package:pos_billingwala_v2/core/logging/screen_context.dart';

/* Posts API / crash events to Admin [reportErrorLog.php] + local file logs. */
abstract final class ErrorReportService {
  ErrorReportService._();

  static Dio? _dio;
  static String? _userId;
  static String? _shopName;
  static String? _deviceId;
  static String? _deviceName;

  static void bindClient(Dio dio) {
    _dio = dio;
  }

  static void setSession({
    String? userId,
    String? shopName,
    String? deviceId,
    String? deviceName,
  }) {
    _userId = userId;
    _shopName = shopName;
    _deviceId = deviceId;
    _deviceName = deviceName;
  }

  static bool _isReportEndpoint(String path) {
    return path.contains(ApiEndpoints.reportErrorLog);
  }

  static Future<void> reportApiFailure({
    required String method,
    required String apiUrl,
    int? httpStatus,
    Object? requestBody,
    Object? responseBody,
    String? errorMessage,
    String? screenName,
  }) async {
    final summary =
        (errorMessage ?? 'API failed').trim().isEmpty
            ? 'API failed'
            : errorMessage!.trim();
    AppLogger.warning('API FAIL $method $apiUrl · $summary');
    FileLogStore.logApi(
      method: method,
      api: apiUrl,
      request: requestBody,
      response: responseBody,
      statusCode: httpStatus,
      error: summary,
      screenName: screenName,
    );
    await _post(
      errorType: 'API',
      severity: 'ERROR',
      category: 'sync_api',
      summary: summary.length > 200 ? summary.substring(0, 200) : summary,
      whatHappened: summary,
      apiMethod: method,
      apiUrl: apiUrl,
      httpStatus: httpStatus,
      requestBody: requestBody,
      responseBody: responseBody,
      screenName: screenName,
    );
  }

  static Future<void> reportCrash({
    required Object error,
    StackTrace? stackTrace,
    String? screenName,
  }) async {
    final message = error.toString();
    AppLogger.error('CRASH $message', error, stackTrace);
    FileLogStore.logApi(
      method: 'CRASH',
      api: 'flutter',
      request: {
        'screen': screenName ?? ScreenContext.screenName,
        'stack': stackTrace?.toString(),
      },
      response: null,
      error: message,
      screenName: screenName,
    );
    await _post(
      errorType: 'CRASH',
      severity: 'CRITICAL',
      category: 'flutter_crash',
      summary: message.length > 200 ? message.substring(0, 200) : message,
      whatHappened: message,
      requestBody: stackTrace?.toString(),
      screenName: screenName,
    );
  }

  static Future<void> _post({
    required String errorType,
    required String severity,
    required String category,
    required String summary,
    required String whatHappened,
    String? apiMethod,
    String? apiUrl,
    int? httpStatus,
    Object? requestBody,
    Object? responseBody,
    String? screenName,
  }) async {
    final dio = _dio;
    if (dio == null || kIsWeb) return;
    if (apiUrl != null && _isReportEndpoint(apiUrl)) return;
    try {
      await dio.post<dynamic>(
        ApiEndpoints.reportErrorLog,
        data: {
          'userId': _userId ?? '',
          'error_type': errorType,
          'severity': severity,
          'error_category': category,
          'summary': summary,
          'app_type': 'flutter_pos',
          'customer_id': _userId ?? '',
          'shop_name': _shopName ?? '',
          'device_id': _deviceId ?? '',
          'device_name': _deviceName ?? '',
          'screen_name': screenName ?? ScreenContext.screenName,
          'what_happened': whatHappened,
          'original_error_message': whatHappened,
          'error_message': whatHappened,
          'api_method': apiMethod ?? '',
          'api_url': apiUrl ?? '',
          'http_status': httpStatus?.toString() ?? '',
          'request_body': _clip(requestBody),
          'response_body': _clip(responseBody),
        },
        options: Options(
          contentType: Headers.formUrlEncodedContentType,
          responseType: ResponseType.json,
          extra: const {'skipErrorReport': true},
        ),
      );
    } catch (e) {
      debugPrint('[ErrorReportService] report failed: $e');
    }
  }

  static String _clip(Object? value) {
    if (value == null) return '';
    final text = value is String ? value : jsonEncode(value);
    if (text.length <= 8000) return text;
    return '${text.substring(0, 8000)}…';
  }
}

void installFlutterCrashReporting() {
  FlutterError.onError = (details) {
    FlutterError.presentError(details);
    unawaited(
      ErrorReportService.reportCrash(
        error: details.exception,
        stackTrace: details.stack,
        screenName: ScreenContext.screenName,
      ),
    );
  };
  PlatformDispatcher.instance.onError = (error, stack) {
    unawaited(
      ErrorReportService.reportCrash(
        error: error,
        stackTrace: stack,
        screenName: ScreenContext.screenName,
      ),
    );
    return true;
  };
}
