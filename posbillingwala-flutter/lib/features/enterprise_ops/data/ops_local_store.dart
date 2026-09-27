import 'package:pos_billingwala_v2/core/database/branch_scope.dart';
import 'package:pos_billingwala_v2/features/auth/domain/user_session.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_cache.dart';

/* Branch-scoped offline JSON store for enterprise ops entities. */
abstract final class OpsLocalStore {
  OpsLocalStore._();

  static const warehouse = 'ops_warehouse';
  static const brand = 'ops_brand';
  static const offer = 'ops_offer';
  static const approval = 'ops_approval';
  static const transfer = 'ops_transfer';
  static const lot = 'ops_lot';
  static const returns = 'ops_return';
  static const serial = 'ops_serial';
  static const audit = 'ops_audit_cache';
  static const maxDiscountPct = 'billing_max_discount_pct';

  static String scoped(String base, UserSession session) {
    final org = BranchScope.effectiveOrganizationId(session);
    final branch = BranchScope.effectiveBranchId(session);
    return '${base}_${org}_$branch';
  }

  static Future<List<Map<String, dynamic>>> load(
    UserSession session,
    String entityKey,
  ) async {
    final rows = await CloudScreenCache.loadMapList(scoped(entityKey, session));
    return rows.map((e) => Map<String, dynamic>.from(e)).toList()
      ..sort((a, b) {
        final at = (a['updatedAt'] ?? a['createdAt'] ?? '').toString();
        final bt = (b['updatedAt'] ?? b['createdAt'] ?? '').toString();
        return bt.compareTo(at);
      });
  }

  static Future<void> saveAll(
    UserSession session,
    String entityKey,
    List<Map<String, dynamic>> rows,
  ) async {
    await CloudScreenCache.saveJson(scoped(entityKey, session), rows);
  }

  static Future<Map<String, dynamic>> upsert(
    UserSession session,
    String entityKey,
    Map<String, dynamic> row,
  ) async {
    final list = await load(session, entityKey);
    final now = DateTime.now().toIso8601String();
    final id = (row['id'] ?? '').toString();
    final withMeta = Map<String, dynamic>.from(row)
      ..['id'] = id.isEmpty ? 'ops_${DateTime.now().millisecondsSinceEpoch}' : id
      ..['organizationId'] = BranchScope.effectiveOrganizationId(session)
      ..['branchId'] = BranchScope.effectiveBranchId(session)
      ..['createdAt'] = row['createdAt'] ?? now
      ..['updatedAt'] = now
      ..['title'] = (row['title'] ?? row['name'] ?? id).toString()
      ..['status'] = (row['status'] ?? 'ACTIVE').toString().toUpperCase();

    final i = list.indexWhere((e) => e['id'] == withMeta['id']);
    if (i >= 0) {
      list[i] = withMeta;
    } else {
      list.add(withMeta);
    }
    await saveAll(session, entityKey, list);
    return withMeta;
  }

  static Future<void> setStatus(
    UserSession session,
    String entityKey,
    String id,
    String status,
  ) async {
    final list = await load(session, entityKey);
    final i = list.indexWhere((e) => e['id'] == id);
    if (i < 0) return;
    list[i] = Map<String, dynamic>.from(list[i])
      ..['status'] = status.toUpperCase()
      ..['updatedAt'] = DateTime.now().toIso8601String();
    await saveAll(session, entityKey, list);
  }

  static Future<double> loadMaxDiscountPct(UserSession session) async {
    final raw = await CloudScreenCache.loadJson(scoped(maxDiscountPct, session));
    if (raw is num) return raw.toDouble();
    if (raw is String) return double.tryParse(raw) ?? 100;
    if (raw is Map && raw['pct'] != null) {
      return double.tryParse(raw['pct'].toString()) ?? 100;
    }
    return 100;
  }

  static Future<void> saveMaxDiscountPct(
    UserSession session,
    double pct,
  ) async {
    final clamped = pct.clamp(0, 100).toDouble();
    await CloudScreenCache.saveJson(
      scoped(maxDiscountPct, session),
      {'pct': clamped},
    );
  }
}
