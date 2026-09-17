import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';
import 'package:pos_billingwala_v2/core/network/api_response.dart';
import 'package:pos_billingwala_v2/features/auth/data/device_identity_service.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/home/data/home_sales_api.dart';
import 'package:pos_billingwala_v2/features/home/domain/home_sales_providers.dart';
import 'package:pos_billingwala_v2/features/inventory/domain/inventory_providers.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/mess/data/mess_api.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_dtos.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_job_dispatcher.dart';
import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';
import 'package:pos_billingwala_v2/features/staff/data/staff_api.dart';
import 'package:pos_billingwala_v2/features/staff/domain/permission_catalog.dart';
import 'package:pos_billingwala_v2/features/support/data/support_api.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_cache.dart';
import 'package:pos_billingwala_v2/features/tables/domain/tables_providers.dart';

/* Result of prefetching API-only screen payloads during Fetch from Cloud. */
class CloudScreenPrefetchResult {
  const CloudScreenPrefetchResult({
    required this.counts,
    this.failed = 0,
  });

  final Map<String, int> counts;
  final int failed;

  int get totalRows =>
      counts.values.where((c) => c > 0).fold(0, (a, b) => a + b);
}

/* Pulls every remaining screen API into [CloudScreenCache]. */
abstract final class CloudScreenPrefetch {
  CloudScreenPrefetch._();

  static Future<CloudScreenPrefetchResult> run(
    Ref ref, {
    required String userId,
  }) async {
    final counts = <String, int>{};
    var failed = 0;
    final client = ref.read(apiClientProvider);
    final messApi = MessApi(client);
    final staffApi = StaffApi(client);
    final supportApi = SupportApi(client);
    final printerApi = ref.read(storePrinterApiProvider);
    final homeApi = HomeSalesApi(client);

    Future<void> saveList(String key, List<dynamic> rows) async {
      await CloudScreenCache.saveJson(key, rows);
      counts[key] = rows.length;
    }

    Future<void> saveMap(String key, Map<String, dynamic>? row) async {
      await CloudScreenCache.saveJson(key, row);
      counts[key] = row == null || row.isEmpty ? 0 : 1;
    }

    try {
      final members = await staffApi.list(userId);
      await saveList(
        CloudScreenCache.staff,
        members
            .map(
              (e) => {
                'id': e.id,
                'name': e.name,
                'mobileNumber': e.mobileNumber,
                'role': e.role,
                'roleLabel': e.roleLabel,
                'address': e.address,
                'profileImage': e.profileImage,
                'status': e.status,
                'monthlySalary': e.monthlySalary,
                'lastLoginAt': e.lastLoginAt,
                'effectivePermissions': e.effectivePermissions,
                'permissionOverrides': e.permissionOverrides,
              },
            )
            .toList(),
      );
    } catch (_) {
      failed++;
      counts[CloudScreenCache.staff] = 0;
    }

    try {
      final monthKey = DateFormat('yyyy-MM').format(DateTime.now());
      final response = await client.dio.post<dynamic>(
        ApiEndpoints.getSalaryList,
        data: {'userId': userId, 'salaryMonth': monthKey},
        options: Options(contentType: Headers.formUrlEncodedContentType),
      );
      final data = asJsonMap(response.data);
      final raw = data['salaryResponse'];
      final rows = raw is List
          ? raw
              .whereType<Map>()
              .map((e) => Map<String, dynamic>.from(e))
              .toList()
          : <Map<String, dynamic>>[];
      await saveList(CloudScreenCache.salary, rows);
    } catch (_) {
      failed++;
      counts[CloudScreenCache.salary] = 0;
    }

    try {
      final defaults = <String, dynamic>{};
      for (final role in posFixedRoles.keys) {
        try {
          final data = await staffApi.roleDefaults(userId, role);
          defaults[role] = data;
        } catch (_) {
          /* one role failing should not abort the rest */
        }
      }
      await CloudScreenCache.saveJson(CloudScreenCache.roleDefaults, defaults);
      counts[CloudScreenCache.roleDefaults] = defaults.length;
      if (defaults.isEmpty) failed++;
    } catch (_) {
      failed++;
      counts[CloudScreenCache.roleDefaults] = 0;
    }

    try {
      final sessions = await messApi.fetchMealSessions(userId);
      await saveList(
        CloudScreenCache.mealSessions,
        sessions
            .map(
              (e) => {
                'sessionId': e.sessionId,
                'sessionName': e.sessionName,
                'startTime': e.startTime,
                'endTime': e.endTime,
                'tokenPrefix': e.tokenPrefix,
                'isActive': e.isActive,
                'menuNotes': e.menuNotes,
                'sortOrder': e.sortOrder,
              },
            )
            .toList(),
      );
    } catch (_) {
      failed++;
      counts[CloudScreenCache.mealSessions] = 0;
    }

    Map<String, dynamic> mealTokenMap(MessMealTokenDto e) => {
          'tokenId': e.tokenId,
          'tokenNumber': e.tokenNumber,
          'registrationNo': e.registrationNo,
          'mealSession': e.mealSession,
          'date': e.date,
          'printStatus': e.printStatus,
          'createdAt': e.createdAt,
          'printedAt': e.printedAt,
          'memberName': e.memberName,
          'memberMobile': e.memberMobile,
        };

    try {
      final today = await messApi.fetchMealTokensToday(userId);
      await saveList(
        CloudScreenCache.mealTokensToday,
        today.tokens.map(mealTokenMap).toList(),
      );
    } catch (_) {
      failed++;
      counts[CloudScreenCache.mealTokensToday] = 0;
    }

    try {
      final device = await DeviceIdentityService().resolve();
      final pending = await messApi.fetchPendingMealTokens(
        userId: userId,
        deviceId: device.deviceId,
      );
      await saveList(
        CloudScreenCache.pendingMealTokens,
        pending.map(mealTokenMap).toList(),
      );
    } catch (_) {
      failed++;
      counts[CloudScreenCache.pendingMealTokens] = 0;
    }

    try {
      final mode = await messApi.fetchShopPayerMode(userId);
      await CloudScreenCache.saveJson(
        CloudScreenCache.messShopPayerMode,
        mode,
      );
      counts[CloudScreenCache.messShopPayerMode] =
          mode == null || mode.trim().isEmpty ? 0 : 1;
    } catch (_) {
      failed++;
      counts[CloudScreenCache.messShopPayerMode] = 0;
    }

    try {
      final qr = await messApi.fetchCommonQr(userId);
      await saveMap(
        CloudScreenCache.messCommonQr,
        qr == null
            ? null
            : {
                'publicToken': qr.publicToken,
                'qrUrl': qr.qrUrl,
                'messLabel': qr.messLabel,
                'branchLabel': qr.branchLabel,
                'status': qr.status,
              },
      );
    } catch (_) {
      failed++;
      counts[CloudScreenCache.messCommonQr] = 0;
    }

    try {
      final printers = await printerApi.list(userId);
      await saveList(
        CloudScreenCache.storePrinters,
        printers
            .map(
              (e) => {
                'id': e.id,
                'printerName': e.printerName,
                'connectionType': e.connectionType,
                'ipAddress': e.ipAddress,
                'port': e.port,
                'bluetoothAddress': e.bluetoothAddress,
                'usbIdentifier': e.usbIdentifier,
                'usbName': e.usbName,
                'paperSize': e.paperSize,
                'purpose': e.purpose,
                'area': e.area,
                'status': e.status,
                'enabled': e.enabled ? '1' : '0',
                'isDefault': e.isDefault ? '1' : '0',
                'isBackup': e.isBackup ? '1' : '0',
                'primaryPrinterId': e.primaryPrinterId,
                'deviceId': e.deviceId,
              },
            )
            .toList(),
      );
    } catch (_) {
      failed++;
      counts[CloudScreenCache.storePrinters] = 0;
    }

    try {
      final routes = await printerApi.routes(userId);
      await saveList(
        CloudScreenCache.printerRoutes,
        routes.map((e) => e.toJson()).toList(),
      );
    } catch (_) {
      failed++;
      counts[CloudScreenCache.printerRoutes] = 0;
    }

    try {
      final jobs = await printerApi.queue(userId);
      await saveList(CloudScreenCache.printJobs, jobs);
    } catch (_) {
      failed++;
      counts[CloudScreenCache.printJobs] = 0;
    }

    try {
      final tickets = await supportApi.getSupportTickets(userId);
      await saveList(
        CloudScreenCache.supportTickets,
        tickets
            .map(
              (e) => {
                'id': e.id,
                'ticketNo': e.ticketNo,
                'appName': e.appName,
                'category': e.category,
                'subject': e.subject,
                'description': e.description,
                'status': e.status,
                'createdAt': e.createdAt,
                'shopName': e.shopName,
              },
            )
            .toList(),
      );
    } catch (_) {
      failed++;
      counts[CloudScreenCache.supportTickets] = 0;
    }

    try {
      final response = await client.dio.post<dynamic>(
        ApiEndpoints.getPosDeviceList,
        data: {'userId': userId},
        options: Options(contentType: Headers.formUrlEncodedContentType),
      );
      final data = asJsonMap(response.data);
      final raw = data['deviceResponse'];
      final rows = raw is List
          ? raw
              .whereType<Map>()
              .map((e) => Map<String, dynamic>.from(e))
              .toList()
          : <Map<String, dynamic>>[];
      await saveList(CloudScreenCache.posDevices, rows);
    } catch (_) {
      failed++;
      counts[CloudScreenCache.posDevices] = 0;
    }

    try {
      final today =
          await homeApi.fetchOverview(userId: userId, period: 'today');
      await saveMap(
        CloudScreenCache.homeOverviewToday,
        today == null
            ? null
            : {
                'primarySales': today.primarySales,
                'todaySales': today.todaySales,
                'monthSales': today.monthSales,
                'allTimeSales': today.allTimeSales,
                'primarySalesTrend': today.primarySalesTrend,
                'todaySalesTrend': today.todaySalesTrend,
                'totalSubcategory': today.totalSubcategory,
                'totalProduct': today.totalProduct,
                'totalCombo': today.totalCombo,
                'period': today.period,
                'primarySalesLabel': today.primarySalesLabel,
                'status': 'true',
              },
      );
    } catch (_) {
      failed++;
      counts[CloudScreenCache.homeOverviewToday] = 0;
    }

    try {
      final month =
          await homeApi.fetchOverview(userId: userId, period: 'month');
      await saveMap(
        CloudScreenCache.homeOverviewMonth,
        month == null
            ? null
            : {
                'primarySales': month.primarySales,
                'todaySales': month.todaySales,
                'monthSales': month.monthSales,
                'allTimeSales': month.allTimeSales,
                'primarySalesTrend': month.primarySalesTrend,
                'todaySalesTrend': month.todaySalesTrend,
                'totalSubcategory': month.totalSubcategory,
                'totalProduct': month.totalProduct,
                'totalCombo': month.totalCombo,
                'period': month.period,
                'primarySalesLabel': month.primarySalesLabel,
                'status': 'true',
              },
      );
    } catch (_) {
      failed++;
      counts[CloudScreenCache.homeOverviewMonth] = 0;
    }

    return CloudScreenPrefetchResult(counts: counts, failed: failed);
  }
}

/* Refresh Riverpod consumers after local DB + screen cache were replaced. */
void invalidateAfterCloudFetch(Ref ref) {
  ref.invalidate(categoriesProvider);
  ref.invalidate(subcategoriesProvider);
  ref.invalidate(foodTypesProvider);
  ref.invalidate(diningAreasProvider);
  ref.invalidate(tableTypesProvider);
  ref.invalidate(portionMastersProvider);
  ref.invalidate(productsProvider);
  ref.invalidate(allProductsProvider);
  ref.invalidate(combosListProvider);
  ref.invalidate(catalogCountsProvider);
  ref.invalidate(homeSalesOverviewProvider);
  ref.invalidate(allTimeInvoicesProvider);
  ref.invalidate(todayInvoicesProvider);
  ref.invalidate(yesterdayInvoicesProvider);
  ref.invalidate(monthInvoicesProvider);
  ref.invalidate(periodInvoicesProvider);
  ref.invalidate(shopReceiptProfileProvider);
  ref.invalidate(inventoryMovementsProvider);
  ref.invalidate(expensesProvider);
  ref.invalidate(messMembersProvider);
  ref.invalidate(todayMessTokensProvider);
  ref.invalidate(messCommonQrProvider);
  ref.invalidate(posTablesProvider);
  ref.invalidate(openDiningSessionsProvider);
  ref.invalidate(floorTablesProvider);
  ref.read(authControllerProvider);
}
