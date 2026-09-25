import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:crypto/crypto.dart';
import 'package:dio/dio.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_crashlytics/firebase_crashlytics.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:path_provider/path_provider.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';
import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
import 'package:pos_billingwala_v2/core/logging/file_log_store.dart';
import 'package:pos_billingwala_v2/core/logging/screen_context.dart';

/* Posts API / crash / ANR events to Admin [reportErrorLog.php] + Crashlytics. */
abstract final class ErrorReportService {
  ErrorReportService._();

  static const _processExitChannel = MethodChannel('pos_billingwala/process_exit');

  static Dio? _dio;
  static String? _userId;
  static String? _shopName;
  static String? _deviceId;
  static String? _deviceName;
  static String? _userLabel;
  static String? _branchLabel;
  static String? _appVersion;
  static bool _crashlyticsReady = false;
  static bool _flushing = false;
  static Directory? _queueDir;

  static void bindClient(Dio dio) {
    _dio = dio;
    unawaited(flushQueue());
  }

  static void setSession({
    String? userId,
    String? shopName,
    String? deviceId,
    String? deviceName,
    String? userLabel,
    String? branchLabel,
  }) {
    _userId = userId;
    _shopName = shopName;
    _deviceId = deviceId;
    _deviceName = deviceName;
    _userLabel = userLabel;
    _branchLabel = branchLabel;
    if (_crashlyticsReady) {
      unawaited(_setCrashlyticsKeys());
    }
  }

  static Future<void> bootstrap() async {
    installFlutterCrashReporting();
    if (kIsWeb) return;
    try {
      await Firebase.initializeApp();
      FlutterError.onError = (details) {
        FlutterError.presentError(details);
        FirebaseCrashlytics.instance.recordFlutterFatalError(details);
        unawaited(
          reportCrash(
            error: details.exception,
            stackTrace: details.stack,
            screenName: ScreenContext.screenName,
            fatal: true,
          ),
        );
      };
      PlatformDispatcher.instance.onError = (error, stack) {
        FirebaseCrashlytics.instance.recordError(error, stack, fatal: true);
        unawaited(
          reportCrash(
            error: error,
            stackTrace: stack,
            screenName: ScreenContext.screenName,
            fatal: true,
          ),
        );
        return true;
      };
      await FirebaseCrashlytics.instance.setCrashlyticsCollectionEnabled(true);
      _crashlyticsReady = true;
      await _setCrashlyticsKeys();
    } catch (e) {
      AppLogger.warning('Crashlytics init skipped/failed', e);
    }
    try {
      final info = await PackageInfo.fromPlatform();
      _appVersion = '${info.version}+${info.buildNumber}';
    } catch (_) {}
    await _ensureQueueDir();
    unawaited(collectProcessExits());
    unawaited(flushQueue());
  }

  static Future<void> _setCrashlyticsKeys() async {
    if (!_crashlyticsReady) return;
    try {
      final crashlytics = FirebaseCrashlytics.instance;
      await crashlytics.setUserIdentifier(_userId ?? '');
      await crashlytics.setCustomKey('shop_name', _shopName ?? '');
      await crashlytics.setCustomKey('device_id', _deviceId ?? '');
      await crashlytics.setCustomKey('device_name', _deviceName ?? '');
      await crashlytics.setCustomKey('app_type', 'flutter_pos');
      if (_appVersion != null) {
        await crashlytics.setCustomKey('app_version', _appVersion!);
      }
    } catch (_) {}
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
    int? durationMs,
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
    if (_crashlyticsReady) {
      try {
        await FirebaseCrashlytics.instance.recordError(
          Exception('API $method $apiUrl · $summary'),
          StackTrace.current,
          reason: 'api_failure',
          fatal: false,
        );
      } catch (_) {}
    }
    await _enqueueOrPost({
      'error_type': 'API',
      'severity': 'ERROR',
      'error_category': 'sync_api',
      'summary': _clipStr(summary, 200),
      'what_happened': summary,
      'api_method': method,
      'api_url': apiUrl,
      'http_status': httpStatus?.toString() ?? '',
      'request_body': _clip(requestBody),
      'response_body': _clip(responseBody),
      'original_api_response': _clip(responseBody),
      'request_duration_ms': durationMs?.toString() ?? '',
      'screen_name': screenName ?? ScreenContext.screenName,
      'original_error_message': summary,
      'error_message': summary,
    });
  }

  static Future<void> reportCrash({
    required Object error,
    StackTrace? stackTrace,
    String? screenName,
    bool fatal = true,
  }) async {
    final message = error.toString();
    final stack = stackTrace?.toString() ?? '';
    final exceptionClass = error.runtimeType.toString();
    AppLogger.error('CRASH $message', error, stackTrace);
    FileLogStore.logApi(
      method: 'CRASH',
      api: 'flutter',
      request: {
        'screen': screenName ?? ScreenContext.screenName,
        'stack': stack,
        'class': exceptionClass,
      },
      response: null,
      error: message,
      screenName: screenName,
    );
    if (_crashlyticsReady && !fatal) {
      try {
        await FirebaseCrashlytics.instance.recordError(
          error,
          stackTrace,
          fatal: false,
        );
      } catch (_) {}
    }
    await _enqueueOrPost({
      'error_type': 'CRASH',
      'severity': 'CRITICAL',
      'error_category': 'flutter_crash',
      'summary': _clipStr(message, 200),
      'what_happened': message,
      'screen_name': screenName ?? ScreenContext.screenName,
      'original_error_message': message,
      'error_message': message,
      'original_exception_class': exceptionClass,
      'original_stack_trace': _clipStr(stack, 20000),
      'fingerprint': _fingerprint('CRASH|$exceptionClass|$message'),
      'request_body': _clipStr(stack, 8000),
    });
  }

  static Future<void> reportProcessExit({
    required String errorType,
    required String category,
    required String exceptionClass,
    required String description,
    required String stackTrace,
    required String severity,
    int? timestampMs,
    int? pid,
    int? reasonCode,
  }) async {
    final summary = description.trim().isEmpty
        ? errorType
        : _clipStr(description.trim(), 200);
    AppLogger.warning('PROCESS EXIT $errorType · $summary');
    if (_crashlyticsReady) {
      try {
        await FirebaseCrashlytics.instance.recordError(
          Exception('$errorType: $summary'),
          StackTrace.fromString(stackTrace),
          reason: category,
          fatal: errorType == 'ANR' || errorType == 'CRASH' || errorType == 'NATIVE_CRASH',
        );
      } catch (_) {}
    }
    await _enqueueOrPost({
      'error_type': errorType,
      'severity': severity,
      'error_category': category,
      'summary': summary,
      'what_happened': description,
      'original_error_message': description,
      'error_message': description,
      'original_exception_class': exceptionClass,
      'original_stack_trace': _clipStr(stackTrace, 20000),
      'fingerprint': _fingerprint(
        '$errorType|$category|${timestampMs ?? 0}|${pid ?? 0}|$reasonCode',
      ),
      'activity_name': 'ApplicationExitInfo',
      'user_action': 'process_exit',
      'breadcrumbs': jsonEncode({
        'timestamp_ms': timestampMs,
        'pid': pid,
        'reason_code': reasonCode,
      }),
    });
  }

  static Future<void> collectProcessExits() async {
    if (kIsWeb || !Platform.isAndroid) return;
    try {
      final raw = await _processExitChannel.invokeMethod<dynamic>(
        'collectProcessExits',
      );
      if (raw is! List) return;
      for (final item in raw) {
        if (item is! Map) continue;
        final map = Map<String, dynamic>.from(item);
        await reportProcessExit(
          errorType: map['errorType']?.toString() ?? 'CRASH',
          category: map['category']?.toString() ?? 'process_exit',
          exceptionClass: map['exceptionClass']?.toString() ?? 'ProcessExit',
          description: map['description']?.toString() ?? '',
          stackTrace: map['stackTrace']?.toString() ?? '',
          severity: map['severity']?.toString() ?? 'CRITICAL',
          timestampMs: int.tryParse(map['timestampMs']?.toString() ?? ''),
          pid: int.tryParse(map['pid']?.toString() ?? ''),
          reasonCode: int.tryParse(map['reasonCode']?.toString() ?? ''),
        );
      }
    } catch (e) {
      debugPrint('[ErrorReportService] process exit collect failed: $e');
    }
  }

  static Future<void> _enqueueOrPost(Map<String, dynamic> fields) async {
    final payload = {
      ...fields,
      'userId': _userId ?? '',
      'app_type': 'flutter_pos',
      'app_version': _appVersion ?? '',
      'customer_id': _userId ?? '',
      'shop_name': _shopName ?? '',
      'branch_label': _branchLabel ?? '',
      'device_id': _deviceId ?? '',
      'device_name': _deviceName ?? '',
      'user_label': _userLabel ?? '',
      'screen_name':
          fields['screen_name']?.toString() ?? ScreenContext.screenName,
    };
    final posted = await _post(payload);
    if (!posted) {
      await _enqueue(payload);
    }
  }

  static Future<bool> _post(Map<String, dynamic> fields) async {
    final dio = _dio;
    if (dio == null || kIsWeb) return false;
    final apiUrl = fields['api_url']?.toString() ?? '';
    if (apiUrl.isNotEmpty && _isReportEndpoint(apiUrl)) return true;
    try {
      await dio.post<dynamic>(
        ApiEndpoints.reportErrorLog,
        data: fields,
        options: Options(
          contentType: Headers.formUrlEncodedContentType,
          responseType: ResponseType.json,
          extra: const {'skipErrorReport': true},
        ),
      );
      return true;
    } catch (e) {
      debugPrint('[ErrorReportService] report failed: $e');
      return false;
    }
  }

  static Future<void> _ensureQueueDir() async {
    if (_queueDir != null || kIsWeb) return;
    try {
      final root = await getApplicationSupportDirectory();
      final dir = Directory('${root.path}/error_log_queue');
      if (!await dir.exists()) await dir.create(recursive: true);
      _queueDir = dir;
    } catch (e) {
      debugPrint('[ErrorReportService] queue dir failed: $e');
    }
  }

  static Future<void> _enqueue(Map<String, dynamic> fields) async {
    if (kIsWeb) return;
    await _ensureQueueDir();
    final dir = _queueDir;
    if (dir == null) return;
    try {
      final name =
          '${DateTime.now().millisecondsSinceEpoch}_${identityHashCode(fields)}.json';
      final file = File('${dir.path}/$name');
      await file.writeAsString(jsonEncode(fields));
    } catch (e) {
      debugPrint('[ErrorReportService] enqueue failed: $e');
    }
  }

  static Future<void> flushQueue() async {
    if (_flushing || kIsWeb || _dio == null) return;
    _flushing = true;
    try {
      await _ensureQueueDir();
      final dir = _queueDir;
      if (dir == null || !await dir.exists()) return;
      final files = <File>[];
      await for (final entity in dir.list()) {
        if (entity is File && entity.path.endsWith('.json')) {
          files.add(entity);
        }
      }
      files.sort((a, b) => a.path.compareTo(b.path));
      for (final file in files.take(40)) {
        try {
          final raw = await file.readAsString();
          final map = jsonDecode(raw);
          if (map is! Map) {
            await file.delete();
            continue;
          }
          final ok = await _post(Map<String, dynamic>.from(map));
          if (ok) await file.delete();
        } catch (_) {}
      }
    } finally {
      _flushing = false;
    }
  }

  static String _fingerprint(String seed) {
    return sha256.convert(utf8.encode(seed)).toString().substring(0, 32);
  }

  static String _clip(Object? value) {
    if (value == null) return '';
    final text = value is String ? value : jsonEncode(value);
    return _clipStr(text, 8000);
  }

  static String _clipStr(String text, int max) {
    if (text.length <= max) return text;
    return '${text.substring(0, max)}…';
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
