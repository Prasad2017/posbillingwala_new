import 'dart:convert';
import 'dart:io';
import 'dart:ui' show Size;

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:path_provider/path_provider.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';
import 'package:pos_billingwala_v2/core/network/api_client.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/json_parsers.dart';
import 'package:shared_preferences/shared_preferences.dart';

/* One admin splash image (network and/or on-disk cache). */
class AppSplashArt {
  const AppSplashArt({this.localPath, this.networkUrl});

  final String? localPath;
  final String? networkUrl;

  bool get isEmpty =>
      (localPath == null || localPath!.trim().isEmpty) &&
      (networkUrl == null || networkUrl!.trim().isEmpty);
}

/* Screen the splash artwork was designed for. */
enum SplashSlot {
  mobilePortrait('mobile_portrait', 'mobilePortrait'),
  mobileLandscape('mobile_landscape', 'mobileLandscape'),
  tabletPortrait('tablet_portrait', 'tabletPortrait'),
  tabletLandscape('tablet_landscape', 'tabletLandscape'),
  webPortrait('web_portrait', 'webPortrait'),
  webLandscape('web_landscape', 'webLandscape');

  const SplashSlot(this.id, this.apiKey);

  final String id;
  final String apiKey;

  static SplashSlot? byId(String? raw) {
    final id = raw?.trim() ?? '';
    for (final slot in values) {
      if (slot.id == id) return slot;
    }
    return null;
  }
}

/* Chosen artwork plus whether it was uploaded for this exact screen. */
class SplashPick {
  const SplashPick({required this.art, required this.exact});

  final AppSplashArt art;
  final bool exact;
}

/* All uploaded splashes. The app picks one from the current window. */
class AppSplashLibrary {
  const AppSplashLibrary({this.slots = const {}, this.legacy});

  static const legacyId = 'legacy';

  final Map<SplashSlot, AppSplashArt> slots;
  final AppSplashArt? legacy;

  bool get isEmpty =>
      slots.values.every((art) => art.isEmpty) &&
      (legacy == null || legacy!.isEmpty);

  /*
   * Web uses the browser window: desktop width → web art, tablet width →
   * tablet art, phone width → phone art.
   * Android / iOS use the device (shortest side), so a wide tablet stays
   * on tablet art instead of the desktop splash.
   */
  static SplashSlot slotForSize(Size size, {required bool isWeb}) {
    final landscape = size.width > size.height;
    if (isWeb) {
      if (size.width >= AppBreakpoints.desktopMin) {
        return landscape ? SplashSlot.webLandscape : SplashSlot.webPortrait;
      }
      if (size.width >= AppBreakpoints.tabletMin) {
        return landscape
            ? SplashSlot.tabletLandscape
            : SplashSlot.tabletPortrait;
      }
      return landscape ? SplashSlot.mobileLandscape : SplashSlot.mobilePortrait;
    }
    final tablet = size.shortestSide >= AppBreakpoints.tabletMin;
    if (tablet) {
      return landscape ? SplashSlot.tabletLandscape : SplashSlot.tabletPortrait;
    }
    return landscape ? SplashSlot.mobileLandscape : SplashSlot.mobilePortrait;
  }

  /* Same orientation first, then the other orientation, then legacy. */
  static List<SplashSlot> fallbackOrder(SplashSlot slot) {
    return switch (slot) {
      SplashSlot.mobilePortrait => const [
        SplashSlot.mobilePortrait,
        SplashSlot.tabletPortrait,
        SplashSlot.webPortrait,
        SplashSlot.mobileLandscape,
        SplashSlot.tabletLandscape,
        SplashSlot.webLandscape,
      ],
      SplashSlot.mobileLandscape => const [
        SplashSlot.mobileLandscape,
        SplashSlot.tabletLandscape,
        SplashSlot.webLandscape,
        SplashSlot.mobilePortrait,
        SplashSlot.tabletPortrait,
        SplashSlot.webPortrait,
      ],
      SplashSlot.tabletPortrait => const [
        SplashSlot.tabletPortrait,
        SplashSlot.webPortrait,
        SplashSlot.mobilePortrait,
        SplashSlot.tabletLandscape,
        SplashSlot.webLandscape,
        SplashSlot.mobileLandscape,
      ],
      SplashSlot.tabletLandscape => const [
        SplashSlot.tabletLandscape,
        SplashSlot.webLandscape,
        SplashSlot.mobileLandscape,
        SplashSlot.tabletPortrait,
        SplashSlot.webPortrait,
        SplashSlot.mobilePortrait,
      ],
      SplashSlot.webPortrait => const [
        SplashSlot.webPortrait,
        SplashSlot.tabletPortrait,
        SplashSlot.mobilePortrait,
        SplashSlot.webLandscape,
        SplashSlot.tabletLandscape,
        SplashSlot.mobileLandscape,
      ],
      SplashSlot.webLandscape => const [
        SplashSlot.webLandscape,
        SplashSlot.tabletLandscape,
        SplashSlot.mobileLandscape,
        SplashSlot.webPortrait,
        SplashSlot.tabletPortrait,
        SplashSlot.mobilePortrait,
      ],
    };
  }

  SplashPick? pick(Size size, {required bool isWeb}) {
    final wanted = slotForSize(size, isWeb: isWeb);
    for (final slot in fallbackOrder(wanted)) {
      final art = slots[slot];
      if (art != null && !art.isEmpty) {
        return SplashPick(art: art, exact: slot == wanted);
      }
    }
    final fallback = legacy;
    if (fallback != null && !fallback.isEmpty) {
      return SplashPick(art: fallback, exact: false);
    }
    return null;
  }
}

/* Admin splash: first load from net → file cache; later show cache; refresh when online. */
class AppSplashStore {
  static const prefsUrlKey = 'app_splash_image_url';
  static const prefsMapKey = 'app_splash_slot_urls';
  static const legacyCacheFileName = 'app_splash_cached.png';

  AppSplashStore(this.client);

  final ApiClient client;

  static String? normalizeUrl(String? raw) {
    final url = raw?.trim() ?? '';
    if (url.isEmpty) return null;
    return url;
  }

  Future<Map<String, String>> readUrlMap() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(prefsMapKey);
    final map = <String, String>{};
    if (raw != null && raw.trim().isNotEmpty) {
      try {
        final decoded = jsonDecode(raw);
        if (decoded is Map) {
          decoded.forEach((key, value) {
            final id = key.toString();
            final url = normalizeUrl(value?.toString());
            if (url != null) map[id] = url;
          });
        }
      } catch (_) {}
    }
    if (map.isEmpty) {
      final legacy = normalizeUrl(prefs.getString(prefsUrlKey));
      if (legacy != null) map[AppSplashLibrary.legacyId] = legacy;
    }
    return map;
  }

  Future<void> writeUrlMap(Map<String, String> urls) async {
    final prefs = await SharedPreferences.getInstance();
    final clean = <String, String>{};
    urls.forEach((key, value) {
      final url = normalizeUrl(value);
      if (url != null) clean[key] = url;
    });
    if (clean.isEmpty) {
      await prefs.remove(prefsMapKey);
      await prefs.remove(prefsUrlKey);
      return;
    }
    await prefs.setString(prefsMapKey, jsonEncode(clean));
    final legacy = clean[AppSplashLibrary.legacyId];
    if (legacy == null) {
      await prefs.remove(prefsUrlKey);
    } else {
      await prefs.setString(prefsUrlKey, legacy);
    }
  }

  Future<File?> cacheFileFor(String slot) async {
    if (kIsWeb) return null;
    try {
      final docs = await getApplicationDocumentsDirectory();
      final name = slot == AppSplashLibrary.legacyId
          ? legacyCacheFileName
          : 'app_splash_$slot.png';
      return File('${docs.path}/$name');
    } catch (_) {
      return null;
    }
  }

  Future<AppSplashArt?> artForSlot(String slot, Map<String, String> urls) async {
    final url = normalizeUrl(urls[slot]);
    if (!kIsWeb) {
      final file = await cacheFileFor(slot);
      if (file != null && await file.exists() && await file.length() > 32) {
        return AppSplashArt(localPath: file.path, networkUrl: url);
      }
    }
    if (url == null) return null;
    return AppSplashArt(networkUrl: url);
  }

  /* Offline / cold start: local files plus last URLs. */
  Future<AppSplashLibrary?> readCachedLibrary() async {
    final urls = await readUrlMap();
    final slots = <SplashSlot, AppSplashArt>{};
    for (final slot in SplashSlot.values) {
      final art = await artForSlot(slot.id, urls);
      if (art != null && !art.isEmpty) slots[slot] = art;
    }
    final legacy = await artForSlot(AppSplashLibrary.legacyId, urls);
    final library = AppSplashLibrary(
      slots: slots,
      legacy: legacy != null && !legacy.isEmpty ? legacy : null,
    );
    if (library.isEmpty) return null;
    return library;
  }

  Future<void> clearCache() async {
    await writeUrlMap(const {});
    if (kIsWeb) return;
    try {
      for (final slot in SplashSlot.values) {
        final file = await cacheFileFor(slot.id);
        if (file != null && await file.exists()) await file.delete();
      }
      final legacy = await cacheFileFor(AppSplashLibrary.legacyId);
      if (legacy != null && await legacy.exists()) await legacy.delete();
      final docs = await getApplicationDocumentsDirectory();
      final oldBin = File('${docs.path}/app_splash_cached.bin');
      if (await oldBin.exists()) await oldBin.delete();
    } catch (_) {}
  }

  static bool looksLikeImage(Uint8List bytes) {
    if (bytes.length < 8) return false;
    /* PNG */
    if (bytes[0] == 0x89 &&
        bytes[1] == 0x50 &&
        bytes[2] == 0x4E &&
        bytes[3] == 0x47) {
      return true;
    }
    /* JPEG */
    if (bytes[0] == 0xFF && bytes[1] == 0xD8 && bytes[2] == 0xFF) return true;
    /* WEBP: RIFF....WEBP */
    if (bytes.length >= 12 &&
        bytes[0] == 0x52 &&
        bytes[1] == 0x49 &&
        bytes[2] == 0x46 &&
        bytes[3] == 0x46 &&
        bytes[8] == 0x57 &&
        bytes[9] == 0x45 &&
        bytes[10] == 0x42 &&
        bytes[11] == 0x50) {
      return true;
    }
    return false;
  }

  /* Separate Dio — app ApiClient defaults to JSON and Accept: application/json. */
  Future<void> downloadToCache(String slot, String url) async {
    if (kIsWeb) return;
    final file = await cacheFileFor(slot);
    if (file == null) return;
    try {
      final dio = Dio(
        BaseOptions(
          connectTimeout: const Duration(seconds: 8),
          receiveTimeout: const Duration(seconds: 20),
          responseType: ResponseType.bytes,
          headers: const {'Accept': '*/*'},
          validateStatus: (s) => s != null && s >= 200 && s < 400,
        ),
      );
      final response = await dio.get<List<int>>(url);
      final raw = response.data;
      if (raw == null || raw.isEmpty) return;
      final bytes = Uint8List.fromList(raw);
      if (!looksLikeImage(bytes)) return;
      await file.writeAsBytes(bytes, flush: true);
    } catch (_) {
      /* Keep previous file if download fails. */
    }
  }

  Future<void> deleteSlotFile(String slot) async {
    if (kIsWeb) return;
    try {
      final file = await cacheFileFor(slot);
      if (file != null && await file.exists()) await file.delete();
    } catch (_) {}
  }

  Map<String, String> urlsFromPayload(Map<String, dynamic> map) {
    final urls = <String, String>{};
    final images = map['images'];
    if (images is Map) {
      final imageMap = Map<String, dynamic>.from(images);
      for (final slot in SplashSlot.values) {
        final url = normalizeUrl(parseString(imageMap[slot.apiKey]));
        if (url != null) urls[slot.id] = url;
      }
    }
    if (map.containsKey('legacyUrl')) {
      final legacy = normalizeUrl(parseString(map['legacyUrl']));
      if (legacy != null) urls[AppSplashLibrary.legacyId] = legacy;
    } else if (urls.isEmpty) {
      final single = normalizeUrl(parseString(map['imageUrl']));
      if (single != null) urls[AppSplashLibrary.legacyId] = single;
    }
    return urls;
  }

  /* Online: fetch URLs, save prefs, download bytes for the next cold start. */
  Future<AppSplashLibrary?> fetchAndCache() async {
    try {
      final response = await client.dio.get<dynamic>(
        ApiEndpoints.getAppSplash,
        options: Options(
          validateStatus: (s) => s != null && s < 500,
          sendTimeout: const Duration(seconds: 4),
          receiveTimeout: const Duration(seconds: 4),
        ),
      );
      final data = response.data;
      if (data is! Map) return await readCachedLibrary();
      final map = Map<String, dynamic>.from(data);
      final status = parseString(map['status'])?.toLowerCase();
      if (status != 'true' && status != '1') return await readCachedLibrary();

      final urls = urlsFromPayload(map);
      if (urls.isEmpty) {
        await clearCache();
        return null;
      }

      final previous = await readUrlMap();
      await writeUrlMap(urls);

      final known = <String>{
        ...SplashSlot.values.map((slot) => slot.id),
        AppSplashLibrary.legacyId,
      };
      for (final slot in known) {
        if (!urls.containsKey(slot)) await deleteSlotFile(slot);
      }

      final downloads = <Future<void>>[];
      for (final entry in urls.entries) {
        final unchanged = previous[entry.key] == entry.value;
        if (unchanged && !kIsWeb) {
          final file = await cacheFileFor(entry.key);
          if (file != null && await file.exists() && await file.length() > 32) {
            continue;
          }
        }
        downloads.add(downloadToCache(entry.key, entry.value));
      }
      if (downloads.isNotEmpty) {
        await Future.wait(downloads).timeout(
          const Duration(seconds: 8),
          onTimeout: () => <void>[],
        );
      }
      return await readCachedLibrary();
    } catch (_) {
      return await readCachedLibrary();
    }
  }
}
