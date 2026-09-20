import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/staff/data/staff_api.dart';
import 'package:pos_billingwala_v2/features/staff/domain/staff_user.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_cache.dart';
import 'package:shared_preferences/shared_preferences.dart';

/* Result of a soft-fail staff/salary save. */
class StaffOfflineSaveResult {
  const StaffOfflineSaveResult({
    required this.synced,
    this.staff,
    this.message = '',
  });

  final bool synced;
  final StaffUser? staff;
  final String message;

  bool get pending => !synced;
}

/* Pending create/update/deactivate/role/pin/salary ops for Sync to Server. */
abstract final class StaffOfflineQueue {
  StaffOfflineQueue._();

  /* Pending create/update/deactivate/role/pin/salary ops for Sync to Server. */
  static const opsKey = 'staff_ops';
  static const mealSessionsPendingKey = 'meal_sessions_pending_upload';

  static String newLocalId() =>
      'staff_local_${DateTime.now().millisecondsSinceEpoch}';

  static Future<List<Map<String, dynamic>>> loadOps() =>
      CloudScreenCache.loadMapList(opsKey);

  static Future<void> saveOps(List<Map<String, dynamic>> ops) =>
      CloudScreenCache.saveJson(opsKey, ops);

  static Future<bool> hasPendingOps() async => (await loadOps()).isNotEmpty;

  static Future<void> enqueue(Map<String, dynamic> op) async {
    final ops = await loadOps();
    ops.add({
      ...op,
      'queuedAt': DateTime.now().toIso8601String(),
    });
    await saveOps(ops);
  }

  static Future<void> setMealSessionsPending(bool pending) async {
    final prefs = await SharedPreferences.getInstance();
    if (pending) {
      await prefs.setBool(mealSessionsPendingKey, true);
    } else {
      await prefs.remove(mealSessionsPendingKey);
    }
  }

  static Future<bool> isMealSessionsPending() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(mealSessionsPendingKey) ?? false;
  }

  static Future<List<StaffUser>> loadStaffCache() async {
    final rows = await CloudScreenCache.loadMapList(CloudScreenCache.staff);
    return rows.map(StaffUser.fromJson).toList();
  }

  static Future<void> saveStaffCache(List<StaffUser> users) async {
    await CloudScreenCache.saveJson(
      CloudScreenCache.staff,
      users.map((e) => e.toJson()).toList(),
    );
  }

  static Future<void> upsertStaffCache(StaffUser user) async {
    final list = await loadStaffCache();
    final i = list.indexWhere((e) => e.id == user.id);
    if (i >= 0) {
      list[i] = user;
    } else {
      list.add(user);
    }
    await saveStaffCache(list);
  }

  static Future<StaffUser?> getStaffFromCache(String id) async {
    final list = await loadStaffCache();
    for (final u in list) {
      if (u.id == id) return u;
    }
    return null;
  }

  static Future<void> upsertSalaryCache(Map<String, dynamic> row) async {
    final list = await CloudScreenCache.loadMapList(CloudScreenCache.salary);
    final staffId = row['staffId']?.toString() ?? '';
    final i = list.indexWhere((e) => e['staffId']?.toString() == staffId);
    if (i >= 0) {
      list[i] = {...list[i], ...row};
    } else {
      list.add(row);
    }
    await CloudScreenCache.saveJson(CloudScreenCache.salary, list);
  }

  /* Soft-fail create: cache + queue, try API when online. */
  static Future<StaffOfflineSaveResult> create({
    required StaffApi api,
    required String userId,
    required String name,
    required String mobileNumber,
    required String role,
    required String pin,
    required String confirmPin,
    String address = '',
    Map<String, String> overrides = const {},
  }) async {
    final localId = newLocalId();
    final draft = StaffUser(
      id: localId,
      name: name,
      mobileNumber: mobileNumber,
      role: role,
      address: address,
      status: 'ACTIVE',
      permissionOverrides: overrides,
      effectivePermissions: {
        for (final e in overrides.entries) e.key: e.value == 'ALLOW' ? 1 : 0,
      },
    );
    await upsertStaffCache(draft);
    await enqueue({
      'op': 'create',
      'localId': localId,
      'name': name,
      'mobileNumber': mobileNumber,
      'role': role,
      'pin': pin,
      'confirmPin': confirmPin,
      'address': address,
      'overrides': overrides,
    });

    if (!await isDeviceOnline()) {
      if (AppPlatform.requiresNetwork) {
        throw StateError(kOnlineRequiredMessage);
      }
      return const StaffOfflineSaveResult(
        synced: false,
        message: 'User saved',
      );
    }

    try {
      final created = await api.create(
        userId: userId,
        name: name,
        mobileNumber: mobileNumber,
        role: role,
        pin: pin,
        confirmPin: confirmPin,
        address: address,
        overrides: overrides,
      );
      await _replaceLocalWithServer(localId, created);
      await _removeOpsForLocalId(localId);
      return StaffOfflineSaveResult(synced: true, staff: created);
    } catch (e) {
      if (AppPlatform.requiresNetwork) rethrow;
      AppLogger.warning('Staff create offline queue keep: $e');
      return const StaffOfflineSaveResult(
        synced: false,
        message: 'User saved',
      );
    }
  }

  static Future<StaffOfflineSaveResult> update({
    required StaffApi api,
    required String userId,
    required String id,
    required String name,
    required String mobileNumber,
    String address = '',
    String status = 'ACTIVE',
    Map<String, String>? overrides,
  }) async {
    final existing = await getStaffFromCache(id);
    final updated = (existing ??
            StaffUser(
              id: id,
              name: name,
              mobileNumber: mobileNumber,
              role: 'WAITER',
            ))
        .copyWith(
          name: name,
          mobileNumber: mobileNumber,
          address: address,
          status: status,
          permissionOverrides: overrides ?? existing?.permissionOverrides,
        );
    await upsertStaffCache(updated);
    await enqueue({
      'op': 'update',
      'id': id,
      'name': name,
      'mobileNumber': mobileNumber,
      'address': address,
      'status': status,
      if (overrides case final o?) 'overrides': o,
    });

    if (!await isDeviceOnline()) {
      if (AppPlatform.requiresNetwork) {
        throw StateError(kOnlineRequiredMessage);
      }
      return const StaffOfflineSaveResult(
        synced: false,
        message: 'User saved',
      );
    }

    try {
      final server = await api.update(
        userId: userId,
        id: id,
        name: name,
        mobileNumber: mobileNumber,
        address: address,
        status: status,
        overrides: overrides,
      );
      await upsertStaffCache(server);
      await _removeOpsMatching((op) =>
          op['op'] == 'update' && op['id']?.toString() == id);
      return StaffOfflineSaveResult(synced: true, staff: server);
    } catch (e) {
      if (AppPlatform.requiresNetwork) rethrow;
      AppLogger.warning('Staff update offline queue keep: $e');
      return const StaffOfflineSaveResult(
        synced: false,
        message: 'User saved',
      );
    }
  }

  static Future<StaffOfflineSaveResult> deactivate({
    required StaffApi api,
    required String userId,
    required String id,
  }) async {
    final existing = await getStaffFromCache(id);
    if (existing != null) {
      await upsertStaffCache(existing.copyWith(status: 'INACTIVE'));
    }
    await enqueue({'op': 'deactivate', 'id': id});

    if (!await isDeviceOnline()) {
      if (AppPlatform.requiresNetwork) {
        throw StateError(kOnlineRequiredMessage);
      }
      return const StaffOfflineSaveResult(
        synced: false,
        message: 'User deactivated',
      );
    }

    try {
      await api.deactivate(userId, id);
      await _removeOpsMatching((op) =>
          op['op'] == 'deactivate' && op['id']?.toString() == id);
      return const StaffOfflineSaveResult(synced: true);
    } catch (e) {
      if (AppPlatform.requiresNetwork) rethrow;
      return const StaffOfflineSaveResult(
        synced: false,
        message: 'User deactivated',
      );
    }
  }

  static Future<StaffOfflineSaveResult> changeRole({
    required StaffApi api,
    required String userId,
    required String id,
    required String role,
  }) async {
    final existing = await getStaffFromCache(id);
    if (existing != null) {
      await upsertStaffCache(existing.copyWith(role: role, roleLabel: ''));
    }
    await enqueue({'op': 'changeRole', 'id': id, 'role': role});

    if (!await isDeviceOnline()) {
      if (AppPlatform.requiresNetwork) {
        throw StateError(kOnlineRequiredMessage);
      }
      return const StaffOfflineSaveResult(
        synced: false,
        message: 'Role changed',
      );
    }

    try {
      await api.changeRole(userId, id, role);
      await _removeOpsMatching((op) =>
          op['op'] == 'changeRole' && op['id']?.toString() == id);
      return const StaffOfflineSaveResult(synced: true);
    } catch (e) {
      if (AppPlatform.requiresNetwork) rethrow;
      return const StaffOfflineSaveResult(
        synced: false,
        message: 'Role changed',
      );
    }
  }

  static Future<StaffOfflineSaveResult> resetPin({
    required StaffApi api,
    required String userId,
    required String id,
    required String pin,
    required String confirmPin,
  }) async {
    await enqueue({
      'op': 'resetPin',
      'id': id,
      'pin': pin,
      'confirmPin': confirmPin,
    });

    if (!await isDeviceOnline()) {
      if (AppPlatform.requiresNetwork) {
        throw StateError(kOnlineRequiredMessage);
      }
      return const StaffOfflineSaveResult(
        synced: false,
        message: 'PIN reset',
      );
    }

    try {
      await api.resetPin(
        userId: userId,
        id: id,
        pin: pin,
        confirmPin: confirmPin,
      );
      await _removeOpsMatching((op) =>
          op['op'] == 'resetPin' && op['id']?.toString() == id);
      return const StaffOfflineSaveResult(synced: true);
    } catch (e) {
      if (AppPlatform.requiresNetwork) rethrow;
      return const StaffOfflineSaveResult(
        synced: false,
        message: 'PIN reset',
      );
    }
  }

  static Future<StaffOfflineSaveResult> setSalary({
    required StaffApi api,
    required String userId,
    required String staffId,
    required double monthlySalary,
  }) async {
    await upsertSalaryCache({
      'staffId': staffId,
      'monthlySalary': monthlySalary,
    });
    await enqueue({
      'op': 'setSalary',
      'staffId': staffId,
      'monthlySalary': monthlySalary.toStringAsFixed(2),
    });

    if (!await isDeviceOnline()) {
      if (AppPlatform.requiresNetwork) {
        throw StateError(kOnlineRequiredMessage);
      }
      return const StaffOfflineSaveResult(
        synced: false,
        message: 'Salary saved',
      );
    }

    try {
      await api.updateSalary(
        userId: userId,
        staffId: staffId,
        monthlySalary: monthlySalary,
      );
      await _removeOpsMatching((op) =>
          op['op'] == 'setSalary' && op['staffId']?.toString() == staffId);
      return const StaffOfflineSaveResult(synced: true);
    } catch (e) {
      if (AppPlatform.requiresNetwork) rethrow;
      return const StaffOfflineSaveResult(
        synced: false,
        message: 'Salary saved',
      );
    }
  }

  static Future<StaffOfflineSaveResult> paySalary({
    required StaffApi api,
    required String userId,
    required String staffId,
    required String salaryMonth,
    required double amount,
    required String paidOn,
  }) async {
    await upsertSalaryCache({
      'staffId': staffId,
      'paymentStatus': 'PAID',
      'paidAmount': amount,
      'paidOn': paidOn,
      'monthlySalary': amount,
    });
    await enqueue({
      'op': 'paySalary',
      'staffId': staffId,
      'salaryMonth': salaryMonth,
      'amount': amount.toStringAsFixed(2),
      'paidOn': paidOn,
    });

    if (!await isDeviceOnline()) {
      if (AppPlatform.requiresNetwork) {
        throw StateError(kOnlineRequiredMessage);
      }
      return const StaffOfflineSaveResult(
        synced: false,
        message: 'Payment saved',
      );
    }

    try {
      await api.saveSalaryPayment(
        userId: userId,
        staffId: staffId,
        salaryMonth: salaryMonth,
        amount: amount,
        paidOn: paidOn,
      );
      await _removeOpsMatching((op) =>
          op['op'] == 'paySalary' &&
          op['staffId']?.toString() == staffId &&
          op['salaryMonth']?.toString() == salaryMonth);
      return const StaffOfflineSaveResult(synced: true);
    } catch (e) {
      if (AppPlatform.requiresNetwork) rethrow;
      return const StaffOfflineSaveResult(
        synced: false,
        message: 'Payment saved',
      );
    }
  }

  /* Flush pending ops during Sync to Server. */
  static Future<({int saved, int failed})> flush(
    StaffApi api,
    String userId,
  ) async {
    final ops = await loadOps();
    if (ops.isEmpty) return (saved: 0, failed: 0);

    var saved = 0;
    var failed = 0;
    final remaining = <Map<String, dynamic>>[];

    for (final op in ops) {
      final type = op['op']?.toString() ?? '';
      try {
        switch (type) {
          case 'create':
            final localId = op['localId']?.toString() ?? '';
            final overridesRaw = op['overrides'];
            final overrides = <String, String>{};
            if (overridesRaw is Map) {
              overridesRaw.forEach(
                (k, v) => overrides[k.toString()] = v.toString(),
              );
            }
            final created = await api.create(
              userId: userId,
              name: op['name']?.toString() ?? '',
              mobileNumber: op['mobileNumber']?.toString() ?? '',
              role: op['role']?.toString() ?? 'WAITER',
              pin: op['pin']?.toString() ?? '',
              confirmPin: op['confirmPin']?.toString() ?? '',
              address: op['address']?.toString() ?? '',
              overrides: overrides,
            );
            if (localId.isNotEmpty) {
              await _replaceLocalWithServer(localId, created);
              /* Remap later ops in this same flush batch. */
              for (var j = 0; j < ops.length; j++) {
                final row = Map<String, dynamic>.from(ops[j]);
                var changed = false;
                if (row['id']?.toString() == localId) {
                  row['id'] = created.id;
                  changed = true;
                }
                if (row['staffId']?.toString() == localId) {
                  row['staffId'] = created.id;
                  changed = true;
                }
                if (changed) ops[j] = row;
              }
            } else {
              await upsertStaffCache(created);
            }
            saved++;
          case 'update':
            final id = op['id']?.toString() ?? '';
            if (id.startsWith('staff_local_')) {
              remaining.add(op);
              continue;
            }
            Map<String, String>? overrides;
            final overridesRaw = op['overrides'];
            if (overridesRaw is Map) {
              overrides = {
                for (final e in overridesRaw.entries)
                  e.key.toString(): e.value.toString(),
              };
            }
            final server = await api.update(
              userId: userId,
              id: id,
              name: op['name']?.toString() ?? '',
              mobileNumber: op['mobileNumber']?.toString() ?? '',
              address: op['address']?.toString() ?? '',
              status: op['status']?.toString() ?? 'ACTIVE',
              overrides: overrides,
            );
            await upsertStaffCache(server);
            saved++;
          case 'deactivate':
            final id = op['id']?.toString() ?? '';
            if (id.startsWith('staff_local_')) {
              /* Local-only user never created on server — drop. */
              final list = await loadStaffCache();
              await saveStaffCache(list.where((e) => e.id != id).toList());
              saved++;
              continue;
            }
            await api.deactivate(userId, id);
            saved++;
          case 'changeRole':
            final id = op['id']?.toString() ?? '';
            if (id.startsWith('staff_local_')) {
              remaining.add(op);
              continue;
            }
            await api.changeRole(
              userId,
              id,
              op['role']?.toString() ?? 'WAITER',
            );
            saved++;
          case 'resetPin':
            final id = op['id']?.toString() ?? '';
            if (id.startsWith('staff_local_')) {
              remaining.add(op);
              continue;
            }
            await api.resetPin(
              userId: userId,
              id: id,
              pin: op['pin']?.toString() ?? '',
              confirmPin: op['confirmPin']?.toString() ?? '',
            );
            saved++;
          case 'setSalary':
            final staffId = op['staffId']?.toString() ?? '';
            if (staffId.startsWith('staff_local_')) {
              remaining.add(op);
              continue;
            }
            await api.updateSalary(
              userId: userId,
              staffId: staffId,
              monthlySalary:
                  double.tryParse(op['monthlySalary']?.toString() ?? '') ?? 0,
            );
            saved++;
          case 'paySalary':
            final staffId = op['staffId']?.toString() ?? '';
            if (staffId.startsWith('staff_local_')) {
              remaining.add(op);
              continue;
            }
            await api.saveSalaryPayment(
              userId: userId,
              staffId: staffId,
              salaryMonth: op['salaryMonth']?.toString() ?? '',
              amount: double.tryParse(op['amount']?.toString() ?? '') ?? 0,
              paidOn: op['paidOn']?.toString() ?? '',
            );
            saved++;
          default:
            AppLogger.warning('StaffOfflineQueue unknown op=$type');
            remaining.add(op);
        }
      } catch (e) {
        failed++;
        remaining.add(op);
        AppLogger.warning('StaffOfflineQueue flush FAIL $type: $e');
      }
    }

    /* Rewrite localIds in remaining ops after creates succeeded. */
    await saveOps(remaining);
    return (saved: saved, failed: failed);
  }

  static Future<void> _replaceLocalWithServer(
    String localId,
    StaffUser server,
  ) async {
    final list = await loadStaffCache();
    final i = list.indexWhere((e) => e.id == localId);
    if (i >= 0) {
      list[i] = server;
    } else {
      list.add(server);
    }
    await saveStaffCache(list);

    /* Remap pending ops that still reference localId. */
    final ops = await loadOps();
    var changed = false;
    for (var i = 0; i < ops.length; i++) {
      final op = Map<String, dynamic>.from(ops[i]);
      if (op['localId']?.toString() == localId) {
        /* create op itself is removed elsewhere */
        continue;
      }
      if (op['id']?.toString() == localId) {
        op['id'] = server.id;
        ops[i] = op;
        changed = true;
      }
      if (op['staffId']?.toString() == localId) {
        op['staffId'] = server.id;
        ops[i] = op;
        changed = true;
      }
    }
    if (changed) await saveOps(ops);

    final salary = await CloudScreenCache.loadMapList(CloudScreenCache.salary);
    var salaryChanged = false;
    for (var i = 0; i < salary.length; i++) {
      if (salary[i]['staffId']?.toString() == localId) {
        salary[i] = {...salary[i], 'staffId': server.id};
        salaryChanged = true;
      }
    }
    if (salaryChanged) {
      await CloudScreenCache.saveJson(CloudScreenCache.salary, salary);
    }
  }

  static Future<void> _removeOpsForLocalId(String localId) async {
    await _removeOpsMatching((op) =>
        op['op'] == 'create' && op['localId']?.toString() == localId);
  }

  static Future<void> _removeOpsMatching(
    bool Function(Map<String, dynamic> op) test,
  ) async {
    final ops = await loadOps();
    final next = ops.where((op) => !test(op)).toList();
    if (next.length != ops.length) await saveOps(next);
  }
}
