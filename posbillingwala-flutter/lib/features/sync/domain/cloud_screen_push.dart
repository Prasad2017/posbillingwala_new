import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/mess/data/mess_api.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_payer_mode.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_job_dispatcher.dart';
import 'package:pos_billingwala_v2/features/print/domain/store_printer.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_cache.dart';

/* Result of pushing API-only / prefs screen state during Sync to Server. */
class CloudScreenPushResult {
  const CloudScreenPushResult({
    required this.counts,
    this.failed = 0,
  });

  final Map<String, int> counts;
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
      } else {
        failed++;
        counts[CloudScreenCache.messShopPayerMode] = 0;
      }
    } catch (_) {
      failed++;
      counts[CloudScreenCache.messShopPayerMode] = 0;
    }

    try {
      final sessions =
          await CloudScreenCache.loadMapList(CloudScreenCache.mealSessions);
      var saved = 0;
      for (final row in sessions) {
        final sessionId = row['sessionId']?.toString() ?? '';
        final sessionName = row['sessionName']?.toString() ?? '';
        if (sessionName.trim().isEmpty) continue;
        final ok = await messApi.saveMealSession(
          userId: userId,
          sessionId: sessionId,
          sessionName: sessionName,
          startTime: row['startTime']?.toString() ?? '',
          endTime: row['endTime']?.toString() ?? '',
          tokenPrefix: row['tokenPrefix']?.toString() ?? '',
          isActive: row['isActive']?.toString() ?? '1',
          menuNotes: row['menuNotes']?.toString() ?? '',
          sortOrder: row['sortOrder']?.toString() ?? '0',
        );
        if (ok) {
          saved++;
        } else {
          failed++;
        }
      }
      counts[CloudScreenCache.mealSessions] = saved;
    } catch (_) {
      failed++;
      counts[CloudScreenCache.mealSessions] = 0;
    }

    try {
      final printers =
          await CloudScreenCache.loadMapList(CloudScreenCache.storePrinters);
      var saved = 0;
      for (final row in printers) {
        final id = row['id']?.toString().trim() ?? '';
        final name = row['printerName']?.toString().trim() ?? '';
        if (name.isEmpty) continue;
        try {
          await printerApi.save(
            userId,
            {
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
            },
            id: id.isEmpty ? null : id,
          );
          saved++;
        } catch (_) {
          failed++;
        }
      }
      counts[CloudScreenCache.storePrinters] = saved;
    } catch (_) {
      failed++;
      counts[CloudScreenCache.storePrinters] = 0;
    }

    try {
      final rawRoutes =
          await CloudScreenCache.loadMapList(CloudScreenCache.printerRoutes);
      final routes = rawRoutes
          .map(PrinterRouteRule.fromJson)
          .where((e) => e.printerId.trim().isNotEmpty)
          .toList();
      if (routes.isNotEmpty) {
        await printerApi.saveRoutes(userId, routes);
        counts[CloudScreenCache.printerRoutes] = routes.length;
      } else {
        counts[CloudScreenCache.printerRoutes] = 0;
      }
    } catch (_) {
      failed++;
      counts[CloudScreenCache.printerRoutes] = 0;
    }

    return CloudScreenPushResult(counts: counts, failed: failed);
  }
}
