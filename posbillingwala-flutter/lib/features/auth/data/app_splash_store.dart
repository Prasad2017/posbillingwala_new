import 'dart:io';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:path_provider/path_provider.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';
import 'package:pos_billingwala_v2/core/network/api_client.dart';
import 'package:pos_billingwala_v2/core/utils/json_parsers.dart';
import 'package:shared_preferences/shared_preferences.dart';

/* Admin splash: first load from net → file cache; later show cache; refresh when online. */
class AppSplashArt {
  const AppSplashArt({this.localPath, this.networkUrl});

  final String? localPath;
  final String? networkUrl;

  bool get isEmpty =>
      (localPath == null || localPath!.trim().isEmpty) &&
      (networkUrl == null || networkUrl!.trim().isEmpty);
}

class AppSplashStore {
  static const prefsUrlKey = 'app_splash_image_url';
  static const cacheFileName = 'app_splash_cached.png';

  AppSplashStore(this.client);

  final ApiClient client;

  static String? normalizeUrl(String? raw) {
    final url = raw?.trim() ?? '';
    if (url.isEmpty) return null;
    return url;
  }

  Future<String?> readCachedUrl() async {
    final prefs = await SharedPreferences.getInstance();
    return normalizeUrl(prefs.getString(prefsUrlKey));
  }

  Future<void> writeCachedUrl(String? url) async {
    final prefs = await SharedPreferences.getInstance();
    final normalized = normalizeUrl(url);
    if (normalized == null) {
      await prefs.remove(prefsUrlKey);
      return;
    }
    await prefs.setString(prefsUrlKey, normalized);
  }

  Future<File?> cacheFile() async {
    if (kIsWeb) return null;
    try {
      final docs = await getApplicationDocumentsDirectory();
      return File('${docs.path}/$cacheFileName');
    } catch (_) {
      return null;
    }
  }

  /* Offline / cold start: prefer local file, else last URL. */
  Future<AppSplashArt?> readCachedArt() async {
    final url = await readCachedUrl();
    if (!kIsWeb) {
      final file = await cacheFile();
      if (file != null && await file.exists() && await file.length() > 32) {
        return AppSplashArt(localPath: file.path, networkUrl: url);
      }
    }
    if (url == null) return null;
    return AppSplashArt(networkUrl: url);
  }

  Future<void> clearCache() async {
    await writeCachedUrl(null);
    if (kIsWeb) return;
    try {
      final file = await cacheFile();
      if (file != null && await file.exists()) await file.delete();
      /* Remove legacy .bin cache if present. */
      final docs = await getApplicationDocumentsDirectory();
      final legacy = File('${docs.path}/app_splash_cached.bin');
      if (await legacy.exists()) await legacy.delete();
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
    if (bytes[0] == 0xFF && bytes[1] == 0xD8 && bytes[2] == 0xFF) {
      return true;
    }
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
  Future<void> downloadToCache(String url) async {
    if (kIsWeb) return;
    final file = await cacheFile();
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

  /* Online: fetch URL from API, save prefs + download image bytes. */
  Future<AppSplashArt?> fetchAndCache() async {
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
      if (data is! Map) return readCachedArt();
      final map = Map<String, dynamic>.from(data);
      final status = parseString(map['status'])?.toLowerCase();
      if (status != 'true' && status != '1') return readCachedArt();

      final url = normalizeUrl(parseString(map['imageUrl']));
      if (url == null) {
        await clearCache();
        return null;
      }

      await writeCachedUrl(url);
      await downloadToCache(url);
      return readCachedArt();
    } catch (_) {
      return readCachedArt();
    }
  }
}
