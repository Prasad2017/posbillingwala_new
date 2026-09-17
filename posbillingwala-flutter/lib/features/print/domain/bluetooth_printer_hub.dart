import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
import 'package:pos_billingwala_v2/core/permissions/app_permission_service.dart';
import 'package:pos_billingwala_v2/features/print/domain/woosim_print_channel.dart';
import 'package:print_bluetooth_thermal/print_bluetooth_thermal.dart';

enum PrinterChannelKind { bill, kot }

/* Android-parity Bluetooth printer session manager. */
/* */
/* Mirrors [BluetoothPrinterChannel] in WithTable: */
/* - Separate bill / KOT saved MACs */
/* - One RFCOMM link via [print_bluetooth_thermal] (plugin is single-connection) */
/* - Same MAC for bill+KOT → reuse connection (no second socket) */
/* - Different MACs → disconnect then reconnect before write */
/* - Skip connect when already linked to the same address */
/* - Soft auto-reconnect with backoff after failed writes / disconnects */
class BluetoothPrinterHub {
  BluetoothPrinterHub({
    this.permissions = const AppPermissionService(),
  });

  static final BluetoothPrinterHub instance = BluetoothPrinterHub();

  static const connectWait = Duration(seconds: 12);
  static const reconnectDelay = Duration(milliseconds: 2500);
  static const reconnectMaxDelay = Duration(seconds: 30);

  final AppPermissionService permissions;

  String billAddress = '';
  String kotAddress = '';
  String connectedAddress = '';
  bool connecting = false;
  bool persistentSession = false;
  Duration reconnectBackoff = reconnectDelay;
  Timer? reconnectTimer;

  bool get isConnecting => connecting;
  bool get isReady => connectedAddress.isNotEmpty;

  void updateSavedAddresses({
    required String billMac,
    required String kotMac,
  }) {
    billAddress = normalizeMac(billMac);
    kotAddress = normalizeMac(kotMac);
  }

  String addressFor(PrinterChannelKind kind) {
    if (kind == PrinterChannelKind.kot) {
      final kot = kotAddress;
      if (kot.isNotEmpty) return kot;
      return billAddress;
    }
    return billAddress;
  }

  Future<bool> isBluetoothOn() async {
    if (kIsWeb || !(Platform.isAndroid || Platform.isIOS)) return false;
    try {
      return await PrintBluetoothThermal.bluetoothEnabled;
    } catch (error) {
      return false;
    }
  }

  Future<List<BluetoothInfo>> pairedDevices() async {
    if (kIsWeb || !(Platform.isAndroid || Platform.isIOS)) return const [];
    final allowed = await permissions.ensurePrintPermissions();
    if (!allowed) return const [];
    try {
      return await PrintBluetoothThermal.pairedBluetooths;
    } catch (e) {
      AppLogger.error('pairedDevices failed', e);
      return const [];
    }
  }

  Future<bool> connectionStatus() async {
    if (kIsWeb || !Platform.isAndroid) return false;
    if (WoosimPrintChannel.isSupported) {
      final bill = await WoosimPrintChannel.instance.isReady(PrinterChannelKind.bill);
      final kot = await WoosimPrintChannel.instance.isReady(PrinterChannelKind.kot);
      if (!bill && !kot) connectedAddress = '';
      return bill || kot;
    }
    try {
      final linked = await PrintBluetoothThermal.connectionStatus;
      if (!linked) {
        connectedAddress = '';
      }
      return linked;
    } catch (error) {
      connectedAddress = '';
      return false;
    }
  }

  /* Silent auto-connect (Home / Settings load), same as Android autoConnect. */
  Future<void> autoConnect(PrinterChannelKind kind) async {
    final mac = addressFor(kind);
    if (mac.isEmpty) return;
    if (!await isBluetoothOn()) return;
    await connectInternal(mac, fromUser: false);
  }

  /* User-initiated connect (Settings Connect button). */
  Future<bool> connect(
    PrinterChannelKind kind, {
    String? address,
    bool fromUser = true,
  }) async {
    final mac = normalizeMac(address ?? addressFor(kind));
    if (mac.isEmpty) return false;

    if (kind == PrinterChannelKind.bill) {
      billAddress = mac;
    } else {
      kotAddress = mac;
    }

    if (!await isBluetoothOn()) return false;

    final allowed = await permissions.ensurePrintPermissions();
    if (!allowed) return false;

    return connectInternal(mac, fromUser: fromUser);
  }

  Future<void> disconnect(PrinterChannelKind kind) async {
    cancelReconnect();
    if (kind == PrinterChannelKind.bill) {
      billAddress = '';
    } else {
      kotAddress = '';
    }

    /* Keep physical link if the other channel still needs the same MAC. */
    final other = addressFor(
      kind == PrinterChannelKind.bill
          ? PrinterChannelKind.kot
          : PrinterChannelKind.bill,
    );
    if (other.isNotEmpty &&
        other.toLowerCase() == connectedAddress.toLowerCase()) {
      persistentSession = true;
      return;
    }

    persistentSession = false;
    try {
      if (WoosimPrintChannel.isSupported) {
        await WoosimPrintChannel.instance.disconnect(kind);
      } else {
        await PrintBluetoothThermal.disconnect;
      }
    } catch (error) {
      AppLogger.warning('BT disconnect', error);
    }
    connectedAddress = '';
  }

  /* Ensures the correct printer is connected before a print write (≤12s). */
  Future<bool> ensureReady(PrinterChannelKind kind) async {
    final mac = addressFor(kind);
    if (mac.isEmpty) return false;

    final allowed = await permissions.ensurePrintPermissions();
    if (!allowed) return false;
    if (!await isBluetoothOn()) return false;

    if (await isLinkedTo(mac)) return true;

    final started = await connectInternal(mac, fromUser: false);
    if (!started && !connecting) return false;

    final deadline = DateTime.now().add(connectWait);
    while (DateTime.now().isBefore(deadline)) {
      if (await isLinkedTo(mac)) return true;
      if (!connecting && !await isLinkedTo(mac)) {
        /* One more attempt before giving up. */
        final retry = await connectInternal(mac, fromUser: false);
        if (!retry) return await isLinkedTo(mac);
      }
      await Future<void>.delayed(const Duration(milliseconds: 200));
    }
    return isLinkedTo(mac);
  }

  Future<bool> writeToMac(String mac, List<int> bytes) async {
    final savedBill = billAddress;
    final savedKot = kotAddress;
    try {
      billAddress = normalizeMac(mac);
      return await write(PrinterChannelKind.bill, bytes);
    } finally {
      billAddress = savedBill;
      kotAddress = savedKot;
    }
  }

  Future<bool> write(PrinterChannelKind kind, List<int> bytes) async {
    if (bytes.isEmpty) return false;
    if (WoosimPrintChannel.isSupported) {
      final mac = addressFor(kind);
      if (mac.isEmpty) return false;
      final connected = await WoosimPrintChannel.instance.connect(kind, mac);
      if (!connected) {
        scheduleReconnect(kind);
        return false;
      }
      connectedAddress = mac;
      final ok = await WoosimPrintChannel.instance.write(kind, bytes);
      if (!ok) {
        connectedAddress = '';
        scheduleReconnect(kind);
      } else {
        cancelReconnect();
        reconnectBackoff = reconnectDelay;
        persistentSession = true;
      }
      return ok;
    }
    final ready = await ensureReady(kind);
    if (!ready) {
      AppLogger.warning(
        'BT write skipped: printer not ready (${addressFor(kind)})',
      );
      scheduleReconnect(kind);
      return false;
    }
    try {
      /* print_bluetooth_thermal Android expects List<Int>, not Uint8List */
      /* (Uint8List arrives as typed data and the Kotlin cast returns null → false). */
      final payload = List<int>.from(bytes);
      final ok = await PrintBluetoothThermal.writeBytes(payload);
      AppLogger.info(
        'BT write channel=${kind.name} bytes=${payload.length} ok=$ok '
        'mac=$connectedAddress',
      );
      if (!ok) {
        connectedAddress = '';
        scheduleReconnect(kind);
      } else {
        cancelReconnect();
        reconnectBackoff = reconnectDelay;
        persistentSession = true;
      }
      return ok;
    } catch (e) {
      AppLogger.error('BT write failed', e);
      connectedAddress = '';
      scheduleReconnect(kind);
      return false;
    }
  }

  Future<bool> isLinkedTo(String mac) async {
    final linked = await connectionStatus();
    if (!linked) return false;
    if (connectedAddress.isEmpty) {
      /* Plugin connected but we lost tracked MAC — treat as linked only if */
      /* caller reconnects explicitly. Force reconnect for safety. */
      return false;
    }
    return connectedAddress.toLowerCase() == mac.toLowerCase();
  }

  Future<bool> connectInternal(String address, {required bool fromUser}) async {
    final mac = normalizeMac(address);
    if (mac.isEmpty) return false;

    /* Already on the right printer — Android skips reconnect. */
    if (await isLinkedTo(mac)) {
      persistentSession = true;
      cancelReconnect();
      reconnectBackoff = reconnectDelay;
      return true;
    }

    if (connecting) {
      /* Wait briefly for in-flight connect to the same MAC. */
      final deadline = DateTime.now().add(const Duration(seconds: 8));
      while (connecting && DateTime.now().isBefore(deadline)) {
        await Future<void>.delayed(const Duration(milliseconds: 150));
      }
      if (await isLinkedTo(mac)) return true;
    }

    connecting = true;
    try {
      if (WoosimPrintChannel.isSupported) {
        final kind = kotAddress.isNotEmpty &&
                mac.toLowerCase() == kotAddress.toLowerCase()
            ? PrinterChannelKind.kot
            : PrinterChannelKind.bill;
        final ok = await WoosimPrintChannel.instance.connect(kind, mac);
        if (ok) {
          connectedAddress = mac;
          persistentSession = true;
          cancelReconnect();
          reconnectBackoff = reconnectDelay;
          return true;
        }
        connectedAddress = '';
        if (persistentSession || fromUser) {
          scheduleReconnectForAddress(mac);
        }
        return false;
      }
      final currentlyLinked = await PrintBluetoothThermal.connectionStatus;
      if (currentlyLinked) {
        final same = connectedAddress.toLowerCase() == mac.toLowerCase();
        if (!same) {
          try {
            await PrintBluetoothThermal.disconnect;
          } catch (error) {
            AppLogger.warning('BT drop previous link', error);
          }
          connectedAddress = '';
          await Future<void>.delayed(const Duration(milliseconds: 350));
        } else {
          connectedAddress = mac;
          persistentSession = true;
          return true;
        }
      }

      final ok = await PrintBluetoothThermal.connect(macPrinterAddress: mac);
      if (ok) {
        connectedAddress = mac;
        persistentSession = true;
        cancelReconnect();
        reconnectBackoff = reconnectDelay;
        return true;
      }

      connectedAddress = '';
      if (persistentSession || fromUser) {
        scheduleReconnectForAddress(mac);
      }
      return false;
    } catch (e) {
      AppLogger.error('BT connect failed', e);
      connectedAddress = '';
      scheduleReconnectForAddress(mac);
      return false;
    } finally {
      connecting = false;
    }
  }

  void scheduleReconnect(PrinterChannelKind kind) {
    final mac = addressFor(kind);
    if (mac.isEmpty) return;
    scheduleReconnectForAddress(mac);
  }

  void scheduleReconnectForAddress(String mac) {
    if (mac.isEmpty) return;
    reconnectTimer?.cancel();
    final delay = reconnectBackoff;
    reconnectBackoff = Duration(
      milliseconds: (reconnectBackoff.inMilliseconds * 2)
          .clamp(0, reconnectMaxDelay.inMilliseconds),
    );
    reconnectTimer = Timer(delay, () async {
      if (!await isBluetoothOn()) return;
      if (await isLinkedTo(mac)) return;
      AppLogger.info('BT auto-reconnect → $mac');
      await connectInternal(mac, fromUser: false);
    });
  }

  void cancelReconnect() {
    reconnectTimer?.cancel();
    reconnectTimer = null;
  }

  String normalizeMac(String? value) => value?.trim() ?? '';
}
