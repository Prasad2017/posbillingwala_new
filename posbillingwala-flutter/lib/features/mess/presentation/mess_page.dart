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
import 'package:pos_billingwala_v2/features/mess/presentation/mess_coupon_page.dart';
import 'package:pos_billingwala_v2/features/mess/presentation/mess_token_qr_page.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';
import 'package:qr_flutter/qr_flutter.dart';
import 'package:share_plus/share_plus.dart';

class MessPage extends ConsumerStatefulWidget {
  const MessPage({super.key});

  @override
  ConsumerState<MessPage> createState() => MessPageState();
}

class MessPageState extends ConsumerState<MessPage> {
  final messPageVerifyController = TextEditingController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(messCommonQrProvider.notifier).load();
      if (AppPlatform.requiresNetwork) {
        ref.read(messControllerProvider.notifier).syncMembers();
      }
    });
  }

  @override
  void dispose() {
    messPageVerifyController.dispose();
    super.dispose();
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

    return Scaffold(
      appBar: AppBar(
        title: Text(AppStrings.of(ref).mess),
        actions: [
          TextButton.icon(
            onPressed: () => messPageAddMember(context),
            icon: const Icon(Icons.person_add_alt_1_rounded, color: Colors.white),
            label: Text(
              AppStrings.of(ref).addMember,
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 8, 12, 4),
            child: Column(
              children: [
                /* All menu cards first (2×2). */
                Row(
                  children: [
                    Expanded(
                      child: MessMenuCard(
                        label: 'Member List',
                        color: AppColors.primary,
                        svgPath: AppAssets.svgPerson,
                        onTap: () => context.push('/mess/members'),
                      ),
                    ),
                    Expanded(
                      child: MessMenuCard(
                        label: 'QR Management',
                        color: AppColors.purple,
                        svgPath: AppAssets.svgQr,
                        onTap: () => context.push('/mess/qr'),
                      ),
                    ),
                  ],
                ),
                Row(
                  children: [
                    Expanded(
                      child: MessMenuCard(
                        label: "Today's Mess Tokens",
                        color: AppColors.orange,
                        svgPath: AppAssets.svgReceipt,
                        onTap: () => context.push('/mess/meal-tokens-today'),
                      ),
                    ),
                    Expanded(
                      child: MessMenuCard(
                        label: 'Meal Sessions',
                        color: AppColors.teal,
                        svgPath: AppAssets.svgClock,
                        onTap: () => context.push('/mess/meal-sessions'),
                      ),
                    ),
                  ],
                ),
                /* Institute pays below cards. */
                SwitchListTile.adaptive(
                  contentPadding: const EdgeInsets.symmetric(horizontal: 4),
                  title: Text(AppStrings.of(ref).institutePays),
                  subtitle: const Text(
                    'When on, mess coupons bill the institute (server setting)',
                  ),
                  value:
                      ref.watch(messInstitutePayProvider).asData?.value ??
                      false,
                  onChanged: (v) async {
                    await ref
                        .read(messControllerProvider.notifier)
                        .setShopPayerMode(v);
                    ref.invalidate(messInstitutePayProvider);
                  },
                ),
              ],
            ),
          ),
          const Divider(height: 1),
          /* Tokens only — Member List / QR Management open from cards. */
          Expanded(
            child: TokensTab(verifyController: messPageVerifyController),
          ),
        ],
      ),
    );
  }

  Future<void> editMember(BuildContext context, MessMember member) async {
    final result = await showMemberFormDialog(
      context,
      title: 'Edit mess member',
      initial: member,
    );
    if (result == null || !context.mounted) return;
    if (result.name.isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(AppStrings.of(ref).nameRequired)));
      return;
    }
    if (messTokenDigits(result.mobile).length != 10) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(AppStrings.of(ref).mobileRequired)),
      );
      return;
    }
    await ref
        .read(messControllerProvider.notifier)
        .updateLocalMember(
          memberId: member.memberId,
          name: result.name,
          mobile: result.mobile,
          altMobile: result.altMobile,
          address: result.address,
          registrationNo: result.registrationNo,
          memberType: result.memberType,
          rollNo: result.rollNo,
          college: result.college,
          studentYear: result.studentYear,
          company: result.company,
        );
    if (!context.mounted) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(AppStrings.of(ref).memberUpdated)));
  }

  Future<void> messPageAddMember(BuildContext context) async {
    final result = await showMemberFormDialog(
      context,
      title: 'Add mess member',
    );
    if (result == null || !context.mounted) return;
    if (result.name.isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(AppStrings.of(ref).nameRequired)));
      return;
    }
    if (messTokenDigits(result.mobile).length != 10) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(AppStrings.of(ref).mobileRequired)),
      );
      return;
    }

    final memberId = await ref
        .read(messControllerProvider.notifier)
        .addLocalMember(
          name: result.name,
          mobile: result.mobile,
          altMobile: result.altMobile,
          address: result.address,
          registrationNo: result.registrationNo,
          memberType: result.memberType,
          rollNo: result.rollNo,
          college: result.college,
          studentYear: result.studentYear,
          company: result.company,
        );
    /* Inventory AddMessMember also collects first payment amounts/days. */
    if (result.messAmount != null || result.messPaidAmount != null) {
      final month = DateFormat('yyyy-MM').format(DateTime.now());
      final network = 'pay_${DateTime.now().millisecondsSinceEpoch}';
      await ref
          .read(appDatabaseProvider)
          .upsertLocalMessPayment(
            memberId: '$memberId',
            memberName: result.name,
            messAmount: result.messAmount ?? 0,
            paidAmount: result.messPaidAmount ?? 0,
            messTotalDays: result.messDays ?? '30',
            paymentDate: month,
            paymentNetworkStatus: network,
          );
    }
    if (!context.mounted) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(AppStrings.of(ref).memberSaved)));
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
  const types = ['student', 'working', 'staff'];
  var memberType = initial?.memberType.trim().isNotEmpty == true
      ? initial!.memberType.trim().toLowerCase()
      : 'student';
  if (!types.contains(memberType)) {
    memberType = 'student';
  }
  var messDays = '30';

  final ok = await showDialog<bool>(
    context: context,
    builder: (context) => StatefulBuilder(
      builder: (context, setLocal) {
        final isStudent = memberType == 'student';
        final isWorking = memberType == 'working' || memberType == 'staff';
        return AlertDialog(
          title: Text(title),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                AppTextField(
                  required: true,
                  controller: nameCtrl,
                  label: 'Member name',
                ),
                const SizedBox(height: 12),
                AppTextField(
                  required: true,
                  controller: mobileCtrl,
                  label: 'Mobile',
                  keyboardType: TextInputType.phone,
                  maxLength: 10,
                  showCounter: false,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                ),
                const SizedBox(height: 12),
                AppTextField(
                  controller: altCtrl,
                  label: 'Alternate mobile',
                  keyboardType: TextInputType.phone,
                ),
                const SizedBox(height: 12),
                AppTextField(
                  controller: addressCtrl,
                  label: 'Address',
                  maxLines: 2,
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
                  AppTextField(controller: rollCtrl, label: 'Roll no'),
                  const SizedBox(height: 12),
                  AppTextField(controller: collegeCtrl, label: 'College'),
                  const SizedBox(height: 12),
                  AppTextField(controller: yearCtrl, label: 'Student year'),
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
                    options: const ['15', '30', '45', '60'],
                    onChanged: (v) => setLocal(() => messDays = v ?? '30'),
                  ),
                  const SizedBox(height: 12),
                  AppTextField(
                    required: true,
                    controller: messAmtCtrl,
                    label: 'Total amount',
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                  ),
                  const SizedBox(height: 12),
                  AppTextField(
                    required: true,
                    controller: paidAmtCtrl,
                    label: 'Paid amount',
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                  ),
                ],
              ],
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

  final result = ok == true
      ? MessMemberFormResult(
          name: nameCtrl.text.trim(),
          mobile: trimOrNull(mobileCtrl),
          altMobile: trimOrNull(altCtrl),
          address: trimOrNull(addressCtrl),
          registrationNo: trimOrNull(regCtrl),
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
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(12),
        elevation: 1,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(12),
          child: SizedBox(
            height: 108,
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

  @override
  Widget build(BuildContext context) {
    final membersAsync = ref.watch(messMembersProvider);
    final query = messPageSearch.text.trim().toLowerCase();

    return membersAsync.when(
      data: (members) {
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
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
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
                      padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                      itemCount: filtered.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 8),
                      itemBuilder: (context, index) {
                        final member = filtered[index];
                        return AppCard(
                          padding: EdgeInsets.zero,
                          child: ListTile(
                            leading: AppModuleIcon(
                              icon: Icons.person_rounded,
                              color:
                                  member.memberType.toLowerCase().contains(
                                    'staff',
                                  )
                                  ? AppColors.purple
                                  : AppColors.teal,
                              size: 48,
                            ),
                            title: Text(
                              member.memberName,
                              style: const TextStyle(
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                            subtitle: Text(
                              [
                                if (member.registrationNo?.isNotEmpty == true)
                                  member.registrationNo!,
                                if (member.memberMobileNumber?.isNotEmpty ==
                                    true)
                                  member.memberMobileNumber!,
                                member.memberType,
                              ].join(' • '),
                            ),
                            trailing: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                IconButton(
                                  tooltip: 'Payments',
                                  onPressed: () => context.push(
                                    '/mess/payments',
                                    extra: member,
                                  ),
                                  icon: const Icon(Icons.payments_outlined),
                                ),
                                IconButton(
                                  tooltip: 'Edit',
                                  onPressed: () => widget.onEditMember(member),
                                  icon: const Icon(Icons.edit_outlined),
                                ),
                                IconButton(
                                  tooltip: 'Paper coupon',
                                  onPressed: () {
                                    Navigator.of(context).push(
                                      MaterialPageRoute<void>(
                                        builder: (_) =>
                                            MessCouponPage(member: member),
                                      ),
                                    );
                                  },
                                  icon: const Icon(
                                    Icons.confirmation_number_outlined,
                                  ),
                                ),
                                IconButton(
                                  tooltip: 'Issue token',
                                  onPressed: () =>
                                      issueToken(context, ref, member),
                                  icon: const Icon(Icons.qr_code_2_rounded),
                                ),
                              ],
                            ),
                            onLongPress: () => widget.onEditMember(member),
                            onTap: () => issueToken(context, ref, member),
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

  Future<void> issueToken(
    BuildContext context,
    WidgetRef ref,
    MessMember member,
  ) async {
    if (!messTokenHasRequiredIdentity(
      member.memberName,
      member.memberMobileNumber,
    )) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(AppStrings.of(ref).tokenPrintNameMobileRequired),
        ),
      );
      return;
    }
    try {
      final result = await ref
          .read(messControllerProvider.notifier)
          .issueMemberToken(member);
      if (!context.mounted) return;
      await Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) => MessTokenQrPage(
            title: 'Member token',
            subtitle: member.memberName,
            memberMobile: member.memberMobileNumber,
            payload: result.payload,
            tokenCode: result.token.tokenCode,
            messType: result.token.messType,
          ),
        ),
      );
    } catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    }
  }
}

class TokensTab extends ConsumerWidget {
  const TokensTab({super.key, required this.verifyController});

  final TextEditingController verifyController;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tokensAsync = ref.watch(todayMessTokensProvider);
    final time = DateFormat('HH:mm');

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
          child: Row(
            children: [
              Expanded(
                child: AppTextField(
                  controller: verifyController,
                  label: 'Scan / paste QR or token code',
                ),
              ),
              const SizedBox(width: 8),
              AppButton(
                label: 'Verify',
                onPressed: () async {
                  final token = await ref
                      .read(messControllerProvider.notifier)
                      .verifyRaw(verifyController.text);
                  if (!context.mounted) return;
                  if (token == null) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text(AppStrings.of(ref).tokenNotFound)),
                    );
                    return;
                  }
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text(
                        'Verified: ${token.memberName ?? token.tokenCode}',
                      ),
                    ),
                  );
                  verifyController.clear();
                },
              ),
              const SizedBox(width: 8),
              IconButton.filledTonal(
                tooltip: 'Camera scan',
                onPressed: () => context.push('/mess/scan'),
                icon: const Icon(Icons.qr_code_scanner_rounded),
              ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Align(
            alignment: Alignment.centerLeft,
            child: AppButton(
              label: 'Issue walk-in token',
              icon: Icons.add_rounded,
              variant: AppButtonVariant.outlined,
              expanded: false,
              onPressed: () async {
                final nameCtrl = TextEditingController();
                final mobileCtrl = TextEditingController();
                final amountCtrl = TextEditingController();
                var messType = 'Lunch';
                String? formError;
                final ok = await showDialog<bool>(
                  context: context,
                  builder: (context) => StatefulBuilder(
                    builder: (context, setLocal) => AlertDialog(
                      title: Text(AppStrings.of(ref).walkInToken),
                      content: SingleChildScrollView(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            const Text(
                              'Issue a one-time QR token for walk-in customer',
                              style: TextStyle(fontSize: 14),
                            ),
                            const SizedBox(height: 16),
                            AppTextField(
                              required: true,
                              controller: nameCtrl,
                              label: 'Customer name',
                            ),
                            const SizedBox(height: 12),
                            AppTextField(
                              required: true,
                              controller: mobileCtrl,
                              label: 'Mobile',
                              keyboardType: TextInputType.phone,
                              maxLength: 10,
                              showCounter: false,
                              inputFormatters: [
                                FilteringTextInputFormatter.digitsOnly,
                              ],
                            ),
                            if (formError != null) ...[
                              const SizedBox(height: 8),
                              Text(
                                formError!,
                                style: TextStyle(
                                  color: Theme.of(context).colorScheme.error,
                                  fontSize: 13,
                                ),
                              ),
                            ],
                            const SizedBox(height: 12),
                            AppTextField(
                              controller: amountCtrl,
                              label: 'Amount collected (optional)',
                              keyboardType:
                                  const TextInputType.numberWithOptions(
                                    decimal: true,
                                  ),
                            ),
                            const SizedBox(height: 12),
                            StringDropdownField(
                              label: 'Meal',
                              value: messType,
                              options: const ['Breakfast', 'Lunch', 'Dinner'],
                              onChanged: (v) {
                                if (v != null) setLocal(() => messType = v);
                              },
                            ),
                          ],
                        ),
                      ),
                      actions: [
                        TextButton(
                          onPressed: () => Navigator.pop(context, false),
                          child: const Text('Cancel'),
                        ),
                        AppButton(
                          label: 'Issue QR token & print',
                          expanded: false,
                          onPressed: () {
                            if (nameCtrl.text.trim().isEmpty) {
                              setLocal(() {
                                formError = AppStrings.of(ref).nameRequired;
                              });
                              return;
                            }
                            if (messTokenDigits(mobileCtrl.text).length != 10) {
                              setLocal(() {
                                formError = AppStrings.of(ref).mobileRequired;
                              });
                              return;
                            }
                            Navigator.pop(context, true);
                          },
                        ),
                      ],
                    ),
                  ),
                );
                if (ok != true || !context.mounted) return;
                final result = await ref
                    .read(messControllerProvider.notifier)
                    .issueWalkInToken(
                      name: nameCtrl.text,
                      mobile: mobileCtrl.text,
                      messType: messType,
                      amount: double.tryParse(amountCtrl.text.trim()) ?? 0,
                    );
                if (!context.mounted) return;
                await Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => MessTokenQrPage(
                      title: 'Walk-in token',
                      subtitle: result.token.memberName ?? nameCtrl.text.trim(),
                      memberMobile:
                          result.token.memberMobile ??
                          messTokenDigits(mobileCtrl.text),
                      payload: result.payload,
                      tokenCode: result.token.tokenCode,
                      messType: messType,
                    ),
                  ),
                );
              },
            ),
          ),
        ),
        const SizedBox(height: 8),
        Expanded(
          child: tokensAsync.when(
            data: (tokens) {
              if (tokens.isEmpty) {
                return Center(child: Text(AppStrings.of(ref).noTokensToday));
              }
              return ListView.separated(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                itemCount: tokens.length,
                separatorBuilder: (_, _) => const SizedBox(height: 8),
                itemBuilder: (context, index) {
                  final token = tokens[index];
                  final verified = token.tokenState == 'verified';
                  return AppCard(
                    padding: EdgeInsets.zero,
                    child: ListTile(
                      leading: Icon(
                        verified
                            ? Icons.verified_rounded
                            : Icons.qr_code_rounded,
                        color: verified ? AppColors.success : AppColors.primary,
                      ),
                      title: Text(
                        token.memberName ?? 'Token',
                        style: const TextStyle(fontWeight: FontWeight.w700),
                      ),
                      subtitle: Text(
                        '${token.messType} Â· ${token.memberType} Â· ${time.format(token.tokenDate)}',
                      ),
                      trailing: Text(
                        verified ? 'Verified' : 'Active',
                        style: TextStyle(
                          color: verified
                              ? AppColors.success
                              : AppColors.warning,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                  );
                },
              );
            },
            loading: () => const Center(child: CircularProgressIndicator()),
            error: (e, _) => Center(child: Text('$e')),
          ),
        ),
      ],
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

  Future<void> shareUrl(BuildContext context, String url) async {
    final box = context.findRenderObject() as RenderBox?;
    await SharePlus.instance.share(
      ShareParams(
        text: url,
        subject: 'Mess Common QR',
        sharePositionOrigin: box == null
            ? null
            : box.localToGlobal(Offset.zero) & box.size,
      ),
    );
  }

  Future<void> printUrl(WidgetRef ref, BuildContext context, String url) async {
    final result = await ref
        .read(printServiceProvider)
        .printRawText('Mess Common QR\n\n$url\n', label: 'Mess QR');
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(result.message ?? result.outcome.name)),
    );
  }

  Future<void> saveToGallery(BuildContext context) async {
    try {
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
                  onPressed: () => controller.generate(),
                ),
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: AppButton(
                      label: 'Share',
                      variant: AppButtonVariant.outlined,
                      onPressed: () => shareUrl(context, qr.qrUrl),
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
