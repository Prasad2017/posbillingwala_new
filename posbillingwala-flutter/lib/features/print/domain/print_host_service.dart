import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/auth/data/device_identity_service.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_job_dispatcher.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_service.dart';
import 'package:pos_billingwala_v2/features/print/domain/store_printer.dart';

class PrintHostController {
  PrintHostController(this.ref);

  final Ref ref;
  Timer? timer;
  bool running = false;

  void start() {
    timer?.cancel();
    if (kIsWeb && !AppPlatform.supportsBluetoothPrint) {
      /* Web can still claim network jobs if a host IP is configured. */
    }
    timer = Timer.periodic(const Duration(seconds: 8), (_) => drain());
    drain();
  }

  void stop() {
    timer?.cancel();
    timer = null;
  }

  Future<void> drain() async {
    if (running) return;
    final auth = ref.read(authControllerProvider);
    if (auth.status != AuthStatus.authenticated || auth.session == null) return;
    running = true;
    try {
      final device = await DeviceIdentityService().resolve();
      final api = ref.read(storePrinterApiProvider);
      await api.heartbeat(
        auth.session!.licenceUserId,
        device.deviceId,
        kIsWeb
            ? 'WEB'
            : (defaultTargetPlatform == TargetPlatform.iOS ? 'IOS' : 'ANDROID'),
      );
      final jobs = await api.claim(
        auth.session!.licenceUserId,
        device.deviceId,
      );
      final service = ref.read(printServiceProvider);
      for (final job in jobs) {
        final id = job['id']?.toString() ?? '';
        if (id.isEmpty) continue;
        await api.ack(auth.session!.licenceUserId, id, 'PRINT_STARTED');
        final printerJson = job['printer'];
        StorePrinter? printer;
        if (printerJson is Map) {
          printer = StorePrinter.fromJson(
            Map<String, dynamic>.from(printerJson),
          );
        }
        final payload = job['payload']?.toString() ?? '';
        String text = payload;
        try {
          final decoded = jsonDecode(payload);
          if (decoded is Map && decoded['text'] != null) {
            text = decoded['text'].toString();
          }
        } catch (_) {}
        var ok = false;
        if (printer != null && !kIsWeb) {
          final builder = receiptBuilderFor(service, printer);
          final bytes = await builder.rawPrintBytes(text);
          final result = await service.dispatchToEndpoint(
            text: text,
            bytes: bytes,
            label: job['documentType']?.toString() ?? 'Print',
            transport: transportOf(printer.connectionType),
            bluetoothAddress: printer.bluetoothAddress,
            usbIdentifier: printer.usbIdentifier,
            usbName: printer.usbName,
            networkHost: printer.ipAddress,
            networkPort: printer.port,
          );
          ok = result.outcome != PrintOutcome.failed;
        }
        await api.ack(
          auth.session!.licenceUserId,
          id,
          ok ? 'PRINTED' : 'FAILED',
          error: ok ? '' : 'Print host could not reach printer',
        );
      }
    } catch (e) {
      AppLogger.error('Print host drain', e);
    } finally {
      running = false;
    }
  }
}

final printHostControllerProvider = Provider<PrintHostController>((ref) {
  final controller = PrintHostController(ref);
  ref.onDispose(controller.stop);
  return controller;
});
