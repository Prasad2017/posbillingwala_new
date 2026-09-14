import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:drift/drift.dart' show Value;
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/mess/data/mess_api.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_dtos.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_providers.dart';
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';

class MessPaymentsPage extends ConsumerStatefulWidget {
  const MessPaymentsPage({super.key, this.member});

  final MessMember? member;

  @override
  ConsumerState<MessPaymentsPage> createState() => MessPaymentsPageState();
}

class MessPaymentsPageState extends ConsumerState<MessPaymentsPage> {
  bool busy = false;
  bool cloudLoaded = false;

  @override
  void initState() {
    super.initState();
    Future.microtask(refreshFromCloud);
  }

  String? get memberIdFilter =>
      widget.member == null ? null : '${widget.member!.memberId}';

  Future<void> refreshFromCloud() async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) return;
    if (!await isDeviceOnline()) {
      if (mounted) setState(() => cloudLoaded = true);
      return;
    }
    try {
      final cloud = await MessApi(ref.read(apiClientProvider))
          .fetchMemberPayments(userId);
      final db = ref.read(appDatabaseProvider);
      if (cloud.isNotEmpty) {
        final pending = await db.getPendingMessPayments();
        await db.replaceMessMemberPayments([
          ...cloud
              .where(
                (e) =>
                    e.memberId.trim().isNotEmpty &&
                    (e.paymentNetworkStatus?.trim().isNotEmpty == true ||
                        e.paymentId > 0),
              )
              .map(
                (e) => MessMemberPaymentsCompanion.insert(
                  memberId: e.memberId,
                  memberName: Value(e.memberName),
                  paymentMessAmount: Value(e.paymentMessAmount),
                  paymentPaidAmount: Value(e.paymentPaidAmount),
                  messTotalDays: Value(e.messTotalDays),
                  paymentDate: e.paymentDate.isNotEmpty
                      ? e.paymentDate
                      : DateFormat('yyyy-MM').format(DateTime.now()),
                  paymentNetworkStatus:
                      e.paymentNetworkStatus?.trim().isNotEmpty == true
                          ? e.paymentNetworkStatus!.trim()
                          : 'pay_${e.paymentId}',
                  paymentStatus: Value(
                    e.paymentStatus.isEmpty ? '1' : e.paymentStatus,
                  ),
                  paymentSyncStatus: const Value('1'),
                ),
              ),
          ...pending.map(
            (e) => MessMemberPaymentsCompanion.insert(
              memberId: e.memberId,
              memberName: Value(e.memberName),
              paymentMessAmount: Value(e.paymentMessAmount),
              paymentPaidAmount: Value(e.paymentPaidAmount),
              messTotalDays: Value(e.messTotalDays),
              paymentDate: e.paymentDate,
              paymentNetworkStatus: e.paymentNetworkStatus,
              paymentStatus: Value(e.paymentStatus),
              paymentSyncStatus: const Value('0'),
            ),
          ),
        ]);
      }
    } catch (_) {
      // Keep local Drift rows.
    }
    if (mounted) setState(() => cloudLoaded = true);
  }

  MessMemberPaymentDto toDto(MessMemberPayment e) {
    return MessMemberPaymentDto(
      paymentId: e.localPaymentId,
      memberId: e.memberId,
      memberName: e.memberName,
      paymentMessAmount: e.paymentMessAmount,
      paymentPaidAmount: e.paymentPaidAmount,
      messTotalDays: e.messTotalDays,
      paymentDate: e.paymentDate,
      paymentNetworkStatus: e.paymentNetworkStatus,
      paymentStatus: e.paymentSyncStatus == '1' ? '1' : '0',
    );
  }

  Future<void> messPaymentsPageAddPayment() async {
    final members = ref.read(messMembersProvider).maybeWhen(
          data: (v) => v,
          orElse: () => const <MessMember>[],
        );
    MessMember? selected = widget.member ??
        (members.isNotEmpty ? members.first : null);
    if (selected == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(AppStrings.of(ref).addAMemberFirst)),
      );
      return;
    }

    final messAmt = TextEditingController();
    final paidAmt = TextEditingController();
    var days = '30';
    final month = DateFormat('yyyy-MM').format(DateTime.now());
    final nameCtrl = TextEditingController(text: selected.memberName);
    final mobileCtrl =
        TextEditingController(text: selected.memberMobileNumber ?? '');

    final ok = await showDialog<bool>(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setLocal) => AlertDialog(
        title: Text(AppStrings.of(ref).addPayment),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (widget.member == null)
                  AppDropdownFormField<MessMember>(
                    label: 'Member',
                    items: members,
                    itemLabel: (m) => m.memberName,
                    value: selected,
                    onChanged: (m) => setLocal(() {
                      selected = m;
                      nameCtrl.text = m?.memberName ?? '';
                      mobileCtrl.text = m?.memberMobileNumber ?? '';
                    }),
                  ),
                AppTextField(
                  controller: nameCtrl,
                  label: 'Member name',
                  enabled: widget.member == null,
                ),
                const SizedBox(height: 12),
                AppTextField(
                  controller: mobileCtrl,
                  label: 'Mobile',
                  enabled: widget.member == null,
                  keyboardType: TextInputType.phone,
                ),
                const SizedBox(height: 12),
                StringDropdownField(
                  label: 'Mess days',
                  value: days,
                  options: const ['15', '30', '45', '60'],
                  onChanged: (v) => setLocal(() => days = v ?? '30'),
                ),
                const SizedBox(height: 12),
                AppTextField(
                      controller: messAmt,
                      label: 'Total amount',
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      inputFormatters: [
                    FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                  ],
                    ),
                const SizedBox(height: 12),
                AppTextField(
                      controller: paidAmt,
                      label: 'Paid amount',
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      inputFormatters: [
                    FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                  ],
                    ),
              ],
            ),
          ),
          actions: [
            AppButton(
            label: 'Add Payment',
            onPressed: () => Navigator.pop(context, true),
          ),
          ],
        ),
      ),
    );

    if (ok != true || selected == null || !mounted) return;
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null) return;

    if (AppPlatform.requiresNetwork && !await ensureOnline()) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text(kOnlineRequiredMessage)),
      );
      return;
    }

    setState(() => busy = true);
    try {
      final network = 'pay_${DateTime.now().millisecondsSinceEpoch}';
      final messAmount = double.tryParse(messAmt.text.trim()) ?? 0;
      final paidAmount = double.tryParse(paidAmt.text.trim()) ?? 0;
      final db = ref.read(appDatabaseProvider);
      if (await db.hasMessPaymentForMonth(
        memberId: '${selected!.memberId}',
        paymentDate: month,
      )) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(AppStrings.of(ref).alreadyPaidMonth)),
        );
        return;
      }

      var success = false;
      if (await isDeviceOnline()) {
        try {
          success =
              await MessApi(ref.read(apiClientProvider)).insertMemberPayment(
            userId: userId,
            memberId: '${selected!.memberId}',
            memberName: selected!.memberName,
            paymentMessAmount: messAmt.text.trim(),
            paymentPaidAmount: paidAmt.text.trim(),
            messTotalDays: days,
            paymentDate: month,
            paymentNetworkStatus: network,
          );
        } catch (_) {
          success = false;
        }
      }

      await db.upsertLocalMessPayment(
        memberId: '${selected!.memberId}',
        memberName: selected!.memberName,
        messAmount: messAmount,
        paidAmount: paidAmount,
        messTotalDays: days,
        paymentDate: month,
        paymentNetworkStatus: network,
      );
      if (success) {
        final pending = await db.getPendingMessPayments();
        for (final row in pending) {
          if (row.paymentNetworkStatus == network) {
            await db.markMessPaymentSynced(row.localPaymentId);
          }
        }
      }
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            success
                ? 'Payment saved'
                : (AppPlatform.supportsOfflineBilling
                    ? 'Saved offline — will sync when online'
                    : 'Could not save payment — check internet and retry'),
          ),
        ),
      );
    } finally {
      if (mounted) setState(() => busy = false);
    }
    messAmt.dispose();
    paidAmt.dispose();
    nameCtrl.dispose();
    mobileCtrl.dispose();
  }

  Future<void> editPayment(MessMemberPaymentDto payment) async {
    final messAmt = TextEditingController(
      text: payment.paymentMessAmount.toStringAsFixed(2),
    );
    final paidAmt = TextEditingController(
      text: payment.paymentPaidAmount.toStringAsFixed(2),
    );
    final pendingCtrl = TextEditingController(
      text: (payment.paymentMessAmount - payment.paymentPaidAmount)
          .clamp(0, double.infinity)
          .toStringAsFixed(2),
    );
    var days = payment.messTotalDays.trim().isEmpty
        ? '30'
        : payment.messTotalDays.trim();
    if (!const ['15', '30', '45', '60'].contains(days)) {
      days = '30';
    }

    void syncPending(void Function(void Function()) setLocal) {
      final mess = double.tryParse(messAmt.text.trim()) ?? 0;
      final paid = double.tryParse(paidAmt.text.trim()) ?? 0;
      pendingCtrl.text = (mess - paid).clamp(0, double.infinity).toStringAsFixed(2);
      setLocal(() {});
    }

    final ok = await showDialog<bool>(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setLocal) => AlertDialog(
          title: const Text('Update mess payment'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  title: Text(payment.memberName),
                  subtitle: Text(payment.paymentDate),
                ),
                StringDropdownField(
                  label: 'Mess Days*',
                  value: days,
                  options: const ['15', '30', '45', '60'],
                  onChanged: (v) => setLocal(() => days = v ?? '30'),
                ),
                const SizedBox(height: 12),
                AppTextField(
                  controller: messAmt,
                  label: 'Mess amount',
                  keyboardType:
                      const TextInputType.numberWithOptions(decimal: true),
                  inputFormatters: [
                    FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                  ],
                  onChanged: (_) => syncPending(setLocal),
                ),
                const SizedBox(height: 12),
                AppTextField(
                  controller: paidAmt,
                  label: 'Paid amount',
                  keyboardType:
                      const TextInputType.numberWithOptions(decimal: true),
                  inputFormatters: [
                    FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                  ],
                  onChanged: (_) => syncPending(setLocal),
                ),
                const SizedBox(height: 12),
                AppTextField(
                  controller: pendingCtrl,
                  label: 'Pending amount',
                  enabled: false,
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: Text(AppStrings.of(ref).cancel),
            ),
            AppButton(
              label: 'Save',
              onPressed: () => Navigator.pop(context, true),
            ),
          ],
        ),
      ),
    );

    if (ok != true || !mounted) {
      messAmt.dispose();
      paidAmt.dispose();
      pendingCtrl.dispose();
      return;
    }

    final messAmount = double.tryParse(messAmt.text.trim()) ?? 0;
    final paidAmount = double.tryParse(paidAmt.text.trim()) ?? 0;
    final db = ref.read(appDatabaseProvider);
    setState(() => busy = true);
    try {
      final locals = await db.getLocalMessPayments(memberId: payment.memberId);
      MessMemberPayment? local;
      for (final row in locals) {
        if (row.localPaymentId == payment.paymentId ||
            (payment.paymentNetworkStatus != null &&
                row.paymentNetworkStatus == payment.paymentNetworkStatus)) {
          local = row;
          break;
        }
      }
      if (local != null) {
        await db.updateLocalMessPayment(
          localPaymentId: local.localPaymentId,
          messAmount: messAmount,
          paidAmount: paidAmount,
          messTotalDays: days,
        );
      } else {
        await db.upsertLocalMessPayment(
          memberId: payment.memberId,
          memberName: payment.memberName,
          messAmount: messAmount,
          paidAmount: paidAmount,
          messTotalDays: days,
          paymentDate: payment.paymentDate,
          paymentNetworkStatus: payment.paymentNetworkStatus ??
              'pay_${DateTime.now().millisecondsSinceEpoch}',
        );
      }
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(AppStrings.of(ref).paymentUpdated)),
      );
    } finally {
      if (mounted) setState(() => busy = false);
      messAmt.dispose();
      paidAmt.dispose();
      pendingCtrl.dispose();
    }
  }

  @override
  Widget build(BuildContext context) {
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
    final paymentsAsync = ref.watch(messPaymentsProvider(memberIdFilter));
    return Scaffold(
      appBar: AppBar(
        title: Text(
          widget.member == null
              ? AppStrings.of(ref).ui('ui_pending_payment')
              : '${widget.member!.memberName} • ${AppStrings.of(ref).ui('ui_paid_amount')}',
        ),
        actions: [
          IconButton(
            onPressed: busy ? null : refreshFromCloud,
            icon: const Icon(Icons.refresh_rounded),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: busy ? null : messPaymentsPageAddPayment,
        icon: const Icon(Icons.payments_rounded),
        label: Text(AppStrings.of(ref).addPayment),
      ),
      body: Column(children: [
        Expanded(
          child: (!cloudLoaded && paymentsAsync.isLoading)
              ? const Center(child: CircularProgressIndicator())
              : paymentsAsync.when(
                  data: (rows) {
                    final dtos = rows.map(toDto).toList();
                    if (dtos.isEmpty) {
                      return Center(child: Text(AppStrings.of(ref).noPaymentsYet));
                    }
                    return ResponsiveScrollShell(
                      dashboard: true,
                      child: ListView.separated(
                        padding: EdgeInsets.fromLTRB(
                          AppBreakpoints.pagePaddingFor(context.widthClass),
                          12,
                          AppBreakpoints.pagePaddingFor(context.widthClass),
                          88,
                        ),
                        itemCount: dtos.length,
                        separatorBuilder: (_, _) => const SizedBox(height: 8),
                        itemBuilder: (context, index) {
                          final p = dtos[index];
                          final color =
                              index.isEven ? AppColors.green : AppColors.orange;
                          return AppCard(
                            accentColor: color,
                            padding: EdgeInsets.zero,
                            child: ListTile(
                              leading: const CircleAvatar(
                                backgroundColor: AppColors.primaryLight,
                                child: Icon(
                                  Icons.payments,
                                  color: AppColors.primary,
                                ),
                              ),
                              title: Text(
                                p.memberName,
                                style: const TextStyle(
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                              subtitle: Text(
                                '${p.paymentDate} • ${p.messTotalDays} days • '
                                'Mess ${currency.format(p.paymentMessAmount)}',
                              ),
                              trailing: Text(
                                currency.format(p.paymentPaidAmount),
                                style: const TextStyle(
                                  fontWeight: FontWeight.w800,
                                  color: AppColors.primary,
                                ),
                              ),
                              onTap: busy ? null : () => editPayment(p),
                            ),
                          );
                        },
                      ),
                    );
                  },
                  loading: () =>
                      const Center(child: CircularProgressIndicator()),
                  error: (e, _) => Center(child: Text('$e')),
                ),
        ),
      ]),
    );
  }
}
