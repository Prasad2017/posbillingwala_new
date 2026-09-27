import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/crm/domain/crm_providers.dart';
import 'package:pos_billingwala_v2/features/enterprise_ops/domain/ops_providers.dart';

/* Deep enterprise rules used by checkout, purchase, CRM. */
abstract final class EnterpriseRuntime {
  EnterpriseRuntime._();

  /* ₹100 bill → 1 loyalty point. */
  static double earnPoints(double billTotal) =>
      billTotal <= 0 ? 0 : (billTotal / 100).floorToDouble();

  static double redeemValue(double points) => points < 0 ? 0 : points;

  static Future<Map<String, dynamic>?> findActiveOffer(
    dynamic ref,
    String code,
  ) async {
    final c = code.trim().toUpperCase();
    if (c.isEmpty) return null;
    final offers = await ref.read(opsListProvider('offer').future);
    for (final o in offers) {
      final oc = (o['code'] ?? '').toString().trim().toUpperCase();
      final status = (o['status'] ?? 'ACTIVE').toString().toUpperCase();
      if (oc != c) continue;
      if (status != 'ACTIVE' && status.isNotEmpty) continue;
      final validTill = (o['validTill'] ?? '').toString().trim();
      if (validTill.isNotEmpty) {
        final till = DateTime.tryParse(validTill);
        if (till != null && till.isBefore(DateTime.now())) continue;
      }
      final max = int.tryParse('${o['maxUses'] ?? ''}');
      final used = int.tryParse('${o['usedCount'] ?? 0}') ?? 0;
      if (max != null && max > 0 && used >= max) continue;
      return o;
    }
    return null;
  }

  static double offerDiscountAmount(
    Map<String, dynamic> offer,
    double subtotal, {
    int cartLineCount = 0,
    double cheapestLine = 0,
  }) {
    if (subtotal <= 0) return 0;
    final type = (offer['type'] ?? 'percent').toString().toLowerCase();
    final pct = double.tryParse('${offer['discountPct'] ?? ''}') ?? 0;
    final amt = double.tryParse('${offer['amount'] ?? ''}') ?? 0;
    double raw;
    if (type.contains('bogo') || type.contains('buy')) {
      /* BOGO: free cheapest line when cart has buyQty+getQty items (default 2). */
      final buy = int.tryParse('${offer['buyQty'] ?? '1'}') ?? 1;
      final get = int.tryParse('${offer['getQty'] ?? '1'}') ?? 1;
      final need = buy + get;
      if (cartLineCount >= need && cheapestLine > 0) {
        raw = cheapestLine * get;
      } else {
        raw = 0;
      }
    } else if (type.contains('flat') || type.contains('amount') || type.contains('modifier')) {
      raw = amt > 0 ? amt : 0;
    } else {
      raw = subtotal * pct.clamp(0, 100) / 100;
      if (raw <= 0 && amt > 0) raw = amt;
    }
    return double.parse(raw.clamp(0, subtotal).toStringAsFixed(2));
  }

  /* Returns error message if required serials missing / invalid. */
  static Future<String?> validateSerialsForSale(
    dynamic ref, {
    required Iterable<int> productIds,
    required Map<int, String> serialByProductId,
  }) async {
    final serials = await ref.read(opsListProvider('serial').future);
    final tracked = <int, List<Map<String, dynamic>>>{};
    for (final s in serials) {
      final status = (s['status'] ?? 'IN_STOCK').toString().toUpperCase();
      if (status != 'IN_STOCK' && status.isNotEmpty && status != 'ACTIVE') {
        continue;
      }
      final pid = int.tryParse('${s['productId'] ?? ''}');
      if (pid == null || pid <= 0) continue;
      tracked.putIfAbsent(pid, () => []).add(s);
    }
    for (final pid in productIds) {
      final list = tracked[pid];
      if (list == null || list.isEmpty) continue;
      final entered = (serialByProductId[pid] ?? '').trim();
      if (entered.isEmpty) {
        return 'Enter serial/IMEI for product #$pid';
      }
      final match = list.any(
        (s) => (s['serialNo'] ?? s['name'] ?? '')
            .toString()
            .trim()
            .toLowerCase() ==
            entered.toLowerCase(),
      );
      if (!match) {
        return 'Serial $entered not in stock for product #$pid';
      }
    }
    return null;
  }

  static Future<void> markSerialSold(
    dynamic ref, {
    required String serialNo,
  }) async {
    final serials = await ref.read(opsListProvider('serial').future);
    final sn = serialNo.trim().toLowerCase();
    for (final s in serials) {
      final cur = (s['serialNo'] ?? s['name'] ?? '').toString().trim().toLowerCase();
      if (cur == sn) {
        final next = Map<String, dynamic>.from(s)..['status'] = 'SOLD';
        await upsertOpsRecord(ref, entity: 'serial', row: next);
        return;
      }
    }
  }

  static Future<Map<String, double>> warehouseStockMap(dynamic ref) async {
    final transfers = await ref.read(opsListProvider('transfer').future);
    final map = <String, double>{};
    for (final t in transfers) {
      final status = (t['status'] ?? '').toString().toUpperCase();
      final qty = double.tryParse('${t['qty'] ?? ''}') ?? 0;
      if (qty <= 0) continue;
      final from = (t['fromWarehouse'] ?? '').toString();
      final to = (t['toWarehouse'] ?? '').toString();
      if (status == 'RECEIVED' || status == 'IN_TRANSIT' || status == 'APPROVED') {
        if (from.isNotEmpty) map[from] = (map[from] ?? 0) - qty;
        if (to.isNotEmpty && status == 'RECEIVED') {
          map[to] = (map[to] ?? 0) + qty;
        }
      }
    }
    return map;
  }

  static Future<List<String>> expiredLotWarnings(
    dynamic ref, {
    required Iterable<int> productIds,
  }) async {
    final lots = await ref.read(opsListProvider('lot').future);
    final today = DateTime.now();
    final warnings = <String>[];
    final idSet = productIds.toSet();
    for (final lot in lots) {
      final status = (lot['status'] ?? 'ACTIVE').toString().toUpperCase();
      if (status == 'EXPIRED' || status == 'QUARANTINE') continue;
      final expiryRaw = (lot['expiryDate'] ?? '').toString().trim();
      if (expiryRaw.isEmpty) continue;
      final expiry = DateTime.tryParse(expiryRaw);
      if (expiry == null) continue;
      final name =
          (lot['productName'] ?? lot['title'] ?? lot['name'] ?? '').toString();
      final batch = (lot['batchNo'] ?? '').toString();
      final pid = int.tryParse('${lot['productId'] ?? ''}');
      final matches = pid != null ? idSet.contains(pid) : name.isNotEmpty;
      if (!matches && pid != null) continue;
      if (expiry.isBefore(today)) {
        warnings.add(
          'Expired lot: $name${batch.isEmpty ? '' : ' ($batch)'} — $expiryRaw',
        );
      } else if (expiry.difference(today).inDays <= 7) {
        warnings.add(
          'Near expiry: $name${batch.isEmpty ? '' : ' ($batch)'} — $expiryRaw',
        );
      }
    }
    return warnings;
  }

  static Future<void> createPurchaseApproval(
    dynamic ref, {
    required String docNo,
    required String vendorName,
    required double amount,
    required String requestedBy,
  }) async {
    await upsertOpsRecord(
      ref,
      entity: 'approval',
      row: {
        'id': 'appr_po_${docNo}_${DateTime.now().millisecondsSinceEpoch}',
        'name': 'PO $docNo — $vendorName',
        'title': 'PO $docNo — $vendorName',
        'requestType': 'PURCHASE',
        'requestedBy': requestedBy,
        'amount': amount.toStringAsFixed(2),
        'notes': 'Auto-created from purchase order',
        'status': 'PENDING',
        'refDocNo': docNo,
      },
    );
  }

  static Future<bool> isPurchaseApproved(dynamic ref, String docNo) async {
    final rows = await ref.read(opsListProvider('approval').future);
    var sawPending = false;
    for (final r in rows) {
      final refDoc = (r['refDocNo'] ?? '').toString();
      final name = (r['name'] ?? r['title'] ?? '').toString();
      final status = (r['status'] ?? '').toString().toUpperCase();
      final matches = refDoc == docNo || name.contains(docNo);
      if (!matches) continue;
      if (status == 'APPROVED') return true;
      if (status == 'PENDING') sawPending = true;
      if (status == 'REJECTED') return false;
    }
    if (sawPending) return false;
    return true;
  }

  static Future<CrmCustomer?> adjustLoyalty(
    dynamic ref, {
    required String customerId,
    required double earn,
    required double redeem,
  }) async {
    final session = ref.read(authControllerProvider).session;
    if (session == null || customerId.isEmpty) return null;
    final list = await CrmLocalStore.load(session);
    final i = list.indexWhere((c) => c.id == customerId);
    if (i < 0) return null;
    final c = list[i];
    var points = c.loyaltyPoints - redeem + earn;
    if (points < 0) points = 0;
    final updated = CrmCustomer(
      id: c.id,
      name: c.name,
      mobile: c.mobile,
      email: c.email,
      address: c.address,
      gstin: c.gstin,
      creditLimit: c.creditLimit,
      walletBalance: c.walletBalance,
      loyaltyPoints: points,
      membership: c.membership,
      status: c.status,
      notes: c.notes,
    );
    await CrmLocalStore.upsert(session, updated);
    ref.invalidate(customersProvider);
    await enterprisePushSafeRef(ref, (api, userId) {
      return api.saveCustomer(userId: userId, customer: updated.toJson());
    });
    return updated;
  }

  static Future<CrmCustomer?> adjustWallet(
    dynamic ref, {
    required String customerId,
    required double debit,
  }) async {
    final session = ref.read(authControllerProvider).session;
    if (session == null || customerId.isEmpty) return null;
    final list = await CrmLocalStore.load(session);
    final i = list.indexWhere((c) => c.id == customerId);
    if (i < 0) return null;
    final c = list[i];
    final wallet =
        (c.walletBalance - debit).clamp(0, double.infinity).toDouble();
    final updated = CrmCustomer(
      id: c.id,
      name: c.name,
      mobile: c.mobile,
      email: c.email,
      address: c.address,
      gstin: c.gstin,
      creditLimit: c.creditLimit,
      walletBalance: wallet,
      loyaltyPoints: c.loyaltyPoints,
      membership: c.membership,
      status: c.status,
      notes: c.notes,
    );
    await CrmLocalStore.upsert(session, updated);
    ref.invalidate(customersProvider);
    await enterprisePushSafeRef(ref, (api, userId) {
      return api.saveCustomer(userId: userId, customer: updated.toJson());
    });
    return updated;
  }

  static Future<void> bumpOfferUse(
    dynamic ref,
    Map<String, dynamic> offer,
  ) async {
    final used = (int.tryParse('${offer['usedCount'] ?? 0}') ?? 0) + 1;
    final max = int.tryParse('${offer['maxUses'] ?? ''}');
    final next = Map<String, dynamic>.from(offer)
      ..['usedCount'] = used
      ..['status'] = (max != null && max > 0 && used >= max)
          ? 'INACTIVE'
          : (offer['status'] ?? 'ACTIVE');
    await upsertOpsRecord(ref, entity: 'offer', row: next);
  }

  static Future<void> createLotFromGrn(
    dynamic ref, {
    required String productName,
    required int productId,
    required double qty,
    String batchNo = '',
    String expiryDate = '',
    String warehouse = '',
  }) async {
    if (qty <= 0 || productName.trim().isEmpty) return;
    final batch = batchNo.trim().isEmpty
        ? 'B${DateTime.now().millisecondsSinceEpoch % 100000}'
        : batchNo.trim();
    await upsertOpsRecord(
      ref,
      entity: 'lot',
      row: {
        'id': 'lot_${productId}_$batch',
        'name': '$productName / $batch',
        'title': '$productName / $batch',
        'productName': productName,
        'productId': productId.toString(),
        'batchNo': batch,
        'expiryDate': expiryDate,
        'qty': qty.toString(),
        'reorderLevel': '0',
        'warehouse': warehouse,
        'status': 'ACTIVE',
      },
    );
  }
}

