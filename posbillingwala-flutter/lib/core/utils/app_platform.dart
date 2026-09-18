import 'package:flutter/foundation.dart';

/* Central platform capability gates (POS upgrade plan §5.4). */
/* */
/* - **Android / iOS:** offline-first. UI always reads local Drift; when online */
/*   auto-sync push/pull every 2 minutes (also reconnect, resume, after bill). */
/* - **Web:** online-only + API-first. Every write must reach the cloud API; */
/*   Drift is a short-lived cache refreshed from the server. */
abstract final class AppPlatform {
  AppPlatform._();

  static bool get isWeb => kIsWeb;

  /* Web must have network for auth, billing, masters, reports, and sync. */
  static bool get requiresNetwork => kIsWeb;

  /* Pending local rows + later upload (Android/iOS only). */
  static bool get supportsOfflineBilling => !kIsWeb;

  /* Settings manual sync + always-on auto-sync when internet is available. */
  static bool get supportsOfflineSync => !kIsWeb;

  /* Web polls / resumes: upload pending then download cloud rows. */
  static bool get autoCloudRefresh => kIsWeb;

  static bool get supportsBluetoothPrint => !kIsWeb;

  static bool get supportsUsbPrint => true;

  static bool get supportsNetworkPrint => !kIsWeb;

  static bool get supportsFcm => !kIsWeb;

  /* Persistent left-nav desktop chrome. Phone/tablet apps keep the mobile home. */
  static bool get useDesktopShell => kIsWeb;

  static bool get useMobileShell => !kIsWeb;
}
