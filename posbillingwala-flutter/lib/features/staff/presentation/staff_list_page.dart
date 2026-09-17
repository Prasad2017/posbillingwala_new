import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/staff/domain/permission_controller.dart';
import 'package:pos_billingwala_v2/features/staff/domain/staff_user.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_cache.dart';

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
      final cached = await CloudScreenCache.loadMapList(CloudScreenCache.staff);
      if (cached.isNotEmpty && mounted) {
        setState(() {
          users = cached.map(StaffUser.fromJson).toList();
          loading = false;
        });
      }
      final list =
          await ref.read(staffApiProvider).list(session.licenceUserId);
      if (!mounted) return;
      setState(() {
        users = list;
        loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        error = e.toString().replaceFirst('Exception: ', '');
        loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final canCreate = ref.watch(permissionControllerProvider).allows('user.create');
    return Scaffold(
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
              : RefreshIndicator(
                  onRefresh: load,
                  child: ListView.separated(
                    padding: const EdgeInsets.all(16),
                    itemCount: users.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 8),
                    itemBuilder: (context, index) {
                      final user = users[index];
                      return ListTile(
                        tileColor: Colors.white,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                        leading: CircleAvatar(
                          backgroundColor: AppColors.navy,
                          child: Text(
                            user.name.isEmpty ? '?' : user.name[0].toUpperCase(),
                            style: const TextStyle(color: Colors.white),
                          ),
                        ),
                        title: Text(user.name),
                        subtitle: Text('${user.roleLabel} · ${user.mobileNumber}'),
                        trailing: Text(user.status),
                        onTap: () async {
                          await context.push('/settings/users/${user.id}');
                          await load();
                        },
                      );
                    },
                  ),
                ),
    );
  }
}
