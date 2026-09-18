import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/auth/data/device_identity_service.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/print/data/store_printer_api.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_service.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/receipt_builder.dart';
import 'package:pos_billingwala_v2/features/print/domain/store_printer.dart';

final storePrinterApiProvider = Provider<StorePrinterApi>((ref) {
  return StorePrinterApi(ref.watch(apiClientProvider));
});

PosPrinterTransport transportOf(String connectionType) {
  switch (connectionType.toUpperCase()) {
    case 'USB':
      return PosPrinterTransport.usb;
    case 'WIFI':
    case 'NETWORK':
      return PosPrinterTransport.network;
    default:
      return PosPrinterTransport.bluetooth;
  }
}

ReceiptBuilder receiptBuilderFor(PrintService service, StorePrinter printer) {
  return ReceiptBuilder(
    service.settings.copyWith(paperSize: printer.paperSizeEnum),
    shopProfile: service.shopProfile,
    labels: service.labels,
  );
}

class PrintJobDispatcher {
  PrintJobDispatcher(this.ref);

  final WidgetRef ref;

  Future<PrintResult> printKotRouted(KotTicket ticket) async {
    final session = ref.read(authControllerProvider).session;
    final service = ref.read(printServiceProvider);
    if (session == null) {
      return service.printKot(ticket);
    }
    try {
      final api = ref.read(storePrinterApiProvider);
      final printers = await api.list(session.licenceUserId);
      final enabled = printers.where((p) => p.enabled).toList();
      if (enabled.isEmpty) {
        return await service.printKot(ticket);
      }
      final routes = await api.routes(session.licenceUserId);
      final db = ref.read(appDatabaseProvider);
      final grouped = <String, List<KotItem>>{};
      for (final item in ticket.items) {
        final printer = await routeForItem(
          db: db,
          item: item,
          printers: enabled,
          routes: routes,
          documentType: 'KOT',
        );
        grouped.putIfAbsent(printer.id, () => []).add(item);
      }
      PrintResult? last;
      final device = await DeviceIdentityService().resolve();
      for (final entry in grouped.entries) {
        final printer = enabled.firstWhere((p) => p.id == entry.key);
        final subset = KotTicket(
          kot: ticket.kot,
          items: entry.value,
          roundNumber: ticket.roundNumber,
        );
        final builder = receiptBuilderFor(service, printer);
        final text = builder.kotText(subset);
        final bytes = await builder.kotPrintBytes(subset);
        final local =
            printer.deviceId.isEmpty || printer.deviceId == device.deviceId;
        if (local && !kIsWeb) {
          last = await service.dispatchToEndpoint(
            text: text,
            bytes: bytes,
            label: 'KOT',
            transport: transportOf(printer.connectionType),
            bluetoothAddress: printer.bluetoothAddress,
            usbIdentifier: printer.usbIdentifier,
            usbName: printer.usbName,
            networkHost: printer.ipAddress,
            networkPort: printer.port,
          );
          final created = await api.createJob(session.licenceUserId, {
            'printerId': printer.id,
            'documentType': 'KOT',
            'documentId': '${ticket.kot.kotId}',
            'payload': jsonEncode({'text': text}),
            'idempotencyKey': 'kot:${ticket.kot.kotId}:${printer.id}',
            'android_device_id': device.deviceId,
          });
          if (last.outcome != PrintOutcome.failed) {
            final jobId = (created['printJob'] is Map)
                ? created['printJob']['id']?.toString()
                : null;
            if (jobId != null && jobId.isNotEmpty) {
              await api.ack(session.licenceUserId, jobId, 'PRINTED');
            }
          }
        } else {
          await api.createJob(session.licenceUserId, {
            'printerId': printer.id,
            'documentType': 'KOT',
            'documentId': '${ticket.kot.kotId}',
            'payload': jsonEncode({'text': text}),
            'idempotencyKey': 'kot:${ticket.kot.kotId}:${printer.id}',
            'android_device_id': device.deviceId,
          });
          last = PrintResult(
            outcome: PrintOutcome.previewOnly,
            text: text,
            message: AppPlatform.requiresNetwork
                ? 'KOT saved. Remote printer is currently unavailable or will print on the print host.'
                : 'KOT saved locally. Remote printer will print when connection is restored.',
          );
        }
      }
      return await service.printKot(ticket);
    } catch (e) {
      AppLogger.warning('printKotRouted fallback', e);
      return service.printKot(ticket);
    }
  }

  Future<PrintResult> printBillRouted({
    required Invoice invoice,
    required List<InvoiceItem> items,
    String? shopName,
    bool duplicate = false,
  }) async {
    final session = ref.read(authControllerProvider).session;
    final service = ref.read(printServiceProvider);
    if (session == null) {
      return await service.printBill(
        invoice: invoice,
        items: items,
        shopName: shopName,
        duplicate: duplicate,
      );
    }
    try {
      final printers = await ref
          .read(storePrinterApiProvider)
          .list(session.licenceUserId);
      final billPrinters = printers
          .where(
            (p) => p.enabled && (p.purpose == 'BILL' || p.area == 'COUNTER'),
          )
          .toList();
      if (billPrinters.isEmpty) {
        return await service.printBill(
          invoice: invoice,
          items: items,
          shopName: shopName,
          duplicate: duplicate,
        );
      }
      var printer = billPrinters.firstWhere(
        (p) => p.isDefault,
        orElse: () => billPrinters.first,
      );
      final device = await DeviceIdentityService().resolve();
      final builder = receiptBuilderFor(service, printer);
      final text = builder.billText(
        invoice: invoice,
        items: items,
        shopName: shopName,
        duplicate: duplicate,
      );
      final bytes = await builder.billPrintBytes(
        invoice: invoice,
        items: items,
        shopName: shopName,
        duplicate: duplicate,
      );
      final local =
          printer.deviceId.isEmpty || printer.deviceId == device.deviceId;
      if (local && !kIsWeb) {
        return await service.dispatchToEndpoint(
          text: text,
          bytes: bytes,
          label: duplicate ? 'Duplicate bill' : 'Bill',
          transport: transportOf(printer.connectionType),
          bluetoothAddress: printer.bluetoothAddress,
          usbIdentifier: printer.usbIdentifier,
          usbName: printer.usbName,
          networkHost: printer.ipAddress,
          networkPort: printer.port,
        );
      }
      await ref.read(storePrinterApiProvider).createJob(session.licenceUserId, {
        'printerId': printer.id,
        'documentType': 'BILL',
        'documentId': '${invoice.invoiceId}',
        'payload': jsonEncode({'text': text}),
        'idempotencyKey':
            'bill:${invoice.invoiceId}:${printer.id}:${duplicate ? 'dup' : 'orig'}',
        'android_device_id': device.deviceId,
      });
      return PrintResult(
        outcome: PrintOutcome.previewOnly,
        text: text,
        message: 'Bill queued for the counter print host',
      );
    } catch (e) {
      AppLogger.warning('printBillRouted fallback', e);
      return await service.printBill(
        invoice: invoice,
        items: items,
        shopName: shopName,
        duplicate: duplicate,
      );
    }
  }

  Future<StorePrinter> routeForItem({
    required AppDatabase db,
    required KotItem item,
    required List<StorePrinter> printers,
    required List<PrinterRouteRule> routes,
    required String documentType,
  }) async {
    int categoryId = 0;
    String foodType = '';
    if (item.productId != null) {
      final product = await db.getProduct(item.productId!);
      categoryId = product?.categoryId ?? 0;
      if (categoryId > 0) {
        final cats = await db.select(db.productCategories).get();
        for (final cat in cats) {
          if (cat.categoryId == categoryId) {
            foodType = (cat.foodTypeCode ?? '').toLowerCase();
            break;
          }
        }
      }
    }
    PrinterRouteRule? match;
    for (final route in routes) {
      if (route.documentType != documentType) continue;
      if (route.categoryId > 0 && route.categoryId == categoryId) {
        match = route;
        break;
      }
    }
    match ??= routes.cast<PrinterRouteRule?>().firstWhere(
      (r) =>
          r != null &&
          r.documentType == documentType &&
          r.foodTypeCode.isNotEmpty &&
          r.foodTypeCode.toLowerCase() == foodType,
      orElse: () => null,
    );
    if (match != null) {
      for (final printer in printers) {
        if (printer.id == match.printerId) return printer;
      }
    }
    final byPurpose = printers.where((p) => p.purpose == 'KOT');
    if (foodType.contains('beverage') || foodType.contains('drink')) {
      final bar = printers.where((p) => p.area == 'BAR');
      if (bar.isNotEmpty) return bar.first;
    }
    if (byPurpose.isNotEmpty) return byPurpose.first;
    return printers.first;
  }
}
