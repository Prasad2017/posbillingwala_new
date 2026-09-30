import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
import 'package:unified_esc_pos_printer/unified_esc_pos_printer.dart';

/* USB / OTG ESC/POS session shared by Settings, checkout, and print jobs. */
/* */
/* Saved identifier is the platform id from [UsbPrinterDevice]: */
/* Android `vid:pid`, Windows spooler name, or a desktop serial path. */
class EscPosTransportHub {
  EscPosTransportHub({PrinterManager? manager})
    : manager = manager ?? PrinterManager();

  static final EscPosTransportHub instance = EscPosTransportHub();

  final PrinterManager manager;

  String savedUsbId = '';
  String savedUsbName = '';

  bool get isConnected => manager.isConnected;

  bool get supportsUsb {
    if (kIsWeb) return false;
    return Platform.isAndroid ||
        Platform.isWindows ||
        Platform.isLinux ||
        Platform.isMacOS;
  }

  void updateSavedUsb({required String identifier, required String name}) {
    savedUsbId = identifier.trim();
    savedUsbName = name.trim();
  }

  Future<void> clearSavedUsb() async {
    savedUsbId = '';
    savedUsbName = '';
    await disconnectLink();
  }

  /* Drop USB session only — keep saved id/name for auto-reconnect. */
  Future<void> disconnectLink() async {
    if (!supportsUsb) return;
    try {
      if (manager.isConnected ||
          manager.state != PrinterConnectionState.disconnected) {
        await manager.disconnect();
      }
    } catch (error) {
      AppLogger.warning('USB disconnect failed', error);
    }
  }

  Future<List<PrinterDevice>> scanUsb() async {
    if (!supportsUsb) return const [];
    try {
      return await manager.scanPrinters(
        timeout: const Duration(seconds: 4),
        types: const {PrinterConnectionType.usb},
      );
    } catch (error) {
      AppLogger.error('USB scan failed', error);
      return const [];
    }
  }

  Future<bool> connectDevice(PrinterDevice device) async {
    if (device is! UsbPrinterDevice) return false;
    if (!supportsUsb) return false;
    updateSavedUsb(identifier: device.identifier, name: device.name);
    try {
      if (linkedTo(device.identifier)) return true;
      await manager.connect(device);
      return linkedTo(device.identifier);
    } catch (error) {
      AppLogger.error('USB connect failed', error);
      try {
        await manager.disconnect();
      } catch (disconnectError) {
        AppLogger.warning('USB cleanup after connect failed', disconnectError);
      }
      return false;
    }
  }

  Future<bool> connectUsb({
    required String identifier,
    required String name,
  }) async {
    final id = identifier.trim();
    if (id.isEmpty || !supportsUsb) return false;
    updateSavedUsb(identifier: id, name: name);
    if (linkedTo(id)) return true;

    final devices = await scanUsb();
    UsbPrinterDevice? match;
    for (final device in devices) {
      if (device is UsbPrinterDevice && sameId(device.identifier, id)) {
        match = device;
        break;
      }
    }
    final platform = !kIsWeb && Platform.isAndroid
        ? UsbPlatform.android
        : UsbPlatform.desktop;
    match ??= UsbPrinterDevice(
      name: name.trim().isEmpty ? id : name.trim(),
      identifier: id,
      usbPlatform: platform,
    );
    return connectDevice(match);
  }

  Future<bool> ensureUsbReady() async {
    final id = savedUsbId.trim();
    if (id.isEmpty) return false;
    if (linkedTo(id)) return true;
    return connectUsb(identifier: id, name: savedUsbName);
  }

  Future<bool> writeBytes(List<int> bytes) async {
    if (bytes.isEmpty || !supportsUsb) return false;
    final ready = await ensureUsbReady();
    if (!ready) {
      AppLogger.warning('USB write skipped: printer not ready ($savedUsbId)');
      return false;
    }
    try {
      await manager.printBytes(List<int>.from(bytes));
      AppLogger.info('USB write bytes=${bytes.length} ok=true id=$savedUsbId');
      return true;
    } catch (error) {
      AppLogger.error('USB write failed', error);
      return false;
    }
  }

  bool linkedTo(String identifier) {
    if (!manager.isConnected) return false;
    final current = manager.connectedDevice;
    if (current is! UsbPrinterDevice) return false;
    if (identifier.trim().isEmpty) return true;
    return sameId(current.identifier, identifier);
  }

  bool sameId(String left, String right) =>
      left.trim().toLowerCase() == right.trim().toLowerCase();
}
