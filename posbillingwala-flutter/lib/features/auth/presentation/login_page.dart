import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_constants.dart';
import 'package:pos_billingwala_v2/core/theme/app_dimensions.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/auth/presentation/device_conflict_dialog.dart';
import 'package:pos_billingwala_v2/features/auth/presentation/web_auth_shell.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

enum _LoginMode { staff, licence }

/* Matches `docs/layout/activity_login.xml` — staff or licence login. */
class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => LoginPageState();
}

class LoginPageState extends ConsumerState<LoginPage> {
  final formKey = GlobalKey<FormState>();
  final licenceController = TextEditingController();
  final mobileController = TextEditingController();
  final pinController = TextEditingController();
  _LoginMode _mode = _LoginMode.staff;

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
    mobileController.dispose();
    pinController.dispose();
    super.dispose();
  }

  Future<void> submitLicence() async {
    if (!formKey.currentState!.validate()) return;
    FocusScope.of(context).unfocus();
    final ok = await ref.read(authControllerProvider.notifier).loginWithLicence(
          licenceController.text.trim(),
        );
    if (!ok || !mounted) return;
    if (ref.read(authControllerProvider).status == AuthStatus.needsMpin) {
      context.go('/mpin');
    }
  }

  Future<void> submitStaff() async {
    if (!formKey.currentState!.validate()) return;
    FocusScope.of(context).unfocus();
    final ok = await ref.read(authControllerProvider.notifier).loginWithStaff(
          mobileNumber: mobileController.text.trim(),
          pin: pinController.text.trim(),
        );
    if (!ok && mounted) {
      pinController.clear();
    }
  }

  Future<void> showForgotLicenceSheet() async {
    final strings = AppStrings.of(ref);
    if (AppPlatform.useDesktopShell) {
      await showDialog<void>(
        context: context,
        builder: (context) => AlertDialog(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
          title: Text(strings.forgotLicence),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Contact support for your shop licence key, or start a free trial.',
                style: AppTypography.body(),
              ),
              const SizedBox(height: 12),
              Text(
                AppConstants.supportPhone,
                style: AppTypography.cardTitle(color: AppColors.primary),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: Text(strings.close),
            ),
            FilledButton(
              onPressed: () {
                Navigator.pop(context);
                context.push('/register');
              },
              child: Text(strings.startFreeTrial),
            ),
          ],
        ),
      );
      return;
    }
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

  Widget modeToggle() {
    return SegmentedButton<_LoginMode>(
      segments: const [
        ButtonSegment(
          value: _LoginMode.staff,
          label: Text('Staff login'),
          icon: Icon(Icons.badge_outlined, size: 18),
        ),
        ButtonSegment(
          value: _LoginMode.licence,
          label: Text('Licence login'),
          icon: Icon(Icons.vpn_key_outlined, size: 18),
        ),
      ],
      selected: {_mode},
      onSelectionChanged: (next) {
        setState(() => _mode = next.first);
        ref.read(authControllerProvider.notifier).clearError();
      },
      style: ButtonStyle(
        visualDensity: VisualDensity.compact,
        foregroundColor: WidgetStateProperty.resolveWith((states) {
          if (states.contains(WidgetState.selected)) {
            return Colors.white;
          }
          return AppColors.primary;
        }),
        backgroundColor: WidgetStateProperty.resolveWith((states) {
          if (states.contains(WidgetState.selected)) {
            return AppColors.primary;
          }
          return Colors.white;
        }),
      ),
    );
  }

  Widget staffFields(AuthState auth, AppStrings strings) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          'Mobile Number',
          style: AppTypography.cardTitle(color: AppColors.primary),
        ),
        const SizedBox(height: 10),
        AppTextField(
          controller: mobileController,
          hint: '10-digit mobile',
          keyboardType: TextInputType.phone,
          textInputAction: TextInputAction.next,
          maxLength: 10,
          showCounter: false,
          inputFormatters: [FilteringTextInputFormatter.digitsOnly],
          validator: (value) {
            if (_mode != _LoginMode.staff) return null;
            final v = value?.trim() ?? '';
            if (v.length != 10) return 'Enter 10-digit mobile number';
            return null;
          },
        ),
        const SizedBox(height: 14),
        Text(
          'App PIN',
          style: AppTypography.cardTitle(color: AppColors.primary),
        ),
        const SizedBox(height: 10),
        AppTextField(
          controller: pinController,
          hint: 'Staff PIN',
          obscureText: true,
          keyboardType: TextInputType.number,
          textInputAction: TextInputAction.done,
          maxLength: 6,
          showCounter: false,
          inputFormatters: [FilteringTextInputFormatter.digitsOnly],
          validator: (value) {
            if (_mode != _LoginMode.staff) return null;
            final v = value?.trim() ?? '';
            if (v.length < 4) return 'Enter staff PIN';
            return null;
          },
          onSubmitted: (_) => submitStaff(),
        ),
        const SizedBox(height: 8),
        Text(
          'No licence key needed. Works on a new phone if User Management is ON.',
          style: AppTypography.bodySmall(),
        ),
        const SizedBox(height: 16),
        AppButton(
          label: auth.busy ? strings.pleaseWait : 'Staff Login',
          isLoading: auth.busy,
          onPressed: auth.busy ? null : submitStaff,
        ),
      ],
    );
  }

  Widget licenceFields(AuthState auth, AppStrings strings) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          strings.licenceKey,
          style: AppTypography.cardTitle(color: AppColors.primary),
        ),
        const SizedBox(height: 10),
        AppTextField(
          controller: licenceController,
          hint: strings.licenceKeyHint,
          prefixSvg: AppAssets.svgKey,
          textCapitalization: TextCapitalization.characters,
          textInputAction: TextInputAction.done,
          inputFormatters: [
            FilteringTextInputFormatter.allow(RegExp(r'[A-Za-z0-9\\-]')),
          ],
          validator: (value) {
            if (_mode != _LoginMode.licence) return null;
            return value == null || value.trim().isEmpty
                ? strings.licenceRequired
                : null;
          },
          onSubmitted: (_) => submitLicence(),
        ),
        Align(
          alignment: Alignment.centerRight,
          child: TextButton(
            onPressed: auth.busy ? null : showForgotLicenceSheet,
            child: Text(strings.forgotLicence),
          ),
        ),
        AppButton(
          label: auth.busy ? strings.pleaseWait : strings.login,
          isLoading: auth.busy,
          onPressed: auth.busy ? null : submitLicence,
        ),
        const SizedBox(height: 8),
        TextButton(
          onPressed: auth.busy ? null : () => context.push('/register'),
          child: Text(
            strings.newUserTrial,
            style: AppTypography.body(color: AppColors.primary),
          ),
        ),
      ],
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

    if (AppPlatform.useDesktopShell) {
      return webLogin(auth, strings);
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
                        _mode == _LoginMode.staff
                            ? 'Staff: mobile + PIN. Owner: licence key.'
                            : strings.signInSubtitle,
                        textAlign: TextAlign.center,
                        style: AppTypography.body(),
                      ),
                      const SizedBox(height: 20),
                      modeToggle(),
                      const SizedBox(height: 20),
                      AppCard(
                        child: _mode == _LoginMode.staff
                            ? staffFields(auth, strings)
                            : licenceFields(auth, strings),
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

  Widget webLogin(AuthState auth, AppStrings strings) {
    final showInlineLogo =
        context.widthClass.index < AppWidthClass.expanded.index;
    return WebAuthShell(
      child: Form(
        key: formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (showInlineLogo) ...[
              const Center(child: BrandLogo(width: 150)),
              const SizedBox(height: 24),
            ],
            Text(
              strings.welcomeBack,
              style: AppTypography.screenTitle(),
            ),
            const SizedBox(height: 8),
            Text(
              _mode == _LoginMode.staff
                  ? 'Staff can open Web POS with mobile + PIN (no licence key).'
                  : 'Enter your shop licence key to open Web POS.',
              style: AppTypography.body(),
            ),
            const SizedBox(height: 20),
            modeToggle(),
            const SizedBox(height: 20),
            Material(
              color: Colors.white,
              elevation: 0,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(20),
                side: BorderSide(color: AppColors.border.withValues(alpha: .8)),
              ),
              child: Padding(
                padding: const EdgeInsets.fromLTRB(22, 22, 22, 20),
                child: _mode == _LoginMode.staff
                    ? staffFields(auth, strings)
                    : licenceFields(auth, strings),
              ),
            ),
            const SizedBox(height: 16),
            Text(
              'Internet is required. Offline billing is available on the Android and iOS apps.',
              style: AppTypography.bodySmall(),
            ),
          ],
        ),
      ),
    );
  }
}
