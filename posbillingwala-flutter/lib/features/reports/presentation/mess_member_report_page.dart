import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_providers.dart';
import 'package:pos_billingwala_v2/features/mess/presentation/mess_payments_page.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_widgets.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';

/// Mess member list → open payment history (refreshed reports UI).
class MessMemberReportPage extends ConsumerStatefulWidget {
  const MessMemberReportPage({super.key});

  @override
  ConsumerState<MessMemberReportPage> createState() =>
      MessMemberReportPageState();
}

class MessMemberReportPageState extends ConsumerState<MessMemberReportPage> {
  final search = TextEditingController();
  String query = '';

  @override
  void dispose() {
    search.dispose();
    super.dispose();
  }

  List<MessMember> messMemberReportPageFiltered(List<MessMember> members) {
    final q = query.trim().toLowerCase();
    if (q.isEmpty) return members;
    return members.where((m) {
      final hay = [
        m.memberName,
        m.memberMobileNumber ?? '',
        m.memberType,
        m.registrationNo ?? '',
      ].join(' ').toLowerCase();
      return hay.contains(q);
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final membersAsync = ref.watch(messMembersProvider);

    return Scaffold(
      backgroundColor: reportPageBg,
      appBar: AppBar(
        title: Text(AppStrings.of(ref).invoiceMemberReport),
        actions: [
          IconButton(
            tooltip: 'Sync members',
            onPressed: () =>
                ref.read(messControllerProvider.notifier).syncMembers(),
            icon: const Icon(Icons.refresh_rounded),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/mess/payments'),
        icon: const Icon(Icons.payments_rounded),
        label: Text(AppStrings.of(ref).allPayments),
      ),
      body: membersAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
        data: (members) {
          final filtered = messMemberReportPageFiltered(members);
          final types = <String>{};
          for (final m in members) {
            if (m.memberType.trim().isNotEmpty) types.add(m.memberType.trim());
          }

          return ResponsiveScrollShell(
        dashboard: true,
        child: ListView(
            padding: EdgeInsets.fromLTRB(
            AppBreakpoints.pagePaddingFor(context.widthClass),
            12,
            AppBreakpoints.pagePaddingFor(context.widthClass),
            88),
            children: [
              ReportSurfaceCard(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                child: TextField(
                  controller: search,
                  onChanged: (v) => setState(() => query = v),
                  decoration: InputDecoration(
                    hintText: 'Search member, mobile, type…',
                    border: InputBorder.none,
                    prefixIcon: Icon(
                      Icons.search_rounded,
                      color: AppColors.navy.withValues(alpha: .45),
                    ),
                    suffixIcon: query.isEmpty
                        ? null
                        : IconButton(
                            onPressed: () {
                              search.clear();
                              setState(() => query = '');
                            },
                            icon: const Icon(Icons.close_rounded),
                          ),
                  ),
                ),
              ),
              const SizedBox(height: 14),
              ReportKpiGrid(
                items: [
                  ReportKpiData(
                    label: 'Members',
                    value: '${members.length}',
                  ),
                  ReportKpiData(
                    label: 'Showing',
                    value: '${filtered.length}',
                  ),
                  ReportKpiData(
                    label: 'Member Types',
                    value: '${types.length}',
                  ),
                  ReportKpiData(
                    label: 'With Mobile',
                    value:
                        '${members.where((m) => (m.memberMobileNumber ?? '').trim().isNotEmpty).length}',
                  ),
                ],
              ),
              const SizedBox(height: 14),
              if (filtered.isEmpty)
                ReportSurfaceCard(
                  padding: const EdgeInsets.all(28),
                  child: Center(child: Text(AppStrings.of(ref).noMessMembers)),
                )
              else
                ReportSurfaceCard(
                  child: Column(
                    children: [
                      for (var i = 0; i < filtered.length; i++) ...[
                        if (i > 0)
                          Divider(
                            height: 1,
                            thickness: 1,
                            color: AppColors.border.withValues(alpha: .7),
                            indent: 72,
                            endIndent: 16,
                          ),
                        MemberRow(
                          member: filtered[i],
                          color: i.isEven ? AppColors.teal : AppColors.purple,
                          onTap: () {
                            Navigator.of(context).push(
                              MaterialPageRoute<void>(
                                builder: (_) =>
                                    MessPaymentsPage(member: filtered[i]),
                              ),
                            );
                          },
                        ),
                      ],
                    ],
                  ),
                ),
            ],
          ),
      );
        },
      ),
    );
  }
}

class MemberRow extends StatelessWidget {
  const MemberRow({super.key, 
    required this.member,
    required this.color,
    required this.onTap,
  });

  final MessMember member;
  final Color color;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final subtitle = [
      if ((member.memberMobileNumber ?? '').trim().isNotEmpty)
        member.memberMobileNumber!,
      if (member.memberType.trim().isNotEmpty) member.memberType,
      if ((member.registrationNo ?? '').trim().isNotEmpty)
        member.registrationNo!,
    ].join(' · ');

    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 14, 12, 14),
          child: Row(
            children: [
              Container(
                width: 44,
                height: 44,
                decoration: BoxDecoration(
                  color: color.withValues(alpha: .12),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(Icons.person_rounded, color: color, size: 22),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      member.memberName,
                      style: const TextStyle(
                        fontFamily: AppFonts.family,
                        color: AppColors.navy,
                        fontWeight: FontWeight.w700,
                        fontSize: 15,
                        height: 1.2,
                      ),
                    ),
                    if (subtitle.isNotEmpty) ...[
                      const SizedBox(height: 3),
                      Text(
                        subtitle,
                        style: TextStyle(
                          fontFamily: AppFonts.family,
                          color: AppColors.navy.withValues(alpha: .48),
                          fontWeight: FontWeight.w400,
                          fontSize: 12.5,
                          height: 1.3,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Icon(
                Icons.chevron_right_rounded,
                color: AppColors.navy.withValues(alpha: .28),
                size: 26,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
