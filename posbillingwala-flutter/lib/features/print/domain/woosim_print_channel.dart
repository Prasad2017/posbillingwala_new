import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:pos_billingwala_v2/features/print/domain/bluetooth_printer_hub.dart';

/// Android MethodChannel to WithTable [BluetoothPrintService] + WoosimService.
class WoosimPrintChannel {
  WoosimPrintChannel();

  static const methodChannel = MethodChannel('pos_billingwala/woosim_print');
  static final WoosimPrintChannel instance = WoosimPrintChannel();

  static bool get isSupported => !kIsWeb && Platform.isAndroid;

  Future<bool> connect(PrinterChannelKind kind, String mac) async {
    if (!isSupported || mac.trim().isEmpty) return false;
    try {
      final ok = await methodChannel.invokeMethod<bool>('connect', {
        'kind': kind.name,
        'mac': mac.trim(),
      });
      return ok == true;
    } catch (e) {
      debugPrint('Woosim connect failed: $e');
      return false;
    }
  }

  Future<bool> write(PrinterChannelKind kind, List<int> bytes) async {
    if (!isSupported || bytes.isEmpty) return false;
    try {
      final ok = await methodChannel.invokeMethod<bool>('write', {
        'kind': kind.name,
        'bytes': Uint8List.fromList(bytes),
      });
      return ok == true;
    } catch (e) {
      debugPrint('Woosim write failed: $e');
      return false;
    }
  }

  Future<void> disconnect(PrinterChannelKind kind) async {
    if (!isSupported) return;
    try {
      await methodChannel.invokeMethod<void>('disconnect', {'kind': kind.name});
    } catch (error) {
      debugPrint('Woosim disconnect: $error');
    }
  }

  Future<bool> isReady(PrinterChannelKind kind) async {
    if (!isSupported) return false;
    try {
      final ok = await methodChannel.invokeMethod<bool>('isReady', {
        'kind': kind.name,
      });
      return ok == true;
    } catch (error) {
      debugPrint('Woosim isReady: $error');
      return false;
    }
  }
}
