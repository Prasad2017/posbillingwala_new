import 'package:flutter/foundation.dart';

/// WithTable AdMob IDs (`AndroidManifest` + login / settings / about layouts).
abstract final class AdConfig {
  AdConfig._();

  static const androidAppId = 'ca-app-pub-2325627373654257~2682249830';

  static const _login = 'ca-app-pub-2325627373654257/8547020221';
  static const _settings = 'ca-app-pub-2325627373654257/3294693541';
  static const _about = 'ca-app-pub-2325627373654257/6815283571';
  static const _googleTestBanner = 'ca-app-pub-3940256099942544/6300978111';

  static String unitId(AdSlot slot) {
    if (!kReleaseMode) return _googleTestBanner;
    switch (slot) {
      case AdSlot.login:
        return _login;
      case AdSlot.settings:
        return _settings;
      case AdSlot.about:
        return _about;
    }
  }
}

enum AdSlot { login, settings, about }
