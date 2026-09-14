import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/app/app.dart';
import 'package:pos_billingwala_v2/features/ads/ad_banner.dart';
import 'package:pos_billingwala_v2/features/notifications/domain/fcm_service.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  try {
    await FcmService().initialize();
  } catch (_) {}
  await initializeMobileAds();

  runApp(
    const ProviderScope(
      child: PosBillingwalaApp(),
    ),
  );
}
