import 'dart:async';

import 'package:flutter/painting.dart';
import 'package:pos_billingwala_v2/core/logging/file_log_store.dart';
import 'package:pos_billingwala_v2/core/security/screenshot_config.dart';
import 'package:pos_billingwala_v2/features/notifications/domain/fcm_service.dart';
import 'package:pos_billingwala_v2/language/locale_catalog.dart';

/* Work that must not block the first Flutter frame. */
abstract final class AppStartup {
  AppStartup._();

  static bool _heavyStarted = false;

  static void configureImageCache() {
    final cache = PaintingBinding.instance.imageCache;
    cache.maximumSize = 250;
    cache.maximumSizeBytes = 80 << 20;
  }

  static void scheduleHeavyServices() {
    if (_heavyStarted) return;
    _heavyStarted = true;
    unawaited(_runHeavyServices());
  }

  static Future<void> _runHeavyServices() async {
    configureImageCache();
    await Future.wait<void>([
      LocaleCatalog.loadRemaining(),
      ScreenshotConfig.apply(),
      FileLogStore.init(),
    ]);
    try {
      await FcmService().initialize();
    } catch (_) {}
  }
}
