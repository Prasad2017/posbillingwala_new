import 'package:flutter/foundation.dart';

/* WithTable AdMob IDs (`AndroidManifest` + login / settings / about layouts). */
abstract final class AdConfig {
  AdConfig._();

  static const androidAppId = 'ca-app-pub-2325627373654257~2682249830';

  static const adConfigLogin = 'ca-app-pub-2325627373654257/8547020221';
  static const adConfigSettings = 'ca-app-pub-2325627373654257/3294693541';
  static const adConfigAbout = 'ca-app-pub-2325627373654257/6815283571';
  static const googleTestBanner = 'ca-app-pub-3940256099942544/6300978111';

  static String unitId(AdSlot slot) {
    if (!kReleaseMode) return googleTestBanner;
    switch (slot) {
      case AdSlot.login:
        return adConfigLogin;
      case AdSlot.settings:
        return adConfigSettings;
      case AdSlot.about:
        return adConfigAbout;
    }
  }
}

enum AdSlot { login, settings, about }
