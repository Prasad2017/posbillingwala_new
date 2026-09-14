import 'dart:async';

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/sync/domain/full_sync_controller.dart';

/// When the device comes back online, push pending offline rows (Android
/// OfflineToNetworkReceiver style).
class ConnectivitySyncListener {
  ConnectivitySyncListener(this.connectivitySyncListenerRef);

  final Ref connectivitySyncListenerRef;
  StreamSubscription<List<ConnectivityResult>>? sub;
  bool wasOffline = false;
  bool running = false;

  void start() {
    if (!AppPlatform.supportsOfflineSync) return;
    sub?.cancel();
    sub = Connectivity().onConnectivityChanged.listen((results) async {
      final online = results.any(
        (r) =>
            r == ConnectivityResult.mobile ||
            r == ConnectivityResult.wifi ||
            r == ConnectivityResult.ethernet ||
            r == ConnectivityResult.vpn ||
            r == ConnectivityResult.other,
      );
      if (!online) {
        wasOffline = true;
        return;
      }
      if (!wasOffline) return;
      wasOffline = false;
      await uploadIfLoggedIn();
    });

    // Seed current state.
    Connectivity().checkConnectivity().then((results) {
      final online = results.any(
        (r) =>
            r == ConnectivityResult.mobile ||
            r == ConnectivityResult.wifi ||
            r == ConnectivityResult.ethernet ||
            r == ConnectivityResult.vpn ||
            r == ConnectivityResult.other,
      );
      wasOffline = !online;
    });
  }

  Future<void> uploadIfLoggedIn() async {
    if (running) return;
    final userId = connectivitySyncListenerRef.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) return;
    running = true;
    try {
      debugPrint('Connectivity restored — uploading pending sync…');
      await connectivitySyncListenerRef.read(fullSyncControllerProvider.notifier).uploadAll();
    } catch (e) {
      debugPrint('Auto upload failed: $e');
    } finally {
      running = false;
    }
  }

  void dispose() {
    sub?.cancel();
    sub = null;
  }
}

final connectivitySyncListenerProvider = Provider<ConnectivitySyncListener>((
  ref,
) {
  final listener = ConnectivitySyncListener(ref);
  listener.start();
  ref.onDispose(listener.dispose);
  return listener;
});
