import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/database/branch_scope.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/auth/domain/user_session.dart';
import 'package:pos_billingwala_v2/features/enterprise/data/enterprise_api.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_cache.dart';

class CrmCustomer {
  const CrmCustomer({
    required this.id,
    required this.name,
    this.mobile = '',
    this.email = '',
    this.address = '',
    this.gstin = '',
    this.creditLimit = 0,
    this.walletBalance = 0,
    this.loyaltyPoints = 0,
    this.membership = '',
    this.status = 'ACTIVE',
    this.notes = '',
  });

  final String id;
  final String name;
  final String mobile;
  final String email;
  final String address;
  final String gstin;
  final double creditLimit;
  final double walletBalance;
  final double loyaltyPoints;
  final String membership;
  final String status;
  final String notes;

  Map<String, dynamic> toJson() => {
    'id': id,
    'clientId': id,
    'name': name,
    'mobile': mobile,
    'email': email,
    'address': address,
    'gstin': gstin,
    'creditLimit': creditLimit,
    'walletBalance': walletBalance,
    'loyaltyPoints': loyaltyPoints,
    'membership': membership,
    'status': status,
    'notes': notes,
  };

  factory CrmCustomer.fromJson(Map<String, dynamic> json) {
    double n(Object? v) =>
        v is num ? v.toDouble() : double.tryParse('$v') ?? 0;
    return CrmCustomer(
      id: '${json['id'] ?? json['clientId'] ?? ''}',
      name: '${json['name'] ?? ''}',
      mobile: '${json['mobile'] ?? ''}',
      email: '${json['email'] ?? ''}',
      address: '${json['address'] ?? ''}',
      gstin: '${json['gstin'] ?? ''}',
      creditLimit: n(json['creditLimit']),
      walletBalance: n(json['walletBalance']),
      loyaltyPoints: n(json['loyaltyPoints']),
      membership: '${json['membership'] ?? ''}',
      status: '${json['status'] ?? 'ACTIVE'}',
      notes: '${json['notes'] ?? ''}',
    );
  }
}

abstract final class CrmLocalStore {
  static String _key(UserSession s) =>
      'crm_customers_${BranchScope.effectiveOrganizationId(s)}_${BranchScope.effectiveBranchId(s)}';

  static Future<List<CrmCustomer>> load(UserSession session) async {
    final rows = await CloudScreenCache.loadMapList(_key(session));
    return rows.map(CrmCustomer.fromJson).toList()
      ..sort((a, b) => a.name.compareTo(b.name));
  }

  static Future<void> save(UserSession session, List<CrmCustomer> list) async {
    await CloudScreenCache.saveJson(
      _key(session),
      list.map((e) => e.toJson()).toList(),
    );
  }

  static Future<CrmCustomer> upsert(UserSession session, CrmCustomer c) async {
    final list = await load(session);
    final i = list.indexWhere((e) => e.id == c.id);
    if (i >= 0) {
      list[i] = c;
    } else {
      list.add(c);
    }
    await save(session, list);
    return c;
  }
}

final customersProvider = FutureProvider<List<CrmCustomer>>((ref) async {
  final session = ref.watch(authControllerProvider).session;
  if (session == null) return const [];
  final local = await CrmLocalStore.load(session);
  if (await isDeviceOnline()) {
    try {
      final remote = await ref
          .read(enterpriseApiProvider)
          .fetchCustomers(session.licenceUserId);
      if (remote.isNotEmpty) {
        final mapped = remote.map(CrmCustomer.fromJson).toList();
        await CrmLocalStore.save(session, mapped);
        return mapped;
      }
    } catch (_) {}
  }
  return local;
});

class CrmController extends Notifier<void> {
  @override
  void build() {}

  Future<CrmCustomer> save({
    String? id,
    required String name,
    String mobile = '',
    String email = '',
    String address = '',
    String gstin = '',
    double creditLimit = 0,
    double walletBalance = 0,
    double loyaltyPoints = 0,
    String membership = '',
    String notes = '',
  }) async {
    final session = ref.read(authControllerProvider).session;
    if (session == null) throw StateError('Not signed in');
    if (name.trim().isEmpty) throw StateError('Customer name is required');
    final customer = CrmCustomer(
      id: (id == null || id.isEmpty)
          ? 'cust_${DateTime.now().millisecondsSinceEpoch}'
          : id,
      name: name.trim(),
      mobile: mobile.trim(),
      email: email.trim(),
      address: address.trim(),
      gstin: gstin.trim(),
      creditLimit: creditLimit,
      walletBalance: walletBalance,
      loyaltyPoints: loyaltyPoints,
      membership: membership.trim(),
      notes: notes.trim(),
    );
    final saved = await CrmLocalStore.upsert(session, customer);
    ref.invalidate(customersProvider);
    if (await isDeviceOnline()) {
      try {
        await ref.read(enterpriseApiProvider).saveCustomer(
              userId: session.licenceUserId,
              customer: saved.toJson(),
            );
      } catch (_) {}
    }
    return saved;
  }
}

final crmControllerProvider = NotifierProvider<CrmController, void>(
  CrmController.new,
);
