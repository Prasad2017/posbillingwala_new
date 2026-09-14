import 'package:pos_billingwala_v2/core/constants/api_constants.dart';

class AppConstants {
  AppConstants._();

  static const String appName = 'POS Billingwala';
  static const String appVersionLabel = 'Version 2';
  static const String appVersionBadge = 'V 2.0.1';
  static const String website = 'https://www.posbillingwala.com';
  static const String websiteDisplay = 'www.posbillingwala.com';
  static const String supportEmail = 'info@posbillingwala.com';
  static const String supportPhone = '+918983149299';
  static const String developerName = 'Cana Tech Solutions Private Limited';
  static const String playStoreUrl =
      'https://play.google.com/store/apps/details?id=com.pos_billingwala';
  static const String supportHours = 'Mon–Sat · 10:00 AM – 07:00 PM IST';
  static const String supportHoursDays = 'Monday - Saturday';
  static const String supportHoursTime = '10:00 AM - 07:00 PM';

  /// Prefer [ApiConstants.baseUrl]; kept for older call sites.
  static const String apiBaseUrl = ApiConstants.baseUrl;
}
