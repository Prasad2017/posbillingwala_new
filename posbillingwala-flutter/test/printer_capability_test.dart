import 'package:flutter_test/flutter_test.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_capability.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_config.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_job_state.dart';

void main() {
  group('PrinterCapabilityManager cut gate', () {
    final mgr = PrinterCapabilityManager();

    test('autoCut off never cuts', () {
      expect(
        mgr.shouldExecuteCut(
          autoCutEnabled: false,
          capability: CutterCapability.supported,
          profileSupportsAutoCut: true,
        ),
        isFalse,
      );
    });

    test('NOT_SUPPORTED never cuts even if autoCut on', () {
      expect(
        mgr.shouldExecuteCut(
          autoCutEnabled: true,
          capability: CutterCapability.notSupported,
          profileSupportsAutoCut: true,
        ),
        isFalse,
      );
    });

    test('UNKNOWN uses profile fallback', () {
      expect(
        mgr.shouldExecuteCut(
          autoCutEnabled: true,
          capability: CutterCapability.unknown,
          profileSupportsAutoCut: true,
        ),
        isTrue,
      );
      expect(
        mgr.shouldExecuteCut(
          autoCutEnabled: true,
          capability: CutterCapability.unknown,
          profileSupportsAutoCut: false,
        ),
        isFalse,
      );
    });

    test('SUPPORTED cuts when autoCut on', () {
      expect(
        mgr.shouldExecuteCut(
          autoCutEnabled: true,
          capability: CutterCapability.supported,
          profileSupportsAutoCut: false,
        ),
        isTrue,
      );
    });
  });

  group('PrinterConfig validation', () {
    test('rejects empty network host', () {
      const cfg = PrinterConfig(
        id: '1',
        name: 'LAN',
        connectionType: PosPrinterTransport.network,
        ipAddress: '',
        port: 9100,
      );
      expect(cfg.validate(), isNotNull);
    });

    test('accepts configured bluetooth', () {
      const cfg = PrinterConfig(
        id: '1',
        name: 'BT',
        connectionType: PosPrinterTransport.bluetooth,
        bluetoothDeviceId: 'AA:BB:CC:DD:EE:FF',
      );
      expect(cfg.validate(), isNull);
    });

    test('fromSettings preserves network transport', () {
      const settings = PrinterSettings(
        billTransport: PosPrinterTransport.network,
        networkHost: '192.168.1.50',
        networkPort: 9100,
      );
      final cfg = PrinterConfig.fromSettings(settings, isKot: false);
      expect(cfg.connectionType, PosPrinterTransport.network);
      expect(cfg.ipAddress, '192.168.1.50');
      expect(cfg.port, 9100);
    });
  });

  test('cut type storage round-trip', () {
    expect(PrinterCutTypeX.fromStorage('full'), PrinterCutType.full);
    expect(PrinterCutTypeX.fromStorage('partial'), PrinterCutType.partial);
    expect(PrinterCutTypeX.fromStorage(null), PrinterCutType.defaultCut);
    expect(PrinterCutType.partial.useFullCut, isFalse);
    expect(PrinterCutType.full.useFullCut, isTrue);
  });

  test('unknown result user message warns against blind retry', () {
    expect(
      PrintErrorCode.unknownResult.userMessage,
      contains('check the printer before retrying'),
    );
  });
}
