import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/app/router.dart';
import 'package:pos_billingwala_v2/app/theme.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_constants.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/aurora_background.dart';
import 'package:pos_billingwala_v2/features/settings/presentation/in_app_update_host.dart';
import 'package:pos_billingwala_v2/features/sync/domain/catalog_bootstrap_listener.dart';
import 'package:pos_billingwala_v2/features/sync/domain/connectivity_sync_listener.dart';
import 'package:pos_billingwala_v2/features/sync/domain/web_cloud_refresh_listener.dart';
import 'package:pos_billingwala_v2/language/app_languages.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

class PosBillingwalaApp extends ConsumerWidget {
  const PosBillingwalaApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(routerProvider);
    /* Recover empty catalog after wrong-id sync / first login. */
    ref.watch(catalogBootstrapListenerProvider);
    /* Mobile: offline-first UI; auto-sync to cloud every 2 minutes when online. */
    if (AppPlatform.supportsOfflineSync) {
      ref.watch(connectivitySyncListenerProvider);
    }
    /* Web: auto-fetch cloud so Android/iOS entries appear. */
    if (AppPlatform.autoCloudRefresh) {
      ref.watch(webCloudRefreshListenerProvider);
    }
    final locale = ref.watch(appLocaleProvider);

    return MaterialApp.router(
      title: AppConstants.appName,
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      locale: locale,
      supportedLocales: [
        for (final code in AppLanguages.supportedCodes) Locale(code),
      ],
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      routerConfig: router,
      builder: (context, child) {
        /* Lock text size at 0.9 — ignore system Display / Text size. */
        final mq = MediaQuery.of(context);
        return MediaQuery(
          data: mq.copyWith(
            textScaler: const TextScaler.linear(0.9),
            boldText: false,
          ),
          child: AnnotatedRegion<SystemUiOverlayStyle>(
            value: AppTheme.lightSystemUi,
            child: ColoredBox(
              color: AppColors.primary,
              child: SafeArea(
                child: AuroraBackground(
                  child: InAppUpdateHost(
                    child: child ?? const SizedBox.shrink(),
                  ),
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}
