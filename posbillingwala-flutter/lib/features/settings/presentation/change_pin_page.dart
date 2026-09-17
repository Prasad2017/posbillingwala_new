import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';

class ChangePinPage extends ConsumerStatefulWidget {
  const ChangePinPage({super.key});

  @override
  ConsumerState<ChangePinPage> createState() => ChangePinPageState();
}

class ChangePinPageState extends ConsumerState<ChangePinPage> {
  final changePinPageCurrent = TextEditingController();
  final changePinPageNext = TextEditingController();
  final changePinPageConfirm = TextEditingController();
  bool busy = false;

  @override
  void dispose() {
    changePinPageCurrent.dispose();
    changePinPageNext.dispose();
    changePinPageConfirm.dispose();
    super.dispose();
  }

  Future<void> save() async {
    final session = ref.read(authControllerProvider).session;
    final stored = session?.appPin?.trim() ?? '';
    final current = changePinPageCurrent.text.trim();
    final next = changePinPageNext.text.trim();
    final confirm = changePinPageConfirm.text.trim();

    if (stored.isNotEmpty && current != stored) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Current PB-PIN is incorrect')),
      );
      return;
    }
    if (next.length != 4 || int.tryParse(next) == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('New PIN must be 4 digits')),
      );
      return;
    }
    if (next != confirm) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('New PIN and confirm do not match')),
      );
      return;
    }

    setState(() => busy = true);
    try {
      final ok =
          await ref.read(authControllerProvider.notifier).updateAppPin(next);
      if (!mounted) return;
      if (ok) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('PB-PIN updated')),
        );
        Navigator.of(context).pop();
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Unable to update PIN')),
        );
      }
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final hasPin =
        (ref.watch(authControllerProvider).session?.appPin?.trim() ?? '')
            .isNotEmpty;

    return Scaffold(
      appBar: AppBar(title: const Text('Change PB-PIN')),
      body: ResponsiveScrollShell(
        dashboard: true,
        child: ListView(
        padding: EdgeInsets.all(
            AppBreakpoints.pagePaddingFor(context.widthClass),
          ),
        children: [
          const Center(
            child: AppModuleIcon(
              icon: Icons.lock_reset_rounded,
              color: AppColors.red,
              size: 72,
            ),
          ),
          const SizedBox(height: 14),
          const Center(
            child: Text(
              'Keep your billing secure',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w900,
                color: AppColors.navy,
              ),
            ),
          ),
          const SizedBox(height: 20),
          AppCard(
            accentColor: AppColors.primary,
            child: Column(
              children: [
                if (hasPin) ...[
                  AppTextField(
                    controller: changePinPageCurrent,
                    label: 'Current PB-PIN',
                    obscureText: true,
                    keyboardType: TextInputType.number,
                    maxLength: 4,
                    showCounter: false,
                    inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  ),
                  const SizedBox(height: 12),
                ],
                AppTextField(
                  controller: changePinPageNext,
                  label: 'New PB-PIN',
                  obscureText: true,
                  keyboardType: TextInputType.number,
                  maxLength: 4,
                  showCounter: false,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                ),
                const SizedBox(height: 12),
                AppTextField(
                  controller: changePinPageConfirm,
                  label: 'Confirm new PB-PIN',
                  obscureText: true,
                  keyboardType: TextInputType.number,
                  maxLength: 4,
                  showCounter: false,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                ),
                const SizedBox(height: 16),
                AppButton(
                  label: 'Update PIN',
                  isLoading: busy,
                  onPressed: save,
                ),
              ],
            ),
          ),
        ],
      ),
      ),
    );
  }
}
