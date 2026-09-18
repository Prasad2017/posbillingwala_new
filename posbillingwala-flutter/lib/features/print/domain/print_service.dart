import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
import 'package:pos_billingwala_v2/features/print/domain/bluetooth_printer_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/esc_pos_transport_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/receipt_builder.dart';
import 'package:pos_billingwala_v2/features/print/domain/receipt_image_share.dart';
import 'package:pos_billingwala_v2/features/print/domain/receipt_labels.dart';
import 'package:pos_billingwala_v2/features/print/domain/receipt_rasterizer.dart';
import 'package:pos_billingwala_v2/features/print/domain/sample_receipt_data.dart';
import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';
import 'package:pos_billingwala_v2/features/print/domain/thermal_ticket.dart';

enum PrintOutcome {
  bluetoothPrinted,
  usbPrinted,
  networkPrinted,
  shared,
  previewOnly,
  failed,
}

class PrintResult {
  const PrintResult({required this.outcome, required this.text, this.message});

  final PrintOutcome outcome;
  final String text;
  final String? message;
}

/* Routes ESC/POS raster bytes to Bluetooth, USB, or Network. */
/* Any ESC/POS thermal model is supported (bitmap path is brand-agnostic). */
class PrintService {
  PrintService(
    this.settings, {
    this.shopProfile = const ShopReceiptProfile(),
    this.labels = const ReceiptLabels(
      originalCopy: '***** Original Copy *****',
      duplicateCopy: '***** Duplicate Copy *****',
      item: 'ITEM',
      rate: 'RATE',
      amount: 'AMOUNT',
      subTotal: 'SUB TOTAL',
      discount: 'DISCOUNT',
      packing: 'PACKING',
      totalAmount: 'TOTAL AMOUNT',
      poweredBy: 'Powered by Billingwala',
      website: 'www.posbillingwala.com',
      customerName: 'Customer Name',
      customerMobile: 'Customer Mobile',
      customerEmail: 'Customer Email',
      customerAddress: 'Customer Address',
      date: 'Date',
      kot: 'KOT Print',
    ),
    BluetoothPrinterHub? hub,
    EscPosTransportHub? usbHub,
  }) : hub = hub ?? BluetoothPrinterHub.instance,
       usbHub = usbHub ?? EscPosTransportHub.instance;

  final PrinterSettings settings;
  final BluetoothPrinterHub hub;
  final EscPosTransportHub usbHub;
  final ShopReceiptProfile shopProfile;
  final ReceiptLabels labels;

  ReceiptBuilder get builder => builderFor(isKot: false);

  ReceiptBuilder builderFor({required bool isKot}) => ReceiptBuilder(
    settings.copyWith(paperSize: settings.paperSizeFor(isKot: isKot)),
    shopProfile: shopProfile,
    labels: labels,
  );

  ThermalTicket billTicket({
    required Invoice invoice,
    required List<InvoiceItem> items,
    String? shopName,
    bool duplicate = false,
  }) {
    return builder.ticket(
      invoice: invoice,
      items: items,
      shopName: shopName,
      duplicate: duplicate,
    );
  }

  String billPreviewText({
    required Invoice invoice,
    required List<InvoiceItem> items,
    String? shopName,
    bool duplicate = false,
    PrinterPaperSize? paperSize,
  }) {
    final previewSettings = paperSize == null
        ? settings
        : settings.copyWith(paperSize: paperSize);
    return ReceiptBuilder(
      previewSettings,
      shopProfile: shopProfile,
      labels: labels,
    ).billText(
      invoice: invoice,
      items: items,
      shopName: shopName,
      duplicate: duplicate,
    ).replaceAll(ReceiptBuilder.upiQrMarker, '[UPI QR]');
  }

  /* Full bill preview (logo + UPI QR) matching thermal raster output. */
  Future<RenderedImage> billPreviewImage({
    required Invoice invoice,
    required List<InvoiceItem> items,
    String? shopName,
    bool duplicate = false,
    PrinterPaperSize? paperSize,
  }) {
    final previewSettings = paperSize == null
        ? settings
        : settings.copyWith(paperSize: paperSize);
    final builder = ReceiptBuilder(
      previewSettings,
      shopProfile: shopProfile,
      labels: labels,
    );
    final upiUri = builder.upiUriFor(invoice);
    final logoPath = previewSettings.logoUse
        ? shopProfile.logoLocalPath
        : null;
    return builder.rasterizer.render(
      builder.billText(
        invoice: invoice,
        items: items,
        shopName: shopName,
        duplicate: duplicate,
      ),
      ReceiptRasterizer.widthPxFor(previewSettings.paperSize),
      qrPayload: upiUri,
      logoPath: logoPath,
      qrMarker: upiUri != null ? ReceiptBuilder.upiQrMarker : null,
      useAssetLogoFallback: false,
    );
  }

  String kotPreviewText({KotTicket? ticket, PrinterPaperSize? paperSize}) {
    final previewSettings = paperSize == null
        ? settings.copyWith(paperSize: settings.kotPaperSize)
        : settings.copyWith(paperSize: paperSize);
    return ReceiptBuilder(
      previewSettings,
      shopProfile: shopProfile,
      labels: labels,
    ).kotText(
      ticket ?? SampleReceiptData.sampleKot(prefix: settings.kotPrefix),
    );
  }

  Future<PrintResult> printBill({
    required Invoice invoice,
    required List<InvoiceItem> items,
    String? shopName,
    bool preferShare = false,
    bool duplicate = false,
  }) async {
    syncSavedEndpoints(isKot: false);
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
    return dispatch(
      text: text,
      bytes: bytes,
      channel: PrinterChannelKind.bill,
      preferShare: preferShare,
      label: duplicate ? 'Duplicate bill' : 'Bill',
    );
  }

  Future<PrintResult> printKot(
    KotTicket ticket, {
    bool preferShare = false,
  }) async {
    syncSavedEndpoints(isKot: true);
    final kotBuilder = builderFor(isKot: true);
    final text = kotBuilder.kotText(ticket);
    final bytes = await kotBuilder.kotPrintBytes(ticket);
    return dispatch(
      text: text,
      bytes: bytes,
      channel: PrinterChannelKind.kot,
      preferShare: preferShare,
      label: 'KOT',
    );
  }

  /* Sample invoice / KOT (same lines as Android printer-settings test). */
  Future<PrintResult> printTest(
    PrinterChannelKind channel, {
    String? shopName,
  }) {
    if (channel == PrinterChannelKind.kot) {
      return printKot(SampleReceiptData.sampleKot(prefix: settings.kotPrefix));
    }
    final sample = SampleReceiptData.sampleBill();
    return printBill(
      invoice: sample.invoice,
      items: sample.items,
      shopName: (shopName?.trim().isNotEmpty ?? false)
          ? shopName
          : SampleReceiptData.demoShopName,
    );
  }

  String previewText(PrinterChannelKind channel, {String? shopName}) {
    if (channel == PrinterChannelKind.kot) {
      return kotPreviewText();
    }
    final sample = SampleReceiptData.sampleBill();
    return billPreviewText(
      invoice: sample.invoice,
      items: sample.items,
      shopName: (shopName?.trim().isNotEmpty ?? false)
          ? shopName
          : SampleReceiptData.demoShopName,
      paperSize: settings.paperSize,
    );
  }

  Future<PrintResult> printRawText(
    String text, {
    PrinterChannelKind channel = PrinterChannelKind.bill,
    bool preferShare = false,
    String label = 'Receipt',
  }) async {
    syncSavedEndpoints(isKot: channel == PrinterChannelKind.kot);
    final bytes = await builderFor(
      isKot: channel == PrinterChannelKind.kot,
    ).rawPrintBytes(text);
    return dispatch(
      text: text,
      bytes: bytes,
      channel: channel,
      preferShare: preferShare,
      label: label,
    );
  }

  void syncSavedEndpoints({required bool isKot}) {
    hub.updateSavedAddresses(
      billMac: settings.billBluetoothAddress,
      kotMac: settings.kotBluetoothAddress,
    );
    usbHub.updateSavedUsb(
      identifier: settings.usbIdFor(isKot: isKot),
      name: settings.usbNameFor(isKot: isKot),
    );
  }

  Future<bool> printNetworkTo(String host, int port, List<int> bytes) async {
    if (host.trim().isEmpty) return false;
    final resolvedPort = port <= 0 ? 9100 : port;
    Socket? socket;
    try {
      socket = await Socket.connect(
        host.trim(),
        resolvedPort,
        timeout: const Duration(seconds: 5),
      );
      socket.add(bytes);
      await socket.flush();
      return true;
    } finally {
      await socket?.close();
    }
  }

  Future<bool> printNetwork(List<int> bytes) async {
    return printNetworkTo(settings.networkHost, settings.networkPort, bytes);
  }

  Future<PrintResult> dispatchToEndpoint({
    required String text,
    required List<int> bytes,
    required String label,
    required PosPrinterTransport transport,
    String bluetoothAddress = '',
    String usbIdentifier = '',
    String usbName = '',
    String networkHost = '',
    int networkPort = 9100,
  }) async {
    if (kIsWeb) {
      await shareReceiptAsImage(text: text, label: label);
      return PrintResult(
        outcome: PrintOutcome.shared,
        text: text,
        message: '$label queued for share (web has no local thermal printer)',
      );
    }
    try {
      switch (transport) {
        case PosPrinterTransport.bluetooth:
          final mac = bluetoothAddress.trim();
          if (mac.isEmpty) break;
          final sent = await hub.writeToMac(mac, bytes);
          if (sent) {
            return PrintResult(
              outcome: PrintOutcome.bluetoothPrinted,
              text: text,
              message: '$label sent to Bluetooth printer',
            );
          }
        case PosPrinterTransport.usb:
          if (usbIdentifier.trim().isEmpty) break;
          usbHub.updateSavedUsb(identifier: usbIdentifier, name: usbName);
          final sent = await usbHub.writeBytes(bytes);
          if (sent) {
            return PrintResult(
              outcome: PrintOutcome.usbPrinted,
              text: text,
              message: '$label sent to USB printer',
            );
          }
        case PosPrinterTransport.network:
          if (networkHost.trim().isEmpty) break;
          final ok = await printNetworkTo(networkHost, networkPort, bytes);
          if (ok) {
            return PrintResult(
              outcome: PrintOutcome.networkPrinted,
              text: text,
              message: '$label sent to $networkHost:$networkPort',
            );
          }
      }
    } catch (e) {
      AppLogger.error('dispatchToEndpoint failed', e);
    }
    return PrintResult(
      outcome: PrintOutcome.failed,
      text: text,
      message: '$label failed — printer not reachable',
    );
  }

  Future<PrintResult> dispatch({
    required String text,
    required List<int> bytes,
    required PrinterChannelKind channel,
    required bool preferShare,
    required String label,
  }) async {
    if (!preferShare && !kIsWeb) {
      final isKot = channel == PrinterChannelKind.kot;
      final transport = settings.transportFor(isKot: isKot);

      /* Only the type chosen for this Bill / KOT printer. */
      final preferred = await tryTransport(
        transport: transport,
        channel: channel,
        isKot: isKot,
        bytes: bytes,
        text: text,
        label: label,
      );
      if (preferred != null) return preferred;

      return PrintResult(
        outcome: PrintOutcome.failed,
        text: text,
        message: '$label failed — ${transport.label} printer not reachable',
      );
    }

    await shareReceiptAsImage(text: text, label: label);
    return PrintResult(
      outcome: PrintOutcome.shared,
      text: text,
      message: '$label shared (no printer available)',
    );
  }

  Future<PrintResult?> tryTransport({
    required PosPrinterTransport transport,
    required PrinterChannelKind channel,
    required bool isKot,
    required List<int> bytes,
    required String text,
    required String label,
  }) async {
    try {
      switch (transport) {
        case PosPrinterTransport.bluetooth:
          if (!Platform.isAndroid && !Platform.isIOS) return null;
          final mac = settings.bluetoothFor(isKot: isKot);
          if (mac.isEmpty) return null;
          hub.updateSavedAddresses(
            billMac: settings.billBluetoothAddress,
            kotMac: settings.kotBluetoothAddress,
          );
          final sent = await hub.write(channel, bytes);
          if (!sent) return null;
          return PrintResult(
            outcome: PrintOutcome.bluetoothPrinted,
            text: text,
            message: '$label sent to Bluetooth printer',
          );

        case PosPrinterTransport.usb:
          if (!(Platform.isAndroid ||
              Platform.isWindows ||
              Platform.isLinux ||
              Platform.isMacOS)) {
            return null;
          }
          final id = settings.usbIdFor(isKot: isKot);
          if (id.isEmpty) return null;
          usbHub.updateSavedUsb(
            identifier: id,
            name: settings.usbNameFor(isKot: isKot),
          );
          final sent = await usbHub.writeBytes(bytes);
          if (!sent) return null;
          return PrintResult(
            outcome: PrintOutcome.usbPrinted,
            text: text,
            message: '$label sent to USB printer',
          );

        case PosPrinterTransport.network:
          if (settings.networkHost.trim().isEmpty) return null;
          final ok = await printNetwork(bytes);
          if (!ok) return null;
          return PrintResult(
            outcome: PrintOutcome.networkPrinted,
            text: text,
            message:
                '$label sent to ${settings.networkHost}:${settings.networkPort}',
          );
      }
    } catch (e) {
      AppLogger.error('Transport $transport failed', e);
      return null;
    }
  }
}
