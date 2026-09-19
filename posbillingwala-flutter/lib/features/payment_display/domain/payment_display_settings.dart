import 'package:shared_preferences/shared_preferences.dart';

/* Persisted Payment Display preferences + last QR state for reconnect. */
class PaymentDisplaySettings {
  const PaymentDisplaySettings({
    this.autoDisplayNewBills = true,
    this.qrDurationSeconds = defaultQrDurationSeconds,
    this.lastLocalUrl = '',
    this.lastBillJson = '',
  });

  static const defaultQrDurationSeconds = 5 * 60;
  static const minQrDurationSeconds = 60;
  static const maxQrDurationSeconds = 30 * 60;

  final bool autoDisplayNewBills;
  final int qrDurationSeconds;
  final String lastLocalUrl;
  final String lastBillJson;

  Duration get qrDuration {
    final secs = qrDurationSeconds.clamp(
      minQrDurationSeconds,
      maxQrDurationSeconds,
    );
    return Duration(seconds: secs);
  }

  PaymentDisplaySettings copyWith({
    bool? autoDisplayNewBills,
    int? qrDurationSeconds,
    String? lastLocalUrl,
    String? lastBillJson,
    bool clearLastBill = false,
  }) {
    return PaymentDisplaySettings(
      autoDisplayNewBills: autoDisplayNewBills ?? this.autoDisplayNewBills,
      qrDurationSeconds: qrDurationSeconds ?? this.qrDurationSeconds,
      lastLocalUrl: lastLocalUrl ?? this.lastLocalUrl,
      lastBillJson: clearLastBill ? '' : (lastBillJson ?? this.lastBillJson),
    );
  }
}

class PaymentDisplaySettingsStore {
  static const _autoKey = 'payment_display_auto_show';
  static const _durationKey = 'payment_display_qr_duration_sec';
  static const _urlKey = 'payment_display_last_url';
  static const _billKey = 'payment_display_last_bill_json';

  Future<PaymentDisplaySettings> load() async {
    final p = await SharedPreferences.getInstance();
    final duration =
        p.getInt(_durationKey) ?? PaymentDisplaySettings.defaultQrDurationSeconds;
    return PaymentDisplaySettings(
      autoDisplayNewBills: p.getBool(_autoKey) ?? true,
      qrDurationSeconds: duration.clamp(
        PaymentDisplaySettings.minQrDurationSeconds,
        PaymentDisplaySettings.maxQrDurationSeconds,
      ),
      lastLocalUrl: p.getString(_urlKey) ?? '',
      lastBillJson: p.getString(_billKey) ?? '',
    );
  }

  Future<void> save(PaymentDisplaySettings settings) async {
    final p = await SharedPreferences.getInstance();
    await p.setBool(_autoKey, settings.autoDisplayNewBills);
    await p.setInt(
      _durationKey,
      settings.qrDurationSeconds.clamp(
        PaymentDisplaySettings.minQrDurationSeconds,
        PaymentDisplaySettings.maxQrDurationSeconds,
      ),
    );
    await p.setString(_urlKey, settings.lastLocalUrl);
    await p.setString(_billKey, settings.lastBillJson);
  }

  Future<void> clearLastBill() async {
    final p = await SharedPreferences.getInstance();
    await p.remove(_billKey);
  }
}
