import 'dart:convert';

import 'package:flutter/services.dart';

// Loads common EN/HI/MR strings from assets/locale JSON files.
class LocaleCatalog {
  LocaleCatalog._();

  static final Map<String, Map<String, String>> _byLang = {};
  static bool _loaded = false;

  static bool get isLoaded => _loaded;

  /* First-paint path: English only. */
  static Future<void> loadEnglish() async {
    if (_byLang.containsKey('en')) return;
    await _loadLang('en');
  }

  /* Remaining locales after the first frame. */
  static Future<void> loadRemaining() async {
    await Future.wait([
      if (!_byLang.containsKey('hi')) _loadLang('hi'),
      if (!_byLang.containsKey('mr')) _loadLang('mr'),
    ]);
    _loaded = true;
  }

  static Future<void> load() async {
    if (_loaded) return;
    await loadEnglish();
    await loadRemaining();
  }

  static Future<void> _loadLang(String lang) async {
    final raw = await rootBundle.loadString('assets/locale/$lang.json');
    final decoded = jsonDecode(raw);
    if (decoded is! Map) {
      _byLang[lang] = {};
      return;
    }
    final map = <String, String>{};
    decoded.forEach((key, value) {
      if (key is String && value != null) {
        map[key] = value.toString();
      }
    });
    _byLang[lang] = map;
  }

  static String get(String lang, String key) {
    final primary = _byLang[lang];
    if (primary != null) {
      final hit = primary[key];
      if (hit != null) return hit;
    }
    final en = _byLang['en'];
    if (en != null) {
      final hit = en[key];
      if (hit != null) return hit;
    }
    return key;
  }
}
