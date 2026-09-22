import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/staff/data/staff_offline_queue.dart';
import 'package:pos_billingwala_v2/features/staff/domain/permission_controller.dart';
import 'package:pos_billingwala_v2/features/staff/domain/staff_user.dart';

class StaffListPage extends ConsumerStatefulWidget {
  const StaffListPage({super.key});

  @override
  ConsumerState<StaffListPage> createState() => StaffListPageState();
}

class StaffListPageState extends ConsumerState<StaffListPage> {
  List<StaffUser> users = const [];
  String? error;
  bool loading = true;

  @override
  void initState() {
    super.initState();
    Future.microtask(load);
  }

  Future<void> load() async {
    final session = ref.read(authControllerProvider).session;
    if (session == null) return;
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final cached = await StaffOfflineQueue.loadStaffCache();
      if (cached.isNotEmpty && mounted) {
        setState(() {
          users = cached;
          loading = false;
        });
      }
      if (!await isDeviceOnline()) {
        if (mounted) setState(() => loading = false);
        return;
      }
      final list = await ref.read(staffApiProvider).list(session.licenceUserId);
      /* Keep pending local-only users until sync creates them. */
      final pendingLocal = cached
          .where((e) => e.id.startsWith('staff_local_'))
          .toList();
      final merged = [
        ...list,
        ...pendingLocal.where(
          (local) => list.every((s) => s.mobileNumber != local.mobileNumber),
        ),
      ];
      if (!await StaffOfflineQueue.hasPendingOps()) {
        await StaffOfflineQueue.saveStaffCache(merged);
      } else {
        final byId = {for (final u in merged) u.id: u};
        for (final local in cached) {
          if (local.id.startsWith('staff_local_')) {
            byId[local.id] = local;
          }
        }
        await StaffOfflineQueue.saveStaffCache(byId.values.toList());
      }
      final painted = await StaffOfflineQueue.loadStaffCache();
      if (!mounted) return;
      setState(() {
        users = painted;
        loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        if (users.isEmpty) {
          error = e.toString().replaceFirst('Exception: ', '');
        }
        loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final canCreate = ref
        .watch(permissionControllerProvider)
        .allows('user.create');
    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(title: const Text('Users')),
      floatingActionButton: canCreate
          ? FloatingActionButton(
              onPressed: () async {
                await context.push('/settings/users/add');
                await load();
              },
              child: const Icon(Icons.add),
            )
          : null,
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : error != null
          ? Center(child: Text(error!))
          : users.isEmpty
          ? const Center(child: Text('No users yet'))
          : RefreshIndicator(
              onRefresh: load,
              child: ResponsiveScrollShell(
                dashboard: true,
                child: ListView.separated(
                padding: EdgeInsets.fromLTRB(
                  AppBreakpoints.pagePaddingFor(context.widthClass),
                  16,
                  AppBreakpoints.pagePaddingFor(context.widthClass),
                  88,
                ),
                itemCount: users.length,
                separatorBuilder: (_, _) => const SizedBox(height: 10),
                itemBuilder: (context, index) {
                  final user = users[index];
                  final active =
                      user.status.toUpperCase() == 'ACTIVE' ||
                      user.status.toLowerCase() == 'active';
                  return Material(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(14),
                    child: InkWell(
                      borderRadius: BorderRadius.circular(14),
                      onTap: () async {
                        await context.push('/settings/users/${user.id}');
                        await load();
                      },
                      child: Container(
                        padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(14),
                          border: Border.all(
                            color: AppColors.primary.withValues(alpha: 0.18),
                            width: 1.5,
                          ),
                        ),
                        child: Row(
                          children: [
                            CircleAvatar(
                              radius: 22,
                              backgroundColor: AppColors.navy,
                              child: Text(
                                user.name.isEmpty
                                    ? '?'
                                    : user.name[0].toUpperCase(),
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontWeight: FontWeight.w800,
                                  fontSize: 16,
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
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: const TextStyle(
                                      fontFamily: AppFonts.family,
                                      fontWeight: FontWeight.w800,
                                      fontSize: 15,
                                      color: AppColors.navy,
                                    ),
                                  ),
                                  const SizedBox(height: 3),
                                  Text(
                                    '${user.roleLabel} · ${user.mobileNumber}',
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: TextStyle(
                                      fontFamily: AppFonts.family,
                                      fontSize: 12.5,
                                      color: AppColors.navy.withValues(
                                        alpha: 0.55,
                                      ),
                                      fontWeight: FontWeight.w500,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            const SizedBox(width: 8),
                            Flexible(
                              child: Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 8,
                                vertical: 4,
                              ),
                              decoration: BoxDecoration(
                                color: active
                                    ? const Color(0xFFE8F8EE)
                                    : const Color(0xFFF3F4F6),
                                borderRadius: BorderRadius.circular(8),
                                border: Border.all(
                                  color: active
                                      ? AppColors.green.withValues(alpha: 0.35)
                                      : const Color(0xFFD1D5DB),
                                  width: 1,
                                ),
                              ),
                              child: Text(
                                user.status.toUpperCase(),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(
                                  fontFamily: AppFonts.family,
                                  fontSize: 11,
                                  fontWeight: FontWeight.w800,
                                  color: active
                                      ? AppColors.green
                                      : AppColors.textSecondary,
                                ),
                              ),
                            ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  );
                },
              ),
              ),
            ),
    );
  }
}
