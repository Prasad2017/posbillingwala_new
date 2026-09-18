import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/network/api_response.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_cache.dart';

class SalaryRow {
  const SalaryRow({
    required this.staffId,
    required this.staffName,
    required this.role,
    required this.monthlySalary,
    required this.paymentStatus,
    required this.paidAmount,
    required this.paidOn,
    required this.note,
  });

  final String staffId;
  final String staffName;
  final String role;
  final double monthlySalary;
  final String paymentStatus;
  final double paidAmount;
  final String paidOn;
  final String note;

  bool get isPaid => paymentStatus.toUpperCase() == 'PAID';

  factory SalaryRow.fromJson(Map<String, dynamic> json) {
    double m(Object? v) => double.tryParse(v?.toString() ?? '') ?? 0;
    return SalaryRow(
      staffId: json['staffId']?.toString() ?? '',
      staffName: json['staffName']?.toString() ?? '',
      role: json['role']?.toString() ?? '',
      monthlySalary: m(json['monthlySalary']),
      paymentStatus: json['paymentStatus']?.toString() ?? 'PENDING',
      paidAmount: m(json['paidAmount']),
      paidOn: json['paidOn']?.toString() ?? '',
      note: json['note']?.toString() ?? '',
    );
  }
}

class SalaryPage extends ConsumerStatefulWidget {
  const SalaryPage({super.key});

  @override
  ConsumerState<SalaryPage> createState() => SalaryPageState();
}

class SalaryPageState extends ConsumerState<SalaryPage> {
  late DateTime month;
  bool busy = false;
  String? error;
  List<SalaryRow> rows = [];
  double totalDue = 0;
  double totalPaid = 0;

  @override
  void initState() {
    super.initState();
    final now = DateTime.now();
    month = DateTime(now.year, now.month);
    WidgetsBinding.instance.addPostFrameCallback((_) => load());
  }

  String get monthKey =>
      '${month.year.toString().padLeft(4, '0')}-${month.month.toString().padLeft(2, '0')}';

  Future<void> load() async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) return;
    setState(() {
      busy = true;
      error = null;
    });
    try {
      final cached = await CloudScreenCache.loadMapList(
        CloudScreenCache.salary,
      );
      if (cached.isNotEmpty && mounted) {
        final list = cached.map(SalaryRow.fromJson).toList();
        setState(() {
          rows = list;
          totalDue = list.fold(0, (s, r) => s + r.monthlySalary);
          totalPaid = list.fold(0, (s, r) => s + r.paidAmount);
          busy = false;
        });
      }
      final client = ref.read(apiClientProvider);
      final response = await client.dio.post<dynamic>(
        ApiEndpoints.getSalaryList,
        data: {'userId': userId, 'salaryMonth': monthKey},
        options: Options(contentType: Headers.formUrlEncodedContentType),
      );
      final data = asJsonMap(response.data);
      if (!isApiSuccess(data)) {
        throw Exception(data['message']?.toString() ?? 'Unable to load salary');
      }
      final list = <SalaryRow>[];
      final raw = data['salaryResponse'];
      if (raw is List) {
        for (final item in raw) {
          if (item is Map) {
            list.add(SalaryRow.fromJson(Map<String, dynamic>.from(item)));
          }
        }
      }
      setState(() {
        rows = list;
        totalDue = double.tryParse(data['totalDue']?.toString() ?? '') ?? 0;
        totalPaid = double.tryParse(data['totalPaid']?.toString() ?? '') ?? 0;
        busy = false;
      });
    } catch (e) {
      setState(() {
        busy = false;
        error = e.toString().replaceFirst('Exception: ', '');
      });
    }
  }

  Future<void> pickMonth() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: month,
      firstDate: DateTime(DateTime.now().year - 3),
      lastDate: DateTime.now(),
      helpText: 'Pick any day in the salary month',
    );
    if (picked == null) return;
    setState(() => month = DateTime(picked.year, picked.month));
    await load();
  }

  Future<void> editSalary(SalaryRow row) async {
    final controller = TextEditingController(
      text: row.monthlySalary.toStringAsFixed(0),
    );
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Salary — ${row.staffName}'),
        content: AppTextField(
          required: true,
          controller: controller,
          label: 'Monthly salary (₹)',
          keyboardType: TextInputType.number,
          inputFormatters: [FilteringTextInputFormatter.digitsOnly],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Save'),
          ),
        ],
      ),
    );
    if (ok != true) return;
    final amount = double.tryParse(controller.text.trim()) ?? -1;
    if (amount < 0) return;
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null) return;
    setState(() => busy = true);
    try {
      final client = ref.read(apiClientProvider);
      final response = await client.dio.post<dynamic>(
        ApiEndpoints.updateStaffSalary,
        data: {
          'userId': userId,
          'staffId': row.staffId,
          'monthlySalary': amount.toStringAsFixed(2),
        },
        options: Options(contentType: Headers.formUrlEncodedContentType),
      );
      final data = asJsonMap(response.data);
      if (!isApiSuccess(data)) {
        throw Exception(data['message']?.toString() ?? 'Save failed');
      }
      await load();
    } catch (e) {
      setState(() {
        busy = false;
        error = e.toString().replaceFirst('Exception: ', '');
      });
    }
  }

  Future<void> markPaid(SalaryRow row) async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null) return;
    final confirm = await showAppConfirmBottomSheet(
      context: context,
      title: 'Mark salary paid?',
      message:
          '${row.staffName} — ₹${row.monthlySalary.toStringAsFixed(0)} for $monthKey',
      confirmLabel: 'Mark paid',
      cancelLabel: 'Cancel',
    );
    if (!confirm) return;
    setState(() => busy = true);
    try {
      final client = ref.read(apiClientProvider);
      final response = await client.dio.post<dynamic>(
        ApiEndpoints.saveSalaryPayment,
        data: {
          'userId': userId,
          'staffId': row.staffId,
          'salaryMonth': monthKey,
          'amount': row.monthlySalary.toStringAsFixed(2),
          'paidOn': DateFormat('yyyy-MM-dd').format(DateTime.now()),
        },
        options: Options(contentType: Headers.formUrlEncodedContentType),
      );
      final data = asJsonMap(response.data);
      if (!isApiSuccess(data)) {
        throw Exception(data['message']?.toString() ?? 'Payment failed');
      }
      await load();
    } catch (e) {
      setState(() {
        busy = false;
        error = e.toString().replaceFirst('Exception: ', '');
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final money = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
    final monthLabel = DateFormat('MMMM yyyy').format(month);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Salary'),
        actions: [
          IconButton(
            tooltip: 'Month',
            onPressed: busy ? null : pickMonth,
            icon: const Icon(Icons.calendar_month_rounded),
          ),
          IconButton(
            tooltip: 'Refresh',
            onPressed: busy ? null : load,
            icon: const Icon(Icons.refresh_rounded),
          ),
        ],
      ),
      body: ResponsiveContent(
        child: busy && rows.isEmpty
            ? const Center(child: CircularProgressIndicator())
            : ListView(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
                children: [
                  Text(
                    monthLabel,
                    style: const TextStyle(
                      fontWeight: FontWeight.w800,
                      fontSize: 18,
                    ),
                  ),
                  const SizedBox(height: 10),
                  AppCard(
                    child: Row(
                      children: [
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('Due'),
                              Text(
                                money.format(totalDue),
                                style: const TextStyle(
                                  fontWeight: FontWeight.w800,
                                  fontSize: 18,
                                ),
                              ),
                            ],
                          ),
                        ),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.end,
                            children: [
                              const Text('Paid'),
                              Text(
                                money.format(totalPaid),
                                style: const TextStyle(
                                  fontWeight: FontWeight.w800,
                                  fontSize: 18,
                                  color: AppColors.success,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                  if (error != null) ...[
                    const SizedBox(height: 10),
                    Text(error!, style: const TextStyle(color: Colors.red)),
                  ],
                  const SizedBox(height: 12),
                  for (final row in rows)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: AppCard(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            Row(
                              children: [
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        row.staffName,
                                        style: const TextStyle(
                                          fontWeight: FontWeight.w700,
                                        ),
                                      ),
                                      Text(row.role),
                                    ],
                                  ),
                                ),
                                Chip(
                                  label: Text(row.isPaid ? 'Paid' : 'Pending'),
                                  backgroundColor: row.isPaid
                                      ? AppColors.success.withValues(
                                          alpha: 0.15,
                                        )
                                      : AppColors.warning.withValues(
                                          alpha: 0.15,
                                        ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 8),
                            Text(
                              'Salary: ${money.format(row.monthlySalary)}',
                              style: const TextStyle(
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                            if (row.isPaid && row.paidOn.isNotEmpty)
                              Text('Paid on ${row.paidOn}'),
                            const SizedBox(height: 10),
                            Row(
                              children: [
                                Expanded(
                                  child: AppButton(
                                    label: 'Set salary',
                                    variant: AppButtonVariant.outlined,
                                    onPressed: busy
                                        ? null
                                        : () => editSalary(row),
                                  ),
                                ),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: AppButton(
                                    label: row.isPaid
                                        ? 'Update paid'
                                        : 'Mark paid',
                                    onPressed: busy || row.monthlySalary <= 0
                                        ? null
                                        : () => markPaid(row),
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                    ),
                ],
              ),
      ),
    );
  }
}
