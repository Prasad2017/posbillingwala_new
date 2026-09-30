import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/store_printer.dart';

/* Unified printer endpoint — transport-agnostic; no hardcoded IP/port/device. */
class PrinterConfig {
  const PrinterConfig({
    required this.id,
    required this.name,
    required this.connectionType,
    this.ipAddress = '',
    this.port = 9100,
    this.bluetoothDeviceId = '',
    this.usbDeviceId = '',
    this.usbName = '',
    this.paperSize = PrinterPaperSize.mm58,
    this.autoCutEnabled = true,
    this.cutType = PrinterCutType.defaultCut,
    this.feedLines = 1,
    this.timeoutMs = 5000,
    this.manufacturer = '',
    this.model = '',
  });

  final String id;
  final String name;
  final PosPrinterTransport connectionType;
  final String ipAddress;
  final int port;
  final String bluetoothDeviceId;
  final String usbDeviceId;
  final String usbName;
  final PrinterPaperSize paperSize;
  final bool autoCutEnabled;
  final PrinterCutType cutType;
  final int feedLines;
  final int timeoutMs;
  final String manufacturer;
  final String model;

  PrinterPaperProfile get paperProfile => PrinterPaperProfile.of(paperSize);

  String get endpointKey {
    switch (connectionType) {
      case PosPrinterTransport.bluetooth:
        return 'bt:${bluetoothDeviceId.trim().toLowerCase()}';
      case PosPrinterTransport.usb:
        return 'usb:${usbDeviceId.trim().toLowerCase()}';
      case PosPrinterTransport.network:
        return 'net:${ipAddress.trim().toLowerCase()}:$port';
    }
  }

  bool get hasEndpoint {
    switch (connectionType) {
      case PosPrinterTransport.bluetooth:
        return bluetoothDeviceId.trim().isNotEmpty;
      case PosPrinterTransport.usb:
        return usbDeviceId.trim().isNotEmpty;
      case PosPrinterTransport.network:
        return ipAddress.trim().isNotEmpty && port > 0 && port <= 65535;
    }
  }

  String? validate() {
    if (!hasEndpoint) {
      switch (connectionType) {
        case PosPrinterTransport.bluetooth:
          return 'Bluetooth device is not configured';
        case PosPrinterTransport.usb:
          return 'USB printer is not configured';
        case PosPrinterTransport.network:
          return 'Printer IP address is not configured';
      }
    }
    if (connectionType == PosPrinterTransport.network) {
      final host = ipAddress.trim();
      if (!_looksLikeHost(host)) {
        return 'Invalid printer IP / host';
      }
      if (port <= 0 || port > 65535) {
        return 'Invalid printer port';
      }
    }
    if (feedLines < 0 || feedLines > 20) {
      return 'Invalid feed lines';
    }
    return null;
  }

  static bool _looksLikeHost(String host) {
    if (host.isEmpty || host.length > 253) return false;
    if (host.contains(' ') || host.contains('/')) return false;
    return true;
  }

  factory PrinterConfig.fromSettings(
    PrinterSettings settings, {
    required bool isKot,
    String id = 'local',
    String name = 'Local printer',
  }) {
    final transport = settings.transportFor(isKot: isKot);
    return PrinterConfig(
      id: isKot ? '${id}_kot' : id,
      name: isKot ? '$name (KOT)' : name,
      connectionType: transport,
      ipAddress: settings.networkHost,
      port: settings.networkPort,
      bluetoothDeviceId: settings.bluetoothFor(isKot: isKot),
      usbDeviceId: settings.usbIdFor(isKot: isKot),
      usbName: settings.usbNameFor(isKot: isKot),
      paperSize: settings.paperSizeFor(isKot: isKot),
      autoCutEnabled: settings.supportsAutoCut,
      cutType: settings.cutType,
      feedLines: isKot ? settings.kotFeedLines : settings.feedLines,
    );
  }

  factory PrinterConfig.fromStorePrinter(StorePrinter printer) {
    return PrinterConfig(
      id: printer.id,
      name: printer.printerName,
      connectionType: _transport(printer.connectionType),
      ipAddress: printer.ipAddress,
      port: printer.port,
      bluetoothDeviceId: printer.bluetoothAddress,
      usbDeviceId: printer.usbIdentifier,
      usbName: printer.usbName,
      paperSize: printer.paperSizeEnum,
      autoCutEnabled: true,
      cutType: PrinterCutType.defaultCut,
      feedLines: 1,
    );
  }

  static PosPrinterTransport _transport(String raw) {
    switch (raw.toUpperCase()) {
      case 'USB':
        return PosPrinterTransport.usb;
      case 'WIFI':
      case 'NETWORK':
      case 'LAN':
        return PosPrinterTransport.network;
      default:
        return PosPrinterTransport.bluetooth;
    }
  }
}
