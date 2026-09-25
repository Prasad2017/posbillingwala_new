import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/data/app_splash_store.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/settings/domain/in_app_update_service.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/*
 * Always: native logo first.
 * Dynamic splash (admin): first from net → cache; later from cache;
 * when online again refresh from server and update cache.
 */
class SplashPage extends ConsumerStatefulWidget {
  const SplashPage({super.key});

  @override
  ConsumerState<SplashPage> createState() => SplashPageState();
}

class SplashPageState extends ConsumerState<SplashPage> {
  static const nativeLogoHold = Duration(milliseconds: 700);
  static const dynamicSplashHold = Duration(seconds: 3);

  String? webOfflineMessage;
  AppSplashArt? splashArt;
  bool showNativeOnly = true;

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
    final store = AppSplashStore(ref.read(apiClientProvider));
    final online = await isDeviceOnline();
    final nativeHold = Future<void>.delayed(nativeLogoHold);

    /* 1) Always keep native logo on screen first. */
    if (mounted) setState(() => showNativeOnly = true);

    /* 2) Prepare cached splash (if any) while native logo shows. */
    final cached = await store.readCachedArt();

    await nativeHold;
    if (!mounted) return;

    /* 3) After native logo: show cache if we have it. */
    if (cached != null && !cached.isEmpty) {
      setState(() {
        splashArt = cached;
        showNativeOnly = false;
      });
    }

    /* 4) Online → fetch from server and refresh cache (first load or update). */
    if (online) {
      final fresh = await store.fetchAndCache();
      if (!mounted) return;
      if (fresh != null && !fresh.isEmpty) {
        setState(() {
          splashArt = fresh;
          showNativeOnly = false;
        });
      } else {
        /* Admin removed splash — stay / return to native logo only. */
        setState(() {
          splashArt = null;
          showNativeOnly = true;
        });
      }
    }

    final hasDynamic = !showNativeOnly && splashArt != null && !splashArt!.isEmpty;
    if (hasDynamic) {
      await Future<void>.delayed(dynamicSplashHold);
    }

    if (!mounted) return;
    if (AppPlatform.requiresNetwork && !online) {
      setState(() => webOfflineMessage = kOnlineRequiredMessage);
    }
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
    final art = splashArt;
    final showDynamic = !showNativeOnly && art != null && !art.isEmpty;

    return Scaffold(
      backgroundColor: Colors.white,
      body: Stack(
        fit: StackFit.expand,
        children: [
          const ColoredBox(color: Colors.white),
          if (showDynamic)
            _DynamicSplashImage(art: art)
          else
            _NativeLogo(size: size),
          if (webOfflineMessage != null)
            Align(
              alignment: Alignment.bottomCenter,
              child: Padding(
                padding: EdgeInsets.fromLTRB(24, 0, 24, 20 + bottomPad),
                child: _SplashOfflineBanner(
                  message: webOfflineMessage!,
                  onRetry: retryWebOnline,
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _DynamicSplashImage extends StatelessWidget {
  const _DynamicSplashImage({required this.art});

  final AppSplashArt art;

  @override
  Widget build(BuildContext context) {
    final url = art.networkUrl?.trim() ?? '';
    final path = art.localPath?.trim() ?? '';

    /* Prefer network when URL exists (Flutter disk/memory cache); file for offline. */
    if (url.isNotEmpty) {
      return Image.network(
        url,
        fit: BoxFit.cover,
        width: double.infinity,
        height: double.infinity,
        alignment: Alignment.center,
        filterQuality: FilterQuality.high,
        gaplessPlayback: true,
        errorBuilder: (_, _, _) {
          if (!kIsWeb && path.isNotEmpty && File(path).existsSync()) {
            return Image.file(
              File(path),
              fit: BoxFit.cover,
              width: double.infinity,
              height: double.infinity,
              alignment: Alignment.center,
              filterQuality: FilterQuality.high,
              gaplessPlayback: true,
              errorBuilder: (_, _, _) =>
                  _NativeLogo(size: MediaQuery.sizeOf(context)),
            );
          }
          return _NativeLogo(size: MediaQuery.sizeOf(context));
        },
      );
    }

    if (!kIsWeb && path.isNotEmpty && File(path).existsSync()) {
      return Image.file(
        File(path),
        fit: BoxFit.cover,
        width: double.infinity,
        height: double.infinity,
        alignment: Alignment.center,
        filterQuality: FilterQuality.high,
        gaplessPlayback: true,
        errorBuilder: (_, _, _) =>
            _NativeLogo(size: MediaQuery.sizeOf(context)),
      );
    }

    return _NativeLogo(size: MediaQuery.sizeOf(context));
  }
}

/* Same look as native splash: app_logo.png centered, as-is (not circular). */
class _NativeLogo extends StatelessWidget {
  const _NativeLogo({required this.size});

  final Size size;

  @override
  Widget build(BuildContext context) {
    final maxW = (size.width * 0.72).clamp(160.0, 420.0);
    return Center(
      child: Image.asset(
        AppAssets.appLogo,
        width: maxW,
        fit: BoxFit.contain,
        filterQuality: FilterQuality.high,
        gaplessPlayback: true,
      ),
    );
  }
}

class _SplashOfflineBanner extends StatelessWidget {
  const _SplashOfflineBanner({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
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
            message,
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
}
