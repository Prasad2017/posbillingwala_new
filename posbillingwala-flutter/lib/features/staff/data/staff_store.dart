import 'dart:convert';

import 'package:pos_billingwala_v2/features/staff/domain/staff_user.dart';
import 'package:shared_preferences/shared_preferences.dart';

class StaffSessionKeys {
  StaffSessionKeys._();
  static const staffId = 'staffId';
  static const staffName = 'staffName';
  static const staffMobile = 'staffMobile';
  static const staffRole = 'staffRole';
  static const staffPermissions = 'staffPermissions';
  static const staffPermissionVersion = 'staffPermissionVersion';
  static const staffSessionId = 'staffSessionId';
}

class StaffStore {
  Future<void> save({
    required StaffUser staff,
    required String permissionVersion,
    String sessionId = '',
  }) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(StaffSessionKeys.staffId, staff.id);
    await prefs.setString(StaffSessionKeys.staffName, staff.name);
    await prefs.setString(StaffSessionKeys.staffMobile, staff.mobileNumber);
    await prefs.setString(StaffSessionKeys.staffRole, staff.role);
    await prefs.setString(
      StaffSessionKeys.staffPermissions,
      jsonEncode(staff.effectivePermissions),
    );
    await prefs.setString(
      StaffSessionKeys.staffPermissionVersion,
      permissionVersion,
    );
    if (sessionId.isNotEmpty) {
      await prefs.setString(StaffSessionKeys.staffSessionId, sessionId);
    }
  }

  Future<StaffUser?> read() async {
    final prefs = await SharedPreferences.getInstance();
    final id = prefs.getString(StaffSessionKeys.staffId) ?? '';
    if (id.isEmpty) return null;
    final raw = prefs.getString(StaffSessionKeys.staffPermissions) ?? '{}';
    Map<String, int> perms = {};
    try {
      final decoded = jsonDecode(raw);
      if (decoded is Map) {
        decoded.forEach((k, v) {
          perms[k.toString()] = v.toString() == '1' ? 1 : 0;
        });
      }
    } catch (_) {}
    return StaffUser(
      id: id,
      name: prefs.getString(StaffSessionKeys.staffName) ?? '',
      mobileNumber: prefs.getString(StaffSessionKeys.staffMobile) ?? '',
      role: prefs.getString(StaffSessionKeys.staffRole) ?? '',
      effectivePermissions: perms,
    );
  }

  Future<String> permissionVersion() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(StaffSessionKeys.staffPermissionVersion) ?? '0';
  }

  Future<void> clear() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(StaffSessionKeys.staffId);
    await prefs.remove(StaffSessionKeys.staffName);
    await prefs.remove(StaffSessionKeys.staffMobile);
    await prefs.remove(StaffSessionKeys.staffRole);
    await prefs.remove(StaffSessionKeys.staffPermissions);
    await prefs.remove(StaffSessionKeys.staffPermissionVersion);
    await prefs.remove(StaffSessionKeys.staffSessionId);
  }
}
