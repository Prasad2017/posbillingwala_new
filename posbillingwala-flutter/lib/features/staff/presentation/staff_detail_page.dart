import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/staff/domain/permission_catalog.dart';
import 'package:pos_billingwala_v2/features/staff/domain/permission_controller.dart';
import 'package:pos_billingwala_v2/features/staff/domain/staff_user.dart';

class StaffDetailPage extends ConsumerStatefulWidget {
  const StaffDetailPage({super.key, required this.staffId});

  final String staffId;

  @override
  ConsumerState<StaffDetailPage> createState() => StaffDetailPageState();
}

class StaffDetailPageState extends ConsumerState<StaffDetailPage> {
  StaffUser? user;
  String? error;

  @override
  void initState() {
    super.initState();
    Future.microtask(load);
  }

  Future<void> load() async {
    final session = ref.read(authControllerProvider).session;
    if (session == null) return;
    try {
      final loaded =
          await ref.read(staffApiProvider).get(session.licenceUserId, widget.staffId);
      if (!mounted) return;
      setState(() => user = loaded);
    } catch (e) {
      if (!mounted) return;
      setState(() => error = e.toString().replaceFirst('Exception: ', ''));
    }
  }

  Future<void> resetPin() async {
    final pin = TextEditingController();
    final confirm = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Reset PIN'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: pin,
              obscureText: true,
              keyboardType: TextInputType.number,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              decoration: const InputDecoration(labelText: 'New PIN'),
            ),
            TextField(
              controller: confirm,
              obscureText: true,
              keyboardType: TextInputType.number,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              decoration: const InputDecoration(labelText: 'Confirm PIN'),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.pop(context, true), child: const Text('Reset')),
        ],
      ),
    );
    if (ok != true || !mounted) return;
    final session = ref.read(authControllerProvider).session;
    if (session == null) return;
    try {
      await ref.read(staffApiProvider).resetPin(
            userId: session.licenceUserId,
            id: widget.staffId,
            pin: pin.text.trim(),
            confirmPin: confirm.text.trim(),
          );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('PIN reset')),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final perms = ref.watch(permissionControllerProvider);
    final user = this.user;
    return Scaffold(
      appBar: AppBar(title: const Text('User details')),
      body: user == null
          ? Center(child: error == null ? const CircularProgressIndicator() : Text(error!))
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                ListTile(title: const Text('Name'), subtitle: Text(user.name)),
                ListTile(title: const Text('Mobile'), subtitle: Text(user.mobileNumber)),
                ListTile(title: const Text('Role'), subtitle: Text(user.roleLabel)),
                ListTile(title: const Text('Status'), subtitle: Text(user.status)),
                ListTile(title: const Text('Last login'), subtitle: Text(user.lastLoginAt.isEmpty ? '—' : user.lastLoginAt)),
                const Divider(),
                const Text('Effective permissions', style: TextStyle(fontWeight: FontWeight.w700)),
                for (final key in posAllPermissionKeys())
                  if (user.allows(key)) Chip(label: Text(key)),
                const SizedBox(height: 16),
                if (perms.allows('user.edit'))
                  AppButton(
                    label: 'Edit',
                    onPressed: () => context.push('/settings/users/${user.id}/edit'),
                  ),
                if (perms.allows('user.edit'))
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: AppButton(
                      label: 'Reset to role defaults',
                      variant: AppButtonVariant.outlined,
                      onPressed: () async {
                        final session = ref.read(authControllerProvider).session;
                        if (session == null) return;
                        await ref.read(staffApiProvider).update(
                              userId: session.licenceUserId,
                              id: widget.staffId,
                              name: user.name,
                              mobileNumber: user.mobileNumber,
                              address: user.address,
                              status: user.status,
                              overrides: const {},
                            );
                        await load();
                        if (!context.mounted) return;
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text('Permissions reset to role defaults'),
                          ),
                        );
                      },
                    ),
                  ),
                if (perms.allows('user.change_role'))
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: AppButton(
                      label: 'Change role',
                      variant: AppButtonVariant.outlined,
                      onPressed: () async {
                        final next = await showDialog<String>(
                          context: context,
                          builder: (context) => SimpleDialog(
                            title: const Text('Change role'),
                            children: [
                              for (final entry in posFixedRoles.entries)
                                SimpleDialogOption(
                                  onPressed: () => Navigator.pop(context, entry.key),
                                  child: Text(entry.value),
                                ),
                            ],
                          ),
                        );
                        if (next == null) return;
                        final session = ref.read(authControllerProvider).session;
                        if (session == null) return;
                        await ref.read(staffApiProvider).changeRole(
                              session.licenceUserId,
                              widget.staffId,
                              next,
                            );
                        await load();
                      },
                    ),
                  ),
                if (perms.allows('user.reset_pin'))
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: AppButton(
                      label: 'Reset PIN',
                      variant: AppButtonVariant.outlined,
                      onPressed: resetPin,
                    ),
                  ),
                if (perms.allows('user.deactivate'))
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: AppButton(
                      label: 'Deactivate',
                      variant: AppButtonVariant.danger,
                      onPressed: () async {
                        final session = ref.read(authControllerProvider).session;
                        if (session == null) return;
                        await ref.read(staffApiProvider).deactivate(
                              session.licenceUserId,
                              widget.staffId,
                            );
                        if (context.mounted) Navigator.pop(context);
                      },
                    ),
                  ),
              ],
            ),
    );
  }
}
