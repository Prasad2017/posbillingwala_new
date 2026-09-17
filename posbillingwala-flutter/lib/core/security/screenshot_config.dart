import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:pos_billingwala_v2/core/constants/app_config.dart';
import 'package:pos_billingwala_v2/core/logging/app_logger.dart';

/* Applies [AppConfig.allowScreenshot] to the native window (Android FLAG_SECURE). */
abstract final class ScreenshotConfig {
  ScreenshotConfig._();

  static const MethodChannel _channel = MethodChannel(
    'pos_billingwala/screenshot',
  );

  static Future<void> apply() async {
    if (kIsWeb) return;
    try {
      await _channel.invokeMethod<void>(
        'setAllowScreenshot',
        AppConfig.allowScreenshot,
      );
    } catch (error) {
      AppLogger.error('ScreenshotConfig.apply', error);
    }
  }
}
