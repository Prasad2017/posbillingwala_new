import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/staff/data/staff_store.dart';
import 'package:pos_billingwala_v2/features/staff/domain/staff_user.dart';

/* Who can see which rows:
 * - Staff login → filter by [staffId] (their own invoices / reports).
 * - Owner PB-PIN / licence (no staff) → full licence / branch data. */
class DataScope {
  const DataScope({
    required this.licenceUserId,
    this.ownerId,
    this.staff,
  });

  final String licenceUserId;
  final String? ownerId;
  final StaffUser? staff;

  int? get staffId {
    final id = int.tryParse(staff?.id.trim() ?? '');
    return (id != null && id > 0) ? id : null;
  }

  String get staffName => staff?.name.trim() ?? '';

  /* True when UI/DB should restrict rows to this staff. */
  bool get isStaffScoped => staffId != null;

  /* False for owner/licence session — show all licence/branch data. */
  bool get isOwnerScoped => !isStaffScoped;
}

/* Rebuilds when auth status / session user changes; reads StaffStore. */
final dataScopeProvider = FutureProvider<DataScope>((ref) async {
  final auth = ref.watch(authControllerProvider);
  final session = auth.session;
  final licenceUserId = session?.licenceUserId.trim() ?? '';
  final ownerId = session?.ownerId?.trim();

  /* Owner MPIN / licence unlock clears staff; only staff login keeps StaffStore. */
  final staff = (auth.status == AuthStatus.authenticated)
      ? await StaffStore().read()
      : null;

  return DataScope(
    licenceUserId: licenceUserId,
    ownerId: (ownerId != null && ownerId.isNotEmpty) ? ownerId : null,
    staff: staff,
  );
});

/* Sync-friendly staff id for Drift / API filters (null = owner scope). */
final invoiceStaffFilterProvider = Provider<AsyncValue<int?>>((ref) {
  return ref.watch(dataScopeProvider).whenData((scope) => scope.staffId);
});
