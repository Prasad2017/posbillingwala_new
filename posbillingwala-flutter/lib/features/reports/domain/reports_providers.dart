import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/network/api_client.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/auth/domain/data_scope.dart';
import 'package:pos_billingwala_v2/features/sync/data/invoice_sync_api.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_invoice_dto.dart';

/* Staff id when staff-scoped; null for owner/licence (full branch data). */
int? resolvedStaffFilter(Ref ref) {
  return ref
      .watch(dataScopeProvider)
      .maybeWhen(data: (scope) => scope.staffId, orElse: () => null);
}
enum ReportPeriodKind { today, month, day, year }

class ReportPeriod {
  const ReportPeriod.today() : kind = ReportPeriodKind.today, day = null;

  const ReportPeriod.month([DateTime? monthAnchor])
    : kind = ReportPeriodKind.month,
      day = monthAnchor;

  const ReportPeriod.day(this.day) : kind = ReportPeriodKind.day;

  const ReportPeriod.year([DateTime? yearAnchor])
    : kind = ReportPeriodKind.year,
      day = yearAnchor;

  final ReportPeriodKind kind;

  /* For [ReportPeriodKind.day]: the day. */
  /* For [ReportPeriodKind.month]: any day in the selected month (null = current). */
  /* For [ReportPeriodKind.year]: any day in the selected year (null = current). */
  final DateTime? day;

  (DateTime start, DateTime end) get range {
    final now = DateTime.now();
    switch (kind) {
      case ReportPeriodKind.today:
        final start = DateTime(now.year, now.month, now.day);
        return (start, start.add(const Duration(days: 1)));
      case ReportPeriodKind.month:
        final anchor = day ?? now;
        final start = DateTime(anchor.year, anchor.month, 1);
        final end = (anchor.month == 12)
            ? DateTime(anchor.year + 1, 1, 1)
            : DateTime(anchor.year, anchor.month + 1, 1);
        return (start, end);
      case ReportPeriodKind.day:
        final d = day ?? now;
        final start = DateTime(d.year, d.month, d.day);
        return (start, start.add(const Duration(days: 1)));
      case ReportPeriodKind.year:
        final anchor = day ?? now;
        final start = DateTime(anchor.year, 1, 1);
        return (start, DateTime(anchor.year + 1, 1, 1));
    }
  }

  String get label {
    switch (kind) {
      case ReportPeriodKind.today:
        return 'Today';
      case ReportPeriodKind.month:
        final anchor = day ?? DateTime.now();
        final now = DateTime.now();
        if (anchor.year == now.year && anchor.month == now.month) {
          return 'This month';
        }
        const months = [
          'Jan',
          'Feb',
          'Mar',
          'Apr',
          'May',
          'Jun',
          'Jul',
          'Aug',
          'Sep',
          'Oct',
          'Nov',
          'Dec',
        ];
        return '${months[anchor.month - 1]} ${anchor.year}';
      case ReportPeriodKind.day:
        final d = day ?? DateTime.now();
        return '${d.day.toString().padLeft(2, '0')}-'
            '${d.month.toString().padLeft(2, '0')}-${d.year}';
      case ReportPeriodKind.year:
        final anchor = day ?? DateTime.now();
        return '${anchor.year}';
    }
  }
}

class SalesSummary {
  const SalesSummary({
    required this.billCount,
    required this.totalSales,
    required this.subTotal,
    required this.gstTotal,
    required this.discountTotal,
    required this.cashTotal,
    required this.upiTotal,
    required this.posCount,
    required this.takeawayCount,
    required this.tableCount,
    required this.avgBill,
  });

  final int billCount;
  final double totalSales;
  final double subTotal;
  final double gstTotal;
  final double discountTotal;
  final double cashTotal;
  final double upiTotal;
  final int posCount;
  final int takeawayCount;
  final int tableCount;
  final double avgBill;

  static const empty = SalesSummary(
    billCount: 0,
    totalSales: 0,
    subTotal: 0,
    gstTotal: 0,
    discountTotal: 0,
    cashTotal: 0,
    upiTotal: 0,
    posCount: 0,
    takeawayCount: 0,
    tableCount: 0,
    avgBill: 0,
  );

  factory SalesSummary.fromInvoices(List<Invoice> invoices) {
    if (invoices.isEmpty) return SalesSummary.empty;

    var total = 0.0;
    var sub = 0.0;
    var gst = 0.0;
    var discount = 0.0;
    var cash = 0.0;
    var upi = 0.0;
    var posCount = 0;
    var takeawayCount = 0;
    var tableCount = 0;

    for (final invoice in invoices) {
      total += invoice.totalAmount;
      sub += invoice.subTotal;
      gst += invoice.totalGstAmount;
      discount += invoice.discount;
      cash += invoice.cashAmount;
      upi += invoice.upiAmount;
      switch (invoice.invoiceType) {
        case 'take_away':
          takeawayCount++;
        case 'table_wise':
          tableCount++;
        default:
          posCount++;
      }
    }

    final count = invoices.length;
    return SalesSummary(
      billCount: count,
      totalSales: r(total),
      subTotal: r(sub),
      gstTotal: r(gst),
      discountTotal: r(discount),
      cashTotal: r(cash),
      upiTotal: r(upi),
      posCount: posCount,
      takeawayCount: takeawayCount,
      tableCount: tableCount,
      avgBill: r(total / count),
    );
  }

  factory SalesSummary.fromAggregate(InvoiceSalesAggregate row) {
    if (row.billCount <= 0) return SalesSummary.empty;
    return SalesSummary(
      billCount: row.billCount,
      totalSales: r(row.totalSales),
      subTotal: r(row.subTotal),
      gstTotal: r(row.gstTotal),
      discountTotal: r(row.discountTotal),
      cashTotal: r(row.cashTotal),
      upiTotal: r(row.upiTotal),
      posCount: row.posCount,
      takeawayCount: row.takeawayCount,
      tableCount: row.tableCount,
      avgBill: r(row.totalSales / row.billCount),
    );
  }

  static double r(double v) => double.parse(v.toStringAsFixed(2));
}

class ReportPeriodController extends Notifier<ReportPeriod> {
  @override
  ReportPeriod build() => const ReportPeriod.today();

  void useToday() => state = const ReportPeriod.today();

  void useMonth([DateTime? monthAnchor]) =>
      state = ReportPeriod.month(monthAnchor);

  void useDay(DateTime day) => state = ReportPeriod.day(day);

  void useYear([DateTime? yearAnchor]) => state = ReportPeriod.year(yearAnchor);
}

final reportPeriodProvider =
    NotifierProvider<ReportPeriodController, ReportPeriod>(
      ReportPeriodController.new,
    );

/* Mobile: local Drift stream. Web: fetch from getPosSalesReport / getInvoiceList. */
final periodInvoicesProvider = StreamProvider<List<Invoice>>((ref) {
  final period = ref.watch(reportPeriodProvider);
  final (start, end) = period.range;
  final staffId = resolvedStaffFilter(ref);
  if (AppPlatform.requiresNetwork) {
    return Stream.fromFuture(
      loadPeriodInvoicesFromApi(
        userId: ref.read(authControllerProvider).session?.userId,
        client: ref.read(apiClientProvider),
        db: ref.read(appDatabaseProvider),
        start: start,
        end: end,
        createdByStaffId: staffId,
      ),
    );
  }
  return ref
      .watch(appDatabaseProvider)
      .watchInvoicesInRange(start, end, createdByStaffId: staffId);
});

Future<List<Invoice>> loadPeriodInvoicesFromApi({
  required String? userId,
  required ApiClient client,
  required AppDatabase db,
  required DateTime start,
  required DateTime end,
  bool includeItems = false,
  int? createdByStaffId,
}) async {
  await requireOnlineForWeb();
  if (userId == null || userId.isEmpty) {
    throw StateError('Please login to view reports on Web POS.');
  }
  final fmt = DateFormat('yyyy-MM-dd');
  /* ReportPeriod.range uses exclusive end — convert to inclusive endDate. */
  final inclusiveEnd = end.subtract(const Duration(days: 1));
  final endDay = inclusiveEnd.isBefore(start) ? start : inclusiveEnd;
  final api = InvoiceSyncApi(client);

  List<CloudInvoiceDto> cloud = const [];
  final staffScoped = createdByStaffId != null && createdByStaffId > 0;
  try {
    final report = await api.fetchPosSalesReport(
      userId: userId,
      startDate: fmt.format(start),
      endDate: fmt.format(endDay),
      staffScope: staffScoped,
    );
    cloud = report.invoices;
  } catch (_) {
    cloud = await api.fetchInvoices(
      userId,
      startDate: fmt.format(start),
      endDate: fmt.format(endDay),
      staffScope: staffScoped,
    );
  }

  /* Extra guard if server ignored staffScope. */
  if (staffScoped) {
    cloud = cloud
        .where((e) => e.createdByStaffId == createdByStaffId)
        .toList(growable: false);
  }

  if (cloud.isNotEmpty) {
    final itemsByNumber = <String, List<InvoiceItemsCompanion>>{};
    if (includeItems) {
      final items = await api.fetchInvoiceItems(userId);
      final numbers = cloud.map((e) => e.invoiceNumber).toSet();
      for (final item in items) {
        if (!numbers.contains(item.invoiceNumber)) continue;
        itemsByNumber
            .putIfAbsent(item.invoiceNumber, () => [])
            .add(item.toCompanion());
      }
    }
    await db.upsertCloudInvoices(
      headers: cloud.map((e) => e.toCompanion()).toList(),
      itemsByNumber: itemsByNumber,
    );
  }

  return (await db
      .watchInvoicesInRange(start, end, createdByStaffId: createdByStaffId)
      .first);
}

/* Hydrate Drift from cloud for web report screens that query local aggregates. */
Future<void> hydrateWebReportRange(
  WidgetRef ref, {
  required DateTime start,
  required DateTime end,
  bool includeItems = false,
}) async {
  if (!AppPlatform.requiresNetwork) return;
  await loadPeriodInvoicesFromApi(
    userId: ref.read(authControllerProvider).session?.userId,
    client: ref.read(apiClientProvider),
    db: ref.read(appDatabaseProvider),
    start: start,
    end: end,
    includeItems: includeItems,
  );
}

enum ReportPaymentFilter { all, cash, upi, cashPlusUpi }

class ReportPaymentFilterController extends Notifier<ReportPaymentFilter> {
  @override
  ReportPaymentFilter build() => ReportPaymentFilter.all;

  void select(ReportPaymentFilter filter) => state = filter;
}

final reportPaymentFilterProvider =
    NotifierProvider<ReportPaymentFilterController, ReportPaymentFilter>(
      ReportPaymentFilterController.new,
    );

enum ReportInvoiceTypeFilter {
  all,
  pos,
  table,
  takeaway,
  mess,
  discountOnly,
  refundOnly,
}

class ReportInvoiceTypeFilterController
    extends Notifier<ReportInvoiceTypeFilter> {
  @override
  ReportInvoiceTypeFilter build() => ReportInvoiceTypeFilter.all;

  void select(ReportInvoiceTypeFilter filter) => state = filter;
}

final reportInvoiceTypeFilterProvider =
    NotifierProvider<
      ReportInvoiceTypeFilterController,
      ReportInvoiceTypeFilter
    >(ReportInvoiceTypeFilterController.new);

bool matchesInvoiceTypeFilter(Invoice invoice, ReportInvoiceTypeFilter filter) {
  final type = invoice.invoiceType.trim().toLowerCase();
  switch (filter) {
    case ReportInvoiceTypeFilter.all:
      return true;
    case ReportInvoiceTypeFilter.pos:
      return type == 'fast_billing' ||
          type == 'pos' ||
          (type != 'take_away' &&
              type != 'table_wise' &&
              !type.contains('mess'));
    case ReportInvoiceTypeFilter.table:
      return type == 'table_wise';
    case ReportInvoiceTypeFilter.takeaway:
      return type == 'take_away';
    case ReportInvoiceTypeFilter.mess:
      return type == 'mess' || type.contains('mess');
    case ReportInvoiceTypeFilter.discountOnly:
      return invoice.discount > 0;
    case ReportInvoiceTypeFilter.refundOnly:
      return invoice.invoiceOrderStatus.toLowerCase() == 'refunded';
  }
}

bool matchesPaymentFilter(Invoice invoice, ReportPaymentFilter filter) {
  switch (filter) {
    case ReportPaymentFilter.all:
      return true;
    case ReportPaymentFilter.cash:
      return invoice.paymentMode == 'Cash';
    case ReportPaymentFilter.upi:
      return invoice.paymentMode == 'UPI' || invoice.paymentMode == 'Online';
    case ReportPaymentFilter.cashPlusUpi:
      return invoice.paymentMode == 'Cash+UPI';
  }
}

final filteredPeriodInvoicesProvider = Provider<List<Invoice>>((ref) {
  final invoices = ref
      .watch(periodInvoicesProvider)
      .maybeWhen(data: (rows) => rows, orElse: () => const <Invoice>[]);
  final paymentFilter = ref.watch(reportPaymentFilterProvider);
  final typeFilter = ref.watch(reportInvoiceTypeFilterProvider);
  return invoices
      .where(
        (e) =>
            matchesPaymentFilter(e, paymentFilter) &&
            matchesInvoiceTypeFilter(e, typeFilter),
      )
      .toList();
});

final periodSalesSummaryProvider = Provider<SalesSummary>((ref) {
  final invoices = ref.watch(filteredPeriodInvoicesProvider);
  return SalesSummary.fromInvoices(invoices);
});

/* Back-compat aliases used by older call sites. */
typedef TodaySalesSummary = SalesSummary;

final todayInvoicesProvider = StreamProvider<List<Invoice>>((ref) {
  final staffId = resolvedStaffFilter(ref);
  return ref
      .watch(appDatabaseProvider)
      .watchTodayInvoices(createdByStaffId: staffId);
});

final todaySalesSummaryProvider = Provider<SalesSummary>((ref) {
  return ref
      .watch(todaySalesAggregateProvider)
      .maybeWhen(
        data: SalesSummary.fromAggregate,
        orElse: () => SalesSummary.empty,
      );
});

final todaySalesAggregateProvider = StreamProvider<InvoiceSalesAggregate>((
  ref,
) {
  final now = DateTime.now();
  final start = DateTime(now.year, now.month, now.day);
  final staffId = resolvedStaffFilter(ref);
  return ref
      .watch(appDatabaseProvider)
      .watchSalesAggregate(
        start: start,
        end: start.add(const Duration(days: 1)),
        createdByStaffId: staffId,
      );
});

final yesterdayInvoicesProvider = StreamProvider<List<Invoice>>((ref) {
  final now = DateTime.now();
  final start = DateTime(
    now.year,
    now.month,
    now.day,
  ).subtract(const Duration(days: 1));
  final end = DateTime(now.year, now.month, now.day);
  final staffId = resolvedStaffFilter(ref);
  return ref
      .watch(appDatabaseProvider)
      .watchInvoicesInRange(start, end, createdByStaffId: staffId);
});

final yesterdaySalesSummaryProvider = Provider<SalesSummary>((ref) {
  return ref
      .watch(yesterdaySalesAggregateProvider)
      .maybeWhen(
        data: SalesSummary.fromAggregate,
        orElse: () => SalesSummary.empty,
      );
});

final yesterdaySalesAggregateProvider = StreamProvider<InvoiceSalesAggregate>((
  ref,
) {
  final now = DateTime.now();
  final start = DateTime(
    now.year,
    now.month,
    now.day,
  ).subtract(const Duration(days: 1));
  final end = DateTime(now.year, now.month, now.day);
  final staffId = resolvedStaffFilter(ref);
  return ref
      .watch(appDatabaseProvider)
      .watchSalesAggregate(start: start, end: end, createdByStaffId: staffId);
});

final monthInvoicesProvider = StreamProvider<List<Invoice>>((ref) {
  final now = DateTime.now();
  final start = DateTime(now.year, now.month, 1);
  final end = (now.month == 12)
      ? DateTime(now.year + 1, 1, 1)
      : DateTime(now.year, now.month + 1, 1);
  final staffId = resolvedStaffFilter(ref);
  return ref
      .watch(appDatabaseProvider)
      .watchInvoicesInRange(start, end, createdByStaffId: staffId);
});

final monthSalesSummaryProvider = Provider<SalesSummary>((ref) {
  return ref
      .watch(monthSalesAggregateProvider)
      .maybeWhen(
        data: SalesSummary.fromAggregate,
        orElse: () => SalesSummary.empty,
      );
});

final monthSalesAggregateProvider = StreamProvider<InvoiceSalesAggregate>((
  ref,
) {
  final now = DateTime.now();
  final start = DateTime(now.year, now.month, 1);
  final end = (now.month == 12)
      ? DateTime(now.year + 1, 1, 1)
      : DateTime(now.year, now.month + 1, 1);
  final staffId = resolvedStaffFilter(ref);
  return ref
      .watch(appDatabaseProvider)
      .watchSalesAggregate(start: start, end: end, createdByStaffId: staffId);
});

final invoiceDetailProvider =
    FutureProvider.family<({Invoice invoice, List<InvoiceItem> items})?, int>((
      ref,
      invoiceId,
    ) async {
      final db = ref.watch(appDatabaseProvider);
      final invoice = await db.getInvoiceById(invoiceId);
      if (invoice == null) return null;
      final staffId = resolvedStaffFilter(ref);
      if (staffId != null &&
          staffId > 0 &&
          (invoice.createdByStaffId ?? 0) != staffId) {
        return null;
      }
      final items = await db.getInvoiceItems(invoice.invoiceNumber);
      return (invoice: invoice, items: items);
    });
