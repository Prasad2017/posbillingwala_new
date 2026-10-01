import 'package:flutter/foundation.dart';

/* Build-time feature flags. Set each to true or false, then rebuild. */
abstract final class AppConfig {
  AppConfig._();

  /* true = Google Play in-app updates on; false = off. */
  static const bool enableInAppUpdate = true;

  /* Play update style when [enableInAppUpdate] is true:
   * - 'immediate' = force update (blocking Play dialog)
   * - 'flexible'  = background download, restart when ready
   * Override: `--dart-define=IN_APP_UPDATE_MODE=flexible|immediate` */
  static const String inAppUpdateModeOverride = 'immediate';

  static String get inAppUpdateMode {
    const hasDartDefine = bool.hasEnvironment('IN_APP_UPDATE_MODE');
    if (hasDartDefine) {
      return const String.fromEnvironment('IN_APP_UPDATE_MODE');
    }
    return inAppUpdateModeOverride;
  }

  /* true when mode is immediate (force). Flexible otherwise. */
  static bool get preferImmediateInAppUpdate {
    final mode = inAppUpdateMode.trim().toLowerCase();
    return mode == 'immediate' || mode == 'force' || mode == '1';
  }

  /* true = screenshots allowed; false = block capture (FLAG_SECURE). */
  static const bool allowScreenshot = true;

  /* true = force logging on; false = force off.
   * Or use `--dart-define=ENABLE_LOGGING=true|false` to override at build time. */
  static const bool enableLoggingOverride = true;

  /* Console + Documents/Pos Billingwala/Logs.
   * Full SQL/API bodies jank the POS — keep off in release unless needed. */
  static bool get enableLogging {
    const hasDartDefine = bool.hasEnvironment('ENABLE_LOGGING');
    if (hasDartDefine) {
      return bool.fromEnvironment('ENABLE_LOGGING');
    }
    return enableLoggingOverride;
  }

  /* true when logging is on in debug: pretty-print API bodies + every SQL. */
  static bool get enableVerboseIoLogging => enableLogging && kDebugMode;
}
