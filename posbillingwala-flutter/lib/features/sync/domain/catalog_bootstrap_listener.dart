import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';

/// If local catalog was wiped by a wrong-id sync (licenceId instead of
/// ownerId), pull masters once after login so Categories/Products appear
/// without a manual Fetch Data.
class CatalogBootstrapListener {
  CatalogBootstrapListener(this._ref);

  final Ref _ref;
  bool _running = false;
  bool _attemptedThisSession = false;

  void onAuthenticated() {
    unawaited(ensureCatalogIfEmpty());
  }

  Future<void> ensureCatalogIfEmpty({bool force = false}) async {
    if (_running) return;
    if (_attemptedThisSession && !force) return;

    final auth = _ref.read(authControllerProvider);
    if (auth.status != AuthStatus.authenticated) return;
    final session = auth.session;
    if (session == null) return;
    final ownerId = session.catalogOwnerId.trim();
    if (ownerId.isEmpty) return;

    final db = _ref.read(appDatabaseProvider);
    final categories = await db.countActiveCategories();
    final products = await db.countActiveProducts();
    if (categories > 0 && products > 0) {
      _attemptedThisSession = true;
      return;
    }

    if (!await ensureOnline()) return;

    _running = true;
    _attemptedThisSession = true;
    try {
      debugPrint(
        'Catalog empty (cats=$categories products=$products) — '
        'syncing with ownerId=$ownerId licenceId=${session.licenceUserId}',
      );
      final result = await _ref.read(mastersRepositoryProvider).syncFromCloud(
            ownerId: ownerId,
            licenceUserId: session.licenceUserId,
          );
      debugPrint(
        'Catalog bootstrap done: cats=${result.categoryCount} '
        'products=${result.productCount} portions=${result.portionCount} '
        'tables=${result.tableCount}',
      );
    } catch (e, st) {
      debugPrint('Catalog bootstrap failed: $e\n$st');
      // Allow retry on next Home open if this attempt failed.
      _attemptedThisSession = false;
    } finally {
      _running = false;
    }
  }
}

final catalogBootstrapListenerProvider =
    Provider<CatalogBootstrapListener>((ref) {
  final listener = CatalogBootstrapListener(ref);
  ref.listen<AuthState>(authControllerProvider, (prev, next) {
    if (next.status == AuthStatus.authenticated &&
        prev?.status != AuthStatus.authenticated) {
      listener.onAuthenticated();
    }
  });
  return listener;
});
