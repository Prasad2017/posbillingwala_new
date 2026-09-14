import 'package:shared_preferences/shared_preferences.dart';

/// Mess-level payer mode: user (default) or institute.
/// Cached locally; synced via `mess_shop_setting_*` APIs (WithTable `MessPayerMode`).
abstract final class MessPayerMode {
  MessPayerMode._();

  static const String modeUser = 'user';
  static const String modeInstitute = 'institute';
  static const String prefKey = 'mess_payer_mode';

  static String normalize(String? mode) {
    if (mode != null && mode.trim().toLowerCase() == modeInstitute) {
      return modeInstitute;
    }
    return modeUser;
  }

  static Future<bool> isInstitutePay() async {
    return (await get()) == modeInstitute;
  }

  static Future<String> get() async {
    final prefs = await SharedPreferences.getInstance();
    return normalize(prefs.getString(prefKey));
  }

  static Future<void> setLocal(String mode) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(prefKey, normalize(mode));
  }
}
