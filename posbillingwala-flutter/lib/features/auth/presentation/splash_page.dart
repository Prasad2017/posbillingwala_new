import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/constants/app_constants.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/auth/presentation/web_auth_shell.dart';
import 'package:pos_billingwala_v2/features/settings/domain/in_app_update_service.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* Matches WithTable splash: full-bleed branding + circular loader above footer. */
class SplashPage extends ConsumerStatefulWidget {
  const SplashPage({super.key});

  @override
  ConsumerState<SplashPage> createState() => SplashPageState();
}

class SplashPageState extends ConsumerState<SplashPage> {
  String? webOfflineMessage;

  @override
  void initState() {
    super.initState();
    /* Native launch splash already branded — start session immediately. */
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      startBootstrap();
    });
    if (!kDebugMode) {
      /* Non-blocking; InAppUpdateHost also listens after first frame. */
      Future<void>.delayed(const Duration(milliseconds: 1500), () {
        if (!mounted) return;
        promptPlayUpdateIfNeeded();
      });
    }
  }

  Future<void> startBootstrap() async {
    if (AppPlatform.requiresNetwork && !await ensureOnline()) {
      if (!mounted) return;
      setState(() => webOfflineMessage = kOnlineRequiredMessage);
      ref.read(authControllerProvider.notifier).bootstrap();
      return;
    }
    if (!mounted) return;
    ref.read(authControllerProvider.notifier).bootstrap();
  }

  Future<void> promptPlayUpdateIfNeeded() async {
    if (!InAppUpdateService.isAndroidPlay) return;
    final outcome = await inAppUpdateService.checkAvailability();
    if (!mounted) return;
    if (outcome.status == InAppUpdateStatus.downloaded) {
      await inAppUpdateService.completeFlexibleUpdate();
      return;
    }
    if (outcome.status != InAppUpdateStatus.available) return;
    final strings = AppStrings.of(ref);
    final go = await showAppConfirmBottomSheet(
      context: context,
      title: strings.newVersionAvailable,
      message: strings.updateBeforeContinue,
      confirmLabel: strings.updateApp,
      cancelLabel: strings.cancel,
      icon: Icons.system_update_rounded,
    );
    if (!mounted || !go) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(strings.dataUploadingOnServer)));
    await inAppUpdateService.startUpdate(preferImmediate: true);
  }

  @override
  Widget build(BuildContext context) {
    if (AppPlatform.useDesktopShell) {
      return webSplash(context);
    }
    return mobileSplash();
  }

  Widget webSplash(BuildContext context) {
    final showInlineLogo =
        context.widthClass.index < AppWidthClass.expanded.index;
    return WebAuthShell(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (showInlineLogo) ...[
            const BrandLogo(width: 160),
            const SizedBox(height: 28),
          ],
          const Text(
            'Starting POS',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontFamily: AppFonts.family,
              fontWeight: FontWeight.w800,
              fontSize: 26,
              color: AppColors.navy,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            '${AppConstants.appName} · Web',
            textAlign: TextAlign.center,
            style: AppTypography.body(),
          ),
          const SizedBox(height: 32),
          if (webOfflineMessage != null) ...[
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AppColors.danger.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(14),
                border: Border.all(
                  color: AppColors.danger.withValues(alpha: 0.25),
                ),
              ),
              child: Text(
                webOfflineMessage!,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  color: AppColors.danger,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
            const SizedBox(height: 16),
            FilledButton(onPressed: retryWebOnline, child: const Text('Retry')),
          ] else
            const SizedBox(
              width: 36,
              height: 36,
              child: CircularProgressIndicator(
                strokeWidth: 3,
                color: AppColors.primary,
              ),
            ),
        ],
      ),
    );
  }

  Future<void> retryWebOnline() async {
    setState(() => webOfflineMessage = null);
    if (!await ensureOnline()) {
      if (!mounted) return;
      setState(() => webOfflineMessage = kOnlineRequiredMessage);
      return;
    }
    if (!mounted) return;
    ref.read(authControllerProvider.notifier).bootstrap();
  }

  Widget mobileSplash() {
    return Scaffold(
      backgroundColor: Colors.white,
      body: Stack(
        fit: StackFit.expand,
        children: [
          Image.asset(
            AppAssets.splashBranding,
            fit: BoxFit.cover,
            width: double.infinity,
            height: double.infinity,
            filterQuality: FilterQuality.low,
            gaplessPlayback: true,
            errorBuilder: (_, _, _) => Image.asset(
              AppAssets.appLogo,
              fit: BoxFit.contain,
              filterQuality: FilterQuality.low,
            ),
          ),
          SafeArea(
            child: Align(
              alignment: Alignment.bottomCenter,
              child: Padding(
                /* Matches WithTable splash: circular loader above footer tagline. */
                padding: const EdgeInsets.fromLTRB(32, 0, 32, 150),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    if (webOfflineMessage != null) ...[
                      Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(12),
                        margin: const EdgeInsets.only(bottom: 16),
                        decoration: BoxDecoration(
                          color: AppColors.danger.withValues(alpha: 0.12),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Text(
                          webOfflineMessage!,
                          textAlign: TextAlign.center,
                          style: const TextStyle(
                            color: AppColors.danger,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                      TextButton(
                        onPressed: retryWebOnline,
                        child: const Text('Retry'),
                      ),
                    ] else
                      const SizedBox(
                        width: 42,
                        height: 42,
                        child: CircularProgressIndicator(
                          strokeWidth: 3.2,
                          color: Colors.white,
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
