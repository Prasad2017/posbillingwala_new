import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

/* SharedPreferences cache filled by Fetch Data From Cloud for API-only screens. */
abstract final class CloudScreenCache {
  CloudScreenCache._();

  static const prefix = 'cloud_screen_v1_';

  static const staff = 'staff';
  static const salary = 'salary';
  static const roleDefaults = 'role_defaults';
  static const mealSessions = 'meal_sessions';
  static const mealTokensToday = 'meal_tokens_today';
  static const pendingMealTokens = 'pending_meal_tokens';
  static const messShopPayerMode = 'mess_shop_payer_mode';
  static const storePrinters = 'store_printers';
  static const printerRoutes = 'printer_routes';
  static const printJobs = 'print_jobs';
  static const supportTickets = 'support_tickets';
  static const posDevices = 'pos_devices';
  static const homeOverviewToday = 'home_overview_today';
  static const homeOverviewMonth = 'home_overview_month';
  static const messCommonQr = 'mess_common_qr';

  static const allKeys = [
    staff,
    salary,
    roleDefaults,
    mealSessions,
    mealTokensToday,
    pendingMealTokens,
    messShopPayerMode,
    storePrinters,
    printerRoutes,
    printJobs,
    supportTickets,
    posDevices,
    homeOverviewToday,
    homeOverviewMonth,
    messCommonQr,
  ];

  static String _key(String name) => '$prefix$name';

  static Future<void> saveJson(String name, Object? value) async {
    final prefs = await SharedPreferences.getInstance();
    if (value == null) {
      await prefs.remove(_key(name));
      return;
    }
    await prefs.setString(_key(name), jsonEncode(value));
  }

  static Future<Object?> loadJson(String name) async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_key(name));
    if (raw == null || raw.isEmpty) return null;
    try {
      return jsonDecode(raw);
    } catch (_) {
      return null;
    }
  }

  static Future<List<Map<String, dynamic>>> loadMapList(String name) async {
    final decoded = await loadJson(name);
    if (decoded is! List) return const [];
    return decoded
        .whereType<Map>()
        .map((e) => Map<String, dynamic>.from(e))
        .toList();
  }

  static Future<Map<String, dynamic>?> loadMap(String name) async {
    final decoded = await loadJson(name);
    if (decoded is! Map) return null;
    return Map<String, dynamic>.from(decoded);
  }

  static Future<int> count(String name) async {
    final decoded = await loadJson(name);
    if (decoded is List) return decoded.length;
    if (decoded is Map) return decoded.isEmpty ? 0 : 1;
    if (decoded is String) return decoded.trim().isEmpty ? 0 : 1;
    return 0;
  }

  static Future<Map<String, int>> allCounts() async {
    final out = <String, int>{};
    for (final key in allKeys) {
      out[key] = await count(key);
    }
    return out;
  }
}
