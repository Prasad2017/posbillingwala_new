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

/// Adaptive banner — same placements as WithTable Login / UserSetting / About.
class AdBanner extends StatefulWidget {
  const AdBanner({super.key, required this.slot});

  final AdSlot slot;

  @override
  State<AdBanner> createState() => _AdBannerState();
}

class _AdBannerState extends State<AdBanner> {
  BannerAd? _ad;
  var _loaded = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_ad != null || !adsSupported) return;
    _load();
  }

  Future<void> _load() async {
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
          setState(() => _loaded = true);
        },
        onAdFailedToLoad: (ad, _) {
          ad.dispose();
          if (mounted) setState(() => _loaded = false);
        },
      ),
      request: const AdRequest(),
    );
    _ad = ad;
    await ad.load();
  }

  @override
  void dispose() {
    _ad?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!adsSupported || !_loaded || _ad == null) {
      return const SizedBox.shrink();
    }
    return SizedBox(
      width: _ad!.size.width.toDouble(),
      height: _ad!.size.height.toDouble(),
      child: AdWidget(ad: _ad!),
    );
  }
}
