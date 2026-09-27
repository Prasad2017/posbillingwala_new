import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/business_type/app_feature.dart';
import 'package:pos_billingwala_v2/core/business_type/business_profile.dart';
import 'package:pos_billingwala_v2/core/business_type/business_type.dart';
import 'package:pos_billingwala_v2/core/business_type/business_type_registry.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/auth/domain/user_session.dart';
import 'package:pos_billingwala_v2/features/enterprise/data/enterprise_api.dart';
import 'package:pos_billingwala_v2/features/staff/domain/permission_controller.dart';
import 'package:shared_preferences/shared_preferences.dart';

const _prefsBusinessTypeKey = 'businessType';

/* Resolves effective business type from session + local override. */
BusinessType resolveBusinessType(UserSession? session) {
  final fromSession = BusinessTypeX.tryParse(session?.businessType);
  if (fromSession != null) return fromSession;

  if (session != null) {
    return BusinessTypeRegistry.inferFromLicence(
      fastBilling: session.fastBilling,
      dineIn: session.dineIn,
      takeAway: session.takeAway,
      mess: session.mess,
    );
  }
  return BusinessType.restaurant;
}

final businessTypeProvider = Provider<BusinessType>((ref) {
  final session = ref.watch(authControllerProvider).session;
  return resolveBusinessType(session);
});

final businessProfileProvider = Provider<BusinessProfile>((ref) {
  return BusinessTypeRegistry.of(ref.watch(businessTypeProvider));
});

final businessTerminologyProvider = Provider<BusinessTerminology>((ref) {
  return ref.watch(businessProfileProvider).terminology;
});

/* True when the active business profile enables [feature]. */
final featureEnabledProvider = Provider.family<bool, AppFeature>((ref, feature) {
  return ref.watch(businessProfileProvider).has(feature);
});

/* Persists business type locally and mirrors into the active session. */
class BusinessTypeController extends Notifier<BusinessType> {
  @override
  BusinessType build() => resolveBusinessType(
    ref.watch(authControllerProvider).session,
  );

  Future<void> setBusinessType(BusinessType type) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_prefsBusinessTypeKey, type.id);

    final auth = ref.read(authControllerProvider.notifier);
    final session = ref.read(authControllerProvider).session;
    if (session != null) {
      await auth.updateSessionFields(businessType: type.id);
      if (await isDeviceOnline()) {
        try {
          await ref.read(enterpriseApiProvider).saveBusinessType(
                userId: session.licenceUserId,
                businessType: type.id,
              );
        } catch (_) {}
      }
    }
    state = type;
  }

  Future<BusinessType?> loadLocalOverride() async {
    final prefs = await SharedPreferences.getInstance();
    return BusinessTypeX.tryParse(prefs.getString(_prefsBusinessTypeKey));
  }
}

final businessTypeControllerProvider =
    NotifierProvider<BusinessTypeController, BusinessType>(
      BusinessTypeController.new,
    );

/* Combines business profile + staff permission for a route/action. */
bool canAccessFeature(
  WidgetRef ref, {
  required AppFeature? feature,
  String? permission,
}) {
  if (feature != null && !ref.read(businessProfileProvider).has(feature)) {
    return false;
  }
  if (permission != null &&
      !ref.read(permissionControllerProvider).allows(permission)) {
    return false;
  }
  return true;
}

bool profileAllowsFeature(BusinessProfile profile, AppFeature? feature) {
  if (feature == null) return true;
  return profile.has(feature);
}
