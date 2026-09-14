import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:google_mobile_ads/google_mobile_ads.dart';
import 'package:pos_billingwala_v2/features/ads/ad_config.dart';

bool get adsSupported =>
    !kIsWeb && (Platform.isAndroid || Platform.isIOS);

Future<void> initializeMobileAds() async {
  if (!adsSupported) return;
  try {
    await MobileAds.instance.initialize();
  } catch (_) {}
}

/* Adaptive banner — same placements as WithTable Login / UserSetting / About. */
class AdBanner extends StatefulWidget {
  const AdBanner({super.key, required this.slot});

  final AdSlot slot;

  @override
  State<AdBanner> createState() => AdBannerState();
}

class AdBannerState extends State<AdBanner> {
  BannerAd? adBannerAd;
  var loaded = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (adBannerAd != null || !adsSupported) return;
    adBannerLoad();
  }

  Future<void> adBannerLoad() async {
    final width = MediaQuery.sizeOf(context).width.truncate();
    final size = await AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
      width < 320 ? 320 : width,
    );
    if (!mounted || size == null) return;
    final ad = BannerAd(
      size: size,
      adUnitId: AdConfig.unitId(widget.slot),
      listener: BannerAdListener(
        onAdLoaded: (ad) {
          if (!mounted) {
            ad.dispose();
            return;
          }
          setState(() => loaded = true);
        },
        onAdFailedToLoad: (ad, _) {
          ad.dispose();
          if (mounted) setState(() => loaded = false);
        },
      ),
      request: const AdRequest(),
    );
    adBannerAd = ad;
    await ad.load();
  }

  @override
  void dispose() {
    adBannerAd?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!adsSupported || !loaded || adBannerAd == null) {
      return const SizedBox.shrink();
    }
    return SizedBox(
      width: adBannerAd!.size.width.toDouble(),
      height: adBannerAd!.size.height.toDouble(),
      child: AdWidget(ad: adBannerAd!),
    );
  }
}
