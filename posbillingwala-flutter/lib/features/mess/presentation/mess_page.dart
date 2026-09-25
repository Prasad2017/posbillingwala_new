import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gal/gal.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_providers.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_payment_args.dart';
import 'package:pos_billingwala_v2/features/mess/presentation/mess_hub_pane.dart';
import 'package:pos_billingwala_v2/features/mess/presentation/mess_token_qr_page.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_slip_builder.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_pin_gate.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';
import 'package:qr_flutter/qr_flutter.dart';
import 'package:share_plus/share_plus.dart';

class MessPage extends ConsumerStatefulWidget {
  const MessPage({super.key});

  @override
  ConsumerState<MessPage> createState() => MessPageState();
}

class MessPageState extends ConsumerState<MessPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      final company = await ref.read(appDatabaseProvider).getLocalCompany();
      final name = (company?.companyName ?? '').trim();
      if (name.isEmpty) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Please set shop details before using Mess'),
          ),
        );
        context.push('/settings/company');
        return;
      }
      ref.read(messCommonQrProvider.notifier).load();
      if (AppPlatform.requiresNetwork) {
        ref.read(messControllerProvider.notifier).syncMembers();
      }
      ref.read(messControllerProvider.notifier).recoverPendingMealTokens();
    });
  }

  @override
  Widget build(BuildContext context) {
    ref.listen(messControllerProvider, (prev, next) {
      next.whenOrNull(
        error: (error, _) {
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(SnackBar(content: Text('$error')));
        },
      );
    });

    final institutePay =
        ref.watch(messInstitutePayProvider).asData?.value ?? false;

    return Scaffold(
      appBar: AppBar(
        title: Text(AppStrings.of(ref).mess),
      ),
      body: Column(
        children: [
          Padding(
            padding: EdgeInsets.fromLTRB(
              12,
              context.isShortHeight ? 4 : 8,
              12,
              4,
            ),
            child: LayoutBuilder(
              builder: (context, constraints) {
                final cols = AppBreakpoints.messMenuColumnsForWidth(
                  constraints.maxWidth,
                );
                final menus = <Widget>[
                  MessMenuCard(
                    label: 'Member List',
                    color: AppColors.primary,
                    svgPath: AppAssets.svgPerson,
                    onTap: () => openMemberList(context),
                  ),
                  MessMenuCard(
                    label: 'QR Management',
                    color: AppColors.purple,
                    svgPath: AppAssets.svgQr,
                    onTap: () => context.push('/mess/qr'),
                  ),
                  MessMenuCard(
                    label: "Today's Mess Tokens",
                    color: AppColors.orange,
                    svgPath: AppAssets.svgReceipt,
                    onTap: () => context.push('/mess/meal-tokens-today'),
                  ),
                  MessMenuCard(
                    label: 'Meal Sessions',
                    color: AppColors.teal,
                    svgPath: AppAssets.svgClock,
                    onTap: () => context.push('/mess/meal-sessions'),
                  ),
                ];
                final gap = 8.0;
                final itemW =
                    (constraints.maxWidth - gap * (cols - 1)) / cols;
                return Wrap(
                  spacing: gap,
                  runSpacing: gap,
                  children: [
                    for (final m in menus) SizedBox(width: itemW, child: m),
                  ],
                );
              },
            ),
          ),
          /* Android messPayerModeCard */
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 4, 12, 0),
            child: Material(
              color: Colors.white.withValues(alpha: 0.92),
              borderRadius: BorderRadius.circular(12),
              elevation: 1,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(12, 6, 4, 6),
                child: SwitchListTile.adaptive(
                  contentPadding: EdgeInsets.zero,
                  dense: context.isShortHeight,
                  title: Text(
                    AppStrings.of(ref).institutePays,
                    style: const TextStyle(
                      fontWeight: FontWeight.w600,
                      fontSize: 14,
                    ),
                  ),
                  subtitle: Text(
                    institutePay
                        ? 'Institute Pay: tokens without counter payment'
                        : 'User Pay: members must pay before tokens',
                    style: const TextStyle(fontSize: 11),
                  ),
                  value: institutePay,
                  onChanged: (v) async {
                    await ref
                        .read(messControllerProvider.notifier)
                        .setShopPayerMode(v);
                    ref.invalidate(messInstitutePayProvider);
                    ref.invalidate(messHubStatsProvider);
                    if (!context.mounted) return;
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                        content: Text('Mess payment mode updated'),
                      ),
                    );
                  },
                ),
              ),
            ),
          ),
          const MessPaymentAlertBanner(),
          const SizedBox(height: 4),
          const Expanded(child: MessHubMembersPane()),
        ],
      ),
    );
  }

  Future<void> openMemberList(BuildContext context) async {
    final ok = await showReportPinGate(
      context,
      ref,
      title: 'Member List Password',
      message: 'Enter PIN to open the mess member list.',
    );
    if (!ok || !context.mounted) return;
    context.push('/mess/members');
  }

}

class MessMemberFormResult {
  const MessMemberFormResult({
    required this.name,
    this.mobile,
    this.altMobile,
    this.address,
    this.registrationNo,
    required this.memberType,
    this.rollNo,
    this.college,
    this.studentYear,
    this.company,
    this.messAmount,
    this.messPaidAmount,
    this.messDays,
  });

  final String name;
  final String? mobile;
  final String? altMobile;
  final String? address;
  final String? registrationNo;
  final String memberType;
  final String? rollNo;
  final String? college;
  final String? studentYear;
  final String? company;
  final double? messAmount;
  final double? messPaidAmount;
  final String? messDays;
}

Future<MessMemberFormResult?> showMemberFormDialog(
  BuildContext context, {
  required String title,
  MessMember? initial,
}) async {
  final isAdd = initial == null;
  final nameCtrl = TextEditingController(text: initial?.memberName ?? '');
  final mobileCtrl = TextEditingController(
    text: initial?.memberMobileNumber ?? '',
  );
  final altCtrl = TextEditingController(
    text: initial?.memberAltenetMobileNumber ?? '',
  );
  final addressCtrl = TextEditingController(text: initial?.memberAddress ?? '');
  final regCtrl = TextEditingController(text: initial?.registrationNo ?? '');
  final rollCtrl = TextEditingController(text: initial?.rollNo ?? '');
  final collegeCtrl = TextEditingController(text: initial?.college ?? '');
  final yearCtrl = TextEditingController(text: initial?.studentYear ?? '');
  final companyCtrl = TextEditingController(text: initial?.company ?? '');
  final messAmtCtrl = TextEditingController();
  final paidAmtCtrl = TextEditingController();
  const types = ['student', 'working'];
  var memberType = initial?.memberType.trim().isNotEmpty == true
      ? initial!.memberType.trim().toLowerCase()
      : 'student';
  if (!types.contains(memberType)) {
    memberType = 'student';
  }
  var messDays = 'Two Time';

  final ok = await showDialog<bool>(
    context: context,
    builder: (context) => StatefulBuilder(
      builder: (context, setLocal) {
        final isStudent = memberType == 'student';
        final isWorking = memberType == 'working';
        final screenW = MediaQuery.sizeOf(context).width;
        return AlertDialog(
          insetPadding: EdgeInsets.symmetric(
            horizontal: screenW < 360 ? 12 : 24,
            vertical: 24,
          ),
          title: Text(title),
          content: ConstrainedBox(
            constraints: BoxConstraints(maxWidth: screenW - 48),
            child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                ResponsiveFormColumns(
                  children: [
                    AppTextField(
                      required: true,
                      controller: nameCtrl,
                      label: 'Member name',
                    ),
                    AppTextField(
                      required: true,
                      controller: mobileCtrl,
                      label: 'Mobile',
                      keyboardType: TextInputType.phone,
                      maxLength: 10,
                      showCounter: false,
                      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                ResponsiveFormColumns(
                  children: [
                    AppTextField(
                      controller: altCtrl,
                      label: 'Alternate mobile',
                      keyboardType: TextInputType.phone,
                      maxLength: 10,
                      showCounter: false,
                      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                    ),
                    AppTextField(
                      controller: addressCtrl,
                      label: 'Address',
                      maxLines: 2,
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                AppDropdownFormField<String>(
                  required: true,
                  label: 'Member Type',
                  items: types,
                  itemLabel: (t) => t[0].toUpperCase() + t.substring(1),
                  value: memberType,
                  onChanged: (v) => setLocal(() => memberType = v ?? 'student'),
                ),
                if (isStudent) ...[
                  const SizedBox(height: 12),
                  ResponsiveFormColumns(
                    children: [
                      AppTextField(controller: rollCtrl, label: 'Roll no'),
                      AppTextField(controller: collegeCtrl, label: 'College'),
                      AppTextField(controller: yearCtrl, label: 'Student year'),
                    ],
                  ),
                ],
                if (isWorking) ...[
                  const SizedBox(height: 12),
                  AppTextField(controller: companyCtrl, label: 'Company'),
                ],
                if (isAdd) ...[
                  const SizedBox(height: 12),
                  StringDropdownField(
                    required: true,
                    label: 'Mess Days',
                    value: messDays,
                    options: const ['One Time', 'Two Time'],
                    onChanged: (v) => setLocal(() => messDays = v ?? 'Two Time'),
                  ),
                  const SizedBox(height: 12),
                  ResponsiveFormColumns(
                    children: [
                      AppTextField(
                        required: true,
                        controller: messAmtCtrl,
                        label: 'Mess Amount',
                        keyboardType: const TextInputType.numberWithOptions(
                          decimal: true,
                        ),
                      ),
                      AppTextField(
                        required: true,
                        controller: paidAmtCtrl,
                        label: 'Paid Amount',
                        keyboardType: const TextInputType.numberWithOptions(
                          decimal: true,
                        ),
                      ),
                    ],
                  ),
                ],
              ],
            ),
          ),
          ),
          actions: [
            AppButton(
              label: title.startsWith('Add') ? 'Add Member' : 'Update Member',
              onPressed: () {
                if (nameCtrl.text.trim().isEmpty) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Name is required')),
                  );
                  return;
                }
                if (messTokenDigits(mobileCtrl.text).length != 10) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Mobile number is required')),
                  );
                  return;
                }
                final alt = messTokenDigits(altCtrl.text);
                if (alt.isNotEmpty && alt.length != 10) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Alternate mobile must be 10 digits'),
                    ),
                  );
                  return;
                }
                if (isAdd) {
                  final mess = double.tryParse(messAmtCtrl.text.trim()) ?? 0;
                  final paid = double.tryParse(paidAmtCtrl.text.trim()) ?? 0;
                  if (mess <= 0) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Mess amount is required')),
                    );
                    return;
                  }
                  if (paid > mess) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                        content: Text('Paid amount cannot exceed mess amount'),
                      ),
                    );
                    return;
                  }
                }
                Navigator.pop(context, true);
              },
            ),
          ],
        );
      },
    ),
  );

  String? trimOrNull(TextEditingController c) {
    final v = c.text.trim();
    return v.isEmpty ? null : v;
  }

  /* Android: registration = roll (student) or mobile on add/update. */
  String? derivedRegistration() {
    if (memberType == 'student') {
      final roll = trimOrNull(rollCtrl);
      if (roll != null) return roll;
    }
    return trimOrNull(mobileCtrl) ?? trimOrNull(regCtrl);
  }

  final result = ok == true
      ? MessMemberFormResult(
          name: nameCtrl.text.trim(),
          mobile: trimOrNull(mobileCtrl),
          altMobile: trimOrNull(altCtrl),
          address: trimOrNull(addressCtrl),
          registrationNo: derivedRegistration(),
          memberType: memberType,
          rollNo: trimOrNull(rollCtrl),
          college: trimOrNull(collegeCtrl),
          studentYear: trimOrNull(yearCtrl),
          company: trimOrNull(companyCtrl),
          messAmount: isAdd
              ? double.tryParse(messAmtCtrl.text.trim()) ?? 0
              : null,
          messPaidAmount: isAdd
              ? double.tryParse(paidAmtCtrl.text.trim()) ?? 0
              : null,
          messDays: isAdd ? messDays : null,
        )
      : null;

  nameCtrl.dispose();
  mobileCtrl.dispose();
  altCtrl.dispose();
  addressCtrl.dispose();
  regCtrl.dispose();
  rollCtrl.dispose();
  collegeCtrl.dispose();
  yearCtrl.dispose();
  companyCtrl.dispose();
  messAmtCtrl.dispose();
  paidAmtCtrl.dispose();
  return result;
}

class MessMenuCard extends StatelessWidget {
  const MessMenuCard({
    super.key,
    required this.label,
    required this.color,
    required this.svgPath,
    required this.onTap,
  });

  final String label;
  final Color color;
  final String svgPath;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(4),
      child: Material(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        elevation: 0,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(12),
          child: Container(
            height: 96,
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(
                color: color.withValues(alpha: 0.55),
                width: 1.6,
              ),
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                AppSvg(svgPath, width: 30, height: 30, color: color),
                const SizedBox(height: 8),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 6),
                  child: Text(
                    label,
                    textAlign: TextAlign.center,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontWeight: FontWeight.w800,
                      fontSize: 13,
                      height: 1.2,
                      color: AppColors.navy,
                    ),
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

class MembersTab extends ConsumerStatefulWidget {
  const MembersTab({super.key, required this.onEditMember});

  final void Function(MessMember member) onEditMember;

  @override
  ConsumerState<MembersTab> createState() => MembersTabState();
}

class MembersTabState extends ConsumerState<MembersTab> {
  final messPageSearch = TextEditingController();

  @override
  void dispose() {
    messPageSearch.dispose();
    super.dispose();
  }

  Future<void> deleteMember(MessMember member) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete member'),
        content: Text('Delete ${member.memberName}?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('No'),
          ),
          AppButton(
            label: 'Delete',
            onPressed: () => Navigator.pop(context, true),
          ),
        ],
      ),
    );
    if (confirm != true || !mounted) return;
    try {
      await ref
          .read(messControllerProvider.notifier)
          .deleteLocalMember(member.memberId);
      ref.invalidate(messHubStatsProvider);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Member deleted')),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    }
  }

  @override
  Widget build(BuildContext context) {
    final membersAsync = ref.watch(messMembersProvider);
    final statsAsync = ref.watch(messHubStatsProvider);
    final query = messPageSearch.text.trim().toLowerCase();
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');

    return membersAsync.when(
      data: (members) {
        final statsById = <int, MessHubMemberStats>{};
        for (final s in statsAsync.asData?.value ?? const <MessHubMemberStats>[]) {
          statsById[s.member.memberId] = s;
        }
        final filtered = query.isEmpty
            ? members
            : members.where((m) {
                final hay = [
                  m.memberName,
                  m.memberMobileNumber ?? '',
                  m.registrationNo ?? '',
                  m.memberType,
                ].join(' ').toLowerCase();
                return hay.contains(query);
              }).toList();

        return Column(
          children: [
            Padding(
              padding: EdgeInsets.fromLTRB(
                AppBreakpoints.pagePaddingFor(context.widthClass),
                12,
                AppBreakpoints.pagePaddingFor(context.widthClass),
                8,
              ),
              child: AppTextField(
                controller: messPageSearch,
                label: 'Search member',
                onChanged: (_) => setState(() {}),
              ),
            ),
            Expanded(
              child: filtered.isEmpty
                  ? Center(
                      child: Text(
                        members.isEmpty
                            ? 'No mess members yet'
                            : 'No members match search',
                      ),
                    )
                  : ListView.separated(
                      padding: EdgeInsets.fromLTRB(
                        AppBreakpoints.pagePaddingFor(context.widthClass),
                        0,
                        AppBreakpoints.pagePaddingFor(context.widthClass),
                        24,
                      ),
                      itemCount: filtered.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 8),
                      itemBuilder: (context, index) {
                        final member = filtered[index];
                        final stats = statsById[member.memberId];
                        final messAmt = stats?.messAmount ?? 0;
                        final paidAmt = stats?.paidAmount ?? 0;
                        final pending = stats?.pending ?? 0;
                        final monthTokens = stats?.monthTokens ?? 0;
                        final hasPending = pending > 0.009;
                        return AppCard(
                          padding: const EdgeInsets.fromLTRB(14, 14, 14, 12),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              Text(
                                member.memberName,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(
                                  fontWeight: FontWeight.w700,
                                  fontSize: 15,
                                ),
                              ),
                              if (member.memberMobileNumber?.isNotEmpty == true)
                                Padding(
                                  padding: const EdgeInsets.only(top: 2),
                                  child: Text(
                                    member.memberMobileNumber!,
                                    style: const TextStyle(
                                      color: AppColors.primary,
                                      fontSize: 12,
                                    ),
                                  ),
                                ),
                              if (member.memberAddress?.isNotEmpty == true)
                                Padding(
                                  padding: const EdgeInsets.only(top: 2),
                                  child: Text(
                                    member.memberAddress!,
                                    maxLines: 2,
                                    overflow: TextOverflow.ellipsis,
                                    style: TextStyle(
                                      color: Colors.grey.shade700,
                                      fontSize: 11,
                                    ),
                                  ),
                                ),
                              const SizedBox(height: 10),
                              Container(
                                padding: const EdgeInsets.all(10),
                                decoration: BoxDecoration(
                                  color: hasPending
                                      ? const Color(0xFFFFF7ED)
                                      : AppColors.primary.withValues(
                                          alpha: 0.06,
                                        ),
                                  borderRadius: BorderRadius.circular(10),
                                ),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      'This Month Mess: ${currency.format(messAmt)}',
                                      style: const TextStyle(fontSize: 12),
                                    ),
                                    const SizedBox(height: 2),
                                    Text(
                                      'This Month Paid: ${currency.format(paidAmt)}',
                                      style: const TextStyle(fontSize: 12),
                                    ),
                                    if (hasPending) ...[
                                      const SizedBox(height: 2),
                                      Text(
                                        'Pending Amount: ${currency.format(pending)}',
                                        style: const TextStyle(
                                          fontSize: 12,
                                          fontWeight: FontWeight.w600,
                                          color: Color(0xFFD97706),
                                        ),
                                      ),
                                    ],
                                    const SizedBox(height: 2),
                                    Text(
                                      'Tokens This Month: $monthTokens',
                                      style: TextStyle(
                                        fontSize: 12,
                                        color: Colors.grey.shade700,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              const SizedBox(height: 12),
                              Wrap(
                                spacing: 6,
                                runSpacing: 6,
                                alignment: WrapAlignment.end,
                                children: [
                                  _MemberActionChip(
                                    label: 'History',
                                    color: const Color(0xFFD97706),
                                    onTap: () => context.push(
                                      '/mess/payments',
                                      extra: MessPaymentsArgs(
                                        member: member,
                                        mode: MessPaymentOpenMode.history,
                                      ),
                                    ),
                                  ),
                                  _MemberActionChip(
                                    label: 'Update',
                                    color: const Color(0xFF15803D),
                                    onTap: () => widget.onEditMember(member),
                                  ),
                                  _MemberActionChip(
                                    label: 'New Payment',
                                    color: AppColors.primary,
                                    onTap: () => context.push(
                                      '/mess/payments',
                                      extra: MessPaymentsArgs(
                                        member: member,
                                        mode: MessPaymentOpenMode.newPayment,
                                      ),
                                    ),
                                  ),
                                  if (hasPending)
                                    _MemberActionChip(
                                      label: 'Pay Pending',
                                      color: const Color(0xFFD97706),
                                      onTap: () => context.push(
                                        '/mess/payments',
                                        extra: MessPaymentsArgs(
                                          member: member,
                                          mode: MessPaymentOpenMode.payPending,
                                        ),
                                      ),
                                    ),
                                  _MemberActionChip(
                                    label: 'Delete',
                                    color: const Color(0xFF991B1B),
                                    onTap: () => deleteMember(member),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        );
                      },
                    ),
            ),
          ],
        );
      },
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => Center(child: Text('$e')),
    );
  }
}

class _MemberActionChip extends StatelessWidget {
  const _MemberActionChip({
    required this.label,
    required this.color,
    required this.onTap,
  });

  final String label;
  final Color color;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: color,
      borderRadius: BorderRadius.circular(8),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(8),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
          child: Text(
            label,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 11,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      ),
    );
  }
}

class CommonQrTab extends ConsumerStatefulWidget {
  const CommonQrTab({super.key});

  @override
  ConsumerState<CommonQrTab> createState() => CommonQrTabState();
}

class CommonQrTabState extends ConsumerState<CommonQrTab> {
  final GlobalKey qrKey = GlobalKey();

  Future<void> shareUrl(String url) async {
    try {
      final bytes = await captureQrPng();
      if (!mounted) return;
      final box = context.findRenderObject() as RenderBox?;
      await SharePlus.instance.share(
        ShareParams(
          files: [
            XFile.fromData(
              bytes,
              mimeType: 'image/png',
              name: 'mess_qr.png',
            ),
          ],
          subject: 'Mess Common QR',
          sharePositionOrigin: box == null
              ? null
              : box.localToGlobal(Offset.zero) & box.size,
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Share failed: $e')));
    }
  }

  Future<void> printUrl(WidgetRef ref, BuildContext context, String url) async {
    final width = ref.read(printerSettingsProvider).charsPerLine;
    final profile = ref.read(shopReceiptProfileProvider);
    final layout = MessSlipBuilder.commonQrLayout(
      profile: profile,
      messTitle: 'MESS QR',
      qrPayload: url,
    );
    final result = await ref.read(printServiceProvider).printMessSlip(
      layout.toPlainText(width: width),
      layout: layout,
      qrPayload: url,
      label: 'Mess QR',
      includeLogo: true,
    );
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(result.message ?? result.outcome.name)),
    );
  }

  Future<Uint8List> captureQrPng() async {
    final boundary =
        qrKey.currentContext?.findRenderObject() as RenderRepaintBoundary?;
    if (boundary == null) {
      throw StateError('QR not ready');
    }
    final image = await boundary.toImage(pixelRatio: 3);
    final byteData = await image.toByteData(format: ui.ImageByteFormat.png);
    final bytes = byteData?.buffer.asUint8List();
    if (bytes == null) {
      throw StateError('Could not capture QR image');
    }
    return bytes;
  }

  Future<void> saveToGallery(BuildContext context) async {
    try {
      final bytes = await captureQrPng();
      final granted = await Gal.requestAccess(toAlbum: true);
      if (!granted) {
        throw StateError('Gallery permission denied');
      }
      await Gal.putImageBytes(
        bytes,
        name: 'mess_qr_${DateTime.now().millisecondsSinceEpoch}',
      );
      if (!context.mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(AppStrings.of(ref).qrSaved)));
    } catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Save failed: $e')));
    }
  }

  @override
  Widget build(BuildContext context) {
    final qrAsync = ref.watch(messCommonQrProvider);
    final controller = ref.read(messCommonQrProvider.notifier);

    return qrAsync.when(
      data: (qr) {
        if (qr == null || qr.qrUrl.isEmpty) {
          return Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Container(
                    width: 88,
                    height: 88,
                    decoration: BoxDecoration(
                      color: AppColors.primary.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: const Icon(
                      Icons.qr_code_2_rounded,
                      size: 48,
                      color: AppColors.primary,
                    ),
                  ),
                  const SizedBox(height: 20),
                  const Text(
                    'Mess Token QR',
                    style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.w800,
                      color: AppColors.navy,
                    ),
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    'Generate a shared QR code for mess check-in.',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      color: AppColors.textSecondary,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  const SizedBox(height: 24),
                  SizedBox(
                    width: double.infinity,
                    height: 52,
                    child: AppButton(
                      label: 'Generate QR Code',
                      icon: Icons.qr_code_2_rounded,
                      onPressed: () => controller.generate(),
                    ),
                  ),
                ],
              ),
            ),
          );
        }

        final active = qr.status.toUpperCase() == 'ACTIVE';
        final meta = [
          if (qr.messLabel?.isNotEmpty == true) qr.messLabel!,
          if (qr.branchLabel?.isNotEmpty == true) qr.branchLabel!,
        ].join(' · ');

        return ResponsiveScrollShell(
          dashboard: true,
          child: ListView(
            padding: EdgeInsets.all(
              AppBreakpoints.pagePaddingFor(context.widthClass) + 8,
            ),
            children: [
              const Text(
                'Mess Token QR',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
              ),
              const SizedBox(height: 16),
              Center(
                child: RepaintBoundary(
                  key: qrKey,
                  child: QrImageView(
                    data: qr.qrUrl,
                    size: 240,
                    backgroundColor: Colors.white,
                  ),
                ),
              ),
              const SizedBox(height: 12),
              Text(
                qr.status,
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontWeight: FontWeight.w600,
                  color: active ? AppColors.success : AppColors.danger,
                ),
              ),
              if (meta.isNotEmpty) ...[
                const SizedBox(height: 4),
                Text(meta, textAlign: TextAlign.center),
              ],
              const SizedBox(height: 20),
              SizedBox(
                height: 52,
                child: AppButton(
                  label: 'Generate New QR',
                  icon: Icons.qr_code_2_rounded,
                  onPressed: () => controller.regenerate(),
                ),
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: AppButton(
                      label: 'Share',
                      variant: AppButtonVariant.outlined,
                      onPressed: () => shareUrl(qr.qrUrl),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: AppButton(
                      label: 'Download',
                      variant: AppButtonVariant.outlined,
                      onPressed: () => saveToGallery(context),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              AppButton(
                label: 'Print QR',
                variant: AppButtonVariant.outlined,
                onPressed: () => printUrl(ref, context, qr.qrUrl),
              ),
              if (active) ...[
                const SizedBox(height: 8),
                AppButton(
                  label: 'Deactivate',
                  variant: AppButtonVariant.outlined,
                  onPressed: () => controller.setStatus('INACTIVE'),
                ),
              ],
            ],
          ),
        );
      },
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text('$e', textAlign: TextAlign.center),
              const SizedBox(height: 12),
              AppButton(
                label: 'Retry',
                onPressed: () => controller.load(),
                expanded: false,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
