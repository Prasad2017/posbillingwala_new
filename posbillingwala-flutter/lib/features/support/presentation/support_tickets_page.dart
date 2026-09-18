import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/support/data/support_api.dart';
import 'package:pos_billingwala_v2/features/support/data/support_dtos.dart';
import 'package:pos_billingwala_v2/features/support/presentation/support_widgets.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_cache.dart';

class SupportTicketsPage extends ConsumerStatefulWidget {
  const SupportTicketsPage({super.key});

  @override
  ConsumerState<SupportTicketsPage> createState() => SupportTicketsPageState();
}

class SupportTicketsPageState extends ConsumerState<SupportTicketsPage> {
  List<SupportTicketDto> supportTicketsPageTickets = const [];
  bool loading = false;
  bool supportTicketsPageOnline = true;
  String? error;
  String statusFilter = 'All Status';

  static const statusFilters = ['All Status', 'Open', 'Closed'];

  List<SupportTicketDto> get visibleTickets {
    if (statusFilter == 'All Status') return supportTicketsPageTickets;
    final needle = statusFilter.toLowerCase();
    return supportTicketsPageTickets.where((t) {
      final status = t.status.toLowerCase();
      if (needle == 'closed') {
        return status.contains('closed') || status.contains('resolved');
      }
      if (needle == 'open') {
        return !status.contains('closed') && !status.contains('resolved');
      }
      return status.contains(needle);
    }).toList();
  }

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => reload());
  }

  Future<void> reload() async {
    final online = await checkOnline();
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) {
      setState(() {
        supportTicketsPageOnline = online;
        error = 'Please login first';
        loading = false;
      });
      return;
    }
    setState(() {
      supportTicketsPageOnline = online;
      loading = true;
      error = null;
    });
    try {
      final cached = await CloudScreenCache.loadMapList(
        CloudScreenCache.supportTickets,
      );
      if (cached.isNotEmpty && mounted) {
        setState(() {
          supportTicketsPageTickets = cached
              .map(SupportTicketDto.fromJson)
              .toList();
          loading = false;
        });
      }
      final api = SupportApi(ref.read(apiClientProvider));
      final tickets = await api.getSupportTickets(userId);
      if (!mounted) return;
      setState(() {
        supportTicketsPageTickets = tickets;
        loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        error = '$e';
        loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final visible = visibleTickets;
    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        title: const Text('My Support Tickets'),
        actions: [
          const Padding(
            padding: EdgeInsets.only(right: 16),
            child: AppSvg(
              AppAssets.svgEmpty,
              width: 22,
              height: 22,
              color: Colors.white,
            ),
          ),
        ],
      ),
      body: error != null && supportTicketsPageTickets.isEmpty
          ? AppErrorState(message: error!, onRetry: reload)
          : RefreshIndicator(
              onRefresh: reload,
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
                    SupportOnlineBanner(
                      online: supportTicketsPageOnline,
                      title: 'Stay Connected',
                    ),
                    const SizedBox(height: 14),
                    AppButton(
                      label: 'REFRESH TICKETS',
                      icon: Icons.refresh_rounded,
                      isLoading: loading,
                      onPressed: loading ? null : reload,
                    ),
                    const SizedBox(height: 18),
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            'Your Tickets (${visible.length})',
                            style: AppTypography.sectionTitle().copyWith(
                              fontSize: 16,
                            ),
                          ),
                        ),
                        PopupMenuButton<String>(
                          initialValue: statusFilter,
                          onSelected: (v) => setState(() => statusFilter = v),
                          itemBuilder: (context) => statusFilters
                              .map(
                                (s) => PopupMenuItem<String>(
                                  value: s,
                                  child: Text(s),
                                ),
                              )
                              .toList(),
                          child: Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 12,
                              vertical: 8,
                            ),
                            decoration: BoxDecoration(
                              color: Colors.white,
                              borderRadius: BorderRadius.circular(12),
                              border: Border.all(color: AppColors.border),
                            ),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Text(
                                  statusFilter == 'All Status'
                                      ? 'Select Item'
                                      : statusFilter,
                                  style: AppTypography.bodySmall(
                                    color: AppColors.navy,
                                  ),
                                ),
                                const SizedBox(width: 4),
                                const Icon(
                                  Icons.keyboard_arrow_down_rounded,
                                  size: 18,
                                  color: AppColors.textSecondary,
                                ),
                              ],
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    if (loading && supportTicketsPageTickets.isEmpty)
                      const Padding(
                        padding: EdgeInsets.symmetric(vertical: 40),
                        child: Center(child: CircularProgressIndicator()),
                      )
                    else if (supportTicketsPageTickets.isEmpty)
                      const Padding(
                        padding: EdgeInsets.only(top: 24),
                        child: AppEmptyState(
                          title: 'No support tickets yet',
                          message: 'Create a ticket whenever you need help.',
                          iconAsset: AppAssets.svgHeadset,
                        ),
                      )
                    else if (visible.isEmpty)
                      const Padding(
                        padding: EdgeInsets.only(top: 24),
                        child: AppEmptyState(
                          title: 'No tickets for this status',
                          message: 'Try another filter.',
                          iconAsset: AppAssets.svgFilter,
                        ),
                      )
                    else
                      ...visible.map((t) => TicketListCard(ticket: t)),
                    const SizedBox(height: 14),
                    const SupportUrgentHelpCard(),
                  ],
                ),
              ),
            ),
    );
  }
}

class TicketListCard extends StatelessWidget {
  const TicketListCard({super.key, required this.ticket});

  final SupportTicketDto ticket;

  @override
  Widget build(BuildContext context) {
    final ticketNo = (ticket.ticketNo ?? '').trim();
    final title = ticketNo.isNotEmpty
        ? ticketNo
        : (ticket.subject.isEmpty ? 'Ticket' : ticket.subject);
    final subtitle = ticket.subject.isEmpty
        ? (ticket.description.isEmpty
              ? (ticket.category.isEmpty ? 'Support' : ticket.category)
              : ticket.description)
        : ticket.subject;

    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Material(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        child: InkWell(
          borderRadius: BorderRadius.circular(16),
          onTap: () {
            final id = ticket.id;
            if (id == null || id.isEmpty) return;
            context.push('/support/$id');
          },
          child: Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppColors.border),
            ),
            child: Row(
              children: [
                Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    color: AppColors.primaryLight,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  alignment: Alignment.center,
                  child: const Icon(
                    Icons.confirmation_number_outlined,
                    color: AppColors.primary,
                    size: 20,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(title, style: AppTypography.cardTitle()),
                      const SizedBox(height: 2),
                      Text(
                        subtitle,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: AppTypography.bodySmall(),
                      ),
                      if ((ticket.createdAt ?? '').isNotEmpty) ...[
                        const SizedBox(height: 6),
                        Row(
                          children: [
                            Icon(
                              Icons.calendar_today_outlined,
                              size: 13,
                              color: Theme.of(context).hintColor,
                            ),
                            const SizedBox(width: 4),
                            Text(
                              ticket.createdAt!,
                              style: AppTypography.caption(),
                            ),
                          ],
                        ),
                      ],
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
