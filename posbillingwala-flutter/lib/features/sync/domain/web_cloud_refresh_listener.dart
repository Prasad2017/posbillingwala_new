import 'dart:async';

import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/sync/domain/full_sync_controller.dart';

/// Web-only: periodically pull cloud data so rows created on Android/iOS
/// appear without a manual "Refresh from Cloud".
///
/// Uses non-destructive [FullSyncController.downloadAll] (no local wipe).
class WebCloudRefreshListener {
  WebCloudRefreshListener(this._ref);

  final Ref _ref;
  AppLifecycleListener? _lifecycle;
  Timer? _timer;
  bool _running = false;
  DateTime? _lastRefreshAt;

  static const pollInterval = Duration(seconds: 45);
  static const minRefreshGap = Duration(seconds: 20);

  void start() {
    if (!AppPlatform.requiresNetwork) return;
    _lifecycle?.dispose();
    _lifecycle = AppLifecycleListener(onResume: () => unawaited(refresh()));
    _timer?.cancel();
    _timer = Timer.periodic(pollInterval, (_) => unawaited(refresh()));
    unawaited(refresh());
  }

  Future<void> refresh({bool force = false}) async {
    if (!AppPlatform.requiresNetwork) return;
    if (_running) return;

    final auth = _ref.read(authControllerProvider);
    if (auth.status != AuthStatus.authenticated) return;
    final userId = auth.session?.userId;
    if (userId == null || userId.isEmpty) return;

    if (!force &&
        _lastRefreshAt != null &&
        DateTime.now().difference(_lastRefreshAt!) < minRefreshGap) {
      return;
    }

    if (!await ensureOnline()) return;

    _running = true;
    try {
      await _ref
          .read(fullSyncControllerProvider.notifier)
          .downloadAll(silent: true);
      _lastRefreshAt = DateTime.now();
    } catch (e, st) {
      debugPrint('Web cloud refresh failed: $e\n$st');
    } finally {
      _running = false;
    }
  }

  void dispose() {
    _timer?.cancel();
    _timer = null;
    _lifecycle?.dispose();
    _lifecycle = null;
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
