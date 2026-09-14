import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';

/* Returns true when the device reports a usable network interface. */
Future<bool> isDeviceOnline() async {
  final results = await Connectivity().checkConnectivity();
  return results.any(
    (r) =>
        r == ConnectivityResult.mobile ||
        r == ConnectivityResult.wifi ||
        r == ConnectivityResult.ethernet ||
        r == ConnectivityResult.vpn ||
        r == ConnectivityResult.other,
  );
}

/* On web ([AppPlatform.requiresNetwork]), blocks when offline. */
/* On mobile, always allows (offline-first) unless [force] is true. */
Future<bool> ensureOnline({bool force = false}) async {
  if (!force && !AppPlatform.requiresNetwork) return true;
  return isDeviceOnline();
}

/* Throws when web (or [force]) has no network. Mobile offline-first skips. */
Future<void> requireOnlineForWeb({bool force = false}) async {
  if (!await ensureOnline(force: force)) {
    throw StateError(kOnlineRequiredMessage);
  }
}

const kOnlineRequiredMessage =
    'Internet connection required. Web POS works online only.';

const kWebApiSaveFailedMessage =
    'Cloud save failed. Check internet and try again.';
