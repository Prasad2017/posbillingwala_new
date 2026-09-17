import 'package:flutter/foundation.dart';

/* Build-time feature flags. Flip here, then rebuild. */
abstract final class AppConfig {
  AppConfig._();

  /* Google Play in-app updates (Android). */
  static const bool enableInAppUpdate = true;

  /* true = screenshots allowed; false = block capture (FLAG_SECURE). */
  static const bool allowScreenshot = true;

  /* Console + Documents/Pos Billingwala/Logs. Off in release unless */
  /* `--dart-define=ENABLE_LOGGING=true` (full SQL/API bodies jank the POS). */
  static bool get enableLogging {
    const override = bool.fromEnvironment('ENABLE_LOGGING');
    if (bool.hasEnvironment('ENABLE_LOGGING')) return override;
    return kDebugMode;
  }

  /* Pretty-print full API bodies and every SQL statement (debug only). */
  static bool get enableVerboseIoLogging => enableLogging && kDebugMode;
}
