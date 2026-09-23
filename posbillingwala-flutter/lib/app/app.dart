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

class PosBillingwalaApp extends ConsumerStatefulWidget {
  const PosBillingwalaApp({super.key});

  @override
  ConsumerState<PosBillingwalaApp> createState() => _PosBillingwalaAppState();
}

class _PosBillingwalaAppState extends ConsumerState<PosBillingwalaApp>
    with WidgetsBindingObserver {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    applyFixedSystemUi();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeMetrics() {
    /* Orientation / fold / nav-mode changes — keep bars fixed. */
    applyFixedSystemUi();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      applyFixedSystemUi();
    }
  }

  void applyFixedSystemUi() {
    SystemChrome.setEnabledSystemUIMode(
      SystemUiMode.manual,
      overlays: SystemUiOverlay.values,
    );
    SystemChrome.setSystemUIOverlayStyle(AppTheme.lightSystemUi);
  }

  @override
  Widget build(BuildContext context) {
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
        /* viewPadding stays stable when system nav briefly hides/shows. */
        final topInset = mq.viewPadding.top;
        final bottomInset = mq.viewPadding.bottom;
        final leftInset = mq.viewPadding.left;
        final rightInset = mq.viewPadding.right;
        return MediaQuery(
          data: mq.copyWith(
            textScaler: const TextScaler.linear(0.9),
            boldText: false,
            /* Layout follows system insets — no jump on nav hide/show. */
            padding: EdgeInsets.only(
              top: topInset,
              bottom: bottomInset,
              left: leftInset,
              right: rightInset,
            ),
          ),
          child: AnnotatedRegion<SystemUiOverlayStyle>(
            value: AppTheme.lightSystemUi,
            /* Aurora under system bars; screens stay inside status + nav insets. */
            child: AuroraBackground(
              child: Stack(
                fit: StackFit.expand,
                children: [
                  /* Status bar fill (primary). */
                  if (topInset > 0)
                    Positioned(
                      top: 0,
                      left: 0,
                      right: 0,
                      height: topInset,
                      child: const ColoredBox(color: AppColors.primary),
                    ),
                  /* Navigation bar fill so content never shows through. */
                  if (bottomInset > 0)
                    Positioned(
                      left: 0,
                      right: 0,
                      bottom: 0,
                      height: bottomInset,
                      child: const ColoredBox(color: Colors.white),
                    ),
                  /* Keep all routes between status bar and system nav bar. */
                  SafeArea(
                    top: true,
                    bottom: true,
                    left: true,
                    right: true,
                    child: InAppUpdateHost(
                      child: child ?? const SizedBox.shrink(),
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}
