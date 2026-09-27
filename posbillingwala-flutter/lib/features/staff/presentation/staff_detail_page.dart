import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/features/staff/data/staff_offline_queue.dart';
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
  bool loading = true;

  @override
  void initState() {
    super.initState();
    Future.microtask(load);
  }

  Future<void> load() async {
    final cached = await StaffOfflineQueue.loadStaffCache();
    StaffUser? match;
    for (final u in cached) {
      if (u.id == widget.staffId) {
        match = u;
        break;
      }
    }
    if (!mounted) return;
    setState(() {
      user = match;
      loading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    final canEdit = ref.watch(permissionControllerProvider).allows('user.edit');
    final u = user;

    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(
        title: const Text('User details'),
        actions: [
          if (canEdit && u != null)
            IconButton(
              tooltip: 'Edit',
              onPressed: () async {
                await context.push('/settings/users/${u.id}/edit');
                await load();
              },
              icon: const Icon(Icons.edit_rounded),
            ),
        ],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : u == null
          ? const Center(child: Text('User not found'))
          : ResponsivePageBody(
              dashboard: true,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Container(
                    padding: const EdgeInsets.all(18),
                    decoration: BoxDecoration(
                      color: AppColors.glassSolid,
                      borderRadius: BorderRadius.circular(18),
                      border: Border.all(color: AppColors.border),
                    ),
                    child: Row(
                      children: [
                        CircleAvatar(
                          radius: 28,
                          backgroundColor: AppColors.primarySoft,
                          child: Text(
                            u.name.isEmpty ? '?' : u.name[0].toUpperCase(),
                            style: const TextStyle(
                              fontFamily: AppFonts.family,
                              fontWeight: FontWeight.w800,
                              color: AppColors.primary,
                              fontSize: 22,
                            ),
                          ),
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                u.name,
                                style: const TextStyle(
                                  fontFamily: AppFonts.family,
                                  fontWeight: FontWeight.w800,
                                  fontSize: 18,
                                  color: AppColors.navy,
                                ),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                u.roleLabel.isNotEmpty ? u.roleLabel : u.role,
                                style: TextStyle(
                                  fontFamily: AppFonts.family,
                                  color: AppColors.navy.withValues(alpha: .6),
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                            ],
                          ),
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 10,
                            vertical: 6,
                          ),
                          decoration: BoxDecoration(
                            color: u.isActive
                                ? AppColors.green.withValues(alpha: .12)
                                : AppColors.red.withValues(alpha: .12),
                            borderRadius: BorderRadius.circular(20),
                          ),
                          child: Text(
                            u.isActive ? 'Active' : u.status,
                            style: TextStyle(
                              fontFamily: AppFonts.family,
                              fontWeight: FontWeight.w700,
                              fontSize: 12,
                              color: u.isActive
                                  ? AppColors.green
                                  : AppColors.red,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 14),
                  _InfoCard(
                    rows: [
                      ('Mobile', u.mobileNumber),
                      ('Address', u.address.isEmpty ? '—' : u.address),
                      (
                        'Monthly salary',
                        u.monthlySalary > 0
                            ? u.monthlySalary.toStringAsFixed(0)
                            : '—',
                      ),
                      (
                        'Last login',
                        u.lastLoginAt.isEmpty ? '—' : u.lastLoginAt,
                      ),
                    ],
                  ),
                  if (canEdit) ...[
                    const SizedBox(height: 20),
                    FilledButton.icon(
                      onPressed: () async {
                        await context.push('/settings/users/${u.id}/edit');
                        await load();
                      },
                      icon: const Icon(Icons.edit_rounded),
                      label: const Text('Edit user'),
                      style: FilledButton.styleFrom(
                        minimumSize: const Size.fromHeight(50),
                        backgroundColor: AppColors.primary,
                      ),
                    ),
                  ],
                ],
              ),
            ),
    );
  }
}

class _InfoCard extends StatelessWidget {
  const _InfoCard({required this.rows});

  final List<(String, String)> rows;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
      decoration: BoxDecoration(
        color: AppColors.glassFill,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.glassBorder),
      ),
      child: Column(
        children: [
          for (var i = 0; i < rows.length; i++) ...[
            if (i > 0) Divider(color: AppColors.border.withValues(alpha: .7)),
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(
                rows[i].$1,
                style: TextStyle(
                  fontFamily: AppFonts.family,
                  fontSize: 12,
                  color: AppColors.navy.withValues(alpha: .5),
                  fontWeight: FontWeight.w600,
                ),
              ),
              subtitle: Text(
                rows[i].$2,
                style: const TextStyle(
                  fontFamily: AppFonts.family,
                  fontSize: 15,
                  color: AppColors.navy,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
