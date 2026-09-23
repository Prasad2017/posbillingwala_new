import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_constants.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/theme/app_dimensions.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/auth/presentation/device_conflict_dialog.dart';
import 'package:pos_billingwala_v2/features/auth/presentation/web_auth_shell.dart';
import 'package:pos_billingwala_v2/language/app_languages.dart';
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
      ref
          .read(authControllerProvider.notifier)
          .setDeviceConflictHandler(
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
    final ok = await ref
        .read(authControllerProvider.notifier)
        .loginWithLicence(licenceController.text.trim());
    if (!ok || !mounted) return;
    if (ref.read(authControllerProvider).status == AuthStatus.needsMpin) {
      context.go('/mpin');
    }
  }

  Future<void> submitStaff() async {
    if (!formKey.currentState!.validate()) return;
    FocusScope.of(context).unfocus();
    final ok = await ref
        .read(authControllerProvider.notifier)
        .loginWithStaff(
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
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(18),
          ),
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

  Future<void> pickLanguage() async {
    final current = ref.read(appLocaleProvider).languageCode;
    final selected = await showDialog<String>(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(18),
          ),
          title: Row(
            children: [
              Icon(Icons.language_rounded, color: AppColors.primary, size: 22),
              const SizedBox(width: 10),
              Text(AppStrings.of(ref).language),
            ],
          ),
          content: SizedBox(
            width: 320,
            child: ListView.separated(
              shrinkWrap: true,
              itemCount: AppLanguages.options.length,
              separatorBuilder: (_, _) => const Divider(height: 1),
              itemBuilder: (context, index) {
                final option = AppLanguages.options[index];
                final selectedLang = current == option.code;
                return ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: Icon(
                    selectedLang
                        ? Icons.radio_button_checked
                        : Icons.radio_button_off,
                    color: selectedLang
                        ? AppColors.primary
                        : AppColors.navy.withValues(alpha: .4),
                  ),
                  title: Text(
                    option.nativeLabel,
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  subtitle: option.label == option.nativeLabel
                      ? null
                      : Text(option.label),
                  trailing: selectedLang
                      ? const Icon(Icons.check_rounded, color: AppColors.primary)
                      : null,
                  onTap: () => Navigator.pop(ctx, option.code),
                );
              },
            ),
          ),
        );
      },
    );
    if (selected == null || selected == current) return;
    await ref.read(appLocaleProvider.notifier).setLanguage(selected);
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(AppStrings.of(ref).languageApplied)),
    );
  }

  Widget modeToggle({required bool web, required AppStrings strings}) {
    return Container(
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: web ? const Color(0xFFF3F6FB) : Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.border.withValues(alpha: .7)),
      ),
      child: Row(
        children: [
          Expanded(
            child: _ModeTab(
              selected: _mode == _LoginMode.staff,
              label: strings.staffLogin,
              icon: Icons.person_outline_rounded,
              onTap: () {
                setState(() => _mode = _LoginMode.staff);
                ref.read(authControllerProvider.notifier).clearError();
              },
            ),
          ),
          Expanded(
            child: _ModeTab(
              selected: _mode == _LoginMode.licence,
              label: strings.licenceLogin,
              icon: Icons.vpn_key_outlined,
              onTap: () {
                setState(() => _mode = _LoginMode.licence);
                ref.read(authControllerProvider.notifier).clearError();
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget fieldLabel(String text, {required IconData icon}) {
    return Row(
      children: [
        Icon(icon, size: 16, color: AppColors.primary),
        const SizedBox(width: 6),
        Text(
          text,
          style: AppTypography.cardTitle(color: AppColors.primary),
        ),
      ],
    );
  }

  Widget staffFields(AuthState auth, AppStrings strings, {required bool web}) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        fieldLabel(strings.mobileNumber, icon: Icons.phone_outlined),
        const SizedBox(height: 10),
        AppTextField(
          required: true,
          controller: mobileController,
          hint: strings.mobileNumberHint,
          prefixText: web ? '+91  ' : null,
          prefixIcon: web ? null : Icons.phone_outlined,
          keyboardType: TextInputType.phone,
          textInputAction: TextInputAction.next,
          maxLength: 10,
          showCounter: false,
          inputFormatters: [FilteringTextInputFormatter.digitsOnly],
          validator: (value) {
            if (_mode != _LoginMode.staff) return null;
            final v = value?.trim() ?? '';
            if (v.length != 10) return strings.mobileNumberHint;
            return null;
          },
        ),
        const SizedBox(height: 16),
        fieldLabel(strings.appPin, icon: Icons.lock_outline_rounded),
        const SizedBox(height: 10),
        AppTextField(
          required: true,
          controller: pinController,
          hint: strings.staffPinHint,
          obscureText: true,
          keyboardType: TextInputType.number,
          textInputAction: TextInputAction.done,
          maxLength: 6,
          showCounter: false,
          inputFormatters: [FilteringTextInputFormatter.digitsOnly],
          validator: (value) {
            if (_mode != _LoginMode.staff) return null;
            final v = value?.trim() ?? '';
            if (v.length < 4) return strings.staffPinRequired;
            return null;
          },
          onSubmitted: (_) => submitStaff(),
        ),
        const SizedBox(height: 18),
        AppButton(
          label: auth.busy ? strings.pleaseWait : strings.loginAsStaff,
          isLoading: auth.busy,
          trailingIcon: auth.busy ? null : Icons.arrow_forward_rounded,
          onPressed: auth.busy ? null : submitStaff,
        ),
        if (web) ...[
          const SizedBox(height: 14),
          _InfoBanner(text: strings.staffNoLicenceHint),
        ] else ...[
          const SizedBox(height: 10),
          Text(
            strings.staffNoLicenceHint,
            style: AppTypography.bodySmall(),
          ),
        ],
      ],
    );
  }

  Widget licenceFields(
    AuthState auth,
    AppStrings strings, {
    required bool web,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        fieldLabel(strings.licenceKey, icon: Icons.vpn_key_outlined),
        const SizedBox(height: 10),
        AppTextField(
          required: true,
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
          trailingIcon: auth.busy ? null : Icons.arrow_forward_rounded,
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

  Widget languageChip() {
    final locale = ref.watch(appLocaleProvider);
    final option = AppLanguages.options.firstWhere(
      (o) => o.code == locale.languageCode,
      orElse: () => AppLanguages.options.first,
    );
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(999),
      elevation: 0,
      child: InkWell(
        onTap: pickLanguage,
        borderRadius: BorderRadius.circular(999),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(999),
            border: Border.all(color: AppColors.border),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                Icons.language_rounded,
                size: 18,
                color: AppColors.primary,
              ),
              const SizedBox(width: 6),
              Text(
                option.label,
                style: TextStyle(
                  fontFamily: AppFonts.family,
                  fontWeight: FontWeight.w600,
                  fontSize: 13,
                  color: AppColors.navy,
                ),
              ),
              const SizedBox(width: 2),
              Icon(
                Icons.keyboard_arrow_down_rounded,
                size: 18,
                color: AppColors.navy.withValues(alpha: .55),
              ),
            ],
          ),
        ),
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
      return webLogin(auth, strings);
    }

    return Scaffold(
      backgroundColor: Colors.transparent,
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
                      Align(
                        alignment: Alignment.centerRight,
                        child: languageChip(),
                      ),
                      const SizedBox(height: 8),
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
                            ? strings.staffOwnerHint
                            : strings.signInSubtitle,
                        textAlign: TextAlign.center,
                        style: AppTypography.body(),
                      ),
                      const SizedBox(height: 20),
                      modeToggle(web: false, strings: strings),
                      const SizedBox(height: 20),
                      AppCard(
                        child: _mode == _LoginMode.staff
                            ? staffFields(auth, strings, web: false)
                            : licenceFields(auth, strings, web: false),
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
                                strings.dataSafeTerms,
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
        context.widthClass.index < AppWidthClass.largeTablet.index;
    return WebAuthShell(
      topBar: languageChip(),
      child: Form(
        key: formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (showInlineLogo) ...[
              const Center(child: BrandLogo(width: 150)),
              const SizedBox(height: 24),
            ],
            Material(
              color: Colors.white,
              elevation: 8,
              shadowColor: AppColors.primaryDark.withValues(alpha: .12),
              borderRadius: BorderRadius.circular(24),
              child: Padding(
                padding: const EdgeInsets.fromLTRB(28, 28, 28, 24),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Text(
                      strings.welcomeBack,
                      style: AppTypography.screenTitle().copyWith(
                        fontSize: 28,
                        fontWeight: FontWeight.w800,
                        color: AppColors.primary,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      _mode == _LoginMode.staff
                          ? strings.staffLoginHint
                          : strings.licenceLoginHint,
                      style: AppTypography.body(
                        color: AppColors.textSecondary,
                      ),
                    ),
                    const SizedBox(height: 22),
                    modeToggle(web: true, strings: strings),
                    const SizedBox(height: 22),
                    _mode == _LoginMode.staff
                        ? staffFields(auth, strings, web: true)
                        : licenceFields(auth, strings, web: true),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 18),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(
                  Icons.wifi_rounded,
                  size: 16,
                  color: AppColors.textSecondary.withValues(alpha: .8),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    strings.internetRequiredFooter,
                    style: AppTypography.bodySmall(),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _ModeTab extends StatelessWidget {
  const _ModeTab({
    required this.selected,
    required this.label,
    required this.icon,
    required this.onTap,
  });

  final bool selected;
  final String label;
  final IconData icon;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: selected ? AppColors.primary : Colors.transparent,
      borderRadius: BorderRadius.circular(11),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(11),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                icon,
                size: 18,
                color: selected ? Colors.white : AppColors.primary,
              ),
              const SizedBox(width: 6),
              Flexible(
                child: Text(
                  label,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    fontWeight: FontWeight.w600,
                    fontSize: 13,
                    color: selected ? Colors.white : AppColors.primary,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _InfoBanner extends StatelessWidget {
  const _InfoBanner({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(12, 12, 14, 12),
      decoration: BoxDecoration(
        color: AppColors.primarySoft,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.primary.withValues(alpha: .15)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 22,
            height: 22,
            decoration: const BoxDecoration(
              color: AppColors.primary,
              shape: BoxShape.circle,
            ),
            child: const Icon(
              Icons.info_rounded,
              size: 14,
              color: Colors.white,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              text,
              style: AppTypography.bodySmall(color: AppColors.navy),
            ),
          ),
        ],
      ),
    );
  }
}
