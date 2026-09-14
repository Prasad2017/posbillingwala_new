import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_constants.dart';
import 'package:pos_billingwala_v2/core/theme/app_dimensions.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/widgets/app_svg.dart';
import 'package:pos_billingwala_v2/core/widgets/brand_logo.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/features/ads/ad_banner.dart';
import 'package:pos_billingwala_v2/features/ads/ad_config.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/auth/presentation/device_conflict_dialog.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';

/// Matches `docs/layout/activity_login.xml` — licence key only, no extra KPIs.
class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => LoginPageState();
}

class LoginPageState extends ConsumerState<LoginPage> {
  final formKey = GlobalKey<FormState>();
  final licenceController = TextEditingController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(authControllerProvider.notifier).setDeviceConflictHandler(
            (message) => showDeviceConflictDialog(context, message),
          );
    });
  }

  @override
  void dispose() {
    licenceController.dispose();
    super.dispose();
  }

  Future<void> submit() async {
    if (!formKey.currentState!.validate()) return;
    FocusScope.of(context).unfocus();
    await ref.read(authControllerProvider.notifier).loginWithLicence(
          licenceController.text.trim(),
        );
  }

  Future<void> showForgotLicenceSheet() async {
    final strings = AppStrings.of(ref);
    await showAppBottomSheet<void>(
      context: context,
      title: strings.forgotLicence,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            'Contact support for your shop licence key, or start a free trial.',
            style: AppTypography.body(),
          ),
          const SizedBox(height: AppDimensions.lg),
          Text(
            AppConstants.supportPhone,
            style: AppTypography.cardTitle(color: AppColors.primary),
          ),
          const SizedBox(height: AppDimensions.xl),
          AppButton(
            label: strings.startFreeTrial,
            onPressed: () {
              Navigator.pop(context);
              context.push('/register');
            },
          ),
          const SizedBox(height: 10),
          AppButton(
            label: strings.close,
            variant: AppButtonVariant.outlined,
            onPressed: () => Navigator.pop(context),
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
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(next.errorMessage!)),
        );
      }
    });

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
                height: 200,
                fit: BoxFit.fill,
              ),
            ),
          ),
          SafeArea(
            child: SingleChildScrollView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.fromLTRB(20, 28, 20, 24),
              child: ResponsiveContent(
                padding: EdgeInsets.zero,
                child: Form(
                  key: formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const Center(child: BrandLogo(width: 220)),
                      const SizedBox(height: 20),
                      Text(
                        strings.welcomeBack,
                        textAlign: TextAlign.center,
                        style: AppTypography.screenTitle(
                          color: AppColors.primary,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        strings.signInSubtitle,
                        textAlign: TextAlign.center,
                        style: AppTypography.body(),
                      ),
                      const SizedBox(height: 28),
                      AppCard(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            Text(
                              strings.licenceKey,
                              style: AppTypography.cardTitle(
                                color: AppColors.primary,
                              ),
                            ),
                            const SizedBox(height: 10),
                            AppTextField(
                              controller: licenceController,
                              hint: strings.licenceKeyHint,
                              prefixSvg: AppAssets.svgKey,
                              textCapitalization:
                                  TextCapitalization.characters,
                              textInputAction: TextInputAction.done,
                              inputFormatters: [
                                FilteringTextInputFormatter.allow(
                                  RegExp(r'[A-Za-z0-9\\-]'),
                                ),
                              ],
                              validator: (value) =>
                                  value == null || value.trim().isEmpty
                                      ? strings.licenceRequired
                                      : null,
                              onSubmitted: (_) => submit(),
                            ),
                            Align(
                              alignment: Alignment.centerRight,
                              child: TextButton(
                                onPressed: auth.busy
                                    ? null
                                    : showForgotLicenceSheet,
                                child: Text(strings.forgotLicence),
                              ),
                            ),
                            AppButton(
                              label: auth.busy ? strings.pleaseWait : strings.login,
                              isLoading: auth.busy,
                              onPressed: auth.busy ? null : submit,
                            ),
                            const SizedBox(height: 16),
                            TextButton(
                              onPressed: auth.busy
                                  ? null
                                  : () => context.push('/register'),
                              child: Text(
                                strings.newUserTrial,
                                style: AppTypography.body(),
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: AppColors.primaryLight,
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: Row(
                          children: [
                            AppSvg(
                              AppAssets.svgLock,
                              width: 20,
                              height: 20,
                              color: AppColors.primary,
                            ),
                            const SizedBox(width: 10),
                            Expanded(
                              child: Text(
                                'Your data is safe with us. By continuing, you agree to our Terms of Service and Privacy Policy.',
                                style: AppTypography.bodySmall(),
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),
                      const Center(child: AdBanner(slot: AdSlot.login)),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
