import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/mess/data/mess_api.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_payer_mode.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_job_dispatcher.dart';
import 'package:pos_billingwala_v2/features/print/domain/store_printer.dart';
import 'package:pos_billingwala_v2/features/staff/data/staff_api.dart';
import 'package:pos_billingwala_v2/features/staff/data/staff_offline_queue.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_cache.dart';

/* Result of pushing API-only / prefs screen state during Sync to Server. */
class CloudScreenPushResult {
  const CloudScreenPushResult({
    required this.counts,
    required this.stepOk,
    this.failed = 0,
  });

  final Map<String, int> counts;
  /* Sync progress step id → whether that step succeeded (empty = success). */
  final Map<String, bool> stepOk;
  final int failed;

  int get totalRows =>
      counts.values.where((c) => c > 0).fold(0, (a, b) => a + b);
}

/* Pushes local screen prefs + cloud-screen cache back to the server. */
abstract final class CloudScreenPush {
  CloudScreenPush._();

  static Future<CloudScreenPushResult> run(
    Ref ref, {
    required String userId,
  }) async {
    final counts = <String, int>{};
    final stepOk = <String, bool>{};
    var failed = 0;
    final client = ref.read(apiClientProvider);
    final messApi = MessApi(client);
    final printerApi = ref.read(storePrinterApiProvider);

    try {
      final mode = await MessPayerMode.get();
      final ok = await messApi.saveShopPayerMode(
        userId: userId,
        payerMode: mode,
      );
      if (ok) {
        await CloudScreenCache.saveJson(
          CloudScreenCache.messShopPayerMode,
          mode,
        );
        counts[CloudScreenCache.messShopPayerMode] = 1;
        stepOk['mess_shop_payer'] = true;
        AppLogger.info('Sync↑ mess_shop_payer OK mode=$mode');
      } else {
        failed++;
        counts[CloudScreenCache.messShopPayerMode] = 0;
        stepOk['mess_shop_payer'] = false;
        AppLogger.warning('Sync↑ mess_shop_payer FAIL mode=$mode');
      }
    } catch (e) {
      failed++;
      counts[CloudScreenCache.messShopPayerMode] = 0;
      stepOk['mess_shop_payer'] = false;
      AppLogger.error('Sync↑ mess_shop_payer exception', e);
    }

    try {
      var sessions = await CloudScreenCache.loadMapList(
        CloudScreenCache.mealSessions,
      );
      /* If cache empty, pull cloud first so sync has something meaningful. */
      if (sessions.isEmpty) {
        try {
          final cloud = await messApi.fetchMealSessions(userId);
          if (cloud.isNotEmpty) {
            sessions = cloud
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
                .toList();
            await CloudScreenCache.saveJson(
              CloudScreenCache.mealSessions,
              sessions,
            );
          }
        } catch (e) {
          AppLogger.warning('Sync↑ meal_sessions prefetch skip: $e');
        }
      }

      if (sessions.isEmpty) {
        counts[CloudScreenCache.mealSessions] = 0;
        stepOk['meal_sessions'] = true;
        AppLogger.info('Sync↑ meal_sessions OK (nothing to upload)');
      } else {
        var saved = 0;
        var rowFailed = 0;
        for (final row in sessions) {
          final rawId = row['sessionId']?.toString().trim() ?? '';
          /* Only numeric server ids; sess_* local placeholders → insert. */
          final sessionId = int.tryParse(rawId) != null ? rawId : '';
          final sessionName = row['sessionName']?.toString() ?? '';
          final startTime = row['startTime']?.toString() ?? '';
          final endTime = row['endTime']?.toString() ?? '';
          if (sessionName.trim().isEmpty) continue;
          if (startTime.trim().isEmpty || endTime.trim().isEmpty) continue;
          final ok = await messApi.saveMealSession(
            userId: userId,
            sessionId: sessionId,
            sessionName: sessionName,
            startTime: startTime,
            endTime: endTime,
            tokenPrefix: row['tokenPrefix']?.toString() ?? '',
            isActive: row['isActive']?.toString() ?? '1',
            menuNotes: row['menuNotes']?.toString() ?? '',
            sortOrder: row['sortOrder']?.toString() ?? '0',
          );
          if (ok) {
            saved++;
          } else {
            rowFailed++;
            AppLogger.warning(
              'Sync↑ meal_session FAIL name=$sessionName id=$rawId',
            );
          }
        }
        counts[CloudScreenCache.mealSessions] = saved;
        if (rowFailed > 0) {
          failed += rowFailed;
          stepOk['meal_sessions'] = false;
        } else {
          stepOk['meal_sessions'] = true;
          AppLogger.info('Sync↑ meal_sessions OK saved=$saved');
          /* Refresh cache so sess_* placeholders become server ids. */
          try {
            final cloud = await messApi.fetchMealSessions(userId);
            if (cloud.isNotEmpty) {
              await CloudScreenCache.saveJson(
                CloudScreenCache.mealSessions,
                cloud
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
            }
            await StaffOfflineQueue.setMealSessionsPending(false);
          } catch (e) {
            AppLogger.warning('Sync↑ meal_sessions refresh skip: $e');
            if (saved > 0) {
              await StaffOfflineQueue.setMealSessionsPending(false);
            }
          }
        }
      }
    } catch (e) {
      failed++;
      counts[CloudScreenCache.mealSessions] = 0;
      stepOk['meal_sessions'] = false;
      AppLogger.error('Sync↑ meal_sessions exception', e);
    }

    try {
      final staffApi = StaffApi(client);
      final flush = await StaffOfflineQueue.flush(staffApi, userId);
      counts[CloudScreenCache.staff] = flush.saved;
      if (flush.failed > 0) {
        failed += flush.failed;
        stepOk['staff'] = false;
        AppLogger.warning(
          'Sync↑ staff FAIL saved=${flush.saved} failed=${flush.failed}',
        );
      } else {
        stepOk['staff'] = true;
        AppLogger.info('Sync↑ staff OK saved=${flush.saved}');
      }
      /* Salary ops are in the same queue; surface a salary step for progress. */
      counts[CloudScreenCache.salary] = flush.saved > 0 ? flush.saved : 0;
      stepOk['salary'] = flush.failed == 0;
    } catch (e) {
      failed++;
      counts[CloudScreenCache.staff] = 0;
      counts[CloudScreenCache.salary] = 0;
      stepOk['staff'] = false;
      stepOk['salary'] = false;
      AppLogger.error('Sync↑ staff/salary exception', e);
    }

    try {
      final printers = await CloudScreenCache.loadMapList(
        CloudScreenCache.storePrinters,
      );
      if (printers.isEmpty) {
        counts[CloudScreenCache.storePrinters] = 0;
        stepOk['store_printers'] = true;
      } else {
        var saved = 0;
        var rowFailed = 0;
        for (final row in printers) {
          final id = row['id']?.toString().trim() ?? '';
          final name = row['printerName']?.toString().trim() ?? '';
          if (name.isEmpty) continue;
          try {
            await printerApi.save(userId, {
              'printerName': name,
              'connectionType': row['connectionType']?.toString() ?? 'BT',
              'ipAddress': row['ipAddress']?.toString() ?? '',
              'port': row['port']?.toString() ?? '9100',
              'bluetoothAddress': row['bluetoothAddress']?.toString() ?? '',
              'usbIdentifier': row['usbIdentifier']?.toString() ?? '',
              'usbName': row['usbName']?.toString() ?? '',
              'paperSize': row['paperSize']?.toString() ?? '2-Inch',
              'purpose': row['purpose']?.toString() ?? 'KOT',
              'area': row['area']?.toString() ?? 'KITCHEN',
              'deviceId': row['deviceId']?.toString() ?? '',
              'enabled': row['enabled']?.toString() ?? '1',
              'isDefault': row['isDefault']?.toString() ?? '0',
              'isBackup': row['isBackup']?.toString() ?? '0',
              'primaryPrinterId': row['primaryPrinterId']?.toString() ?? '',
            }, id: id.isEmpty ? null : id);
            saved++;
          } catch (e) {
            rowFailed++;
            AppLogger.warning('Sync↑ store_printer FAIL $name: $e');
          }
        }
        counts[CloudScreenCache.storePrinters] = saved;
        if (rowFailed > 0) {
          failed += rowFailed;
          stepOk['store_printers'] = false;
        } else {
          stepOk['store_printers'] = true;
        }
      }
    } catch (e) {
      failed++;
      counts[CloudScreenCache.storePrinters] = 0;
      stepOk['store_printers'] = false;
      AppLogger.error('Sync↑ store_printers exception', e);
    }

    try {
      final rawRoutes = await CloudScreenCache.loadMapList(
        CloudScreenCache.printerRoutes,
      );
      final routes = rawRoutes
          .map(PrinterRouteRule.fromJson)
          .where((e) => e.printerId.trim().isNotEmpty)
          .toList();
      if (routes.isEmpty) {
        counts[CloudScreenCache.printerRoutes] = 0;
        stepOk['printer_routes'] = true;
      } else {
        await printerApi.saveRoutes(userId, routes);
        counts[CloudScreenCache.printerRoutes] = routes.length;
        stepOk['printer_routes'] = true;
      }
    } catch (e) {
      failed++;
      counts[CloudScreenCache.printerRoutes] = 0;
      stepOk['printer_routes'] = false;
      AppLogger.error('Sync↑ printer_routes exception', e);
    }

    return CloudScreenPushResult(
      counts: counts,
      stepOk: stepOk,
      failed: failed,
    );
  }
}
