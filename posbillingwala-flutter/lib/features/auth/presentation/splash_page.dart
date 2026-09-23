import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/settings/domain/in_app_update_service.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* Splash — POS BILLING WALA mockup UI (mobile / landscape / web). */
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
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      startBootstrap();
    });
    if (!kDebugMode) {
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

  @override
  Widget build(BuildContext context) {
    final bottomPad = MediaQuery.paddingOf(context).bottom;
    final size = MediaQuery.sizeOf(context);
    final art = _SplashArt.resolve(context, size);

    return Scaffold(
      backgroundColor: Colors.white,
      body: Stack(
        fit: StackFit.expand,
        children: [
          ColoredBox(color: Colors.white),
          Image.asset(
            art.asset,
            fit: art.fit,
            width: double.infinity,
            height: double.infinity,
            alignment: art.alignment,
            filterQuality: FilterQuality.high,
            gaplessPlayback: true,
            errorBuilder: (_, _, _) => Image.asset(
              AppAssets.splashScreen,
              fit: BoxFit.contain,
              width: double.infinity,
              height: double.infinity,
              errorBuilder: (_, _, _) => Image.asset(
                AppAssets.splashBranding,
                fit: BoxFit.contain,
                width: double.infinity,
                height: double.infinity,
              ),
            ),
          ),
          Align(
            alignment: Alignment.bottomCenter,
            child: Container(
              width: double.infinity,
              padding: EdgeInsets.fromLTRB(
                24,
                art.loaderTopPad,
                24,
                20 + bottomPad,
              ),
              decoration: const BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [
                    Color(0x00FFFFFF),
                    Color(0xCCFFFFFF),
                    Color(0xFFFFFFFF),
                  ],
                  stops: [0.0, 0.45, 1.0],
                ),
              ),
              child: _SplashLoader(
                offlineMessage: webOfflineMessage,
                onRetry: retryWebOnline,
                compact: art.compactLoader,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/* Picks splash art for portrait mobile, landscape, or web. */
class _SplashArt {
  const _SplashArt({
    required this.asset,
    required this.fit,
    required this.alignment,
    required this.loaderTopPad,
    required this.compactLoader,
  });

  final String asset;
  final BoxFit fit;
  final Alignment alignment;
  final double loaderTopPad;
  final bool compactLoader;

  static _SplashArt resolve(BuildContext context, Size size) {
    final landscape = size.width > size.height;
    final webOrDesktop = AppPlatform.useDesktopShell ||
        AppBreakpoints.isDesktopClass(AppBreakpoints.ofWidth(size.width));

    /* Web, desktop, and device landscape share the same wide art. */
    if (webOrDesktop || landscape || AppBreakpoints.isWideLayout(context)) {
      return const _SplashArt(
        asset: AppAssets.splashLandscape,
        fit: BoxFit.contain,
        alignment: Alignment.center,
        loaderTopPad: 36,
        compactLoader: true,
      );
    }

    /* Portrait mobile — vertical logo → features → devices. */
    return const _SplashArt(
      asset: AppAssets.splashMobile,
      fit: BoxFit.contain,
      alignment: Alignment.center,
      loaderTopPad: 48,
      compactLoader: false,
    );
  }
}

class _SplashLoader extends StatelessWidget {
  const _SplashLoader({
    required this.offlineMessage,
    required this.onRetry,
    this.compact = false,
  });

  final String? offlineMessage;
  final VoidCallback onRetry;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    if (offlineMessage != null) {
      return Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: double.infinity,
            constraints: const BoxConstraints(maxWidth: 360),
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.danger.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Text(
              offlineMessage!,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontFamily: AppFonts.family,
                color: AppColors.danger,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          TextButton(onPressed: onRetry, child: const Text('Retry')),
        ],
      );
    }

    final spinner = Size.square(compact ? 34 : 42);
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        SizedBox(
          width: spinner.width,
          height: spinner.height,
          child: const CircularProgressIndicator(
            strokeWidth: 3.2,
            color: AppColors.primary,
            backgroundColor: Color(0xFFE8EEF8),
          ),
        ),
        SizedBox(height: compact ? 8 : 12),
        Text(
          'Loading...',
          style: TextStyle(
            fontFamily: AppFonts.family,
            color: AppColors.textSecondary.withValues(alpha: .75),
            fontWeight: FontWeight.w500,
            fontSize: compact ? 13 : 14,
          ),
        ),
      ],
    );
  }
}
