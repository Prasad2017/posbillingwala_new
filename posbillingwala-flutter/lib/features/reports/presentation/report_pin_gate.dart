import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* Android `report_password_dialog` — unlock Reports / Mess Member List. */
Future<bool> showReportPinGate(
  BuildContext context,
  WidgetRef ref, {
  String title = 'Report Detail',
  String message = 'Enter PIN to open sales and invoice reports.',
}) async {
  final expected = ref.read(authControllerProvider).session?.reportPin?.trim();
  final pin = (expected == null || expected.isEmpty) ? '9082' : expected;
  final controller = TextEditingController();

  final ok = await showAppBottomSheet<bool>(
    context: context,
    title: title,
    icon: Icons.lock_outline_rounded,
    child: Builder(
      builder: (sheetContext) => Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Center(
            child: AppModuleIcon(
              icon: Icons.lock_rounded,
              color: AppColors.primary,
              size: 68,
            ),
          ),
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.primary.withValues(alpha: .07),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Text(
              message,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontWeight: FontWeight.w700,
                color: AppColors.navy,
              ),
            ),
          ),
          const SizedBox(height: 16),
          AppTextField(
            required: true,
            controller: controller,
            label: 'Enter PIN',
            hint: 'PIN',
            keyboardType: TextInputType.number,
            obscureText: true,
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: AppButton(
                  label: 'Dismiss',
                  variant: AppButtonVariant.danger,
                  onPressed: () => Navigator.pop(sheetContext, false),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: AppButton(
                  label: 'Continue',
                  onPressed: () {
                    final match =
                        controller.text.trim().toLowerCase() ==
                        pin.toLowerCase();
                    Navigator.pop(sheetContext, match);
                  },
                ),
              ),
            ],
          ),
        ],
      ),
    ),
  );
  controller.dispose();
  if (ok == true) return true;
  if (context.mounted) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(AppStrings.of(ref).incorrectReportPin)),
    );
  }
  return false;
}

/* Android MessInvoiceAdapter setBillPrintPassword — PIN = member mobile. */
Future<bool> showMessMobilePinGate(
  BuildContext context, {
  required String memberMobile,
  required String title,
}) async {
  final expected = memberMobile.trim();
  if (expected.isEmpty) return false;
  final controller = TextEditingController();

  final ok = await showAppBottomSheet<bool>(
    context: context,
    title: title,
    icon: Icons.lock_outline_rounded,
    child: Builder(
      builder: (sheetContext) => Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Center(
            child: AppModuleIcon(
              icon: Icons.lock_rounded,
              color: AppColors.primary,
              size: 68,
            ),
          ),
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.primary.withValues(alpha: .07),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Text(
              title,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontWeight: FontWeight.w700,
                color: AppColors.navy,
              ),
            ),
          ),
          const SizedBox(height: 16),
          AppTextField(
            required: true,
            controller: controller,
            label: 'Enter mobile PIN',
            hint: 'Member mobile',
            keyboardType: TextInputType.phone,
            obscureText: true,
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: AppButton(
                  label: 'Dismiss',
                  variant: AppButtonVariant.danger,
                  onPressed: () => Navigator.pop(sheetContext, false),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: AppButton(
                  label: 'Continue',
                  onPressed: () {
                    final match =
                        controller.text.trim().toLowerCase() ==
                        expected.toLowerCase();
                    Navigator.pop(sheetContext, match);
                  },
                ),
              ),
            ],
          ),
        ],
      ),
    ),
  );
  controller.dispose();
  if (ok == true) return true;
  if (context.mounted) {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Enter correct pin')),
    );
  }
  return false;
}

Future<void> pushReportsUnlocked(
  BuildContext context,
  WidgetRef ref, {
  String route = '/reports',
}) async {
  if (!await showReportPinGate(context, ref)) return;
  if (!context.mounted) return;
  context.push(route);
}
