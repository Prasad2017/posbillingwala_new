import 'package:flutter/foundation.dart';

/// Central platform capability gates (POS upgrade plan §5.4).
///
/// - **Android / iOS:** online + offline. Local Drift always; when online also
///   push/pull API. Pending rows upload on reconnect.
/// - **Web:** online-only. Local Drift is a cache; auto-refresh pulls cloud
///   rows created on mobile devices.
abstract final class AppPlatform {
  AppPlatform._();

  static bool get isWeb => kIsWeb;

  /// Web must have network for auth, billing, masters, and sync.
  static bool get requiresNetwork => kIsWeb;

  /// Pending local rows + later upload (Android/iOS only).
  static bool get supportsOfflineBilling => !kIsWeb;

  /// Settings "Offline Data Synchronize…" and connectivity auto-upload.
  static bool get supportsOfflineSync => !kIsWeb;

  /// Web polls / resumes cloud download so Android/iOS entries appear.
  static bool get autoCloudRefresh => kIsWeb;

  static bool get supportsBluetoothPrint => !kIsWeb;

  static bool get supportsUsbPrint => true;

  static bool get supportsNetworkPrint => !kIsWeb;

  static bool get supportsFcm => !kIsWeb;
}
