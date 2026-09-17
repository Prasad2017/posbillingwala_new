import 'dart:async';

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/pos/domain/payment_checkout_controller.dart';
import 'package:pos_billingwala_v2/features/sync/domain/auto_sync_status.dart';
import 'package:pos_billingwala_v2/features/sync/domain/full_sync_controller.dart';
import 'package:pos_billingwala_v2/features/sync/domain/sync_progress.dart';
import 'package:pos_billingwala_v2/features/sync/domain/sync_providers.dart';

/* Android / iOS: always-on auto-sync of ALL data when internet is available. */
/* */
/* Triggers: reconnect, app resume, login, after a bill, periodic while online. */
/* Every run: upload pending then non-destructive download of masters, bills, */
/* mess, dining, inventory, expenses, company/printer. Never wipe-and-fetch. */
class ConnectivitySyncListener {
  ConnectivitySyncListener(this.connectivitySyncListenerRef);

  final Ref connectivitySyncListenerRef;
  StreamSubscription<List<ConnectivityResult>>? sub;
  AppLifecycleListener? lifecycle;
  Timer? pollTimer;
  Timer? retryTimer;
  bool wasOffline = false;
  bool running = false;
  DateTime? lastSyncAt;

  static const pollInterval = Duration(minutes: 3);
  static const minSyncGap = Duration(seconds: 20);
  static const checkoutRetryDelay = Duration(seconds: 8);

  AutoSyncStatusController get status =>
      connectivitySyncListenerRef.read(autoSyncStatusProvider.notifier);

  void start() {
    if (!AppPlatform.supportsOfflineSync) return;
    sub?.cancel();
    sub = Connectivity().onConnectivityChanged.listen((results) {
      final online = connectivityResultsOnline(results);
      status.setOnline(online);
      if (!online) {
        wasOffline = true;
        return;
      }
      final reconnect = wasOffline;
      wasOffline = false;
      unawaited(
        syncNow(
          force: reconnect,
          reason: reconnect ? 'reconnect' : 'online',
        ),
      );
    });

    Connectivity().checkConnectivity().then((results) {
      final online = connectivityResultsOnline(results);
      status.setOnline(online);
      wasOffline = !online;
      if (online) {
        Future<void>.delayed(const Duration(seconds: 2), () {
          unawaited(syncNow(force: true, reason: 'start'));
        });
      }
    });

    lifecycle?.dispose();
    lifecycle = AppLifecycleListener(
      onResume: () => unawaited(syncNow(reason: 'resume')),
    );

    pollTimer?.cancel();
    pollTimer = Timer.periodic(pollInterval, (_) {
      unawaited(syncNow(reason: 'periodic'));
    });
  }

  Future<void> refreshPendingCount() async {
    if (!AppPlatform.supportsOfflineSync) return;
    try {
      final snap = await connectivitySyncListenerRef
          .read(appDatabaseProvider)
          .getSyncPendingSnapshot();
      status.setPendingCount(snap.total);
    } catch (_) {
      /* Keep last known count. */
    }
  }

  Future<void> syncNow({
    bool force = false,
    String reason = '',
  }) async {
    if (!AppPlatform.supportsOfflineSync) return;
    if (running) return;

    final auth = connectivitySyncListenerRef.read(authControllerProvider);
    if (auth.status != AuthStatus.authenticated) return;
    final userId = auth.session?.userId;
    if (userId == null || userId.isEmpty) return;

    if (connectivitySyncListenerRef.read(paymentCheckoutControllerProvider).busy) {
      retryTimer?.cancel();
      retryTimer = Timer(checkoutRetryDelay, () {
        unawaited(syncNow(force: force, reason: 'after-checkout'));
      });
      return;
    }

    if (connectivitySyncListenerRef.read(syncProgressProvider).isRunning) {
      return;
    }

    if (!await isDeviceOnline()) {
      status.setOnline(false);
      wasOffline = true;
      await refreshPendingCount();
      return;
    }
    status.setOnline(true);

    if (!force &&
        lastSyncAt != null &&
        DateTime.now().difference(lastSyncAt!) < minSyncGap) {
      return;
    }

    running = true;
    status.setSyncing(true);
    try {
      AppLogger.info(
        'Auto-sync ($reason) — all data upload then merge-download',
      );
      final result = await connectivitySyncListenerRef
          .read(fullSyncControllerProvider.notifier)
          .syncEverythingSilent();
      lastSyncAt = DateTime.now();
      final snap = await connectivitySyncListenerRef
          .read(appDatabaseProvider)
          .getSyncPendingSnapshot();
      if (result.failed > 0) {
        status.setError(result.message, pendingCount: snap.total);
        AppLogger.warning(
          'Auto-sync finished with issues: ${result.message}',
        );
      } else {
        status.setSuccess(pendingCount: snap.total);
      }
    } catch (e) {
      AppLogger.error('Auto-sync failed', e);
      int pending =
          connectivitySyncListenerRef.read(autoSyncStatusProvider).pendingCount;
      try {
        pending = (await connectivitySyncListenerRef
                .read(appDatabaseProvider)
                .getSyncPendingSnapshot())
            .total;
      } catch (_) {}
      status.setError('$e', pendingCount: pending);
    } finally {
      running = false;
    }
  }

  void dispose() {
    sub?.cancel();
    sub = null;
    pollTimer?.cancel();
    pollTimer = null;
    retryTimer?.cancel();
    retryTimer = null;
    lifecycle?.dispose();
    lifecycle = null;
  }
}

final connectivitySyncListenerProvider = Provider<ConnectivitySyncListener>((
  ref,
) {
  final listener = ConnectivitySyncListener(ref);
  if (AppPlatform.supportsOfflineSync) {
    listener.start();
    ref.listen<AuthState>(authControllerProvider, (prev, next) {
      if (next.status == AuthStatus.authenticated &&
          prev?.status != AuthStatus.authenticated) {
        unawaited(listener.syncNow(force: true, reason: 'login'));
      }
    });
    ref.listen(pendingInvoicesProvider, (_, next) {
      next.whenData((_) {
        unawaited(listener.refreshPendingCount());
      });
    });
    ref.listen(syncPendingSnapshotProvider, (_, next) {
      next.whenData((snap) {
        listener.status.setPendingCount(snap.total);
      });
    });
    unawaited(listener.refreshPendingCount());
  }
  ref.onDispose(listener.dispose);
  return listener;
});
