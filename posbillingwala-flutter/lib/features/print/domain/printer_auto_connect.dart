import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
import 'package:pos_billingwala_v2/features/print/domain/bluetooth_printer_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/esc_pos_transport_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';

/* App-wide reconnect for printers the user already saved / connected. */
/* */
/* Call after settings load, on Home, and when the app resumes. Safe to call */
/* often — concurrent callers share one in-flight attempt. */
abstract final class PrinterAutoConnect {
  PrinterAutoConnect._();

  static Completer<void>? _inFlight;
  static DateTime? _lastAttemptAt;

  /* Soft debounce so Home + settings reload + resume don't thrash BT/USB. */
  static const _minGap = Duration(seconds: 2);

  static Future<void> ensureSavedPrinters(
    PrinterSettings settings, {
    bool force = false,
  }) async {
    if (kIsWeb) return;

    /* Warm path: already linked — skip reconnect wait on the bill critical path. */
    if (!force && await isBillPrinterOnline(settings)) {
      return;
    }

    final existing = _inFlight;
    if (existing != null) {
      await existing.future;
      if (!force) return;
      /* force after shared wait: try again below. */
      if (await isBillPrinterOnline(settings)) return;
    }

    final now = DateTime.now();
    final last = _lastAttemptAt;
    if (!force && last != null && now.difference(last) < _minGap) {
      return;
    }

    final completer = Completer<void>();
    _inFlight = completer;
    _lastAttemptAt = now;
    try {
      await _ensureSavedPrinters(settings);
      if (!completer.isCompleted) completer.complete();
    } catch (error) {
      AppLogger.warning('Printer auto-connect failed', error);
      if (!completer.isCompleted) completer.complete();
    } finally {
      if (_inFlight == completer) _inFlight = null;
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

  /* True when the bill printer endpoint is currently linked. */
  static Future<bool> isBillPrinterOnline(PrinterSettings settings) async {
    switch (settings.billTransport) {
      case PosPrinterTransport.bluetooth:
        final mac = settings.billBluetoothAddress.trim();
        if (mac.isEmpty) return false;
        final hub = BluetoothPrinterHub.instance;
        if (hub.isConnecting) return false;
        /* Trust hub session first — avoids platform round-trip on every Print. */
        if (hub.connectedAddress.isNotEmpty &&
            hub.connectedAddress.toLowerCase() == mac.toLowerCase()) {
          return true;
        }
        if (!await hub.connectionStatus()) return false;
        return hub.connectedAddress.toLowerCase() == mac.toLowerCase();
      case PosPrinterTransport.usb:
        final id = settings.billUsbIdentifier.trim();
        if (id.isEmpty) return false;
        return EscPosTransportHub.instance.linkedTo(id);
      case PosPrinterTransport.network:
        return settings.networkHost.trim().isNotEmpty;
    }
  }
}
