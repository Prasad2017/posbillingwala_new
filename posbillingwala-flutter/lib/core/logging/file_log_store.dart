import 'dart:convert';
import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:intl/intl.dart';
import 'package:path_provider/path_provider.dart';
import 'package:pos_billingwala_v2/core/constants/app_config.dart';
import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
import 'package:pos_billingwala_v2/core/logging/screen_context.dart';

/* Writes API + DB logs under Documents/Pos Billingwala/Logs. */
abstract final class FileLogStore {
  FileLogStore._();

  static const String folderBrand = 'Pos Billingwala';
  static const String folderLogs = 'Logs';
  static const int maxBodyChars = 8000;

  static Directory? _logsDir;
  static Future<void>? _initFuture;
  static final List<Future<void> Function()> _writeQueue = [];
  static bool _draining = false;

  static Directory? get logsDirectory => _logsDir;

  static Future<void> init() async {
    if (!AppConfig.enableLogging || kIsWeb) return;
    _initFuture ??= _resolveLogsDir();
    await _initFuture;
  }

  static Future<void> _resolveLogsDir() async {
    try {
      final dir = await _preferredLogsDirectory();
      if (!await dir.exists()) {
        await dir.create(recursive: true);
      }
      _logsDir = dir;
      await _appendRaw(
        'session',
        '===== Log session started '
            '${DateFormat('yyyy-MM-dd HH:mm:ss').format(DateTime.now())} =====\n'
            'Logs folder: ${dir.path}\n',
      );
      AppLogger.info('File logs → ${dir.path}');
    } catch (e, st) {
      debugPrint('[FileLogStore] init failed: $e\n$st');
      _logsDir = null;
    }
  }

  static Future<Directory> _preferredLogsDirectory() async {
    /* Desktop: user Documents/Pos Billingwala/Logs */
    if (Platform.isWindows || Platform.isLinux || Platform.isMacOS) {
      final home = Platform.isWindows
          ? Platform.environment['USERPROFILE']
          : Platform.environment['HOME'];
      if (home != null && home.isNotEmpty) {
        return Directory(
          '$home${Platform.pathSeparator}Documents'
          '${Platform.pathSeparator}$folderBrand'
          '${Platform.pathSeparator}$folderLogs',
        );
      }
    }

    /* Android: prefer public / shared Documents when writable. */
    if (Platform.isAndroid) {
      try {
        final external = await getExternalStorageDirectories(
          type: StorageDirectory.documents,
        );
        if (external != null && external.isNotEmpty) {
          return Directory(
            '${external.first.path}${Platform.pathSeparator}$folderBrand'
            '${Platform.pathSeparator}$folderLogs',
          );
        }
      } catch (_) {}
      /* Common public Documents path on many devices. */
      final publicDocs = Directory(
        '/storage/emulated/0/Documents/$folderBrand/$folderLogs',
      );
      try {
        if (!await publicDocs.exists()) {
          await publicDocs.create(recursive: true);
        }
        final probe = File('${publicDocs.path}/.write_probe');
        await probe.writeAsString('ok');
        await probe.delete();
        return publicDocs;
      } catch (_) {}
    }

    /* iOS / Android fallback: app documents. */
    final appDocs = await getApplicationDocumentsDirectory();
    return Directory(
      '${appDocs.path}${Platform.pathSeparator}$folderBrand'
      '${Platform.pathSeparator}$folderLogs',
    );
  }

  /* Screen -> API, request, response (or error). */
  static void logApi({
    required String method,
    required String api,
    required Object? request,
    Object? response,
    int? statusCode,
    String? error,
    String? screenName,
  }) {
    if (!AppConfig.enableLogging || kIsWeb) return;
    final screen = (screenName ?? ScreenContext.screenName).trim();
    final stamp = DateFormat('yyyy-MM-dd HH:mm:ss.SSS').format(DateTime.now());
    final buf = StringBuffer()
      ..writeln('[$stamp]')
      ..writeln('Screen: ${screen.isEmpty ? 'App' : screen}')
      ..writeln('API: $method $api');
    if (statusCode != null) {
      buf.writeln('Status: $statusCode');
    }
    buf
      ..writeln('Request:')
      ..writeln(_pretty(request))
      ..writeln('Response:');
    if (error != null && error.isNotEmpty) {
      buf.writeln('ERROR: $error');
      if (response != null) {
        buf.writeln(_pretty(response));
      }
    } else {
      buf.writeln(_pretty(response));
    }
    buf.writeln('${'-' * 60}\n');
    _enqueue('api', buf.toString());
  }

  static void logDbQuery({
    required String kind,
    required String sql,
    List<Object?> args = const [],
    Object? resultSummary,
    String? error,
    String? screenName,
  }) {
    if (!AppConfig.enableLogging || kIsWeb) return;
    final screen = (screenName ?? ScreenContext.screenName).trim();
    final stamp = DateFormat('yyyy-MM-dd HH:mm:ss.SSS').format(DateTime.now());
    final buf = StringBuffer()
      ..writeln('[$stamp]')
      ..writeln('Screen: ${screen.isEmpty ? 'App' : screen}')
      ..writeln('DB: $kind')
      ..writeln('SQL:')
      ..writeln(sql.trim())
      ..writeln('Args: ${_pretty(args)}');
    if (resultSummary != null) {
      buf.writeln('Result: $resultSummary');
    }
    if (error != null && error.isNotEmpty) {
      buf.writeln('ERROR: $error');
    }
    buf.writeln('${'-' * 60}\n');
    _enqueue('db', buf.toString());
  }

  static void _enqueue(String kind, String text) {
    if (_writeQueue.length > 80) {
      _writeQueue.removeRange(0, _writeQueue.length - 80);
    }
    _writeQueue.add(() => _appendRaw(kind, text));
    _drain();
  }

  static Future<void> _drain() async {
    if (_draining) return;
    _draining = true;
    while (_writeQueue.isNotEmpty) {
      final next = _writeQueue.removeAt(0);
      try {
        await next();
      } catch (e) {
        debugPrint('[FileLogStore] write failed: $e');
      }
    }
    _draining = false;
  }

  static Future<void> _appendRaw(String kind, String text) async {
    await init();
    final dir = _logsDir;
    if (dir == null) return;
    final day = DateFormat('yyyy-MM-dd').format(DateTime.now());
    final file = File('${dir.path}${Platform.pathSeparator}${kind}_$day.log');
    await file.writeAsString(text, mode: FileMode.append);
  }

  static String _pretty(Object? value) {
    if (value == null) return '(null)';
    try {
      if (value is FormData) {
        final fields = <String, dynamic>{};
        for (final e in value.fields) {
          fields[e.key] = e.value;
        }
        for (final f in value.files) {
          fields[f.key] = '(file:${f.value.filename ?? 'unknown'})';
        }
        return _pretty(fields);
      }
      final sanitized = sanitizeForLog(value);
      final encoded = const JsonEncoder.withIndent('  ').convert(sanitized);
      if (encoded.length <= maxBodyChars) return encoded;
      return '${encoded.substring(0, maxBodyChars)}\n... (truncated)';
    } catch (_) {
      final raw = value.toString();
      if (raw.length <= maxBodyChars) return raw;
      return '${raw.substring(0, maxBodyChars)}\n... (truncated)';
    }
  }

  static Object? sanitizeForLog(Object? value) {
    const sensitive = {
      'password',
      'mpin',
      'pin',
      'new_mpin',
      'old_mpin',
      'auth_token',
      'token',
      'refresh_token',
      'authorization',
    };
    if (value is Map) {
      return value.map((key, val) {
        final k = key.toString();
        if (sensitive.contains(k.toLowerCase())) {
          return MapEntry(k, '***');
        }
        return MapEntry(k, sanitizeForLog(val));
      });
    }
    if (value is List) {
      return value.map(sanitizeForLog).toList();
    }
    if (value is String) {
      final trimmed = value.trim();
      if ((trimmed.startsWith('{') && trimmed.endsWith('}')) ||
          (trimmed.startsWith('[') && trimmed.endsWith(']'))) {
        try {
          return sanitizeForLog(jsonDecode(trimmed));
        } catch (_) {}
      }
    }
    return value;
  }
}
