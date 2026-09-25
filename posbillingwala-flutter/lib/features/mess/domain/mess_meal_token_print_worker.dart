import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/auth/data/device_identity_service.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/mess/data/mess_api.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_slip_builder.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_service.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';

/* Android MessMealTokenPrintWorker — single-flight drain of local print queue. */
class MessMealTokenPrintWorker {
  MessMealTokenPrintWorker(this.ref);

  final Ref ref;
  Timer? timer;
  bool running = false;

  void start() {
    timer?.cancel();
    if (AppPlatform.isWeb && !AppPlatform.supportsBluetoothPrint) {
      /* Web still recovers/acks when a printer host is available. */
    }
    timer = Timer.periodic(const Duration(seconds: 6), (_) => kick());
    kick();
  }

  void stop() {
    timer?.cancel();
    timer = null;
  }

  Future<void> kick() async {
    if (running) return;
    final auth = ref.read(authControllerProvider);
    if (auth.status != AuthStatus.authenticated || auth.session == null) {
      return;
    }
    running = true;
    try {
      final db = ref.read(appDatabaseProvider);
      final api = MessApi(ref.read(apiClientProvider));
      final printService = ref.read(printServiceProvider);
      final userId = auth.session!.userId;
      final device = await DeviceIdentityService().resolve();
      final width = ref.read(printerSettingsProvider).charsPerLine;

      while (true) {
        final job = await db.getNextMessMealPrintJob();
        if (job == null) break;

        await db.updateMessMealTokenQueueStatus(
          id: job.id,
          printStatus: 'PRINTING',
        );

        final layout = MessSlipBuilder.mealTokenLayout(
          tokenNumber: job.tokenNumber ?? '',
          memberName: job.memberName ?? '',
          registrationNo: job.registrationNo ?? '',
          mealSession: job.mealSession ?? '',
          tokenDate: job.tokenDate ?? '',
          createdAt: job.createdAt,
        );

        var printed = false;
        try {
          final result = await printService.printMessSlip(
            layout.toPlainText(width: width),
            layout: layout,
            label: 'Mess meal token',
            includeLogo: false,
          );
          printed =
              result.outcome != PrintOutcome.failed &&
              result.outcome != PrintOutcome.previewOnly;
        } catch (e) {
          AppLogger.error('Mess meal token print failed', e);
          printed = false;
        }

        if (printed) {
          await db.markMessMealTokenPrinted(job.id);
          try {
            await api.ackMealTokenPrint(
              userId: userId,
              tokenId: job.serverPublicId,
              result: 'SUCCESS',
              deviceId: device.deviceId,
              deviceName: device.deviceName,
            );
          } catch (_) {}
        } else {
          await db.updateMessMealTokenQueueStatus(
            id: job.id,
            printStatus: 'PRINT_FAILED',
          );
          try {
            await api.ackMealTokenPrint(
              userId: userId,
              tokenId: job.serverPublicId,
              result: 'FAILED',
              deviceId: device.deviceId,
              deviceName: device.deviceName,
            );
          } catch (_) {}
          /* Match Android: stop drain when printer unavailable. */
          break;
        }
      }
    } catch (e) {
      AppLogger.error('Mess meal token drain', e);
    } finally {
      running = false;
    }
  }
}

final messMealTokenPrintWorkerProvider = Provider<MessMealTokenPrintWorker>((
  ref,
) {
  final worker = MessMealTokenPrintWorker(ref);
  ref.onDispose(worker.stop);
  return worker;
});
