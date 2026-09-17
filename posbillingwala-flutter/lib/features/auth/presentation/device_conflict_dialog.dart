import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/auth/data/auth_repository.dart';

Future<DeviceConflictAction> showDeviceConflictDialog(
  BuildContext context,
  String message,
) async {
  final result = await showDialog<DeviceConflictAction>(
    context: context,
    barrierDismissible: false,
    barrierColor: Colors.black.withValues(alpha: 0.45),
    builder: (context) => DeviceConflictDialog(message: message),
  );
  return result ?? DeviceConflictAction.cancel;
}

class DeviceConflictDialog extends StatelessWidget {
  const DeviceConflictDialog({super.key, required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    final desktop = AppPlatform.useDesktopShell;

    return Dialog(
      insetPadding: EdgeInsets.symmetric(
        horizontal: desktop ? 24 : 20,
        vertical: 24,
      ),
      backgroundColor: Colors.transparent,
      elevation: 0,
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: desktop ? 460 : 420),
        child: Material(
          color: Colors.white,
          elevation: desktop ? 18 : 8,
          shadowColor: AppColors.navy.withValues(alpha: 0.18),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(desktop ? 20 : 22),
            side: BorderSide(color: AppColors.border.withValues(alpha: 0.7)),
          ),
          child: Padding(
            padding: EdgeInsets.fromLTRB(
              desktop ? 28 : 22,
              desktop ? 26 : 22,
              desktop ? 28 : 22,
              desktop ? 22 : 18,
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      width: 48,
                      height: 48,
                      decoration: BoxDecoration(
                        color: AppColors.orange.withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(14),
                      ),
                      child: const Icon(
                        Icons.phonelink_lock_rounded,
                        color: AppColors.orange,
                        size: 26,
                      ),
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'Device already registered',
                            style: TextStyle(
                              fontFamily: AppFonts.family,
                              color: AppColors.navy,
                              fontWeight: FontWeight.w800,
                              fontSize: 18,
                              height: 1.25,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            desktop
                                ? 'This licence is already signed in on another browser or device.'
                                : 'This licence is already linked to another device.',
                            style: AppTypography.bodySmall(),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 18),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
                  decoration: BoxDecoration(
                    color: const Color(0xFFF7F9FC),
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(
                      color: AppColors.border.withValues(alpha: 0.85),
                    ),
                  ),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Padding(
                        padding: EdgeInsets.only(top: 2),
                        child: Icon(
                          Icons.info_outline_rounded,
                          size: 18,
                          color: AppColors.textSecondary,
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          message.trim().isEmpty
                              ? 'Continue only if you want to use this device instead.'
                              : message,
                          style: AppTypography.body(
                            color: AppColors.navy.withValues(alpha: 0.82),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                Text(
                  'Choosing “Yes, bind here” will move the licence to this ${desktop ? 'browser' : 'device'}.',
                  style: AppTypography.bodySmall(),
                ),
                const SizedBox(height: 22),
                if (desktop)
                  Row(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      TextButton(
                        onPressed: () => Navigator.of(context).pop(
                          DeviceConflictAction.cancel,
                        ),
                        child: const Text(
                          'Cancel',
                          style: TextStyle(
                            fontFamily: AppFonts.family,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                      const SizedBox(width: 8),
                      FilledButton(
                        onPressed: () => Navigator.of(context).pop(
                          DeviceConflictAction.rebind,
                        ),
                        style: FilledButton.styleFrom(
                          backgroundColor: AppColors.primary,
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(
                            horizontal: 18,
                            vertical: 14,
                          ),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                        ),
                        child: const Text(
                          'Yes, bind here',
                          style: TextStyle(
                            fontFamily: AppFonts.family,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ),
                    ],
                  )
                else
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      FilledButton(
                        onPressed: () => Navigator.of(context).pop(
                          DeviceConflictAction.rebind,
                        ),
                        style: FilledButton.styleFrom(
                          backgroundColor: AppColors.primary,
                          foregroundColor: Colors.white,
                          minimumSize: const Size.fromHeight(48),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(14),
                          ),
                        ),
                        child: const Text(
                          'Yes, bind here',
                          style: TextStyle(
                            fontFamily: AppFonts.family,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ),
                      const SizedBox(height: 8),
                      TextButton(
                        onPressed: () => Navigator.of(context).pop(
                          DeviceConflictAction.cancel,
                        ),
                        child: const Text(
                          'No, keep other device',
                          style: TextStyle(
                            fontFamily: AppFonts.family,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ],
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
