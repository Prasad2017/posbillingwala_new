import 'package:pos_billingwala_v2/features/staff/domain/permission_catalog.dart';

class StaffUser {
  const StaffUser({
    required this.id,
    required this.name,
    required this.mobileNumber,
    required this.role,
    this.roleLabel = '',
    this.address = '',
    this.profileImage = '',
    this.status = 'ACTIVE',
    this.monthlySalary = 0,
    this.lastLoginAt = '',
    this.effectivePermissions = const {},
    this.permissionOverrides = const {},
  });

  final String id;
  final String name;
  final String mobileNumber;
  final String role;
  final String roleLabel;
  final String address;
  final String profileImage;
  final String status;
  final double monthlySalary;
  final String lastLoginAt;
  final Map<String, int> effectivePermissions;
  final Map<String, String> permissionOverrides;

  bool get isActive => status.toUpperCase() == 'ACTIVE';

  bool allows(String key) => (effectivePermissions[key] ?? 0) == 1;

  factory StaffUser.fromJson(Map<String, dynamic> json) {
    String s(Object? v) => v?.toString() ?? '';
    final effective = <String, int>{};
    final rawEff = json['effectivePermissions'];
    if (rawEff is Map) {
      rawEff.forEach((k, v) {
        effective[k.toString()] = v.toString() == '1' ? 1 : 0;
      });
    }
    final overrides = <String, String>{};
    final rawOv = json['permissionOverrides'];
    if (rawOv is Map) {
      rawOv.forEach((k, v) {
        final state = v.toString().toUpperCase();
        if (state == 'ALLOW' || state == 'DENY') {
          overrides[k.toString()] = state;
        }
      });
    }
    return StaffUser(
      id: s(json['id']),
      name: s(json['name']),
      mobileNumber: s(json['mobileNumber']),
      role: s(json['role']),
      roleLabel: s(json['roleLabel']).isEmpty
          ? posRoleLabel(s(json['role']))
          : s(json['roleLabel']),
      address: s(json['address']),
      profileImage: s(json['profileImage']),
      status: s(json['status']).isEmpty ? 'ACTIVE' : s(json['status']),
      monthlySalary: double.tryParse(s(json['monthlySalary'])) ?? 0,
      lastLoginAt: s(json['lastLoginAt']),
      effectivePermissions: effective,
      permissionOverrides: overrides,
    );
  }
}
