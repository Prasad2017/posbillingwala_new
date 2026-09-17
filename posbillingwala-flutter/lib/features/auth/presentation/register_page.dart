import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_constants.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/auth/presentation/web_auth_shell.dart';

/* Matches `docs/layout/activity_register.xml` — trial fields only. */
class RegisterPage extends ConsumerStatefulWidget {
  const RegisterPage({super.key});

  @override
  ConsumerState<RegisterPage> createState() => RegisterPageState();
}

class RegisterPageState extends ConsumerState<RegisterPage> {
  final formKey = GlobalKey<FormState>();
  final registerPageName = TextEditingController();
  final contact = TextEditingController();
  final registerPageShop = TextEditingController();
  final registerPageAddress = TextEditingController();
  bool busy = false;

  @override
  void dispose() {
    registerPageName.dispose();
    contact.dispose();
    registerPageShop.dispose();
    registerPageAddress.dispose();
    super.dispose();
  }

  Future<void> submit() async {
    if (!formKey.currentState!.validate()) return;
    setState(() => busy = true);
    try {
      final result = await ref.read(authRepositoryProvider).registerTrial(
            name: registerPageName.text,
            contactNumber: contact.text,
            address: registerPageAddress.text,
            shopName: registerPageShop.text,
          );
      if (!mounted) return;
      if (!result.isSuccess ||
          result.licenceKey == null ||
          result.licenceKey!.isEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(result.message ?? 'Registration failed')),
        );
        return;
      }

      final mpin = (result.mpin == null || result.mpin!.isEmpty)
          ? '9082'
          : result.mpin!;
      final reportPin = (result.reportPin == null || result.reportPin!.isEmpty)
          ? '9082'
          : result.reportPin!;

      await showDialog<void>(
        context: context,
        barrierDismissible: false,
        builder: (context) => AlertDialog(
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
          title: const Text('Trial account created'),
          content: Text(
            'Licence key: ${result.licenceKey}\n'
            'PB-PIN: $mpin\n'
            'Report PIN: $reportPin\n\n'
            'Save these details. We will log you in next.',
          ),
          actions: [
            AppButton(
              label: 'Continue',
              expanded: false,
              onPressed: () => Navigator.of(context).pop(),
            ),
          ],
        ),
      );

      final ok = await ref
          .read(authControllerProvider.notifier)
          .loginWithLicence(result.licenceKey!);
      if (ok && mounted) {
        context.go('/');
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Registration failed.\n$e')),
        );
      }
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final form = Form(
      key: formKey,
      child: Column(
        children: [
          if (!AppPlatform.useDesktopShell ||
              context.widthClass.index < AppWidthClass.expanded.index)
            const BrandLogo(width: 180),
          if (!AppPlatform.useDesktopShell ||
              context.widthClass.index < AppWidthClass.expanded.index)
            const SizedBox(height: 16),
          Align(
            alignment: AppPlatform.useDesktopShell
                ? Alignment.centerLeft
                : Alignment.center,
            child: Text(
              AppPlatform.useDesktopShell
                  ? 'Create a free trial'
                  : 'Create a free trial account for your shop.',
              textAlign: AppPlatform.useDesktopShell
                  ? TextAlign.start
                  : TextAlign.center,
              style: AppPlatform.useDesktopShell
                  ? AppTypography.screenTitle()
                  : AppTypography.body(),
            ),
          ),
          const SizedBox(height: 20),
          AppTextField(
            controller: registerPageName,
            label: 'Your name',
            prefixSvg: AppAssets.svgPerson,
            autofocus: AppPlatform.useDesktopShell,
            validator: (v) =>
                (v == null || v.trim().isEmpty) ? 'Required' : null,
          ),
          const SizedBox(height: 12),
          AppTextField(
            controller: contact,
            label: 'Contact number',
            prefixSvg: AppAssets.svgPhone,
            keyboardType: TextInputType.phone,
            maxLength: 15,
            validator: (v) =>
                (v == null || v.trim().isEmpty) ? 'Required' : null,
          ),
          const SizedBox(height: 12),
          AppTextField(
            controller: registerPageShop,
            label: 'Shop name',
            prefixSvg: AppAssets.svgBusiness,
            textCapitalization: TextCapitalization.words,
            validator: (v) =>
                (v == null || v.trim().isEmpty) ? 'Required' : null,
          ),
          const SizedBox(height: 12),
          AppTextField(
            controller: registerPageAddress,
            label: 'Address',
            prefixSvg: AppAssets.svgLocation,
            maxLines: 3,
            validator: (v) =>
                (v == null || v.trim().isEmpty) ? 'Required' : null,
          ),
          const SizedBox(height: 28),
          AppButton(
            label: busy ? 'Please wait…' : 'Create Free Account',
            isLoading: busy,
            onPressed: busy ? null : submit,
          ),
          const SizedBox(height: 12),
          TextButton(
            onPressed: busy ? null : () => context.go('/login'),
            child: Text(
              'Already have an account? Login',
              style: AppTypography.cardTitle(color: AppColors.primary),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            AppConstants.supportPhone,
            style: AppTypography.body(color: AppColors.primary),
          ),
        ],
      ),
    );

    if (AppPlatform.useDesktopShell) {
      return WebAuthShell(
        maxContentWidth: 480,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Align(
              alignment: Alignment.centerLeft,
              child: TextButton.icon(
                onPressed: busy ? null : () => context.go('/login'),
                icon: const Icon(Icons.arrow_back_rounded, size: 18),
                label: const Text('Back to login'),
              ),
            ),
            form,
          ],
        ),
      );
    }

    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        title: const Text(''),
        leading: IconButton(
          tooltip: 'Back to login',
          onPressed: () => context.go('/login'),
          icon: const Icon(Icons.arrow_back_rounded),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
          child: ResponsiveContent(
            padding: EdgeInsets.zero,
            child: form,
          ),
        ),
      ),
    );
  }
}
