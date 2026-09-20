import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
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
import 'package:pos_billingwala_v2/features/mess/domain/mess_payer_mode.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_job_dispatcher.dart';
import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';
import 'package:pos_billingwala_v2/features/staff/data/staff_api.dart';
import 'package:pos_billingwala_v2/features/staff/data/staff_offline_queue.dart';
import 'package:pos_billingwala_v2/features/staff/domain/permission_catalog.dart';
import 'package:pos_billingwala_v2/features/support/data/support_api.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_cache.dart';
import 'package:pos_billingwala_v2/features/tables/domain/tables_providers.dart';

/* Result of prefetching API-only screen payloads during Fetch from Cloud. */
class CloudScreenPrefetchResult {
  const CloudScreenPrefetchResult({
    required this.counts,
    required this.stepOk,
    this.failed = 0,
  });

  final Map<String, int> counts;
  /* Sync progress step id → whether that step succeeded. */
  final Map<String, bool> stepOk;
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
    final stepOk = <String, bool>{};
    var failed = 0;
    final client = ref.read(apiClientProvider);
    final messApi = MessApi(client);
    final staffApi = StaffApi(client);
    final supportApi = SupportApi(client);
    final printerApi = ref.read(storePrinterApiProvider);
    final homeApi = HomeSalesApi(client);

    void markStep(String stepId, bool ok) {
      stepOk[stepId] = ok;
      if (!ok) failed++;
    }

    Future<void> saveList(String key, List<dynamic> rows) async {
      await CloudScreenCache.saveJson(key, rows);
      counts[key] = rows.length;
    }

    Future<void> saveMap(String key, Map<String, dynamic>? row) async {
      await CloudScreenCache.saveJson(key, row);
      counts[key] = row == null || row.isEmpty ? 0 : 1;
    }

    try {
      if (await StaffOfflineQueue.hasPendingOps()) {
        final cached = await CloudScreenCache.loadMapList(CloudScreenCache.staff);
        counts[CloudScreenCache.staff] = cached.length;
        markStep('staff', true);
        AppLogger.info('Sync↓ staff skip (pending upload)');
      } else {
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
        markStep('staff', true);
      }
    } catch (_) {
      counts[CloudScreenCache.staff] = 0;
      markStep('staff', false);
    }

    try {
      if (await StaffOfflineQueue.hasPendingOps()) {
        final cached = await CloudScreenCache.loadMapList(
          CloudScreenCache.salary,
        );
        counts[CloudScreenCache.salary] = cached.length;
        markStep('salary', true);
      } else {
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
        markStep('salary', true);
      }
    } catch (_) {
      counts[CloudScreenCache.salary] = 0;
      markStep('salary', false);
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
      markStep('role_defaults', defaults.isNotEmpty);
    } catch (_) {
      counts[CloudScreenCache.roleDefaults] = 0;
      markStep('role_defaults', false);
    }

    try {
      if (await StaffOfflineQueue.isMealSessionsPending()) {
        final cached = await CloudScreenCache.loadMapList(
          CloudScreenCache.mealSessions,
        );
        counts[CloudScreenCache.mealSessions] = cached.length;
        markStep('meal_sessions', true);
      } else {
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
        markStep('meal_sessions', true);
      }
    } catch (_) {
      final cached = await CloudScreenCache.loadMapList(
        CloudScreenCache.mealSessions,
      );
      counts[CloudScreenCache.mealSessions] = cached.length;
      /* Soft Complete — upload already pushed; GET flake shouldn't fail UI. */
      markStep('meal_sessions', true);
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
      markStep('meal_tokens', true);
    } catch (_) {
      counts[CloudScreenCache.mealTokensToday] = 0;
      markStep('meal_tokens', false);
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
      markStep('pending_meal_tokens', true);
    } catch (_) {
      counts[CloudScreenCache.pendingMealTokens] = 0;
      markStep('pending_meal_tokens', true);
    }

    try {
      final mode = await messApi.fetchShopPayerMode(userId);
      final normalized = MessPayerMode.normalize(mode);
      await MessPayerMode.setLocal(normalized);
      await CloudScreenCache.saveJson(
        CloudScreenCache.messShopPayerMode,
        normalized,
      );
      counts[CloudScreenCache.messShopPayerMode] = 1;
      markStep('mess_shop_payer', true);
    } catch (_) {
      final local = await MessPayerMode.get();
      await CloudScreenCache.saveJson(
        CloudScreenCache.messShopPayerMode,
        local,
      );
      counts[CloudScreenCache.messShopPayerMode] = 1;
      markStep('mess_shop_payer', true);
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
      markStep('mess_common_qr', true);
    } catch (_) {
      counts[CloudScreenCache.messCommonQr] = 0;
      markStep('mess_common_qr', false);
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
      markStep('store_printers', true);
    } catch (_) {
      counts[CloudScreenCache.storePrinters] = 0;
      markStep('store_printers', false);
    }

    try {
      final routes = await printerApi.routes(userId);
      await saveList(
        CloudScreenCache.printerRoutes,
        routes.map((e) => e.toJson()).toList(),
      );
      markStep('printer_routes', true);
    } catch (_) {
      counts[CloudScreenCache.printerRoutes] = 0;
      markStep('printer_routes', false);
    }

    try {
      final jobs = await printerApi.queue(userId);
      await saveList(CloudScreenCache.printJobs, jobs);
      markStep('print_jobs', true);
    } catch (_) {
      counts[CloudScreenCache.printJobs] = 0;
      markStep('print_jobs', false);
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
      markStep('support_tickets', true);
    } catch (_) {
      counts[CloudScreenCache.supportTickets] = 0;
      markStep('support_tickets', false);
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
      markStep('pos_devices', true);
    } catch (_) {
      counts[CloudScreenCache.posDevices] = 0;
      markStep('pos_devices', false);
    }

    var homeOk = true;
    try {
      final today = await homeApi.fetchOverview(
        userId: userId,
        period: 'today',
      );
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
      counts[CloudScreenCache.homeOverviewToday] = 0;
      homeOk = false;
    }

    try {
      final month = await homeApi.fetchOverview(
        userId: userId,
        period: 'month',
      );
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
      counts[CloudScreenCache.homeOverviewMonth] = 0;
      homeOk = false;
    }
    markStep('home_overview', homeOk);

    return CloudScreenPrefetchResult(
      counts: counts,
      stepOk: stepOk,
      failed: failed,
    );
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
  ref.invalidate(catalogCategoryCountProvider);
  ref.invalidate(catalogSubcategoryCountProvider);
  ref.invalidate(catalogProductCountProvider);
  ref.invalidate(catalogComboCountProvider);
  ref.invalidate(homeSalesOverviewProvider);
  ref.invalidate(allTimeSalesAggregateProvider);
  ref.invalidate(todayInvoicesProvider);
  ref.invalidate(todaySalesAggregateProvider);
  ref.invalidate(yesterdayInvoicesProvider);
  ref.invalidate(yesterdaySalesAggregateProvider);
  ref.invalidate(monthInvoicesProvider);
  ref.invalidate(monthSalesAggregateProvider);
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
