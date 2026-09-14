import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/app_svg.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/features/support/presentation/support_widgets.dart';

class SupportPage extends ConsumerStatefulWidget {
  const SupportPage({super.key});

  @override
  ConsumerState<SupportPage> createState() => SupportPageState();
}

class SupportPageState extends ConsumerState<SupportPage> {
  bool supportPageOnline = true;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => refreshOnline());
  }

  Future<void> refreshOnline() async {
    final online = await checkOnline();
    if (!mounted) return;
    setState(() => supportPageOnline = online);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        title: const Text('Help & Support'),
        actions: [
          IconButton(
            tooltip: 'Call support',
            onPressed: () => callSupport(context),
            icon: const AppSvg(
              AppAssets.svgHeadset,
              width: 22,
              height: 22,
              color: Colors.white,
            ),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: refreshOnline,
        child: ResponsiveScrollShell(
          dashboard: true,
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: EdgeInsets.fromLTRB(
              AppBreakpoints.pagePaddingFor(context.widthClass),
              16,
              AppBreakpoints.pagePaddingFor(context.widthClass),
              24,
            ),
            children: [
            SupportOnlineBanner(online: supportPageOnline),
            const SizedBox(height: 14),
            Container(
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(18),
                border: Border.all(color: AppColors.border),
              ),
              child: Column(
                children: [
                  SupportNavTile(
                    title: 'Create Support Ticket',
                    subtitle:
                        "Need help? Create a new support ticket and we'll get back to you.",
                    icon: Icons.confirmation_number_outlined,
                    iconColor: AppColors.primary,
                    iconBg: AppColors.primaryLight,
                    onTap: () => context.push('/support/create'),
                  ),
                  SupportNavTile(
                    title: 'My Support Tickets',
                    subtitle: 'View your previous tickets and check their status.',
                    icon: Icons.folder_open_rounded,
                    iconColor: AppColors.green,
                    iconBg: AppColors.green.withValues(alpha: 0.12),
                    showDivider: true,
                    onTap: () => context.push('/support/tickets'),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 14),
            const SupportUrgentHelpCard(),
            const SizedBox(height: 14),
            const SupportHoursCard(),
          ],
          ),
        ),
      ),
    );
  }
}
