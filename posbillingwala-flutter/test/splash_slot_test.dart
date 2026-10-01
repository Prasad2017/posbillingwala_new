import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pos_billingwala_v2/features/auth/data/app_splash_store.dart';

void main() {
  const phone = AppSplashArt(networkUrl: 'https://example.com/phone.png');
  const tabletLand = AppSplashArt(networkUrl: 'https://example.com/tablet-land.png');
  const legacy = AppSplashArt(networkUrl: 'https://example.com/legacy.png');

  SplashPick? pick(Size size, {required bool isWeb, AppSplashLibrary? library}) {
    final lib = library ??
        const AppSplashLibrary(
          slots: {
            SplashSlot.mobilePortrait: phone,
            SplashSlot.tabletLandscape: tabletLand,
          },
          legacy: legacy,
        );
    return lib.pick(size, isWeb: isWeb);
  }

  test('phone portrait and landscape stay on the phone slots', () {
    expect(
      AppSplashLibrary.slotForSize(const Size(390, 844), isWeb: false),
      SplashSlot.mobilePortrait,
    );
    expect(
      AppSplashLibrary.slotForSize(const Size(844, 390), isWeb: false),
      SplashSlot.mobileLandscape,
    );
  });

  test('tablet uses shortest side, including a wide landscape tablet', () {
    expect(
      AppSplashLibrary.slotForSize(const Size(800, 1280), isWeb: false),
      SplashSlot.tabletPortrait,
    );
    expect(
      AppSplashLibrary.slotForSize(const Size(1366, 1024), isWeb: false),
      SplashSlot.tabletLandscape,
    );
  });

  test('web follows the browser window, not the physical device', () {
    expect(
      AppSplashLibrary.slotForSize(const Size(1440, 900), isWeb: true),
      SplashSlot.webLandscape,
    );
    expect(
      AppSplashLibrary.slotForSize(const Size(1400, 1800), isWeb: true),
      SplashSlot.webPortrait,
    );
    expect(
      AppSplashLibrary.slotForSize(const Size(900, 700), isWeb: true),
      SplashSlot.tabletLandscape,
    );
    expect(
      AppSplashLibrary.slotForSize(const Size(390, 844), isWeb: true),
      SplashSlot.mobilePortrait,
    );
  });

  test('exact upload fills that screen; a missing slot falls back', () {
    final exact = pick(const Size(390, 844), isWeb: false);
    expect(exact!.exact, isTrue);
    expect(exact.art.networkUrl, phone.networkUrl);

    final fallback = pick(const Size(1280, 800), isWeb: false);
    expect(fallback!.exact, isTrue);
    expect(fallback.art.networkUrl, tabletLand.networkUrl);

    final missing = pick(
      const Size(844, 390),
      isWeb: false,
      library: const AppSplashLibrary(legacy: legacy),
    );
    expect(missing!.exact, isFalse);
    expect(missing.art.networkUrl, legacy.networkUrl);
  });

  test('same orientation is preferred over the opposite orientation', () {
    final library = const AppSplashLibrary(
      slots: {
        SplashSlot.webLandscape: AppSplashArt(
          networkUrl: 'https://example.com/web-land.png',
        ),
        SplashSlot.mobilePortrait: phone,
      },
    );
    final pick = library.pick(const Size(844, 390), isWeb: false);
    expect(pick!.art.networkUrl, 'https://example.com/web-land.png');
    expect(pick.exact, isFalse);
  });
}
