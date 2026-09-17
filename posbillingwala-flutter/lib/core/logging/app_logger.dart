import 'package:flutter/foundation.dart';
import 'package:pos_billingwala_v2/core/constants/app_config.dart';

/* Gated by [AppConfig.enableLogging]. No-op when false. */
/* Console only — file logs for API/DB live in [FileLogStore]. */
abstract final class AppLogger {
  AppLogger._();

  static void info(String message, [Object? error, StackTrace? stackTrace]) {
    _log('INFO', message, error, stackTrace);
  }

  static void warning(String message, [Object? error, StackTrace? stackTrace]) {
    _log('WARN', message, error, stackTrace);
  }

  static void error(String message, [Object? error, StackTrace? stackTrace]) {
    _log('ERROR', message, error, stackTrace);
  }

  static void _log(
    String level,
    String message, [
    Object? error,
    StackTrace? stackTrace,
  ]) {
    if (!AppConfig.enableLogging) return;
    final buffer = StringBuffer('[$level] $message');
    if (error != null) buffer.write(' | $error');
    if (stackTrace != null) buffer.write('\n$stackTrace');
    debugPrint(buffer.toString());
  }
}
