import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/app/app.dart';
import 'package:pos_billingwala_v2/core/logging/file_log_store.dart';
import 'package:pos_billingwala_v2/core/security/screenshot_config.dart';
import 'package:pos_billingwala_v2/features/notifications/domain/fcm_service.dart';
import 'package:pos_billingwala_v2/language/locale_catalog.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Color(0xFF0756C9),
      statusBarIconBrightness: Brightness.light,
      statusBarBrightness: Brightness.dark,
      systemNavigationBarColor: Colors.white,
      systemNavigationBarIconBrightness: Brightness.dark,
      systemNavigationBarDividerColor: Colors.transparent,
    ),
  );
  await LocaleCatalog.load();
  await ScreenshotConfig.apply();
  await FileLogStore.init();
  try {
    await FcmService().initialize();
  } catch (_) {}

  runApp(
    const ProviderScope(
      child: PosBillingwalaApp(),
    ),
  );
}
