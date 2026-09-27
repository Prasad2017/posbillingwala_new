import 'package:pos_billingwala_v2/core/database/branch_scope.dart';
import 'package:pos_billingwala_v2/features/auth/domain/user_session.dart';
import 'package:pos_billingwala_v2/features/purchase/domain/purchase_document.dart';
import 'package:pos_billingwala_v2/features/purchase/domain/vendor.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_cache.dart';

/* Branch-scoped offline store for vendors + purchase documents. */
abstract final class PurchaseLocalStore {
  PurchaseLocalStore._();

  static const vendorsKey = 'purchase_vendors';
  static const docsKey = 'purchase_documents';

  static String _scoped(String base, UserSession session) {
    final org = BranchScope.effectiveOrganizationId(session);
    final branch = BranchScope.effectiveBranchId(session);
    return '${base}_${org}_$branch';
  }

  static Future<List<Vendor>> loadVendors(UserSession session) async {
    final rows = await CloudScreenCache.loadMapList(_scoped(vendorsKey, session));
    return rows.map(Vendor.fromJson).toList()
      ..sort((a, b) => a.name.toLowerCase().compareTo(b.name.toLowerCase()));
  }

  static Future<void> saveVendors(
    UserSession session,
    List<Vendor> vendors,
  ) async {
    await CloudScreenCache.saveJson(
      _scoped(vendorsKey, session),
      vendors.map((e) => e.toJson()).toList(),
    );
  }

  static Future<Vendor> upsertVendor(UserSession session, Vendor vendor) async {
    final list = await loadVendors(session);
    final now = DateTime.now();
    final withMeta = vendor.copyWith(
      organizationId: BranchScope.effectiveOrganizationId(session),
      branchId: BranchScope.effectiveBranchId(session),
      createdAt: vendor.createdAt ?? now,
      updatedAt: now,
    );
    final i = list.indexWhere((e) => e.id == withMeta.id);
    if (i >= 0) {
      list[i] = withMeta;
    } else {
      list.add(withMeta);
    }
    await saveVendors(session, list);
    return withMeta;
  }

  static Future<void> deactivateVendor(
    UserSession session,
    String vendorId,
  ) async {
    final list = await loadVendors(session);
    final i = list.indexWhere((e) => e.id == vendorId);
    if (i < 0) return;
    list[i] = list[i].copyWith(
      status: 'INACTIVE',
      updatedAt: DateTime.now(),
    );
    await saveVendors(session, list);
  }

  static Future<List<PurchaseDocument>> loadDocuments(
    UserSession session,
  ) async {
    final rows = await CloudScreenCache.loadMapList(_scoped(docsKey, session));
    return rows.map(PurchaseDocument.fromJson).toList()
      ..sort((a, b) {
        final at = a.updatedAt ?? a.createdAt ?? DateTime.fromMillisecondsSinceEpoch(0);
        final bt = b.updatedAt ?? b.createdAt ?? DateTime.fromMillisecondsSinceEpoch(0);
        return bt.compareTo(at);
      });
  }

  static Future<void> saveDocuments(
    UserSession session,
    List<PurchaseDocument> docs,
  ) async {
    await CloudScreenCache.saveJson(
      _scoped(docsKey, session),
      docs.map((e) => e.toJson()).toList(),
    );
  }

  static Future<PurchaseDocument> upsertDocument(
    UserSession session,
    PurchaseDocument doc,
  ) async {
    final list = await loadDocuments(session);
    final now = DateTime.now();
    final withMeta = doc.copyWith(
      organizationId: BranchScope.effectiveOrganizationId(session),
      branchId: BranchScope.effectiveBranchId(session),
      createdAt: doc.createdAt ?? now,
      updatedAt: now,
    );
    final i = list.indexWhere((e) => e.id == withMeta.id);
    if (i >= 0) {
      list[i] = withMeta;
    } else {
      list.add(withMeta);
    }
    await saveDocuments(session, list);
    return withMeta;
  }

  static Future<PurchaseDocument?> getDocument(
    UserSession session,
    String id,
  ) async {
    final list = await loadDocuments(session);
    for (final d in list) {
      if (d.id == id) return d;
    }
    return null;
  }

  static String newVendorId() =>
      'vnd_${DateTime.now().millisecondsSinceEpoch}';

  static String newDocId(PurchaseDocType type) =>
      '${type.shortLabel.toLowerCase()}_${DateTime.now().millisecondsSinceEpoch}';

  static Future<String> nextDocNo(
    UserSession session,
    PurchaseDocType type,
  ) async {
    final docs = await loadDocuments(session);
    final prefix = type.shortLabel;
    var max = 0;
    final re = RegExp('^$prefix-(\\d+)\$', caseSensitive: false);
    for (final d in docs) {
      if (d.type != type) continue;
      final m = re.firstMatch(d.docNo.trim());
      if (m != null) {
        final n = int.tryParse(m.group(1) ?? '') ?? 0;
        if (n > max) max = n;
      }
    }
    return '$prefix-${(max + 1).toString().padLeft(4, '0')}';
  }
}
