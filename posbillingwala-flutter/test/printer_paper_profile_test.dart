import 'package:flutter_test/flutter_test.dart';
import 'package:pos_billingwala_v2/features/print/domain/esc_pos_encoder.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';

void main() {
  group('PrinterPaperProfile', () {
    test('58mm and 80mm keep legacy printable geometry', () {
      expect(PrinterPaperProfile.mm58.imageWidthPx, 384);
      expect(PrinterPaperProfile.mm58.charsPerLine, 32);
      expect(PrinterPaperProfile.mm58.printableWidthMm, 48);
      expect(PrinterPaperProfile.mm80.imageWidthPx, 576);
      expect(PrinterPaperProfile.mm80.charsPerLine, 48);
      expect(PrinterPaperProfile.mm80.printableWidthMm, 72);
    });

    test('60mm and 78mm use independent printable widths', () {
      expect(PrinterPaperProfile.mm60.imageWidthPx, 400);
      expect(PrinterPaperProfile.mm60.charsPerLine, 33);
      expect(PrinterPaperProfile.mm60.printableWidthMm, 50);
      expect(PrinterPaperProfile.mm60.imageWidthPx % 8, 0);

      expect(PrinterPaperProfile.mm78.imageWidthPx, 560);
      expect(PrinterPaperProfile.mm78.charsPerLine, 46);
      expect(PrinterPaperProfile.mm78.printableWidthMm, 70);
      expect(PrinterPaperProfile.mm78.imageWidthPx % 8, 0);

      /* Not simple 58→60 or 80→78 copies. */
      expect(
        PrinterPaperProfile.mm60.imageWidthPx,
        isNot(PrinterPaperProfile.mm58.imageWidthPx),
      );
      expect(
        PrinterPaperProfile.mm78.imageWidthPx,
        isNot(PrinterPaperProfile.mm80.imageWidthPx),
      );
    });

    test('all profiles validate', () {
      for (final size in PrinterPaperSize.values) {
        expect(() => PrinterPaperProfile.of(size).validateOrThrow(), returnsNormally);
      }
    });

    test('QR constrained to printable area', () {
      for (final size in PrinterPaperSize.values) {
        final p = PrinterPaperProfile.of(size);
        final qr = p.qrSizeFor(p.imageWidthPx);
        expect(qr, lessThanOrEqualTo(p.imageWidthPx.toDouble()));
        expect(qr, lessThanOrEqualTo(p.qrMaxPx));
      }
    });
  });

  group('PrinterPaperSize persistence', () {
    test('legacy 2-Inch / 3-Inch round-trip', () {
      expect(PrinterPaperSizeX.fromDb('2-Inch'), PrinterPaperSize.mm58);
      expect(PrinterPaperSizeX.fromDb('3-Inch'), PrinterPaperSize.mm80);
      expect(PrinterPaperSize.mm58.dbValue, '2-Inch');
      expect(PrinterPaperSize.mm80.dbValue, '3-Inch');
    });

    test('60mm / 78mm round-trip', () {
      expect(PrinterPaperSizeX.fromDb('60mm'), PrinterPaperSize.mm60);
      expect(PrinterPaperSizeX.fromDb('78mm'), PrinterPaperSize.mm78);
      expect(PrinterPaperSize.mm60.dbValue, '60mm');
      expect(PrinterPaperSize.mm78.dbValue, '78mm');
    });

    test('settings charsPerLine follows profile', () {
      expect(
        const PrinterSettings(paperSize: PrinterPaperSize.mm58).charsPerLine,
        32,
      );
      expect(
        const PrinterSettings(paperSize: PrinterPaperSize.mm60).charsPerLine,
        33,
      );
      expect(
        const PrinterSettings(paperSize: PrinterPaperSize.mm78).charsPerLine,
        46,
      );
      expect(
        const PrinterSettings(paperSize: PrinterPaperSize.mm80).charsPerLine,
        48,
      );
    });
  });

  test('ESC/POS cut with caller feed uses ESC d then cut', () {
    final encoder = EscPosEncoder(charsPerLine: 32)
      ..init()
      ..cut(full: true, feedToCutter: 3);
    final bytes = encoder.bytes;
    expect(
      bytes.sublist(bytes.length - 10),
      [0x1B, 0x64, 0x03, 0x1D, 0x56, 0x41, 0x00, 0x1D, 0x56, 0x00],
    );
  });

  test('ESC/POS cut with zero feed has no ESC d advance', () {
    final encoder = EscPosEncoder(charsPerLine: 32)
      ..init()
      ..cut(full: true, feedToCutter: 0);
    final bytes = encoder.bytes;
    expect(
      bytes.sublist(bytes.length - 7),
      [0x1D, 0x56, 0x41, 0x00, 0x1D, 0x56, 0x00],
    );
  });

  test('bill ending feed is ESC d n and works without cut', () {
    final withCut = EscPosEncoder(charsPerLine: 32)
      ..init()
      ..feed(2)
      ..cut(full: true, feedToCutter: 0);
    expect(withCut.bytes, [
      0x1B, 0x40,
      0x1B, 0x64, 0x02,
      0x1D, 0x56, 0x41, 0x00,
      0x1D, 0x56, 0x00,
    ]);

    final noCut = EscPosEncoder(charsPerLine: 32)
      ..init()
      ..feed(2);
    expect(noCut.bytes, [0x1B, 0x40, 0x1B, 0x64, 0x02]);

    final zeroFeed = EscPosEncoder(charsPerLine: 32)
      ..init()
      ..feed(0);
    expect(zeroFeed.bytes, [0x1B, 0x40]);
  });
}
