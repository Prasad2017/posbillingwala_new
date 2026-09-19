import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/payment_display/data/payment_display_server.dart';
import 'package:pos_billingwala_v2/features/payment_display/domain/payment_display_models.dart';
import 'package:pos_billingwala_v2/features/payment_display/domain/payment_display_settings.dart';

class PaymentDisplayUiState {
  const PaymentDisplayUiState({
    this.status = PaymentDisplayConnectionStatus.stopped,
    this.localUrl = '',
    this.pairingUrl = '',
    this.connectedClients = 0,
    this.autoDisplayNewBills = true,
    this.qrDurationSeconds = PaymentDisplaySettings.defaultQrDurationSeconds,
    this.activeBill,
    this.errorMessage,
    this.serverRunning = false,
  });

  final PaymentDisplayConnectionStatus status;
  final String localUrl;
  final String pairingUrl;
  final int connectedClients;
  final bool autoDisplayNewBills;
  final int qrDurationSeconds;
  final PaymentDisplayBillPayload? activeBill;
  final String? errorMessage;
  final bool serverRunning;

  bool get isConnected =>
      status == PaymentDisplayConnectionStatus.connected &&
      connectedClients > 0;

  PaymentDisplayUiState copyWith({
    PaymentDisplayConnectionStatus? status,
    String? localUrl,
    String? pairingUrl,
    int? connectedClients,
    bool? autoDisplayNewBills,
    int? qrDurationSeconds,
    PaymentDisplayBillPayload? activeBill,
    String? errorMessage,
    bool? serverRunning,
    bool clearError = false,
    bool clearBill = false,
  }) {
    return PaymentDisplayUiState(
      status: status ?? this.status,
      localUrl: localUrl ?? this.localUrl,
      pairingUrl: pairingUrl ?? this.pairingUrl,
      connectedClients: connectedClients ?? this.connectedClients,
      autoDisplayNewBills: autoDisplayNewBills ?? this.autoDisplayNewBills,
      qrDurationSeconds: qrDurationSeconds ?? this.qrDurationSeconds,
      activeBill: clearBill ? null : (activeBill ?? this.activeBill),
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      serverRunning: serverRunning ?? this.serverRunning,
    );
  }
}

/* Owns local server lifecycle, pairing, QR expiry (authoritative), reconnect. */
class DisplayConnectionManager extends Notifier<PaymentDisplayUiState> {
  final _settingsStore = PaymentDisplaySettingsStore();
  PaymentDisplayServer? _server;
  Timer? _expiryTimer;
  Timer? _ipWatchTimer;
  PaymentDisplaySettings _settings = const PaymentDisplaySettings();

  @override
  PaymentDisplayUiState build() {
    ref.onDispose(() {
      _expiryTimer?.cancel();
      _ipWatchTimer?.cancel();
      unawaited(_server?.stop());
    });
    Future.microtask(_bootstrap);
    return const PaymentDisplayUiState();
  }

  Future<void> _bootstrap() async {
    if (kIsWeb || AppPlatform.isWeb) return;
    _settings = await _settingsStore.load();
    PaymentDisplayBillPayload? restored;
    final raw = _settings.lastBillJson.trim();
    if (raw.isNotEmpty) {
      try {
        final map = jsonDecode(raw);
        if (map is Map<String, dynamic>) {
          final bill = PaymentDisplayBillPayload.fromJson(map);
          if (!bill.isExpired) {
            restored = bill;
          } else {
            await _settingsStore.clearLastBill();
          }
        }
      } catch (_) {
        await _settingsStore.clearLastBill();
      }
    }
    state = state.copyWith(
      autoDisplayNewBills: _settings.autoDisplayNewBills,
      qrDurationSeconds: _settings.qrDurationSeconds,
      localUrl: _settings.lastLocalUrl,
      activeBill: restored,
      clearBill: restored == null,
    );
    if (restored != null) {
      _scheduleExpiry(restored.expiresAt);
    }
  }

  PaymentDisplayServer get _ensureServer {
    return _server ??= PaymentDisplayServer(
      onClientCountChanged: (count) {
        final nextStatus = !_server!.isRunning
            ? PaymentDisplayConnectionStatus.stopped
            : count > 0
            ? PaymentDisplayConnectionStatus.connected
            : PaymentDisplayConnectionStatus.waitingForPair;
        state = state.copyWith(
          connectedClients: count,
          status: nextStatus,
          pairingUrl: _server?.pairingUrl ?? state.pairingUrl,
          localUrl: _server?.displayBaseUrl ?? state.localUrl,
        );
      },
      onError: (msg) {
        state = state.copyWith(
          status: PaymentDisplayConnectionStatus.error,
          errorMessage: msg,
        );
      },
    );
  }

  Future<bool> startServer({bool rotateToken = true}) async {
    if (kIsWeb || AppPlatform.isWeb) {
      state = state.copyWith(
        status: PaymentDisplayConnectionStatus.error,
        errorMessage: 'Payment display requires the Android POS app.',
      );
      return false;
    }
    state = state.copyWith(
      status: PaymentDisplayConnectionStatus.starting,
      clearError: true,
    );
    final server = _ensureServer;
    final ok = await server.start();
    if (!ok) {
      state = state.copyWith(
        status: PaymentDisplayConnectionStatus.error,
        serverRunning: false,
        errorMessage: state.errorMessage ?? 'Could not start display server.',
      );
      return false;
    }
    if (rotateToken) server.rotatePairingToken();
    if (state.activeBill != null) {
      server.setActiveBill(state.activeBill);
    }
    _startIpWatch();
    final url = server.displayBaseUrl ?? '';
    final pairing = server.pairingUrl ?? '';
    _settings = _settings.copyWith(lastLocalUrl: url);
    await _settingsStore.save(_settings);
    state = state.copyWith(
      status: server.connectedClientCount > 0
          ? PaymentDisplayConnectionStatus.connected
          : PaymentDisplayConnectionStatus.waitingForPair,
      serverRunning: true,
      localUrl: url,
      pairingUrl: pairing,
      connectedClients: server.connectedClientCount,
      clearError: true,
    );
    return true;
  }

  Future<void> stopServer() async {
    _ipWatchTimer?.cancel();
    _expiryTimer?.cancel();
    await _server?.stop();
    state = state.copyWith(
      status: PaymentDisplayConnectionStatus.stopped,
      serverRunning: false,
      connectedClients: 0,
      pairingUrl: '',
      clearError: true,
    );
  }

  Future<void> showPairingQr() async {
    final running = state.serverRunning
        ? true
        : await startServer(rotateToken: true);
    if (!running) return;
    _server?.rotatePairingToken();
    state = state.copyWith(
      pairingUrl: _server?.pairingUrl ?? '',
      localUrl: _server?.displayBaseUrl ?? state.localUrl,
      status: state.connectedClients > 0
          ? PaymentDisplayConnectionStatus.connected
          : PaymentDisplayConnectionStatus.waitingForPair,
    );
    AppLogger.info('PAYMENT_DISPLAY_PAIRING_STARTED');
  }

  Future<void> reconnect() async {
    await _server?.refreshNetworkIdentity();
    if (!state.serverRunning) {
      await startServer(rotateToken: false);
      return;
    }
    state = state.copyWith(
      localUrl: _server?.displayBaseUrl ?? state.localUrl,
      pairingUrl: _server?.pairingUrl ?? state.pairingUrl,
      clearError: true,
    );
    AppLogger.info('PAYMENT_DISPLAY_RECONNECT');
  }

  Future<void> disconnectDisplays() async {
    await _server?.disconnectAllClients();
    state = state.copyWith(
      connectedClients: 0,
      status: state.serverRunning
          ? PaymentDisplayConnectionStatus.waitingForPair
          : PaymentDisplayConnectionStatus.stopped,
    );
  }

  Future<void> setAutoDisplayNewBills(bool value) async {
    _settings = _settings.copyWith(autoDisplayNewBills: value);
    await _settingsStore.save(_settings);
    state = state.copyWith(autoDisplayNewBills: value);
  }

  Future<void> setQrDurationSeconds(int seconds) async {
    final clamped = seconds.clamp(
      PaymentDisplaySettings.minQrDurationSeconds,
      PaymentDisplaySettings.maxQrDurationSeconds,
    );
    _settings = _settings.copyWith(qrDurationSeconds: clamped);
    await _settingsStore.save(_settings);
    state = state.copyWith(qrDurationSeconds: clamped);
  }

  Duration get qrDuration => _settings.qrDuration;

  bool get autoDisplayEnabled => _settings.autoDisplayNewBills;

  Future<ShowPaymentDisplayResult> publishBill(
    PaymentDisplayBillPayload bill,
  ) async {
    if (kIsWeb || AppPlatform.isWeb) {
      return const ShowPaymentDisplayResult(
        code: ShowPaymentDisplayResultCode.notSupported,
        message: 'Payment display requires the Android POS app.',
      );
    }
    if (!state.serverRunning || _server == null || !_server!.isRunning) {
      return const ShowPaymentDisplayResult(
        code: ShowPaymentDisplayResultCode.displayNotConnected,
        message: 'Payment display is not connected.',
      );
    }
    if (state.connectedClients < 1) {
      return const ShowPaymentDisplayResult(
        code: ShowPaymentDisplayResultCode.displayNotConnected,
        message: 'Payment display is not connected.',
      );
    }

    _expiryTimer?.cancel();
    _server!.setActiveBill(bill);
    await _server!.broadcastBillUpdated(bill);
    _scheduleExpiry(bill.expiresAt);
    state = state.copyWith(activeBill: bill, clearError: true);
    _settings = _settings.copyWith(lastBillJson: jsonEncode(bill.toJson()));
    await _settingsStore.save(_settings);
    return ShowPaymentDisplayResult(
      code: ShowPaymentDisplayResultCode.success,
      payload: bill,
    );
  }

  Future<void> clearBill({String reason = 'expired'}) async {
    _expiryTimer?.cancel();
    if (_server != null && _server!.isRunning) {
      await _server!.broadcastBillCleared(reason: reason);
    }
    state = state.copyWith(clearBill: true);
    await _settingsStore.clearLastBill();
    _settings = _settings.copyWith(clearLastBill: true);
  }

  void _scheduleExpiry(DateTime expiresAt) {
    _expiryTimer?.cancel();
    final wait = expiresAt.difference(DateTime.now());
    if (wait <= Duration.zero) {
      unawaited(clearBill(reason: 'expired'));
      return;
    }
    _expiryTimer = Timer(wait, () {
      unawaited(clearBill(reason: 'expired'));
    });
  }

  void _startIpWatch() {
    _ipWatchTimer?.cancel();
    _ipWatchTimer = Timer.periodic(const Duration(seconds: 20), (_) async {
      if (_server == null || !_server!.isRunning) return;
      await _server!.refreshNetworkIdentity();
      final url = _server!.displayBaseUrl ?? state.localUrl;
      final pairing = _server!.pairingUrl ?? state.pairingUrl;
      if (url != state.localUrl || pairing != state.pairingUrl) {
        state = state.copyWith(localUrl: url, pairingUrl: pairing);
        _settings = _settings.copyWith(lastLocalUrl: url);
        await _settingsStore.save(_settings);
      }
    });
  }
}

final displayConnectionManagerProvider =
    NotifierProvider<DisplayConnectionManager, PaymentDisplayUiState>(
      DisplayConnectionManager.new,
    );
