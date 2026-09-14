import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:pos_billingwala_v2/app/router.dart';
import 'package:pos_billingwala_v2/app/theme.dart';
import 'package:pos_billingwala_v2/core/constants/app_constants.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/sync/domain/catalog_bootstrap_listener.dart';
import 'package:pos_billingwala_v2/features/sync/domain/connectivity_sync_listener.dart';
import 'package:pos_billingwala_v2/features/sync/domain/web_cloud_refresh_listener.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';
import 'package:pos_billingwala_v2/features/settings/presentation/in_app_update_host.dart';

class PosBillingwalaApp extends ConsumerWidget {
  const PosBillingwalaApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(routerProvider);
    /* Recover empty catalog after wrong-id sync / first login. */
    ref.watch(catalogBootstrapListenerProvider);
    /* Mobile: push pending rows when connectivity returns. */
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
      supportedLocales: const [
        Locale('en'),
        Locale('hi'),
        Locale('mr'),
      ],
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      routerConfig: router,
      builder: (context, child) {
        return InAppUpdateHost(child: child ?? const SizedBox.shrink());
      },
    );
  }
}
