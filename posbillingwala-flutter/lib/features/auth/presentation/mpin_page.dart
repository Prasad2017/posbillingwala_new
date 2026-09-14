import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
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

/// Matches `docs/layout/activity_login_mpin.xml`.
class MpinPage extends ConsumerStatefulWidget {
  const MpinPage({super.key});

  @override
  ConsumerState<MpinPage> createState() => _MpinPageState();
}

class _MpinPageState extends ConsumerState<MpinPage> {
  final _controllers = List.generate(4, (_) => TextEditingController());
  final _focusNodes = List.generate(4, (_) => FocusNode());

  @override
  void initState() {
    super.initState();
    for (final node in _focusNodes) {
      node.addListener(() {
        if (mounted) setState(() {});
      });
    }
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(authControllerProvider.notifier).setDeviceConflictHandler(
            (message) => showDeviceConflictDialog(context, message),
          );
      _focusNodes.first.requestFocus();
    });
  }

  @override
  void dispose() {
    for (final c in _controllers) {
      c.dispose();
    }
    for (final n in _focusNodes) {
      n.dispose();
    }
    super.dispose();
  }

  String get _mpin => _controllers.map((c) => c.text).join();

  Future<void> _submit() async {
    if (_mpin.length != 4) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter your 4-digit PB-PIN')),
      );
      return;
    }
    FocusScope.of(context).unfocus();
    final ok =
        await ref.read(authControllerProvider.notifier).loginWithMpin(_mpin);
    if (!ok && mounted) {
      for (final c in _controllers) {
        c.clear();
      }
      _focusNodes.first.requestFocus();
    }
  }

  void _onDigitChanged(int index, String value) {
    setState(() {});
    if (value.length == 1 && index < 3) {
      _focusNodes[index + 1].requestFocus();
    } else if (value.isEmpty && index > 0) {
      _focusNodes[index - 1].requestFocus();
    }
    if (_mpin.length == 4) _submit();
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
                  Text(strings.welcomeBack, style: AppTypography.screenTitle()),
                  const SizedBox(height: 8),
                  Text(
                    strings.signInSubtitle,
                    textAlign: TextAlign.center,
                    style: AppTypography.body(),
                  ),
                  const SizedBox(height: 24),
                  AppCard(
                    child: Column(
                      children: [
                        SizedBox(
                          width: 72,
                          height: 72,
                          child: DecoratedBox(
                            decoration: BoxDecoration(
                              color: Colors.white,
                              shape: BoxShape.circle,
                              border: Border.all(
                                color: AppColors.border,
                                width: 1.5,
                              ),
                              boxShadow: [
                                BoxShadow(
                                  color: AppColors.primary.withValues(
                                    alpha: 0.10,
                                  ),
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
                        Text(
                          'Enter PB-PIN',
                          style: AppTypography.sectionTitle(),
                        ),
                        const SizedBox(height: 6),
                        Text(
                          'Enter the 4-digit PIN for this device.',
                          textAlign: TextAlign.center,
                          style: AppTypography.bodySmall(),
                        ),
                        const SizedBox(height: 24),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: List.generate(4, (index) {
                            final filled =
                                _controllers[index].text.isNotEmpty;
                            final focused = _focusNodes[index].hasFocus;
                            return Padding(
                              padding: EdgeInsets.only(
                                left: index == 0 ? 0 : 12,
                              ),
                              child: SizedBox(
                                width: 58,
                                height: 58,
                                child: DecoratedBox(
                                  decoration: BoxDecoration(
                                    color: Colors.white,
                                    borderRadius: BorderRadius.circular(18),
                                    border: Border.all(
                                      color: filled || focused
                                          ? AppColors.primary
                                          : AppColors.border,
                                      width: filled || focused ? 2 : 1.5,
                                    ),
                                    boxShadow: [
                                      BoxShadow(
                                        color: AppColors.primary.withValues(
                                          alpha: filled ? 0.12 : 0.06,
                                        ),
                                        blurRadius: 8,
                                        offset: const Offset(0, 3),
                                      ),
                                    ],
                                  ),
                                  child: TextField(
                                    controller: _controllers[index],
                                    focusNode: _focusNodes[index],
                                    textAlign: TextAlign.center,
                                    keyboardType: TextInputType.number,
                                    obscureText: true,
                                    maxLength: 1,
                                    cursorColor: AppColors.primary,
                                    style: AppTypography.screenTitle(
                                      color: AppColors.primary,
                                    ),
                                    inputFormatters: [
                                      FilteringTextInputFormatter.digitsOnly,
                                    ],
                                    decoration: const InputDecoration(
                                      counterText: '',
                                      border: InputBorder.none,
                                      enabledBorder: InputBorder.none,
                                      focusedBorder: InputBorder.none,
                                      disabledBorder: InputBorder.none,
                                      errorBorder: InputBorder.none,
                                      focusedErrorBorder: InputBorder.none,
                                      filled: false,
                                      isCollapsed: true,
                                      contentPadding: EdgeInsets.zero,
                                    ),
                                    onChanged: (value) =>
                                        _onDigitChanged(index, value),
                                  ),
                                ),
                              ),
                            );
                          }),
                        ),
                        const SizedBox(height: 14),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            AppSvg(
                              AppAssets.svgLock,
                              width: 14,
                              height: 14,
                              color: AppColors.primary,
                            ),
                            const SizedBox(width: 6),
                            Text(
                              'Your data is safe with us',
                              style: AppTypography.caption(),
                            ),
                          ],
                        ),
                        const SizedBox(height: 20),
                        AppButton(
                          label: auth.busy ? strings.pleaseWait : strings.login,
                          isLoading: auth.busy,
                          onPressed: auth.busy ? null : _submit,
                        ),
                        const SizedBox(height: 16),
                        const AdBanner(slot: AdSlot.login),
                      ],
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
