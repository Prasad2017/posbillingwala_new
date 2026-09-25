import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/app/app.dart';
import 'package:pos_billingwala_v2/core/logging/error_report_service.dart';
import 'package:pos_billingwala_v2/core/startup/app_startup.dart';
import 'package:pos_billingwala_v2/language/locale_catalog.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await ErrorReportService.bootstrap();
  /* Keep status + nav bars always visible (no immersive hide/show). */
  await SystemChrome.setEnabledSystemUIMode(
    SystemUiMode.manual,
    overlays: SystemUiOverlay.values,
  );
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Color(0xFF0756C9),
      statusBarIconBrightness: Brightness.light,
      statusBarBrightness: Brightness.dark,
      systemNavigationBarColor: Colors.white,
      systemNavigationBarIconBrightness: Brightness.dark,
      systemNavigationBarDividerColor: Colors.transparent,
      systemNavigationBarContrastEnforced: true,
    ),
  );
  /* English is enough to paint the first frame; HI/MR load after. */
  await LocaleCatalog.loadEnglish();

  runApp(const ProviderScope(child: PosBillingwalaApp()));

  WidgetsBinding.instance.addPostFrameCallback((_) {
    AppStartup.scheduleHeavyServices();
  });
}
