import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/auth/presentation/web_auth_shell.dart';

class StaffLoginPage extends ConsumerStatefulWidget {
  const StaffLoginPage({super.key});

  @override
  ConsumerState<StaffLoginPage> createState() => StaffLoginPageState();
}

class StaffLoginPageState extends ConsumerState<StaffLoginPage> {
  final mobile = TextEditingController();
  final pin = TextEditingController();

  @override
  void dispose() {
    mobile.dispose();
    pin.dispose();
    super.dispose();
  }

  Future<void> submit() async {
    final ok = await ref
        .read(authControllerProvider.notifier)
        .loginWithStaff(mobileNumber: mobile.text.trim(), pin: pin.text.trim());
    if (!ok && mounted) {
      pin.clear();
    }
  }

  @override
  Widget build(BuildContext context) {
    final auth = ref.watch(authControllerProvider);
    final children = <Widget>[
      if (!AppPlatform.useDesktopShell) const SizedBox(height: 24),
      if (AppPlatform.useDesktopShell &&
          context.widthClass.index < AppWidthClass.expanded.index) ...[
        const Center(child: BrandLogo(width: 140)),
        const SizedBox(height: 20),
      ],
      Text(
        AppPlatform.useDesktopShell ? 'Staff login' : 'POS LOGIN',
        style: AppPlatform.useDesktopShell
            ? AppTypography.screenTitle()
            : const TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.w700,
                color: AppColors.navy,
              ),
      ),
      const SizedBox(height: 8),
      Text(
        auth.session?.shopName ?? 'Staff PIN login',
        style: AppPlatform.useDesktopShell
            ? AppTypography.body()
            : const TextStyle(color: Colors.black54),
      ),
      const SizedBox(height: 28),
      AppTextField(
        required: true,
        controller: mobile,
        label: 'Mobile Number',
        autofocus: AppPlatform.useDesktopShell,
        keyboardType: TextInputType.phone,
        maxLength: 10,
        showCounter: false,
        inputFormatters: [FilteringTextInputFormatter.digitsOnly],
      ),
      const SizedBox(height: 12),
      AppTextField(
        required: true,
        controller: pin,
        label: 'App PIN',
        obscureText: true,
        keyboardType: TextInputType.number,
        maxLength: 6,
        showCounter: false,
        inputFormatters: [FilteringTextInputFormatter.digitsOnly],
        onSubmitted: (_) => submit(),
      ),
      if (auth.errorMessage != null) ...[
        const SizedBox(height: 12),
        Text(auth.errorMessage!, style: const TextStyle(color: Colors.red)),
      ],
      const SizedBox(height: 24),
      AppButton(
        label: auth.busy ? 'Please wait…' : 'Login',
        onPressed: auth.busy ? null : submit,
      ),
      TextButton(
        onPressed: () =>
            ref.read(authControllerProvider.notifier).clearLicence(),
        child: const Text('Change licence'),
      ),
    ];

    if (AppPlatform.useDesktopShell) {
      return WebAuthShell(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: children,
        ),
      );
    }

    return Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: ListView(padding: const EdgeInsets.all(24), children: children),
      ),
    );
  }
}
