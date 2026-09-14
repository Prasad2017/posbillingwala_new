import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_constants.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/widgets/app_svg.dart';
import 'package:url_launcher/url_launcher.dart';

const kSupportOnlineMessage =
    'Support tickets work only when you are online. Create a ticket or refresh to see admin replies. For urgent help, call support.';

String formatSupportPhone(String raw) {
  final digits = raw.replaceAll(RegExp(r'\D'), '');
  if (digits.length == 12 && digits.startsWith('91')) {
    return '+91 ${digits.substring(2, 7)} ${digits.substring(7)}';
  }
  if (digits.length == 10) {
    return '+91 ${digits.substring(0, 5)} ${digits.substring(5)}';
  }
  return raw;
}

Future<void> callSupport(BuildContext context) async {
  final uri = Uri(scheme: 'tel', path: AppConstants.supportPhone);
  final ok = await launchUrl(uri);
  if (!ok && context.mounted) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Call ${formatSupportPhone(AppConstants.supportPhone)}')),
    );
  }
}

Future<bool> checkOnline() async {
  final results = await Connectivity().checkConnectivity();
  return results.any(
    (r) =>
        r == ConnectivityResult.mobile ||
        r == ConnectivityResult.wifi ||
        r == ConnectivityResult.ethernet ||
        r == ConnectivityResult.vpn ||
        r == ConnectivityResult.other,
  );
}

Color ticketStatusColor(String status) {
  final value = status.toLowerCase();
  if (value.contains('closed') || value.contains('resolved')) {
    return AppColors.danger;
  }
  if (value.contains('pending')) return AppColors.orange;
  if (value.contains('urgent')) return AppColors.red;
  return AppColors.primary;
}

class SupportOnlineBanner extends StatelessWidget {
  const SupportOnlineBanner({
    super.key,
    required this.online,
    this.title = "We're Here to Help!",
  });

  final bool online;
  final String title;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.primaryLight,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.primary.withValues(alpha: 0.12)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Stack(
            clipBehavior: Clip.none,
            children: [
              Container(
                width: 46,
                height: 46,
                decoration: BoxDecoration(
                  color: Colors.white,
                  shape: BoxShape.circle,
                  border: Border.all(color: AppColors.primary.withValues(alpha: 0.15)),
                ),
                alignment: Alignment.center,
                child: Icon(
                  Icons.wifi_rounded,
                  color: online ? AppColors.primary : AppColors.textSecondary,
                  size: 24,
                ),
              ),
              Positioned(
                right: -2,
                bottom: -2,
                child: Container(
                  width: 16,
                  height: 16,
                  decoration: BoxDecoration(
                    color: online ? AppColors.green : AppColors.danger,
                    shape: BoxShape.circle,
                    border: Border.all(color: Colors.white, width: 2),
                  ),
                  child: Icon(
                    online ? Icons.check_rounded : Icons.close_rounded,
                    size: 10,
                    color: Colors.white,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: AppTypography.cardTitle()),
                const SizedBox(height: 4),
                Text(
                  kSupportOnlineMessage,
                  style: AppTypography.bodySmall(),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class SupportUrgentHelpCard extends StatelessWidget {
  const SupportUrgentHelpCard({super.key});

  @override
  Widget build(BuildContext context) {
    final phone = formatSupportPhone(AppConstants.supportPhone);
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.primaryLight,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.primary.withValues(alpha: 0.12)),
      ),
      child: Row(
        children: [
          Container(
            width: 46,
            height: 46,
            decoration: BoxDecoration(
              color: Colors.white,
              shape: BoxShape.circle,
              border: Border.all(color: AppColors.primary.withValues(alpha: 0.12)),
            ),
            alignment: Alignment.center,
            child: const AppSvg(
              AppAssets.svgHeadset,
              width: 22,
              height: 22,
              color: AppColors.primary,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Need Urgent Help?', style: AppTypography.cardTitle()),
                const SizedBox(height: 2),
                Text(
                  'Call our support team for immediate assistance.',
                  style: AppTypography.bodySmall(),
                ),
                const SizedBox(height: 10),
                Material(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(999),
                  child: InkWell(
                    onTap: () => callSupport(context),
                    borderRadius: BorderRadius.circular(999),
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(999),
                        border: Border.all(color: AppColors.primary.withValues(alpha: 0.45)),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const AppSvg(
                            AppAssets.svgPhone,
                            width: 16,
                            height: 16,
                            color: AppColors.primaryDark,
                          ),
                          const SizedBox(width: 8),
                          Text(
                            phone,
                            style: AppTypography.bodySmall(color: AppColors.primaryDark)
                                .copyWith(fontWeight: FontWeight.w700),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class SupportHoursCard extends StatelessWidget {
  const SupportHoursCard({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.border),
      ),
      child: Row(
        children: [
          Container(
            width: 46,
            height: 46,
            decoration: BoxDecoration(
              color: AppColors.primaryLight,
              shape: BoxShape.circle,
            ),
            alignment: Alignment.center,
            child: const AppSvg(
              AppAssets.svgCalendar,
              width: 22,
              height: 22,
              color: AppColors.primary,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Support Hours', style: AppTypography.cardTitle()),
                const SizedBox(height: 4),
                Text(
                  AppConstants.supportHoursDays,
                  style: AppTypography.bodySmall(),
                ),
                const SizedBox(height: 2),
                Text(
                  AppConstants.supportHoursTime,
                  style: AppTypography.body(color: AppColors.navy)
                      .copyWith(fontWeight: FontWeight.w700),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class SupportNavTile extends StatelessWidget {
  const SupportNavTile({
    super.key,
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.iconColor,
    required this.iconBg,
    required this.onTap,
    this.showDivider = false,
  });

  final String title;
  final String subtitle;
  final IconData icon;
  final Color iconColor;
  final Color iconBg;
  final VoidCallback onTap;
  final bool showDivider;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        if (showDivider)
          const Divider(height: 1, indent: 14, endIndent: 14, color: AppColors.border),
        InkWell(
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
            child: Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    color: iconBg,
                    shape: BoxShape.circle,
                  ),
                  alignment: Alignment.center,
                  child: Icon(icon, color: iconColor, size: 22),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(title, style: AppTypography.cardTitle()),
                      const SizedBox(height: 2),
                      Text(subtitle, style: AppTypography.bodySmall()),
                    ],
                  ),
                ),
                Container(
                  width: 32,
                  height: 32,
                  decoration: BoxDecoration(
                    color: AppColors.surface,
                    shape: BoxShape.circle,
                    border: Border.all(color: AppColors.border),
                  ),
                  child: const Icon(
                    Icons.chevron_right_rounded,
                    size: 20,
                    color: AppColors.primary,
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
