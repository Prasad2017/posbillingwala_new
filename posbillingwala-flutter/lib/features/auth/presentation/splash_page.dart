import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';

/// Matches `docs/layout/activity_splash_screen.xml`:
/// full-bleed splash branding + bottom indeterminate progress only.
class SplashPage extends ConsumerStatefulWidget {
  const SplashPage({super.key});

  @override
  ConsumerState<SplashPage> createState() => _SplashPageState();
}

class _SplashPageState extends ConsumerState<SplashPage> {
  String? _webOfflineMessage;

  @override
  void initState() {
    super.initState();
    Future<void>.delayed(const Duration(milliseconds: 1200), () async {
      if (!mounted) return;
      if (AppPlatform.requiresNetwork && !await ensureOnline()) {
        if (!mounted) return;
        setState(() => _webOfflineMessage = kOnlineRequiredMessage);
        ref.read(authControllerProvider.notifier).bootstrap();
        return;
      }
      if (!mounted) return;
      ref.read(authControllerProvider.notifier).bootstrap();
    });
  }

  @override
  Widget build(BuildContext context) {
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
            errorBuilder: (_, _, _) => Image.asset(
              AppAssets.appLogo,
              fit: BoxFit.contain,
            ),
          ),
          SafeArea(
            child: Align(
              alignment: Alignment.bottomCenter,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(24, 0, 24, 40),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    if (_webOfflineMessage != null) ...[
                      Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(12),
                        margin: const EdgeInsets.only(bottom: 16),
                        decoration: BoxDecoration(
                          color: AppColors.danger.withValues(alpha: 0.12),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Text(
                          _webOfflineMessage!,
                          textAlign: TextAlign.center,
                          style: const TextStyle(
                            color: AppColors.danger,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                      TextButton(
                        onPressed: () async {
                          setState(() => _webOfflineMessage = null);
                          if (!await ensureOnline()) {
                            if (!mounted) return;
                            setState(
                              () => _webOfflineMessage = kOnlineRequiredMessage,
                            );
                            return;
                          }
                          if (!mounted) return;
                          ref.read(authControllerProvider.notifier).bootstrap();
                        },
                        child: const Text('Retry'),
                      ),
                      const SizedBox(height: 12),
                    ] else
                      const SizedBox(
                        width: 42,
                        height: 42,
                        child: CircularProgressIndicator(
                          strokeWidth: 3,
                          color: AppColors.navy,
                        ),
                      ),
                    const SizedBox(height: 150),
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
