import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';

final deviceOnlineProvider = StreamProvider<bool>((ref) async* {
  yield await isDeviceOnline();
  yield* Connectivity().onConnectivityChanged.map(connectivityResultsOnline);
});

bool connectivityResultsOnline(List<ConnectivityResult> results) {
  return results.any(
    (r) =>
        r == ConnectivityResult.mobile ||
        r == ConnectivityResult.wifi ||
        r == ConnectivityResult.ethernet ||
        r == ConnectivityResult.vpn ||
        r == ConnectivityResult.other,
  );
}

/* Returns true when the device reports a usable network interface. */
Future<bool> isDeviceOnline() async {
  return connectivityResultsOnline(await Connectivity().checkConnectivity());
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
