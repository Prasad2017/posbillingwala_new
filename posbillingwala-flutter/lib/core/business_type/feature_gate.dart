import 'package:pos_billingwala_v2/core/business_type/app_feature.dart';
import 'package:pos_billingwala_v2/core/business_type/business_profile.dart';
import 'package:pos_billingwala_v2/core/business_type/business_type.dart';
import 'package:pos_billingwala_v2/core/business_type/business_type_registry.dart';
import 'package:pos_billingwala_v2/features/auth/domain/license_modules.dart';
import 'package:pos_billingwala_v2/features/auth/domain/user_session.dart';

/*
 * Central gate: BusinessType features ∩ Licence modules ∩ (optional) permissions.
 *
 * Restaurant users never see clothing matrix.
 * Clothing users never see KOT / Tables.
 * Hotel users see rooms + restaurant modules.
 */
abstract final class FeatureGate {
  static BusinessProfile profileFor(UserSession? session) {
    final type = BusinessTypeX.tryParse(session?.businessType) ??
        (session == null
            ? BusinessType.restaurant
            : BusinessTypeRegistry.inferFromLicence(
                fastBilling: session.fastBilling,
                dineIn: session.dineIn,
                takeAway: session.takeAway,
                mess: session.mess,
              ));
    return BusinessTypeRegistry.of(type);
  }

  static bool has(UserSession? session, AppFeature feature) =>
      profileFor(session).has(feature);

  /* Licence flags still apply for legacy F&B modules. */
  static bool allowBillingMode(
    UserSession? session, {
    required AppFeature feature,
  }) {
    if (!has(session, feature)) return false;
    return switch (feature) {
      AppFeature.quickBilling || AppFeature.barcodeBilling =>
        LicenseModules.fastBilling(session),
      AppFeature.dineIn || AppFeature.tables =>
        LicenseModules.dineIn(session),
      AppFeature.takeaway || AppFeature.parcel =>
        LicenseModules.takeAway(session),
      AppFeature.mess => LicenseModules.mess(session),
      _ => true,
    };
  }

  static bool showTables(UserSession? session) =>
      allowBillingMode(session, feature: AppFeature.tables);

  static bool showKot(UserSession? session) => has(session, AppFeature.kot);

  static bool showPortions(UserSession? session) =>
      has(session, AppFeature.portions);

  static bool showVariants(UserSession? session) =>
      has(session, AppFeature.sizeColorMatrix) ||
      has(session, AppFeature.variants);

  static bool showRooms(UserSession? session) => has(session, AppFeature.rooms);

  static bool showPurchase(UserSession? session) =>
      has(session, AppFeature.purchaseFlow);

  static bool showBatchExpiry(UserSession? session) =>
      has(session, AppFeature.batchExpiry);

  static bool showMess(UserSession? session) =>
      allowBillingMode(session, feature: AppFeature.mess);
}
