import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/staff/data/staff_api.dart';
import 'package:pos_billingwala_v2/features/staff/data/staff_store.dart';
import 'package:pos_billingwala_v2/features/staff/domain/staff_user.dart';

class PermissionState {
  const PermissionState({
    this.userManagementEnabled = false,
    this.staff,
    this.permissionVersion = '0',
  });

  final bool userManagementEnabled;
  final StaffUser? staff;
  final String permissionVersion;

  bool allows(String key) {
    if (!userManagementEnabled) return true;
    final staffUser = staff;
    /* Owner PB-PIN session (no staff) has full access. */
    if (staffUser == null) return true;
    return staffUser.allows(key);
  }
}

final staffStoreProvider = Provider<StaffStore>((ref) => StaffStore());

final staffApiProvider = Provider<StaffApi>((ref) {
  return StaffApi(ref.watch(apiClientProvider));
});

class PermissionController extends Notifier<PermissionState> {
  @override
  PermissionState build() {
    final session = ref.watch(authControllerProvider).session;
    final um = session?.userManagementEnabled ?? false;
    return PermissionState(userManagementEnabled: um);
  }

  Future<void> hydrate() async {
    final session = ref.read(authControllerProvider).session;
    final um = session?.userManagementEnabled ?? false;
    if (!um) {
      state = const PermissionState();
      return;
    }
    final cached = await ref.read(staffStoreProvider).read();
    final version = await ref.read(staffStoreProvider).permissionVersion();
    state = PermissionState(
      userManagementEnabled: true,
      staff: cached,
      permissionVersion: version,
    );
  }

  Future<void> applyLogin({
    required StaffUser staff,
    required String permissionVersion,
    required String sessionId,
  }) async {
    await ref.read(staffStoreProvider).save(
          staff: staff,
          permissionVersion: permissionVersion,
          sessionId: sessionId,
        );
    ref.read(apiClientProvider).setStaffId(staff.id);
    state = PermissionState(
      userManagementEnabled: true,
      staff: staff,
      permissionVersion: permissionVersion,
    );
  }

  Future<void> refreshFromServer() async {
    final session = ref.read(authControllerProvider).session;
    final staff = state.staff;
    if (session == null || staff == null) return;
    try {
      final latest = await ref.read(staffApiProvider).effective(
            session.licenceUserId,
            staff.id,
          );
      await applyLogin(
        staff: latest,
        permissionVersion: state.permissionVersion,
        sessionId: '',
      );
    } catch (_) {}
  }

  Future<void> clearStaff() async {
    await ref.read(staffStoreProvider).clear();
    ref.read(apiClientProvider).setStaffId(null);
    state = PermissionState(
      userManagementEnabled:
          ref.read(authControllerProvider).session?.userManagementEnabled ??
              false,
    );
  }
}

final permissionControllerProvider =
    NotifierProvider<PermissionController, PermissionState>(
  PermissionController.new,
);

bool staffCan(WidgetRef ref, String key) =>
    ref.read(permissionControllerProvider).allows(key);
