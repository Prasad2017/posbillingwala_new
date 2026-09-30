import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';

/* ESC/POS does NOT expose one universal cutter-capability query.
 * Values must stay honest: UNKNOWN is common and must not be treated as OFF. */
enum CutterCapability {
  supported,
  notSupported,
  unknown,
}

class PrinterCapabilitySnapshot {
  const PrinterCapabilitySnapshot({
    this.cutter = CutterCapability.unknown,
    this.supportsStatus = false,
    this.supportsQr = true,
    this.supportsCashDrawer = false,
    this.probed = false,
  });

  final CutterCapability cutter;
  final bool supportsStatus;
  final bool supportsQr;
  final bool supportsCashDrawer;
  final bool probed;

  PrinterCapabilitySnapshot copyWith({
    CutterCapability? cutter,
    bool? supportsStatus,
    bool? supportsQr,
    bool? supportsCashDrawer,
    bool? probed,
  }) {
    return PrinterCapabilitySnapshot(
      cutter: cutter ?? this.cutter,
      supportsStatus: supportsStatus ?? this.supportsStatus,
      supportsQr: supportsQr ?? this.supportsQr,
      supportsCashDrawer: supportsCashDrawer ?? this.supportsCashDrawer,
      probed: probed ?? this.probed,
    );
  }
}

/* Decides cut safely from capability + user setting + optional profile. */
class PrinterCapabilityManager {
  PrinterCapabilityManager();

  final Map<String, PrinterCapabilitySnapshot> _cache = {};

  PrinterCapabilitySnapshot snapshotFor(String printerKey) =>
      _cache[printerKey] ?? const PrinterCapabilitySnapshot();

  void remember(String printerKey, PrinterCapabilitySnapshot snapshot) {
    _cache[printerKey] = snapshot;
  }

  void clear([String? printerKey]) {
    if (printerKey == null) {
      _cache.clear();
    } else {
      _cache.remove(printerKey);
    }
  }

  /* Connection-time probe. Most ESC/POS models cannot reliably report cutter.
   * Returns UNKNOWN unless a profile / setting asserts otherwise. */
  Future<PrinterCapabilitySnapshot> probe({
    required String printerKey,
    required bool autoCutEnabled,
    bool profileSupportsAutoCut = true,
    String manufacturer = '',
    String model = '',
  }) async {
    CutterCapability cutter = CutterCapability.unknown;
    if (!autoCutEnabled || !profileSupportsAutoCut) {
      cutter = CutterCapability.notSupported;
    } else if (_knownCutterModel(manufacturer, model)) {
      cutter = CutterCapability.supported;
    }
    final snap = PrinterCapabilitySnapshot(
      cutter: cutter,
      supportsStatus: false,
      supportsQr: true,
      supportsCashDrawer: false,
      probed: true,
    );
    remember(printerKey, snap);
    return snap;
  }

  /* Production cut gate — never blind-assume SUPPORTED. */
  bool shouldExecuteCut({
    required bool autoCutEnabled,
    required CutterCapability capability,
    required bool profileSupportsAutoCut,
  }) {
    if (!autoCutEnabled) return false;
    switch (capability) {
      case CutterCapability.supported:
        return true;
      case CutterCapability.notSupported:
        return false;
      case CutterCapability.unknown:
        /* Safe fallback: honour user/profile configuration only. */
        return profileSupportsAutoCut;
    }
  }

  bool shouldCutForSettings(PrinterSettings settings, {String printerKey = 'local'}) {
    final snap = snapshotFor(printerKey);
    final profile = settings.billProfile;
    return shouldExecuteCut(
      autoCutEnabled: settings.supportsAutoCut,
      capability: snap.cutter,
      profileSupportsAutoCut: profile.supportsAutoCut,
    );
  }

  PrinterCutType resolveCutType(PrinterSettings settings) => settings.cutType;

  static bool _knownCutterModel(String manufacturer, String model) {
    final blob = '${manufacturer.toLowerCase()} ${model.toLowerCase()}';
    if (blob.trim().isEmpty) return false;
    /* Conservative allow-list — extend via profile, not billing UI. */
    const known = [
      'epson tm-t20',
      'epson tm-t82',
      'epson tm-m30',
      'star mC-Print',
      'xprinter xp-80c',
    ];
    for (final k in known) {
      if (blob.contains(k.toLowerCase())) return true;
    }
    return false;
  }
}

final printerCapabilityManager = PrinterCapabilityManager();

