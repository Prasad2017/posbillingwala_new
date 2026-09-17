import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
import 'package:unified_esc_pos_printer/unified_esc_pos_printer.dart';

/* USB (+ optional BLE) ESC/POS connection via [PrinterManager]. */
/* Works with generic USB Printer Class (0x07) and USB-serial chips */
/* (FTDI / CP210x / PL2303 / CH34x) — any ESC/POS thermal model. */
class EscPosTransportHub {
  EscPosTransportHub({PrinterManager? manager})
      : escPosTransportHubManager = manager ?? PrinterManager();

  static final EscPosTransportHub instance = EscPosTransportHub();

  final PrinterManager escPosTransportHubManager;
  PrinterDevice? connected;
  String escPosTransportHubSavedUsbId = '';
  String escPosTransportHubSavedUsbName = '';

  PrinterDevice? get connectedDevice => connected;
  bool get isConnected => escPosTransportHubManager.isConnected;
  String get savedUsbId => escPosTransportHubSavedUsbId;
  String get savedUsbName => escPosTransportHubSavedUsbName;

  void updateSavedUsb({required String identifier, String name = ''}) {
    escPosTransportHubSavedUsbId = identifier.trim();
    escPosTransportHubSavedUsbName = name.trim();
  }

  Future<List<PrinterDevice>> scanUsb({
    Duration timeout = const Duration(seconds: 4),
  }) async {
    if (kIsWeb || !(Platform.isAndroid || Platform.isWindows || Platform.isLinux || Platform.isMacOS)) {
      return const [];
    }
    try {
      return await escPosTransportHubManager.scanPrinters(
        timeout: timeout,
        types: const {PrinterConnectionType.usb},
      );
    } catch (e) {
      AppLogger.error('USB scan failed', e);
      return const [];
    }
  }

  Future<List<PrinterDevice>> scanBle({
    Duration timeout = const Duration(seconds: 5),
  }) async {
    if (kIsWeb) return const [];
    try {
      return await escPosTransportHubManager.scanPrinters(
        timeout: timeout,
        types: const {PrinterConnectionType.ble},
      );
    } catch (e) {
      AppLogger.error('BLE scan failed', e);
      return const [];
    }
  }

  Future<bool> connectUsb({
    required String identifier,
    String name = '',
  }) async {
    final id = identifier.trim();
    if (id.isEmpty) return false;
    escPosTransportHubSavedUsbId = id;
    if (name.trim().isNotEmpty) escPosTransportHubSavedUsbName = name.trim();

    final platform = Platform.isAndroid ? UsbPlatform.android : UsbPlatform.desktop;
    final device = UsbPrinterDevice(
      name: escPosTransportHubSavedUsbName.isEmpty ? 'USB Printer' : escPosTransportHubSavedUsbName,
      identifier: id,
      usbPlatform: platform,
    );
    return escPosTransportHubConnect(device);
  }

  Future<bool> connectDevice(PrinterDevice device) async {
    if (device is UsbPrinterDevice) {
      escPosTransportHubSavedUsbId = device.identifier;
      escPosTransportHubSavedUsbName = device.name;
    }
    return escPosTransportHubConnect(device);
  }

  Future<bool> ensureUsbReady() async {
    if (escPosTransportHubSavedUsbId.isEmpty) return false;
    if (escPosTransportHubManager.isConnected &&
        connected is UsbPrinterDevice &&
        (connected as UsbPrinterDevice).identifier == escPosTransportHubSavedUsbId) {
      return true;
    }
    return connectUsb(identifier: escPosTransportHubSavedUsbId, name: escPosTransportHubSavedUsbName);
  }

  Future<bool> writeBytes(List<int> bytes) async {
    if (bytes.isEmpty) return false;
    try {
      if (!escPosTransportHubManager.isConnected) {
        final ok = await ensureUsbReady();
        if (!ok) return false;
      }
      await escPosTransportHubManager.printBytes(bytes);
      return true;
    } catch (e) {
      AppLogger.error('USB/BLE write failed', e);
      try {
        await escPosTransportHubManager.disconnect();
      } catch (_) {}
      connected = null;
      return false;
    }
  }

  Future<void> disconnect() async {
    try {
      await escPosTransportHubManager.disconnect();
    } catch (_) {}
    connected = null;
  }

  Future<void> clearSavedUsb() async {
    escPosTransportHubSavedUsbId = '';
    escPosTransportHubSavedUsbName = '';
    await disconnect();
  }

  Future<bool> escPosTransportHubConnect(PrinterDevice device) async {
    try {
      await escPosTransportHubManager.connect(device);
      connected = device;
      return true;
    } catch (e) {
      AppLogger.error('Printer connect failed', e);
      connected = null;
      return false;
    }
  }

  void dispose() {
    escPosTransportHubManager.dispose();
  }
}
