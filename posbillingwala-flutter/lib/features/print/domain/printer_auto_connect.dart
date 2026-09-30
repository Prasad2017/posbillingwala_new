import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
import 'package:pos_billingwala_v2/features/print/domain/bluetooth_printer_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/esc_pos_transport_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';

/* App-wide reconnect for printers the user already saved / connected. */
/* */
/* Call after settings load, on Home, and when the app resumes. Safe to call */
/* often — no-ops when already linked or nothing is configured. */
abstract final class PrinterAutoConnect {
  PrinterAutoConnect._();

  static bool _running = false;
  static DateTime? _lastAttemptAt;

  /* Soft debounce so Home + settings reload + resume don't thrash BT/USB. */
  static const _minGap = Duration(seconds: 2);

  static Future<void> ensureSavedPrinters(PrinterSettings settings) async {
    if (kIsWeb) return;
    if (_running) return;
    final now = DateTime.now();
    final last = _lastAttemptAt;
    if (last != null && now.difference(last) < _minGap) return;
    _lastAttemptAt = now;
    _running = true;
    try {
      await _ensureSavedPrinters(settings);
    } catch (error) {
      AppLogger.warning('Printer auto-connect failed', error);
    } finally {
      _running = false;
    }
  }

  static Future<void> _ensureSavedPrinters(PrinterSettings settings) async {
    final bt = BluetoothPrinterHub.instance;
    final usb = EscPosTransportHub.instance;

    bt.updateSavedAddresses(
      billMac: settings.billBluetoothAddress,
      kotMac: settings.kotBluetoothAddress,
    );

    /* Prefer bill endpoint for the shared BT link; KOT shares or reconnects
     * on print via ensureReady when MACs differ. */
    await _ensureChannel(
      settings,
      isKot: false,
      bt: bt,
      usb: usb,
    );

    if (!settings.kotEnable) return;

    final billMac = settings.bluetoothFor(isKot: false).toLowerCase();
    final kotMac = settings.bluetoothFor(isKot: true).toLowerCase();
    final billUsb = settings.usbIdFor(isKot: false).toLowerCase();
    final kotUsb = settings.usbIdFor(isKot: true).toLowerCase();
    final sameBt =
        billMac.isNotEmpty && kotMac.isNotEmpty && billMac == kotMac;
    final sameUsb =
        billUsb.isNotEmpty && kotUsb.isNotEmpty && billUsb == kotUsb;
    final kotTransport = settings.transportFor(isKot: true);

    /* Only auto-link KOT when it won't steal the bill connection. */
    if (kotTransport == PosPrinterTransport.bluetooth && sameBt) return;
    if (kotTransport == PosPrinterTransport.usb && sameUsb) return;
    if (kotTransport == PosPrinterTransport.bluetooth && billMac.isNotEmpty) {
      return;
    }
    if (kotTransport == PosPrinterTransport.usb && billUsb.isNotEmpty) {
      return;
    }

    await _ensureChannel(
      settings,
      isKot: true,
      bt: bt,
      usb: usb,
    );
  }

  static Future<void> _ensureChannel(
    PrinterSettings settings, {
    required bool isKot,
    required BluetoothPrinterHub bt,
    required EscPosTransportHub usb,
  }) async {
    final transport = settings.transportFor(isKot: isKot);
    switch (transport) {
      case PosPrinterTransport.bluetooth:
        final mac = settings.bluetoothFor(isKot: isKot).trim();
        if (mac.isEmpty) return;
        if (!await bt.isBluetoothOn()) return;
        AppLogger.info(
          'Printer auto-connect BT ${isKot ? 'KOT' : 'bill'} → $mac',
        );
        await bt.autoConnect(
          isKot ? PrinterChannelKind.kot : PrinterChannelKind.bill,
        );
      case PosPrinterTransport.usb:
        final id = settings.usbIdFor(isKot: isKot).trim();
        if (id.isEmpty) return;
        usb.updateSavedUsb(
          identifier: id,
          name: settings.usbNameFor(isKot: isKot),
        );
        AppLogger.info(
          'Printer auto-connect USB ${isKot ? 'KOT' : 'bill'} → $id',
        );
        await usb.ensureUsbReady();
      case PosPrinterTransport.network:
        /* Network is connected per print job — nothing to keep open. */
        break;
    }
  }
}
