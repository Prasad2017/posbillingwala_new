import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
import 'package:pos_billingwala_v2/features/print/domain/bluetooth_printer_hub.dart';

/* Legacy Android Woosim MethodChannel — kept for reference / optional debug. */
/* Bill / KOT printing now uses [BluetoothPrinterHub] + print_bluetooth_thermal */
/* on Android and iOS (same shared ESC/POS raster bytes). */
class WoosimPrintChannel {
  WoosimPrintChannel();

  static const methodChannel = MethodChannel('pos_billingwala/woosim_print');
  static final WoosimPrintChannel instance = WoosimPrintChannel();

  /* Disabled: Android uses the same Dart BT path as iOS. */
  static bool get isSupported => false;

  static bool get isAndroidNativeAvailable => !kIsWeb && Platform.isAndroid;

  Future<bool> connect(PrinterChannelKind kind, String mac) async {
    if (!isAndroidNativeAvailable || mac.trim().isEmpty) return false;
    try {
      final ok = await methodChannel.invokeMethod<bool>('connect', {
        'kind': kind.name,
        'mac': mac.trim(),
      });
      return ok == true;
    } catch (e) {
      AppLogger.error('Woosim connect failed', e);
      return false;
    }
  }

  Future<bool> write(PrinterChannelKind kind, List<int> bytes) async {
    if (!isAndroidNativeAvailable || bytes.isEmpty) return false;
    try {
      final ok = await methodChannel.invokeMethod<bool>('write', {
        'kind': kind.name,
        'bytes': Uint8List.fromList(bytes),
      });
      return ok == true;
    } catch (e) {
      AppLogger.error('Woosim write failed', e);
      return false;
    }
  }

  Future<void> disconnect(PrinterChannelKind kind) async {
    if (!isAndroidNativeAvailable) return;
    try {
      await methodChannel.invokeMethod<void>('disconnect', {'kind': kind.name});
    } catch (error) {
      AppLogger.warning('Woosim disconnect', error);
    }
  }

  Future<bool> isReady(PrinterChannelKind kind) async {
    if (!isAndroidNativeAvailable) return false;
    try {
      final ok = await methodChannel.invokeMethod<bool>('isReady', {
        'kind': kind.name,
      });
      return ok == true;
    } catch (error) {
      AppLogger.warning('Woosim isReady', error);
      return false;
    }
  }
}
