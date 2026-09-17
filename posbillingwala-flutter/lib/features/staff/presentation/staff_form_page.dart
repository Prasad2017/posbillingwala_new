import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/staff/domain/permission_catalog.dart';
import 'package:pos_billingwala_v2/features/staff/domain/permission_controller.dart';

class StaffFormPage extends ConsumerStatefulWidget {
  const StaffFormPage({super.key, this.staffId});

  final String? staffId;

  @override
  ConsumerState<StaffFormPage> createState() => StaffFormPageState();
}

class StaffFormPageState extends ConsumerState<StaffFormPage> {
  final name = TextEditingController();
  final mobile = TextEditingController();
  final address = TextEditingController();
  final pin = TextEditingController();
  final confirmPin = TextEditingController();
  String role = 'WAITER';
  Map<String, int> defaults = {};
  Map<String, String> overrides = {};
  bool loading = false;
  bool dirty = false;

  bool get isEdit => widget.staffId != null;

  @override
  void initState() {
    super.initState();
    Future.microtask(loadDefaults);
  }

  @override
  void dispose() {
    name.dispose();
    mobile.dispose();
    address.dispose();
    pin.dispose();
    confirmPin.dispose();
    super.dispose();
  }

  Future<void> loadDefaults() async {
    final session = ref.read(authControllerProvider).session;
    if (session == null) return;
    final data =
        await ref.read(staffApiProvider).roleDefaults(session.licenceUserId, role);
    final raw = data['defaults'];
    final map = <String, int>{};
    if (raw is Map) {
      raw.forEach((k, v) => map[k.toString()] = v.toString() == '1' ? 1 : 0);
    }
    if (!mounted) return;
    setState(() => defaults = map);
  }

  bool effective(String key) {
    final override = overrides[key];
    if (override == 'ALLOW') return true;
    if (override == 'DENY') return false;
    return (defaults[key] ?? 0) == 1;
  }

  void toggle(String key, bool value) {
    setState(() {
      dirty = true;
      final fallback = (defaults[key] ?? 0) == 1;
      if (value == fallback) {
        overrides.remove(key);
      } else {
        overrides[key] = value ? 'ALLOW' : 'DENY';
      }
      final parent = posPermissionDependencies[key];
      if (value && parent != null && !effective(parent)) {
        overrides[parent] = 'ALLOW';
      }
    });
  }

  Future<void> save() async {
    final session = ref.read(authControllerProvider).session;
    if (session == null) return;
    if (name.text.trim().isEmpty || mobile.text.trim().length != 10) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter name and 10-digit mobile')),
      );
      return;
    }
    if (!isEdit && (pin.text != confirmPin.text || pin.text.length < 4)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('PIN must be 4 or 6 digits and match')),
      );
      return;
    }
    setState(() => loading = true);
    try {
      if (isEdit) {
        await ref.read(staffApiProvider).update(
              userId: session.licenceUserId,
              id: widget.staffId!,
              name: name.text.trim(),
              mobileNumber: mobile.text.trim(),
              address: address.text.trim(),
              overrides: overrides,
            );
      } else {
        await ref.read(staffApiProvider).create(
              userId: session.licenceUserId,
              name: name.text.trim(),
              mobileNumber: mobile.text.trim(),
              role: role,
              pin: pin.text.trim(),
              confirmPin: confirmPin.text.trim(),
              address: address.text.trim(),
              overrides: overrides,
            );
      }
      if (!mounted) return;
      Navigator.pop(context);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
      );
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> resetDefaults() async {
    if (dirty) {
      final ok = await showAppConfirmBottomSheet(
        context: context,
        title: 'Reset to role defaults?',
        message: 'User-specific permission changes will be removed.',
        confirmLabel: 'Reset',
      );
      if (!ok) return;
    }
    setState(() {
      overrides = {};
      dirty = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(isEdit ? 'Edit User' : 'Add User')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          TextField(controller: name, decoration: const InputDecoration(labelText: 'Name *')),
          TextField(
            controller: mobile,
            keyboardType: TextInputType.phone,
            maxLength: 10,
            inputFormatters: [FilteringTextInputFormatter.digitsOnly],
            decoration: const InputDecoration(labelText: 'Mobile Number *'),
          ),
          TextField(controller: address, decoration: const InputDecoration(labelText: 'Address')),
          const SizedBox(height: 12),
          AppDropdownFormField<String>(
            label: 'Role *',
            items: posFixedRoles.keys.toList(),
            itemLabel: (key) => posFixedRoles[key] ?? key,
            value: role,
            onChanged: isEdit
                ? (_) {}
                : (value) async {
                    if (value == null) return;
                    setState(() {
                      role = value;
                      overrides = {};
                    });
                    await loadDefaults();
                  },
          ),
          if (!isEdit) ...[
            TextField(
              controller: pin,
              obscureText: true,
              keyboardType: TextInputType.number,
              maxLength: 6,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              decoration: const InputDecoration(labelText: 'App Login PIN *'),
            ),
            TextField(
              controller: confirmPin,
              obscureText: true,
              keyboardType: TextInputType.number,
              maxLength: 6,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              decoration: const InputDecoration(labelText: 'Confirm PIN *'),
            ),
          ],
          const SizedBox(height: 16),
          Row(
            children: [
              const Text('PERMISSIONS', style: TextStyle(fontWeight: FontWeight.w700)),
              const Spacer(),
              TextButton(onPressed: resetDefaults, child: const Text('Reset to Role Defaults')),
            ],
          ),
          for (final module in posPermissionCatalog.entries)
            Card(
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Text(module.key.toUpperCase(), style: const TextStyle(fontWeight: FontWeight.w700)),
                        const Spacer(),
                        TextButton(
                          onPressed: () {
                            for (final action in module.value) {
                              toggle('${module.key}.$action', true);
                            }
                          },
                          child: const Text('All'),
                        ),
                        TextButton(
                          onPressed: () {
                            for (final action in module.value) {
                              toggle('${module.key}.$action', false);
                            }
                          },
                          child: const Text('Clear'),
                        ),
                      ],
                    ),
                    for (final action in module.value)
                      SwitchListTile(
                        dense: true,
                        title: Text(action.replaceAll('_', ' ')),
                        value: effective('${module.key}.$action'),
                        activeThumbColor: AppColors.navy,
                        onChanged: (v) => toggle('${module.key}.$action', v),
                      ),
                  ],
                ),
              ),
            ),
          const SizedBox(height: 12),
          AppButton(
            label: loading ? 'Saving…' : 'Save User',
            onPressed: loading ? null : save,
          ),
        ],
      ),
    );
  }
}
