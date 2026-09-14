import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/app_module_icon.dart';
import 'package:pos_billingwala_v2/core/widgets/app_states.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/features/notifications/domain/fcm_service.dart';
import 'package:pos_billingwala_v2/features/notifications/domain/notification_navigator.dart';
import 'package:pos_billingwala_v2/features/notifications/domain/notification_providers.dart';

class NotificationsPage extends ConsumerStatefulWidget {
  const NotificationsPage({super.key});

  @override
  ConsumerState<NotificationsPage> createState() => NotificationsPageState();
}

class NotificationsPageState extends ConsumerState<NotificationsPage> {
  List<Map<String, dynamic>> messTokens = const [];

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      await ref.read(inAppNotificationsProvider.notifier).reload();
      final tokens = await FcmService.loadPendingMessTokens();
      if (mounted) setState(() => messTokens = tokens);
    });
  }

  @override
  Widget build(BuildContext context) {
    final items = ref.watch(inAppNotificationsProvider);
    final time = DateFormat('dd MMM, hh:mm a');

    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        title: const Text('Notifications'),
        actions: [
          TextButton(
            onPressed: () =>
                ref.read(inAppNotificationsProvider.notifier).markAllRead(),
            child: const Text(
              'Mark all read',
              style: TextStyle(color: Colors.white),
            ),
          ),
        ],
      ),
      body: ResponsiveScrollShell(
        dashboard: true,
        child: ListView(
          padding: EdgeInsets.all(
            AppBreakpoints.pagePaddingFor(context.widthClass),
          ),
          children: [
          if (messTokens.isNotEmpty) ...[
            Text("Pending mess meal tokens",
                style: AppTypography.sectionTitle()),
            const SizedBox(height: 8),
            ...messTokens.take(20).map(
                  (t) => AppCard(
                    accentColor: AppColors.orange,
                    color: AppColors.warning.withValues(alpha: 0.12),
                    padding: EdgeInsets.zero,
                    child: ListTile(
                      leading: const AppModuleIcon(
                        svgPath: AppAssets.svgQr,
                        color: AppColors.orange,
                        size: 48,
                      ),
                      title: Text(
                        t['tokenNumber']?.toString().isNotEmpty == true
                            ? t['tokenNumber'].toString()
                            : 'Meal token',
                        style: AppTypography.cardTitle(),
                      ),
                      subtitle: Text(
                        [
                          if ((t['mealSession']?.toString() ?? '').isNotEmpty)
                            t['mealSession'],
                          if ((t['registrationNo']?.toString() ?? '')
                              .isNotEmpty)
                            t['registrationNo'],
                        ].join(' • '),
                      ),
                    ),
                  ),
                ),
            const SizedBox(height: 16),
          ],
          Text('Alerts', style: AppTypography.sectionTitle()),
          const SizedBox(height: 8),
          if (items.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 24),
              child: AppEmptyState(
                title: 'No notifications',
                message: 'Licence and promo pushes appear here.',
                iconAsset: AppAssets.svgNotification,
              ),
            )
          else
            ...items.map(
              (n) => AppCard(
                accentColor: n.read ? AppColors.teal : AppColors.primary,
                color: n.read ? null : AppColors.primaryLight,
                padding: EdgeInsets.zero,
                child: ListTile(
                  contentPadding:
                      const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                  onTap: () async {
                    await ref
                        .read(inAppNotificationsProvider.notifier)
                        .markRead(n.id);
                    if (!context.mounted) return;
                    await openNotificationTarget(
                      context,
                      type: n.type,
                      url: n.url,
                    );
                  },
                  leading: AppModuleIcon(
                    svgPath: n.type == 'license_expiring'
                        ? AppAssets.svgWarning
                        : AppAssets.svgNotification,
                    color: n.type == 'license_expiring'
                        ? AppColors.orange
                        : AppColors.primary,
                    size: 48,
                  ),
                  title: Text(
                    n.title,
                    style: TextStyle(
                      fontWeight: n.read ? FontWeight.w600 : FontWeight.w800,
                    ),
                  ),
                  subtitle: Text(
                    '${n.body}\n${time.format(n.createdAt)}',
                  ),
                  isThreeLine: true,
                ),
              ),
            ),
        ],
        ),
      ),
    );
  }
}
