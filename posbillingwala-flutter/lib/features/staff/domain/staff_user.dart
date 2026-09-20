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

  StaffUser copyWith({
    String? id,
    String? name,
    String? mobileNumber,
    String? role,
    String? roleLabel,
    String? address,
    String? profileImage,
    String? status,
    double? monthlySalary,
    String? lastLoginAt,
    Map<String, int>? effectivePermissions,
    Map<String, String>? permissionOverrides,
  }) {
    return StaffUser(
      id: id ?? this.id,
      name: name ?? this.name,
      mobileNumber: mobileNumber ?? this.mobileNumber,
      role: role ?? this.role,
      roleLabel: roleLabel ?? this.roleLabel,
      address: address ?? this.address,
      profileImage: profileImage ?? this.profileImage,
      status: status ?? this.status,
      monthlySalary: monthlySalary ?? this.monthlySalary,
      lastLoginAt: lastLoginAt ?? this.lastLoginAt,
      effectivePermissions:
          effectivePermissions ?? this.effectivePermissions,
      permissionOverrides: permissionOverrides ?? this.permissionOverrides,
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'name': name,
    'mobileNumber': mobileNumber,
    'role': role,
    'roleLabel': roleLabel,
    'address': address,
    'profileImage': profileImage,
    'status': status,
    'monthlySalary': monthlySalary,
    'lastLoginAt': lastLoginAt,
    'effectivePermissions': effectivePermissions,
    'permissionOverrides': permissionOverrides,
  };

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
