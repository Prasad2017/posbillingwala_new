import 'package:flutter/foundation.dart';

/* Build-time feature flags. Set each to true or false, then rebuild. */
abstract final class AppConfig {
  AppConfig._();

  /* true = Google Play in-app updates on; false = off. */
  static const bool enableInAppUpdate = false;

  /* true = screenshots allowed; false = block capture (FLAG_SECURE). */
  static const bool allowScreenshot = true;

  /* true = force logging on; false = force off.
   * Omit both and logging follows debug/release (see getter below).
   * Or use `--dart-define=ENABLE_LOGGING=true|false`. */
  static const bool? enableLoggingOverride = true;

  /* Console + Documents/Pos Billingwala/Logs.
   * Full SQL/API bodies jank the POS — keep off in release unless needed. */
  static bool get enableLogging {
    const hasDartDefine = bool.hasEnvironment('ENABLE_LOGGING');
    if (hasDartDefine) {
      return bool.fromEnvironment('ENABLE_LOGGING');
    }
    final override = enableLoggingOverride;
    if (override != null) return override;
    return kDebugMode;
  }

  /* true when logging is on in debug: pretty-print API bodies + every SQL. */
  static bool get enableVerboseIoLogging => enableLogging && kDebugMode;
}
