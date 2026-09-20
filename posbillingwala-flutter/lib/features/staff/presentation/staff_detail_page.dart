import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/staff/data/staff_offline_queue.dart';
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
  bool permissionsExpanded = true;

  @override
  void initState() {
    super.initState();
    Future.microtask(load);
  }

  Future<void> load() async {
    final session = ref.read(authControllerProvider).session;
    if (session == null) return;
    final cached = await StaffOfflineQueue.getStaffFromCache(widget.staffId);
    if (cached != null && mounted) {
      setState(() {
        user = cached;
        error = null;
      });
    }
    if (!await isDeviceOnline()) {
      if (cached == null && mounted) {
        setState(() => error = 'User not found offline');
      }
      return;
    }
    try {
      final loaded = await ref
          .read(staffApiProvider)
          .get(session.licenceUserId, widget.staffId);
      await StaffOfflineQueue.upsertStaffCache(loaded);
      if (!mounted) return;
      setState(() => user = loaded);
    } catch (e) {
      if (!mounted) return;
      if (user != null) return;
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
            AppTextField(
              required: true,
              controller: pin,
              label: 'New PIN',
              obscureText: true,
              keyboardType: TextInputType.number,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
            ),
            AppTextField(
              required: true,
              controller: confirm,
              label: 'Confirm PIN',
              obscureText: true,
              keyboardType: TextInputType.number,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Reset'),
          ),
        ],
      ),
    );
    if (ok != true || !mounted) return;
    final session = ref.read(authControllerProvider).session;
    if (session == null) return;
    try {
      final result = await StaffOfflineQueue.resetPin(
        api: ref.read(staffApiProvider),
        userId: session.licenceUserId,
        id: widget.staffId,
        pin: pin.text.trim(),
        confirmPin: confirm.text.trim(),
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            result.synced ? 'PIN reset' : result.message,
          ),
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
      );
    }
  }

  Future<void> resetRoleDefaults(StaffUser user) async {
    final session = ref.read(authControllerProvider).session;
    if (session == null) return;
    try {
      final result = await StaffOfflineQueue.update(
        api: ref.read(staffApiProvider),
        userId: session.licenceUserId,
        id: widget.staffId,
        name: user.name,
        mobileNumber: user.mobileNumber,
        address: user.address,
        status: user.status,
        overrides: const {},
      );
      await load();
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            result.synced
                ? 'Permissions reset to role defaults'
                : result.message,
          ),
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
      );
    }
  }

  Future<void> changeRole() async {
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
    try {
      final result = await StaffOfflineQueue.changeRole(
        api: ref.read(staffApiProvider),
        userId: session.licenceUserId,
        id: widget.staffId,
        role: next,
      );
      await load();
      if (!mounted) return;
      if (result.pending) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(result.message)),
        );
      }
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
      );
    }
  }

  Future<void> deactivateUser() async {
    final session = ref.read(authControllerProvider).session;
    if (session == null) return;
    try {
      final result = await StaffOfflineQueue.deactivate(
        api: ref.read(staffApiProvider),
        userId: session.licenceUserId,
        id: widget.staffId,
      );
      if (!mounted) return;
      if (result.pending) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(result.message)),
        );
      }
      Navigator.pop(context);
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
    final effective = user == null
        ? const <String>[]
        : posAllPermissionKeys().where(user.allows).toList(growable: false);

    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(title: const Text('User details')),
      body: user == null
          ? Center(
              child: error == null
                  ? const CircularProgressIndicator()
                  : Text(error!),
            )
          : ListView(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 28),
              children: [
                _DetailCard(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          CircleAvatar(
                            radius: 24,
                            backgroundColor: AppColors.navy,
                            child: Text(
                              user.name.isEmpty
                                  ? '?'
                                  : user.name[0].toUpperCase(),
                              style: const TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.w800,
                                fontSize: 18,
                              ),
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  user.name,
                                  style: const TextStyle(
                                    fontFamily: AppFonts.family,
                                    fontWeight: FontWeight.w800,
                                    fontSize: 17,
                                    color: AppColors.navy,
                                  ),
                                ),
                                const SizedBox(height: 2),
                                Text(
                                  user.roleLabel,
                                  style: TextStyle(
                                    fontFamily: AppFonts.family,
                                    fontSize: 13,
                                    fontWeight: FontWeight.w600,
                                    color: AppColors.navy.withValues(
                                      alpha: 0.55,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                          _StatusBadge(status: user.status),
                        ],
                      ),
                      const SizedBox(height: 14),
                      const Divider(height: 1),
                      const SizedBox(height: 10),
                      _InfoRow(label: 'Mobile', value: user.mobileNumber),
                      _InfoRow(label: 'Role', value: user.roleLabel),
                      _InfoRow(label: 'Status', value: user.status),
                      _InfoRow(
                        label: 'Last login',
                        value: user.lastLoginAt.isEmpty
                            ? '—'
                            : user.lastLoginAt,
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                _DetailCard(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      InkWell(
                        onTap: () => setState(
                          () => permissionsExpanded = !permissionsExpanded,
                        ),
                        borderRadius: BorderRadius.circular(8),
                        child: Padding(
                          padding: const EdgeInsets.symmetric(vertical: 2),
                          child: Row(
                            children: [
                              const Expanded(
                                child: Text(
                                  'Effective permissions',
                                  style: TextStyle(
                                    fontFamily: AppFonts.family,
                                    fontWeight: FontWeight.w800,
                                    fontSize: 15,
                                    color: AppColors.navy,
                                  ),
                                ),
                              ),
                              Text(
                                '${effective.length}',
                                style: TextStyle(
                                  fontFamily: AppFonts.family,
                                  fontWeight: FontWeight.w700,
                                  fontSize: 12.5,
                                  color: AppColors.navy.withValues(alpha: 0.5),
                                ),
                              ),
                              const SizedBox(width: 4),
                              Icon(
                                permissionsExpanded
                                    ? Icons.expand_less_rounded
                                    : Icons.expand_more_rounded,
                                color: AppColors.navy,
                              ),
                            ],
                          ),
                        ),
                      ),
                      if (permissionsExpanded) ...[
                        const SizedBox(height: 10),
                        if (effective.isEmpty)
                          Text(
                            'No permissions assigned',
                            style: TextStyle(
                              fontFamily: AppFonts.family,
                              color: AppColors.navy.withValues(alpha: 0.5),
                            ),
                          )
                        else
                          Align(
                            alignment: Alignment.centerLeft,
                            child: Wrap(
                              alignment: WrapAlignment.start,
                              crossAxisAlignment: WrapCrossAlignment.start,
                              spacing: 8,
                              runSpacing: 8,
                              children: [
                                for (final key in effective)
                                  Container(
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 10,
                                      vertical: 6,
                                    ),
                                    decoration: BoxDecoration(
                                      color: AppColors.primarySoft,
                                      borderRadius: BorderRadius.circular(8),
                                      border: Border.all(
                                        color: AppColors.primary.withValues(
                                          alpha: 0.22,
                                        ),
                                        width: 1,
                                      ),
                                    ),
                                    child: Text(
                                      key,
                                      style: const TextStyle(
                                        fontFamily: AppFonts.family,
                                        fontSize: 12,
                                        fontWeight: FontWeight.w600,
                                        color: AppColors.navy,
                                      ),
                                    ),
                                  ),
                              ],
                            ),
                          ),
                      ],
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                _DetailCard(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Actions',
                        style: TextStyle(
                          fontFamily: AppFonts.family,
                          fontWeight: FontWeight.w800,
                          fontSize: 15,
                          color: AppColors.navy,
                        ),
                      ),
                      const SizedBox(height: 12),
                      LayoutBuilder(
                        builder: (context, constraints) {
                          final actions = <Widget>[
                            if (perms.allows('user.edit'))
                              _ActionTile(
                                icon: Icons.edit_outlined,
                                label: 'Edit',
                                onTap: () => context.push(
                                  '/settings/users/${user.id}/edit',
                                ),
                              ),
                            if (perms.allows('user.edit'))
                              _ActionTile(
                                icon: Icons.restart_alt_rounded,
                                label: 'Role defaults',
                                onTap: () => resetRoleDefaults(user),
                              ),
                            if (perms.allows('user.change_role'))
                              _ActionTile(
                                icon: Icons.badge_outlined,
                                label: 'Change role',
                                onTap: changeRole,
                              ),
                            if (perms.allows('user.reset_pin'))
                              _ActionTile(
                                icon: Icons.pin_outlined,
                                label: 'Reset PIN',
                                onTap: resetPin,
                              ),
                            if (perms.allows('user.deactivate'))
                              _ActionTile(
                                icon: Icons.person_off_outlined,
                                label: 'Deactivate',
                                danger: true,
                                onTap: deactivateUser,
                              ),
                          ];
                          if (actions.isEmpty) {
                            return Text(
                              'No actions available',
                              style: TextStyle(
                                fontFamily: AppFonts.family,
                                color: AppColors.navy.withValues(alpha: 0.5),
                              ),
                            );
                          }
                          final cols = constraints.maxWidth >= 420 ? 3 : 2;
                          return GridView.count(
                            crossAxisCount: cols,
                            mainAxisSpacing: 10,
                            crossAxisSpacing: 10,
                            childAspectRatio: cols == 3 ? 2.4 : 2.2,
                            shrinkWrap: true,
                            physics: const NeverScrollableScrollPhysics(),
                            children: actions,
                          );
                        },
                      ),
                    ],
                  ),
                ),
              ],
            ),
    );
  }
}

class _DetailCard extends StatelessWidget {
  const _DetailCard({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(14, 14, 14, 14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(
          color: AppColors.primary.withValues(alpha: 0.18),
          width: 1.5,
        ),
      ),
      child: child,
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 5),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 92,
            child: Text(
              label,
              style: TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 13,
                fontWeight: FontWeight.w700,
                color: AppColors.navy.withValues(alpha: 0.55),
              ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: const TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 13.5,
                fontWeight: FontWeight.w600,
                color: AppColors.navy,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _StatusBadge extends StatelessWidget {
  const _StatusBadge({required this.status});

  final String status;

  @override
  Widget build(BuildContext context) {
    final active =
        status.toUpperCase() == 'ACTIVE' || status.toLowerCase() == 'active';
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: active ? const Color(0xFFE8F8EE) : const Color(0xFFF3F4F6),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(
          color: active
              ? AppColors.green.withValues(alpha: 0.35)
              : const Color(0xFFD1D5DB),
          width: 1,
        ),
      ),
      child: Text(
        status.toUpperCase(),
        style: TextStyle(
          fontFamily: AppFonts.family,
          fontSize: 11,
          fontWeight: FontWeight.w800,
          color: active ? AppColors.green : AppColors.textSecondary,
        ),
      ),
    );
  }
}

class _ActionTile extends StatelessWidget {
  const _ActionTile({
    required this.icon,
    required this.label,
    required this.onTap,
    this.danger = false,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;
  final bool danger;

  @override
  Widget build(BuildContext context) {
    final color = danger ? AppColors.danger : AppColors.primary;
    return Material(
      color: danger ? const Color(0xFFFFF1F1) : AppColors.primarySoft,
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(12),
            border: Border.all(
              color: color.withValues(alpha: 0.28),
              width: 1,
            ),
          ),
          child: Row(
            children: [
              Icon(icon, size: 18, color: color),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  label,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    fontSize: 12.5,
                    fontWeight: FontWeight.w700,
                    color: color,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
