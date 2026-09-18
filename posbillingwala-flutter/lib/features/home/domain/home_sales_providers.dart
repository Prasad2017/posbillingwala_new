import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/home/data/home_sales_api.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_cache.dart';

/* Home period filter — Android `SALES_FILTER_TODAY` / `SALES_FILTER_MONTH`. */
/* - today → primary card = all-time total sales */
/* - month → primary card = this month */
enum HomeSalesPeriod { today, month }

final homeSalesPeriodProvider =
    NotifierProvider<HomeSalesPeriodNotifier, HomeSalesPeriod>(
      HomeSalesPeriodNotifier.new,
    );

class HomeSalesPeriodNotifier extends Notifier<HomeSalesPeriod> {
  @override
  HomeSalesPeriod build() => HomeSalesPeriod.today;

  void setPeriod(HomeSalesPeriod period) => state = period;

  void toggle() {
    state = state == HomeSalesPeriod.month
        ? HomeSalesPeriod.today
        : HomeSalesPeriod.month;
  }
}

final allTimeSalesAggregateProvider = StreamProvider<InvoiceSalesAggregate>((
  ref,
) {
  final staffId = resolvedStaffFilter(ref);
  return ref
      .watch(appDatabaseProvider)
      .watchSalesAggregate(createdByStaffId: staffId);
});

final allTimeSalesSummaryProvider = Provider<SalesSummary>((ref) {
  return ref
      .watch(allTimeSalesAggregateProvider)
      .maybeWhen(
        data: SalesSummary.fromAggregate,
        orElse: () => SalesSummary.empty,
      );
});

/* Cloud overview (Android `getHomeSalesOverview`) — cache first, then network. */
final homeSalesOverviewProvider =
    FutureProvider.autoDispose<HomeSalesOverview?>((ref) async {
      final userId = ref.watch(authControllerProvider).session?.licenceUserId;
      if (userId == null || userId.isEmpty) return null;

      final period = ref.watch(homeSalesPeriodProvider);
      final staffId = resolvedStaffFilter(ref);
      final staffScoped = staffId != null && staffId > 0;
      final cacheKey = period == HomeSalesPeriod.month
          ? CloudScreenCache.homeOverviewMonth
          : CloudScreenCache.homeOverviewToday;
      /* Staff UI uses local aggregates; avoid showing full-licence cloud KPIs. */
      if (!staffScoped) {
        final cached = await CloudScreenCache.loadMap(cacheKey);
        HomeSalesOverview? cachedOverview;
        if (cached != null) {
          cachedOverview = HomeSalesOverview.fromJson(cached);
        }

        if (!await isDeviceOnline()) return cachedOverview;

        final api = HomeSalesApi(ref.read(apiClientProvider));
        final live = await api.fetchOverview(
          userId: userId,
          period: period == HomeSalesPeriod.month ? 'month' : 'today',
        );
        return live ?? cachedOverview;
      }

      if (!await isDeviceOnline()) return null;
      final api = HomeSalesApi(ref.read(apiClientProvider));
      return api.fetchOverview(
        userId: userId,
        period: period == HomeSalesPeriod.month ? 'month' : 'today',
        staffScope: true,
      );
    });

/* Display model for home Sales Overview + catalog KPI tiles. */
class HomeDashboardKpis {
  const HomeDashboardKpis({
    required this.primarySales,
    required this.todaySales,
    required this.primaryTitle,
    this.growthText,
    this.growthUp = true,
    required this.subcategories,
    required this.products,
    required this.combos,
    this.fromCloud = false,
  });

  final double primarySales;
  final double todaySales;
  final String primaryTitle;
  final String? growthText;
  final bool growthUp;
  final int subcategories;
  final int products;
  final int combos;
  final bool fromCloud;
}

final homeDashboardKpisProvider = Provider<HomeDashboardKpis>((ref) {
  final period = ref.watch(homeSalesPeriodProvider);
  final overviewAsync = ref.watch(homeSalesOverviewProvider);
  final localToday = ref.watch(todaySalesSummaryProvider);
  final localMonth = ref.watch(monthSalesSummaryProvider);
  final localAll = ref.watch(allTimeSalesSummaryProvider);
  final yesterday = ref.watch(yesterdaySalesSummaryProvider);
  final catalog = ref.watch(catalogCountsProvider);

  final overview = overviewAsync.maybeWhen(data: (v) => v, orElse: () => null);

  if (overview != null) {
    final parsed = parseTrend(overview.primarySalesTrend);
    return HomeDashboardKpis(
      /* API already selects all-time vs month via `period`. */
      primarySales: overview.primarySales,
      todaySales: overview.todaySales,
      primaryTitle: period == HomeSalesPeriod.month
          ? 'Monthly Sales'
          : 'Total Sales',
      growthText: parsed.$1,
      growthUp: parsed.$2,
      /* Prefer local catalog; fall back to cloud (web often has empty Drift). */
      subcategories: catalog.subcategories > 0
          ? catalog.subcategories
          : overview.totalSubcategory,
      products: catalog.products > 0 ? catalog.products : overview.totalProduct,
      combos: catalog.combos > 0 ? catalog.combos : overview.totalCombo,
      fromCloud: true,
    );
  }

  final primarySummary = period == HomeSalesPeriod.month
      ? localMonth
      : localAll;
  final growthPct = growthPercent(localToday.totalSales, yesterday.totalSales);
  return HomeDashboardKpis(
    primarySales: primarySummary.totalSales,
    todaySales: localToday.totalSales,
    primaryTitle: period == HomeSalesPeriod.month
        ? 'Monthly Sales'
        : 'Total Sales',
    growthText: growthLabel(growthPct),
    growthUp: (growthPct ?? 0) >= 0,
    subcategories: catalog.subcategories,
    products: catalog.products,
    combos: catalog.combos,
    fromCloud: false,
  );
});

double? growthPercent(double current, double previous) {
  if (previous <= 0) {
    if (current <= 0) return null;
    return 100;
  }
  return ((current - previous) / previous) * 100;
}

String? growthLabel(double? growth) {
  if (growth == null) return null;
  final abs = growth.abs().round();
  final arrow = growth >= 0 ? '↑' : '↓';
  return '$arrow $abs% vs Yesterday';
}

(String?, bool) parseTrend(String raw) {
  final text = raw.trim();
  if (text.isEmpty || text == '0' || text == '0%') {
    return (null, true);
  }
  final down = text.startsWith('-');
  final cleaned = text.replaceAll('+', '').replaceAll('-', '');
  final arrow = down ? '↓' : '↑';
  return ('$arrow $cleaned vs prior', !down);
}
