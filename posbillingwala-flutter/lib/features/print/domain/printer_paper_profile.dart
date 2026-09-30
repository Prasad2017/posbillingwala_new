/* Paper sizes supported by the ESC/POS raster print pipeline. */
enum PrinterPaperSize { mm58, mm60, mm78, mm80 }

extension PrinterPaperSizeX on PrinterPaperSize {
  /* Prefer legacy labels for 58/80 so Android + existing prefs keep working. */
  String get dbValue {
    switch (this) {
      case PrinterPaperSize.mm58:
        return '2-Inch';
      case PrinterPaperSize.mm60:
        return '60mm';
      case PrinterPaperSize.mm78:
        return '78mm';
      case PrinterPaperSize.mm80:
        return '3-Inch';
    }
  }

  PrinterPaperProfile get profile => PrinterPaperProfile.of(this);

  String get shortLabel => profile.shortLabel;

  static PrinterPaperSize fromDb(String? raw) {
    final n = (raw ?? '').toLowerCase().replaceAll(' ', '');
    if (n.isEmpty) return PrinterPaperSize.mm58;
    if (n.contains('60')) return PrinterPaperSize.mm60;
    if (n.contains('78')) return PrinterPaperSize.mm78;
    if (n.contains('58') ||
        n == '2' ||
        n.contains('2-inch') ||
        n.contains('2inch')) {
      return PrinterPaperSize.mm58;
    }
    if (n.contains('80') ||
        n == '3' ||
        n.contains('3-inch') ||
        n.contains('3inch')) {
      return PrinterPaperSize.mm80;
    }
    /* Last-resort legacy: any remaining "3" → 80mm. */
    if (n.contains('3')) return PrinterPaperSize.mm80;
    return PrinterPaperSize.mm58;
  }
}

/* Single source of truth for thermal paper geometry.
 *
 * Physical roll width ≠ printable width. Values are independent per size —
 * not proportional copies of 58↔60 or 80↔78.
 *
 * Baseline (unchanged from prior app behaviour):
 *   58mm / 2-Inch → 48mm printable → 384 dots → 32 chars (Font A @ 12 dots)
 *   80mm / 3-Inch → 72mm printable → 576 dots → 48 chars
 *
 * Added profiles (typical ESC/POS 203 dpi / 8 dots per mm):
 *   60mm → 50mm printable → 400 dots → 33 chars
 *   78mm → 70mm printable → 560 dots → 46 chars
 *
 * imageWidthPx is always divisible by 8 (GS v 0 raster requirement).
 */
class PrinterPaperProfile {
  const PrinterPaperProfile({
    required this.size,
    required this.physicalWidthMm,
    required this.printableWidthMm,
    required this.imageWidthPx,
    required this.charsPerLine,
    required this.marginLeft,
    required this.marginRight,
    required this.previewWidthMm,
    required this.colQtyBase,
    required this.colRateBase,
    required this.colAmountBase,
    required this.qrMaxPx,
    required this.shopFontSize,
    required this.bodyFontSize,
    required this.bannerFontSize,
    required this.lineGap,
    required this.sectionGap,
    required this.isNarrowLayout,
    this.supportsAutoCut = true,
    this.logoWidthFraction = 0.28,
  });

  final PrinterPaperSize size;

  /* Physical paper roll width (mm). */
  final double physicalWidthMm;

  /* Effective print-head / printable width (mm). */
  final double printableWidthMm;

  /* Bitmap / raster width in dots (must be % 8 == 0). */
  final int imageWidthPx;

  /* ESC/POS Font A columns used by text helpers. */
  final int charsPerLine;

  final double marginLeft;
  final double marginRight;

  /* On-screen WoosimTicket / preview width in mm (printable, not physical). */
  final double previewWidthMm;

  /* Base column widths before previewScale (same units as legacy WoosimTicket). */
  final double colQtyBase;
  final double colRateBase;
  final double colAmountBase;

  final double qrMaxPx;
  final double shopFontSize;
  final double bodyFontSize;
  final double bannerFontSize;
  final double lineGap;
  final double sectionGap;

  /* Narrow ≈ 58/60 style fonts; wide ≈ 78/80. */
  final bool isNarrowLayout;

  /* Default capability — overridable per PrinterSettings. */
  final bool supportsAutoCut;

  final double logoWidthFraction;

  int get contentWidthPx =>
      (imageWidthPx - marginLeft.round() - marginRight.round()).clamp(
        40,
        imageWidthPx,
      );

  double get previewScale => imageWidthPx / (previewWidthMm * 3.78);

  double get colScale => previewScale * 0.72;

  double get qtyColumnWidth => colQtyBase * colScale;

  double get rateColumnWidth => colRateBase * colScale;

  double get amountColumnWidth => colAmountBase * colScale;

  double qrSizeFor(int widthPx) =>
      (widthPx * 0.42).clamp(100.0, qrMaxPx);

  String get shortLabel {
    switch (size) {
      case PrinterPaperSize.mm58:
        return '58mm';
      case PrinterPaperSize.mm60:
        return '60mm';
      case PrinterPaperSize.mm78:
        return '78mm';
      case PrinterPaperSize.mm80:
        return '80mm';
    }
  }

  String get detailLabel =>
      '$shortLabel · $charsPerLine characters · multi-language data';

  /* ~7.5px per thermal character for on-screen monospace-style preview. */
  double get onScreenPreviewWidth => charsPerLine * 7.5;

  static const PrinterPaperProfile mm58 = PrinterPaperProfile(
    size: PrinterPaperSize.mm58,
    physicalWidthMm: 58,
    printableWidthMm: 48,
    imageWidthPx: 384,
    charsPerLine: 32,
    marginLeft: 6,
    marginRight: 6,
    previewWidthMm: 48,
    colQtyBase: 36,
    colRateBase: 48,
    colAmountBase: 56,
    qrMaxPx: 160,
    shopFontSize: 20,
    bodyFontSize: 17,
    bannerFontSize: 15,
    lineGap: 2,
    sectionGap: 3.5,
    isNarrowLayout: true,
  );

  /* 60mm roll — distinct printable geometry (not 58+2). */
  static const PrinterPaperProfile mm60 = PrinterPaperProfile(
    size: PrinterPaperSize.mm60,
    physicalWidthMm: 60,
    printableWidthMm: 50,
    imageWidthPx: 400,
    charsPerLine: 33,
    marginLeft: 6,
    marginRight: 6,
    previewWidthMm: 50,
    colQtyBase: 38,
    colRateBase: 50,
    colAmountBase: 58,
    qrMaxPx: 168,
    shopFontSize: 20,
    bodyFontSize: 17,
    bannerFontSize: 15,
    lineGap: 2,
    sectionGap: 3.5,
    isNarrowLayout: true,
  );

  /* 78mm roll — distinct from 80mm (not 80-2). */
  static const PrinterPaperProfile mm78 = PrinterPaperProfile(
    size: PrinterPaperSize.mm78,
    physicalWidthMm: 78,
    printableWidthMm: 70,
    imageWidthPx: 560,
    charsPerLine: 46,
    marginLeft: 8,
    marginRight: 8,
    previewWidthMm: 70,
    colQtyBase: 44,
    colRateBase: 62,
    colAmountBase: 70,
    qrMaxPx: 192,
    shopFontSize: 24,
    bodyFontSize: 20,
    bannerFontSize: 17,
    lineGap: 2.5,
    sectionGap: 4.5,
    isNarrowLayout: false,
  );

  static const PrinterPaperProfile mm80 = PrinterPaperProfile(
    size: PrinterPaperSize.mm80,
    physicalWidthMm: 80,
    printableWidthMm: 72,
    imageWidthPx: 576,
    charsPerLine: 48,
    marginLeft: 8,
    marginRight: 8,
    previewWidthMm: 72,
    colQtyBase: 46,
    colRateBase: 64,
    colAmountBase: 72,
    qrMaxPx: 200,
    shopFontSize: 24,
    bodyFontSize: 20,
    bannerFontSize: 17,
    lineGap: 2.5,
    sectionGap: 4.5,
    isNarrowLayout: false,
  );

  static PrinterPaperProfile of(PrinterPaperSize size) {
    switch (size) {
      case PrinterPaperSize.mm58:
        return mm58;
      case PrinterPaperSize.mm60:
        return mm60;
      case PrinterPaperSize.mm78:
        return mm78;
      case PrinterPaperSize.mm80:
        return mm80;
    }
  }

  static bool isValidImageWidth(int widthPx) =>
      widthPx > 0 && widthPx % 8 == 0 && widthPx <= 576;

  void validateOrThrow() {
    if (imageWidthPx <= 0 || imageWidthPx % 8 != 0) {
      throw StateError(
        'Invalid image width ${imageWidthPx}px for $shortLabel '
        '(must be positive and divisible by 8)',
      );
    }
    if (charsPerLine < 16 || charsPerLine > 64) {
      throw StateError(
        'Invalid charsPerLine $charsPerLine for $shortLabel',
      );
    }
    if (printableWidthMm > physicalWidthMm) {
      throw StateError(
        'Printable width ${printableWidthMm}mm exceeds physical '
        '${physicalWidthMm}mm for $shortLabel',
      );
    }
  }
}
