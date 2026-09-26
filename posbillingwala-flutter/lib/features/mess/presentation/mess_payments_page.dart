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
import 'package:pos_billingwala_v2/core/utils/money_format.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/mess/data/mess_api.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_dtos.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_payment_args.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_providers.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/mess_invoice_report_page.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

class MessPaymentsPage extends ConsumerStatefulWidget {
  const MessPaymentsPage({
    super.key,
    this.member,
    this.mode = MessPaymentOpenMode.list,
  });

  final MessMember? member;
  final MessPaymentOpenMode mode;

  @override
  ConsumerState<MessPaymentsPage> createState() => MessPaymentsPageState();
}

class MessPaymentsPageState extends ConsumerState<MessPaymentsPage> {
  bool busy = false;
  bool cloudLoaded = false;

  @override
  void initState() {
    super.initState();
    /* Mobile: always read local Drift. Cloud merge is background sync only. */
    Future.microtask(() async {
      if (AppPlatform.requiresNetwork) {
        await refreshFromCloud();
      } else if (mounted) {
        setState(() => cloudLoaded = true);
      }
      if (!mounted) return;
      if (widget.mode == MessPaymentOpenMode.newPayment &&
          widget.member != null) {
        await messPaymentsPageAddPayment();
      } else if (widget.mode == MessPaymentOpenMode.payPending &&
          widget.member != null) {
        await payPending();
      }
    });
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
      final cloud = await MessApi(
        ref.read(apiClientProvider),
      ).fetchMemberPayments(userId);
      final db = ref.read(appDatabaseProvider);
      if (cloud.isNotEmpty) {
        /* replaceMessMemberPayments preserves local pending rows. */
        await db.replaceMessMemberPayments(
          cloud
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
              )
              .toList(),
        );
      }
    } catch (_) {
      /* Keep local Drift rows. */
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
    final members = ref
        .read(messMembersProvider)
        .maybeWhen(data: (v) => v, orElse: () => const <MessMember>[]);
    MessMember? selected =
        widget.member ?? (members.isNotEmpty ? members.first : null);
    if (selected == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(AppStrings.of(ref).addAMemberFirst)),
      );
      return;
    }

    final messAmt = TextEditingController();
    final paidAmt = TextEditingController();
    var days = 'Two Time';
    final month = DateFormat('yyyy-MM').format(DateTime.now());
    final nameCtrl = TextEditingController(text: selected.memberName);
    final mobileCtrl = TextEditingController(
      text: selected.memberMobileNumber ?? '',
    );

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
                    required: true,
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
                  required: true,
                  controller: nameCtrl,
                  label: 'Member name',
                  enabled: widget.member == null,
                ),
                const SizedBox(height: 12),
                AppTextField(
                  required: true,
                  controller: mobileCtrl,
                  label: 'Mobile',
                  enabled: widget.member == null,
                  keyboardType: TextInputType.phone,
                ),
                const SizedBox(height: 12),
                StringDropdownField(
                  required: true,
                  label: 'Mess days',
                  value: days,
                  options: const ['One Time', 'Two Time'],
                  onChanged: (v) => setLocal(() => days = v ?? 'Two Time'),
                ),
                const SizedBox(height: 12),
                AppTextField(
                  required: true,
                  controller: messAmt,
                  label: 'Total amount',
                  keyboardType: const TextInputType.numberWithOptions(
                    decimal: true,
                  ),
                  inputFormatters: [
                    FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                  ],
                ),
                const SizedBox(height: 12),
                AppTextField(
                  required: true,
                  controller: paidAmt,
                  label: 'Paid amount',
                  keyboardType: const TextInputType.numberWithOptions(
                    decimal: true,
                  ),
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
    final messAmount = double.tryParse(messAmt.text.trim()) ?? 0;
    final paidAmount = double.tryParse(paidAmt.text.trim()) ?? 0;
    if (paidAmount > messAmount) {
      messAmt.dispose();
      paidAmt.dispose();
      nameCtrl.dispose();
      mobileCtrl.dispose();
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Paid amount cannot exceed mess amount')),
      );
      return;
    }
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null) return;

    if (AppPlatform.requiresNetwork && !await ensureOnline()) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text(kOnlineRequiredMessage)));
      return;
    }

    setState(() => busy = true);
    try {
      final network = 'pay_${DateTime.now().millisecondsSinceEpoch}';
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
          success = await MessApi(ref.read(apiClientProvider))
              .insertMemberPayment(
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

      if (AppPlatform.requiresNetwork && !success) {
        if (!mounted) return;
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text(kWebApiSaveFailedMessage)));
        return;
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
      if (!success && !AppPlatform.supportsOfflineBilling) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text(kWebApiSaveFailedMessage)),
        );
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Payment saved')),
        );
      }
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
      text: amountInputText(payment.paymentMessAmount),
    );
    final paidAmt = TextEditingController(
      text: amountInputText(payment.paymentPaidAmount),
    );
    final pendingCtrl = TextEditingController(
      text: amountInputText(
        (payment.paymentMessAmount - payment.paymentPaidAmount)
            .clamp(0, double.infinity),
      ),
    );
    var days = payment.messTotalDays.trim().isEmpty
        ? 'Two Time'
        : payment.messTotalDays.trim();
    if (!const ['One Time', 'Two Time'].contains(days)) {
      days = 'Two Time';
    }

    void syncPending(void Function(void Function()) setLocal) {
      final mess = double.tryParse(messAmt.text.trim()) ?? 0;
      final paid = double.tryParse(paidAmt.text.trim()) ?? 0;
      pendingCtrl.text = amountInputText(
        (mess - paid).clamp(0, double.infinity),
      );
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
                  required: true,
                  label: 'Mess Days',
                  value: days,
                  options: const ['One Time', 'Two Time'],
                  onChanged: (v) => setLocal(() => days = v ?? 'Two Time'),
                ),
                const SizedBox(height: 12),
                AppTextField(
                  required: true,
                  controller: messAmt,
                  label: 'Mess amount',
                  keyboardType: const TextInputType.numberWithOptions(
                    decimal: true,
                  ),
                  inputFormatters: [
                    FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                  ],
                  onChanged: (_) => syncPending(setLocal),
                ),
                const SizedBox(height: 12),
                AppTextField(
                  required: true,
                  controller: paidAmt,
                  label: 'Paid amount',
                  keyboardType: const TextInputType.numberWithOptions(
                    decimal: true,
                  ),
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
    final userId = ref.read(authControllerProvider).session?.userId;
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
      final network =
          payment.paymentNetworkStatus ??
          local?.paymentNetworkStatus ??
          'pay_${DateTime.now().millisecondsSinceEpoch}';
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
          paymentNetworkStatus: network,
        );
      }

      var success = false;
      if (userId != null && userId.isNotEmpty && await isDeviceOnline()) {
        try {
          success = await MessApi(ref.read(apiClientProvider)).insertMemberPayment(
            userId: userId,
            memberId: payment.memberId,
            memberName: payment.memberName,
            paymentMessAmount: messAmt.text.trim(),
            paymentPaidAmount: paidAmt.text.trim(),
            messTotalDays: days,
            paymentDate: payment.paymentDate,
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
        } catch (_) {
          success = false;
        }
      }
      if (!mounted) return;
      if (AppPlatform.requiresNetwork && !success) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text(kWebApiSaveFailedMessage)),
        );
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Payment saved')),
        );
      }
    } finally {
      if (mounted) setState(() => busy = false);
      messAmt.dispose();
      paidAmt.dispose();
      pendingCtrl.dispose();
    }
  }

  /* Android UpdateMessPayment — pay toward current-month pending. */
  Future<void> payPending() async {
    final member = widget.member;
    if (member == null) return;
    final month = DateFormat('yyyy-MM').format(DateTime.now());
    final db = ref.read(appDatabaseProvider);
    final payment = await db.getMessPaymentForMonth(
      memberId: '${member.memberId}',
      yyyyMm: month,
    );
    if (payment == null) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('No payment record for this month')),
      );
      return;
    }
    final pending =
        (payment.paymentMessAmount - payment.paymentPaidAmount)
            .clamp(0, double.infinity);
    if (pending <= 0.009) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('No pending amount')),
      );
      return;
    }
    if (!mounted) return;
    final payCtrl = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Pay Pending'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(member.memberName, style: const TextStyle(fontWeight: FontWeight.w700)),
            const SizedBox(height: 4),
            Text('Mess: ₹${payment.paymentMessAmount.toStringAsFixed(2)}'),
            Text('Paid: ₹${payment.paymentPaidAmount.toStringAsFixed(2)}'),
            Text(
              'Pending: ₹${pending.toStringAsFixed(2)}',
              style: const TextStyle(
                fontWeight: FontWeight.w600,
                color: Color(0xFFD97706),
              ),
            ),
            const SizedBox(height: 12),
            AppTextField(
              required: true,
              controller: payCtrl,
              label: 'Amount to pay now',
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              inputFormatters: [
                FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
              ],
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: Text(AppStrings.of(ref).cancel),
          ),
          AppButton(
            label: 'Pay',
            onPressed: () => Navigator.pop(context, true),
          ),
        ],
      ),
    );
    if (ok != true || !mounted) {
      payCtrl.dispose();
      return;
    }
    final payNow = double.tryParse(payCtrl.text.trim()) ?? 0;
    payCtrl.dispose();
    if (payNow <= 0 || payNow > pending + 0.009) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter amount up to pending')),
      );
      return;
    }
    final newPaid = payment.paymentPaidAmount + payNow;
    final userId = ref.read(authControllerProvider).session?.userId;
    setState(() => busy = true);
    try {
      await db.updateLocalMessPayment(
        localPaymentId: payment.localPaymentId,
        messAmount: payment.paymentMessAmount,
        paidAmount: newPaid,
        messTotalDays: payment.messTotalDays,
      );
      var success = false;
      if (userId != null && userId.isNotEmpty && await isDeviceOnline()) {
        try {
          success = await MessApi(ref.read(apiClientProvider)).insertMemberPayment(
            userId: userId,
            memberId: payment.memberId,
            memberName: payment.memberName,
            paymentMessAmount: payment.paymentMessAmount.toStringAsFixed(2),
            paymentPaidAmount: newPaid.toStringAsFixed(2),
            messTotalDays: payment.messTotalDays,
            paymentDate: payment.paymentDate,
            paymentNetworkStatus: payment.paymentNetworkStatus,
          );
          if (success) {
            await db.markMessPaymentSynced(payment.localPaymentId);
          }
        } catch (_) {}
      }
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            AppPlatform.requiresNetwork && !success
                ? kWebApiSaveFailedMessage
                : 'Payment updated',
          ),
        ),
      );
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
    final paymentsAsync = ref.watch(messPaymentsProvider(memberIdFilter));
    final historyOnly = widget.mode == MessPaymentOpenMode.history;
    return Scaffold(
      appBar: AppBar(
        title: Text(
          historyOnly
              ? '${widget.member?.memberName ?? ''} • History'
              : widget.member == null
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
      floatingActionButton: historyOnly
          ? null
          : FloatingActionButton.extended(
              onPressed: busy ? null : messPaymentsPageAddPayment,
              icon: const Icon(Icons.payments_rounded),
              label: Text(AppStrings.of(ref).addPayment),
            ),
      body: Column(
        children: [
          if (widget.member != null)
            FutureBuilder<(int, int)>(
              future: () async {
                final db = ref.read(appDatabaseProvider);
                final name = widget.member!.memberName;
                final invoices = await db.watchMessInvoices().first;
                final tokens = await db.getAllMessTokens();
                final rows = MessInvoiceReportPage.buildRows(
                  invoices: invoices,
                  tokens: tokens,
                );
                var coupons = 0;
                var qr = 0;
                for (final r in rows) {
                  final matchName =
                      name.trim().toLowerCase() ==
                      r.memberName.trim().toLowerCase();
                  if (!matchName) continue;
                  if (r.isQr) {
                    qr++;
                  } else {
                    coupons++;
                  }
                }
                return (coupons, qr);
              }(),
              builder: (context, snap) {
                final coupons = snap.data?.$1 ?? 0;
                final qr = snap.data?.$2 ?? 0;
                return Padding(
                  padding: const EdgeInsets.fromLTRB(16, 10, 16, 0),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text(
                          'Coupons: $coupons',
                          textAlign: TextAlign.center,
                          style: const TextStyle(
                            fontWeight: FontWeight.w700,
                            color: AppColors.primary,
                          ),
                        ),
                      ),
                      Expanded(
                        child: Text(
                          'QR Tokens: $qr',
                          textAlign: TextAlign.center,
                          style: const TextStyle(
                            fontWeight: FontWeight.w700,
                            color: AppColors.primary,
                          ),
                        ),
                      ),
                    ],
                  ),
                );
              },
            ),
          Expanded(
            child: (!cloudLoaded && paymentsAsync.isLoading)
                ? const Center(child: CircularProgressIndicator())
                : paymentsAsync.when(
                    data: (rows) {
                      final dtos = rows.map(toDto).toList();
                      if (dtos.isEmpty) {
                        return Center(
                          child: Text(AppStrings.of(ref).noPaymentsYet),
                        );
                      }
                      if (historyOnly) {
                        /* Android MessMemberPaymentHistory — month rows. */
                        final byMonth = <String, MessMemberPaymentDto>{};
                        for (final p in dtos) {
                          final key = p.paymentDate.length >= 7
                              ? p.paymentDate.substring(0, 7)
                              : p.paymentDate;
                          final existing = byMonth[key];
                          if (existing == null ||
                              p.paymentId >= existing.paymentId) {
                            byMonth[key] = p;
                          }
                        }
                        final months = byMonth.keys.toList()
                          ..sort((a, b) => b.compareTo(a));
                        return ResponsiveScrollShell(
                          dashboard: true,
                          child: ListView.separated(
                            padding: EdgeInsets.fromLTRB(
                              AppBreakpoints.pagePaddingFor(context.widthClass),
                              12,
                              AppBreakpoints.pagePaddingFor(context.widthClass),
                              24,
                            ),
                            itemCount: months.length,
                            separatorBuilder: (_, _) =>
                                const SizedBox(height: 8),
                            itemBuilder: (context, index) {
                              final key = months[index];
                              final p = byMonth[key]!;
                              final pending =
                                  (p.paymentMessAmount - p.paymentPaidAmount)
                                      .clamp(0, double.infinity);
                              final paid = pending <= 0.009;
                              return AppCard(
                                accentColor: paid
                                    ? AppColors.green
                                    : AppColors.orange,
                                padding: const EdgeInsets.all(14),
                                child: Column(
                                  crossAxisAlignment:
                                      CrossAxisAlignment.stretch,
                                  children: [
                                    Text(
                                      key,
                                      style: const TextStyle(
                                        fontWeight: FontWeight.w800,
                                        fontSize: 15,
                                      ),
                                    ),
                                    const SizedBox(height: 8),
                                    Text(
                                      'Mess: ${currency.format(p.paymentMessAmount)}',
                                    ),
                                    Text(
                                      'Paid: ${currency.format(p.paymentPaidAmount)}',
                                    ),
                                    Text(
                                      paid
                                          ? 'Status: Paid'
                                          : 'Pending: ${currency.format(pending)}',
                                      style: TextStyle(
                                        fontWeight: FontWeight.w600,
                                        color: paid
                                            ? AppColors.green
                                            : const Color(0xFFD97706),
                                      ),
                                    ),
                                    if (p.messTotalDays.isNotEmpty)
                                      Text(
                                        'Days: ${p.messTotalDays}',
                                        style: const TextStyle(fontSize: 12),
                                      ),
                                  ],
                                ),
                              );
                            },
                          ),
                        );
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
                            final color = index.isEven
                                ? AppColors.green
                                : AppColors.orange;
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
                                  '${p.paymentDate} • ${p.messTotalDays} • '
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
        ],
      ),
    );
  }
}
