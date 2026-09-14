import 'dart:async';

import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/sync/domain/full_sync_controller.dart';

/* Web-only: periodically push pending local rows then pull cloud data so */
/* Android/iOS entries appear without a manual "Refresh from Cloud". */
/* */
/* Uses [FullSyncController.syncEverything] (upload + non-destructive download). */
class WebCloudRefreshListener {
  WebCloudRefreshListener(this.webCloudRefreshListenerRef);

  final Ref webCloudRefreshListenerRef;
  AppLifecycleListener? lifecycle;
  Timer? timer;
  bool running = false;
  DateTime? lastRefreshAt;

  static const pollInterval = Duration(seconds: 45);
  static const minRefreshGap = Duration(seconds: 20);

  void start() {
    if (!AppPlatform.requiresNetwork) return;
    lifecycle?.dispose();
    lifecycle = AppLifecycleListener(onResume: () => unawaited(refresh()));
    timer?.cancel();
    timer = Timer.periodic(pollInterval, (_) => unawaited(refresh()));
    unawaited(refresh());
  }

  Future<void> refresh({bool force = false}) async {
    if (!AppPlatform.requiresNetwork) return;
    if (running) return;

    final auth = webCloudRefreshListenerRef.read(authControllerProvider);
    if (auth.status != AuthStatus.authenticated) return;
    final userId = auth.session?.userId;
    if (userId == null || userId.isEmpty) return;

    if (!force &&
        lastRefreshAt != null &&
        DateTime.now().difference(lastRefreshAt!) < minRefreshGap) {
      return;
    }

    if (!await ensureOnline()) return;

    running = true;
    try {
      /* API-first web: flush pending writes, then refresh cache from cloud. */
      await webCloudRefreshListenerRef
          .read(fullSyncControllerProvider.notifier)
          .syncEverythingSilent();
      lastRefreshAt = DateTime.now();
    } catch (e, st) {
      debugPrint('Web cloud refresh failed: $e\n$st');
    } finally {
      running = false;
    }
  }

  void dispose() {
    timer?.cancel();
    timer = null;
    lifecycle?.dispose();
    lifecycle = null;
  }
}

final webCloudRefreshListenerProvider = Provider<WebCloudRefreshListener>((
  ref,
) {
  final listener = WebCloudRefreshListener(ref);
  if (AppPlatform.requiresNetwork) {
    listener.start();
    ref.listen<AuthState>(authControllerProvider, (prev, next) {
      if (next.status == AuthStatus.authenticated &&
          prev?.status != AuthStatus.authenticated) {
        unawaited(listener.refresh(force: true));
      }
    });
  }
  ref.onDispose(listener.dispose);
  return listener;
});
