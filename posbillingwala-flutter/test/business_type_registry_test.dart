import 'package:flutter_test/flutter_test.dart';
import 'package:pos_billingwala_v2/core/business_type/app_feature.dart';
import 'package:pos_billingwala_v2/core/business_type/business_type.dart';
import 'package:pos_billingwala_v2/core/business_type/business_type_registry.dart';

void main() {
  group('BusinessTypeRegistry', () {
    test('restaurant exposes tables and KOT but not size matrix', () {
      final profile = BusinessTypeRegistry.of(BusinessType.restaurant);
      expect(profile.has(AppFeature.tables), isTrue);
      expect(profile.has(AppFeature.kot), isTrue);
      expect(profile.has(AppFeature.portions), isTrue);
      expect(profile.has(AppFeature.sizeColorMatrix), isFalse);
      expect(profile.has(AppFeature.rooms), isFalse);
    });

    test('clothing exposes variants and hides tables/KOT', () {
      final profile = BusinessTypeRegistry.of(BusinessType.clothingStore);
      expect(profile.has(AppFeature.sizeColorMatrix), isTrue);
      expect(profile.has(AppFeature.exchange), isTrue);
      expect(profile.has(AppFeature.tables), isFalse);
      expect(profile.has(AppFeature.kot), isFalse);
      expect(profile.has(AppFeature.mess), isFalse);
    });

    test('supermarket exposes batch/expiry and purchase', () {
      final profile = BusinessTypeRegistry.of(BusinessType.supermarket);
      expect(profile.has(AppFeature.batchExpiry), isTrue);
      expect(profile.has(AppFeature.purchaseFlow), isTrue);
      expect(profile.has(AppFeature.warehouses), isTrue);
      expect(profile.has(AppFeature.tables), isFalse);
    });

    test('hotel exposes rooms plus restaurant modules', () {
      final profile = BusinessTypeRegistry.of(BusinessType.hotel);
      expect(profile.has(AppFeature.rooms), isTrue);
      expect(profile.has(AppFeature.roomBooking), isTrue);
      expect(profile.has(AppFeature.tables), isTrue);
      expect(profile.has(AppFeature.kot), isTrue);
    });

    test('bakery is counter-only without dine-in tables', () {
      final profile = BusinessTypeRegistry.of(BusinessType.cakeShopBakery);
      expect(profile.has(AppFeature.quickBilling), isTrue);
      expect(profile.has(AppFeature.takeaway), isTrue);
      expect(profile.has(AppFeature.tables), isFalse);
      expect(profile.has(AppFeature.dineIn), isFalse);
    });

    test('inferFromLicence prefers restaurant when F&B flags set', () {
      expect(
        BusinessTypeRegistry.inferFromLicence(
          fastBilling: true,
          dineIn: true,
          takeAway: false,
          mess: false,
        ),
        BusinessType.restaurant,
      );
      expect(
        BusinessTypeRegistry.inferFromLicence(
          fastBilling: true,
          dineIn: false,
          takeAway: false,
          mess: false,
        ),
        BusinessType.retailStore,
      );
    });

    test('BusinessType.tryParse accepts aliases', () {
      expect(BusinessTypeX.tryParse('clothing'), BusinessType.clothingStore);
      expect(BusinessTypeX.tryParse('dmart'), BusinessType.supermarket);
      expect(BusinessTypeX.tryParse('restaurant'), BusinessType.restaurant);
    });
  });
}
