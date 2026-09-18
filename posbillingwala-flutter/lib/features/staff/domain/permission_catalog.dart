const posFixedRoles = <String, String>{
  'OWNER': 'Owner',
  'MANAGER': 'Manager',
  'WAITER': 'Waiter',
  'KITCHEN': 'Kitchen',
  'HELPER': 'Helper',
  'BAR_ATTENDER': 'Bar Attender',
  'SECURITY': 'Security',
  'ACCOUNTANT': 'Accountant',
};

const posPermissionCatalog = <String, List<String>>{
  'dashboard': ['view'],
  'billing': ['view', 'create', 'edit', 'delete'],
  'order': ['view', 'create', 'edit', 'delete'],
  'kot': ['view', 'create', 'edit', 'delete', 'print', 'reprint'],
  'bill': ['view', 'create', 'edit', 'delete', 'print', 'reprint'],
  'table': ['view', 'manage'],
  'takeaway': ['view', 'create'],
  'product': ['view', 'create', 'edit', 'delete'],
  'inventory': ['view', 'manage'],
  'customer': ['view', 'manage'],
  'report': ['view', 'export'],
  'expense': ['view', 'manage'],
  'mess': ['view', 'manage'],
  'user': ['view', 'create', 'edit', 'deactivate', 'change_role', 'reset_pin'],
  'printer': ['view', 'manage', 'test'],
  'device': ['view', 'manage'],
  'settings': ['view', 'manage'],
};

const posPermissionDependencies = <String, String>{
  'billing.create': 'billing.view',
  'billing.edit': 'billing.view',
  'billing.delete': 'billing.view',
  'order.create': 'order.view',
  'kot.create': 'kot.view',
  'kot.print': 'kot.view',
  'kot.reprint': 'kot.view',
  'bill.create': 'bill.view',
  'bill.print': 'bill.view',
  'bill.reprint': 'bill.view',
  'product.create': 'product.view',
  'user.create': 'user.view',
  'user.edit': 'user.view',
  'user.deactivate': 'user.view',
  'user.reset_pin': 'user.view',
  'printer.manage': 'printer.view',
  'printer.test': 'printer.view',
};

String posRoleLabel(String role) => posFixedRoles[role.toUpperCase()] ?? role;

String posPermissionLabel(String key) {
  final parts = key.split('.');
  if (parts.length != 2) return key;
  final action = parts[1].replaceAll('_', ' ');
  return '${parts[0][0].toUpperCase()}${parts[0].substring(1)} ${action[0].toUpperCase()}${action.substring(1)}';
}

List<String> posAllPermissionKeys() {
  final keys = <String>[];
  posPermissionCatalog.forEach((module, actions) {
    for (final action in actions) {
      keys.add('$module.$action');
    }
  });
  return keys;
}
