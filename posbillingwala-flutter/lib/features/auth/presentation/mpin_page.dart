import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pinput/pinput.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/auth/presentation/device_conflict_dialog.dart';
import 'package:pos_billingwala_v2/features/auth/presentation/web_auth_shell.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* Matches `docs/layout/activity_login_mpin.xml`. */
class MpinPage extends ConsumerStatefulWidget {
  const MpinPage({super.key});

  @override
  ConsumerState<MpinPage> createState() => MpinPageState();
}

class MpinPageState extends ConsumerState<MpinPage> {
  final pinController = TextEditingController();
  final pinFocus = FocusNode();
  final pinKey = GlobalKey<FormState>();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref
          .read(authControllerProvider.notifier)
          .setDeviceConflictHandler(
            (message) => showDeviceConflictDialog(context, message),
          );
      pinFocus.requestFocus();
    });
  }

  @override
  void dispose() {
    pinController.dispose();
    pinFocus.dispose();
    super.dispose();
  }

  PinTheme get defaultPinTheme => PinTheme(
    width: 58,
    height: 58,
    textStyle: AppTypography.screenTitle(color: AppColors.primary),
    decoration: BoxDecoration(
      color: Colors.white,
      borderRadius: BorderRadius.circular(18),
      border: Border.all(color: AppColors.border, width: 1.5),
      boxShadow: [
        BoxShadow(
          color: AppColors.primary.withValues(alpha: 0.06),
          blurRadius: 8,
          offset: const Offset(0, 3),
        ),
      ],
    ),
  );

  PinTheme get focusedPinTheme => defaultPinTheme.copyWith(
    decoration: defaultPinTheme.decoration!.copyWith(
      border: Border.all(color: AppColors.primary, width: 2),
      boxShadow: [
        BoxShadow(
          color: AppColors.primary.withValues(alpha: 0.12),
          blurRadius: 8,
          offset: const Offset(0, 3),
        ),
      ],
    ),
  );

  PinTheme get submittedPinTheme => focusedPinTheme;

  Future<void> submit([String? value]) async {
    final mpin = (value ?? pinController.text).trim();
    if (mpin.length != 4) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter your 4-digit PB-PIN')),
      );
      return;
    }
    FocusScope.of(context).unfocus();
    final ok = await ref
        .read(authControllerProvider.notifier)
        .loginWithMpin(mpin);
    if (!ok && mounted) {
      pinController.clear();
      pinFocus.requestFocus();
    }
  }

  Widget pinFields({required bool busy}) {
    return Pinput(
      length: 4,
      controller: pinController,
      focusNode: pinFocus,
      enabled: !busy,
      obscureText: true,
      obscuringCharacter: '•',
      keyboardType: TextInputType.number,
      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
      defaultPinTheme: defaultPinTheme,
      focusedPinTheme: focusedPinTheme,
      submittedPinTheme: submittedPinTheme,
      separatorBuilder: (index) => const SizedBox(width: 12),
      hapticFeedbackType: HapticFeedbackType.lightImpact,
      cursor: Container(width: 2, height: 22, color: AppColors.primary),
      onCompleted: submit,
      onSubmitted: submit,
    );
  }

  Widget pinCard({required AuthState auth, required AppStrings strings}) {
    return AppCard(
      child: Column(
        children: [
          SizedBox(
            width: 72,
            height: 72,
            child: DecoratedBox(
              decoration: BoxDecoration(
                color: Colors.white,
                shape: BoxShape.circle,
                border: Border.all(color: AppColors.border, width: 1.5),
                boxShadow: [
                  BoxShadow(
                    color: AppColors.primary.withValues(alpha: 0.10),
                    blurRadius: 12,
                    offset: const Offset(0, 4),
                  ),
                ],
              ),
              child: const Center(
                child: AppSvg(
                  AppAssets.svgLock,
                  width: 28,
                  height: 28,
                  color: AppColors.primary,
                ),
              ),
            ),
          ),
          const SizedBox(height: 16),
          Text('Enter PB-PIN', style: AppTypography.sectionTitle()),
          const SizedBox(height: 6),
          Text(
            'Enter the 4-digit PIN for this device.',
            textAlign: TextAlign.center,
            style: AppTypography.bodySmall(),
          ),
          const SizedBox(height: 24),
          Form(
            key: pinKey,
            child: pinFields(busy: auth.busy),
          ),
          const SizedBox(height: 14),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const AppSvg(
                AppAssets.svgLock,
                width: 14,
                height: 14,
                color: AppColors.primary,
              ),
              const SizedBox(width: 6),
              Text('Your data is safe with us', style: AppTypography.caption()),
            ],
          ),
          const SizedBox(height: 20),
          AppButton(
            label: auth.busy ? strings.pleaseWait : strings.login,
            isLoading: auth.busy,
            onPressed: auth.busy ? null : submit,
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final auth = ref.watch(authControllerProvider);
    final strings = AppStrings.of(ref);

    ref.listen(authControllerProvider, (prev, next) {
      if (next.errorMessage != null &&
          next.errorMessage != prev?.errorMessage &&
          next.errorMessage != 'Device binding cancelled') {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(next.errorMessage!)));
      }
    });

    if (AppPlatform.useDesktopShell) {
      final showInlineLogo =
          context.widthClass.index < AppWidthClass.expanded.index;
      return WebAuthShell(
        child: Column(
          children: [
            Align(
              alignment: Alignment.centerLeft,
              child: TextButton.icon(
                onPressed: auth.busy
                    ? null
                    : () => ref
                          .read(authControllerProvider.notifier)
                          .clearLicence(),
                icon: const Icon(Icons.arrow_back_rounded, size: 18),
                label: const Text('Change licence'),
              ),
            ),
            if (showInlineLogo) ...[
              const BrandLogo(width: 140),
              const SizedBox(height: 16),
            ],
            Text(strings.welcomeBack, style: AppTypography.screenTitle()),
            const SizedBox(height: 8),
            Text(
              'Enter the 4-digit PB-PIN for this store.',
              textAlign: TextAlign.center,
              style: AppTypography.body(),
            ),
            const SizedBox(height: 24),
            pinCard(auth: auth, strings: strings),
          ],
        ),
      );
    }

    return Scaffold(
      backgroundColor: AppColors.surface,
      body: Stack(
        fit: StackFit.expand,
        children: [
          Positioned(
            left: 0,
            right: 0,
            bottom: 0,
            child: IgnorePointer(
              child: AppSvg(
                AppAssets.svgSplashWave,
                width: MediaQuery.sizeOf(context).width,
                height: 180,
                fit: BoxFit.fill,
              ),
            ),
          ),
          SafeArea(
            child: SingleChildScrollView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
              child: ResponsiveContent(
                padding: EdgeInsets.zero,
                child: Column(
                  children: [
                    Align(
                      alignment: Alignment.centerLeft,
                      child: IconButton(
                        tooltip: 'Change licence',
                        onPressed: auth.busy
                            ? null
                            : () => ref
                                  .read(authControllerProvider.notifier)
                                  .clearLicence(),
                        icon: const AppSvg(
                          AppAssets.svgBack,
                          width: 22,
                          height: 22,
                          color: AppColors.primary,
                        ),
                      ),
                    ),
                    const BrandLogo(width: 180),
                    const SizedBox(height: 20),
                    Text(
                      strings.welcomeBack,
                      style: AppTypography.screenTitle(),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      strings.signInSubtitle,
                      textAlign: TextAlign.center,
                      style: AppTypography.body(),
                    ),
                    const SizedBox(height: 24),
                    pinCard(auth: auth, strings: strings),
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
