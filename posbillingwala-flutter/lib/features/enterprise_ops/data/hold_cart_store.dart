import 'package:pos_billingwala_v2/core/database/branch_scope.dart';
import 'package:pos_billingwala_v2/features/auth/domain/user_session.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_cache.dart';

class HeldInvoice {
  const HeldInvoice({
    required this.id,
    required this.label,
    required this.createdAt,
    required this.lines,
    this.customerName = '',
    this.customerPhone = '',
    this.customerId = '',
    this.note = '',
  });

  final String id;
  final String label;
  final DateTime createdAt;
  final List<Map<String, dynamic>> lines;
  final String customerName;
  final String customerPhone;
  final String customerId;
  final String note;

  Map<String, dynamic> toJson() => {
        'id': id,
        'label': label,
        'createdAt': createdAt.toIso8601String(),
        'lines': lines,
        'customerName': customerName,
        'customerPhone': customerPhone,
        'customerId': customerId,
        'note': note,
      };

  factory HeldInvoice.fromJson(Map<String, dynamic> json) {
    final rawLines = json['lines'];
    return HeldInvoice(
      id: '${json['id'] ?? ''}',
      label: '${json['label'] ?? 'Hold'}',
      createdAt: DateTime.tryParse('${json['createdAt'] ?? ''}') ?? DateTime.now(),
      lines: rawLines is List
          ? rawLines
              .whereType<Map>()
              .map((e) => Map<String, dynamic>.from(e))
              .toList()
          : const [],
      customerName: '${json['customerName'] ?? ''}',
      customerPhone: '${json['customerPhone'] ?? ''}',
      customerId: '${json['customerId'] ?? ''}',
      note: '${json['note'] ?? ''}',
    );
  }
}

abstract final class HoldCartStore {
  HoldCartStore._();

  static String _key(UserSession s) =>
      'held_invoices_${BranchScope.effectiveOrganizationId(s)}_${BranchScope.effectiveBranchId(s)}';

  static Future<List<HeldInvoice>> load(UserSession session) async {
    final rows = await CloudScreenCache.loadMapList(_key(session));
    return rows.map(HeldInvoice.fromJson).toList()
      ..sort((a, b) => b.createdAt.compareTo(a.createdAt));
  }

  static Future<void> saveAll(UserSession session, List<HeldInvoice> list) async {
    await CloudScreenCache.saveJson(
      _key(session),
      list.map((e) => e.toJson()).toList(),
    );
  }

  static Future<HeldInvoice> upsert(UserSession session, HeldInvoice hold) async {
    final list = await load(session);
    final i = list.indexWhere((e) => e.id == hold.id);
    if (i >= 0) {
      list[i] = hold;
    } else {
      list.insert(0, hold);
    }
    await saveAll(session, list);
    return hold;
  }

  static Future<void> remove(UserSession session, String id) async {
    final list = await load(session);
    list.removeWhere((e) => e.id == id);
    await saveAll(session, list);
  }
}
