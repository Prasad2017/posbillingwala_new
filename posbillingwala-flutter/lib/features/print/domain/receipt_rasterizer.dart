import 'dart:async';
import 'dart:io';
import 'dart:ui' as ui;

import 'package:flutter/foundation.dart';
import 'package:flutter/painting.dart';
import 'package:flutter/services.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_slip_builder.dart';
import 'package:pos_billingwala_v2/features/print/domain/esc_pos_encoder.dart';
import 'package:pos_billingwala_v2/features/print/domain/kot_slip_layout.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_image_encoder.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_capability.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/thermal_ticket.dart';
import 'package:qr_flutter/qr_flutter.dart';

/* Renders receipt Unicode text (any language + ₹) to ESC/POS raster bytes, */
/* same approach as Android layout → bitmap → [PrintImage]. */
class ReceiptRasterizer {
  const ReceiptRasterizer();

  static PrinterPaperProfile profileFor(PrinterPaperSize size) =>
      PrinterPaperProfile.of(size);

  static int widthPxFor(PrinterPaperSize size) => profileFor(size).imageWidthPx;

  /* Scale WoosimTicket (mm×3.78 preview) metrics onto printer pixel width. */
  static double previewScale(PrinterPaperSize size) =>
      profileFor(size).previewScale;

  Future<List<int>> encodeTicket(
    ThermalTicket ticket, {
    required PrinterSettings settings,
    String? logoPath,
    int? feedLinesOverride,
    bool useAssetLogoFallback = false,
  }) async {
    final profile = settings.billProfile;
    profile.validateOrThrow();
    final width = profile.imageWidthPx;
    final rendered = await renderTicket(
      ticket,
      width,
      paperSize: settings.paperSize,
      logoPath: logoPath,
      useAssetLogoFallback: useAssetLogoFallback,
    );
    final doCut = printerCapabilityManager.shouldCutForSettings(settings);
    return await _escPosRaster(
      rendered,
      charsPerLine: profile.charsPerLine,
      feedLines: feedLinesOverride ?? settings.feedLines,
      autoCut: doCut,
      fullCut: settings.cutType.useFullCut,
    );
  }

  /* Kitchen ticket — same paper width / Poppins metrics as invoice ticket. */
  Future<List<int>> encodeKot(
    KotSlipLayout layout, {
    required PrinterSettings settings,
    int? feedLinesOverride,
  }) async {
    final profile = settings.billProfile;
    profile.validateOrThrow();
    final width = profile.imageWidthPx;
    final rendered = await renderKot(
      layout,
      width,
      paperSize: settings.paperSize,
    );
    final doCut = printerCapabilityManager.shouldCutForSettings(settings);
    return await _escPosRaster(
      rendered,
      charsPerLine: profile.charsPerLine,
      feedLines: feedLinesOverride ?? settings.feedLines,
      autoCut: doCut,
      fullCut: settings.cutType.useFullCut,
    );
  }

  Future<List<int>> encodeText(
    String text, {
    required PrinterSettings settings,
    String? qrPayload,
    String? logoPath,
    String? qrMarker,
    int? feedLinesOverride,
    bool useAssetLogoFallback = false,
    double fontSizeScale = 1,
  }) async {
    final profile = settings.billProfile;
    profile.validateOrThrow();
    final width = profile.imageWidthPx;
    final rendered = await render(
      text,
      width,
      paperSize: settings.paperSize,
      qrPayload: qrPayload,
      logoPath: logoPath,
      qrMarker: qrMarker,
      useAssetLogoFallback: useAssetLogoFallback,
      fontSizeScale: fontSizeScale,
    );
    final doCut = printerCapabilityManager.shouldCutForSettings(settings);
    return await _escPosRaster(
      rendered,
      charsPerLine: profile.charsPerLine,
      feedLines: feedLinesOverride ?? settings.feedLines,
      autoCut: doCut,
      fullCut: settings.cutType.useFullCut,
    );
  }

  /* Same bitmap as printMessSlip — PNG for on-screen thermal preview. */
  Future<Uint8List> renderTextPng(
    String text, {
    required PrinterSettings settings,
    String? qrPayload,
    String? logoPath,
    String? qrMarker,
    bool useAssetLogoFallback = false,
  }) async {
    final width = widthPxFor(settings.paperSize);
    final rendered = await render(
      text,
      width,
      paperSize: settings.paperSize,
      qrPayload: qrPayload,
      logoPath: logoPath,
      qrMarker: qrMarker,
      useAssetLogoFallback: useAssetLogoFallback,
    );
    return rgbaToPng(rendered);
  }

  /* Mess QR / coupon — shop header painted like invoice [renderTicket]. */
  Future<List<int>> encodeMessLayout(
    MessSlipLayout layout, {
    required PrinterSettings settings,
    String? logoPath,
    int? feedLinesOverride,
    bool useAssetLogoFallback = false,
  }) async {
    final profile = settings.billProfile;
    profile.validateOrThrow();
    final width = profile.imageWidthPx;
    final rendered = await renderMessLayout(
      layout,
      width,
      paperSize: settings.paperSize,
      logoPath: logoPath,
      useAssetLogoFallback: useAssetLogoFallback,
    );
    final doCut = printerCapabilityManager.shouldCutForSettings(settings);
    return await _escPosRaster(
      rendered,
      charsPerLine: profile.charsPerLine,
      feedLines: feedLinesOverride ?? settings.feedLines,
      autoCut: doCut,
      fullCut: settings.cutType.useFullCut,
    );
  }

  Future<Uint8List> renderMessLayoutPng(
    MessSlipLayout layout, {
    required PrinterSettings settings,
    String? logoPath,
    bool useAssetLogoFallback = false,
  }) async {
    final width = widthPxFor(settings.paperSize);
    final rendered = await renderMessLayout(
      layout,
      width,
      paperSize: settings.paperSize,
      logoPath: logoPath,
      useAssetLogoFallback: useAssetLogoFallback,
    );
    return rgbaToPng(rendered);
  }

  Future<RenderedImage> renderMessLayout(
    MessSlipLayout layout,
    int widthPx, {
    required PrinterPaperSize paperSize,
    String? logoPath,
    bool useAssetLogoFallback = false,
  }) async {
    final profile = profileFor(paperSize);
    final hPad = profile.marginLeft;
    final shopSize = profile.shopFontSize;
    final bodySize = profile.bodyFontSize;
    final bannerSize = profile.bannerFontSize;
    final lineGap = profile.lineGap;
    final sectionGap = profile.sectionGap;
    final contentW = widthPx - hPad * 2;

    TextStyle style({
      double? size,
      FontWeight weight = FontWeight.w500,
    }) => AppFonts.printBody(
      fontSize: size ?? bodySize,
      weight: weight,
      height: 1.15,
    );

    final logoImage = await loadLogo(
      logoPath: logoPath,
      widthPx: widthPx,
      useAssetLogoFallback: useAssetLogoFallback,
      widthFraction: profile.logoWidthFraction,
    );

    final ops = <_PaintOp>[];
    var y = 8.0;

    if (logoImage != null) {
      final lw = logoImage.width.toDouble();
      final lh = logoImage.height.toDouble();
      ops.add(_PaintOp.image(logoImage, (widthPx - lw) / 2, y));
      y += lh + 4;
    }

    /* Shop name larger+bold; address / mobile / GST = body — same as bill. */
    for (var i = 0; i < layout.shopLines.length; i++) {
      y = _paintCentered(
        ops,
        layout.shopLines[i],
        style(
          size: i == 0 ? shopSize : bodySize,
          weight: i == 0 ? FontWeight.w700 : FontWeight.w500,
        ),
        widthPx: widthPx,
        maxWidth: contentW,
        y: y,
        gap: lineGap,
      );
    }

    if (layout.shopLines.isNotEmpty && layout.bodyLines.isNotEmpty) {
      y += sectionGap;
    }

    for (var i = 0; i < layout.bodyLines.length; i++) {
      final isTitle = i == 0;
      y = _paintCentered(
        ops,
        layout.bodyLines[i],
        style(
          size: isTitle ? bannerSize + 2 : bodySize,
          weight: isTitle ? FontWeight.w700 : FontWeight.w500,
        ),
        widthPx: widthPx,
        maxWidth: contentW,
        y: y,
        gap: lineGap,
      );
    }

    final qr = layout.qrPayload?.trim() ?? '';
    if (qr.isNotEmpty) {
      final qrSize = profile.qrSizeFor(widthPx);
      y += sectionGap;
      ops.add(_PaintOp.qr(qr, (widthPx - qrSize) / 2, y, qrSize));
      y += qrSize + sectionGap + lineGap * 2;
    }

    for (var i = 0; i < layout.footerLines.length; i++) {
      final line = layout.footerLines[i];
      final isToken = line.toLowerCase().startsWith('token:');
      y = _paintCentered(
        ops,
        line,
        style(
          size: bodySize,
          weight: isToken ? FontWeight.w700 : FontWeight.w500,
        ),
        widthPx: widthPx,
        maxWidth: contentW,
        y: y,
        gap: lineGap,
      );
    }

    final height = (y + 16).ceil().clamp(24, 12000);
    final recorder = ui.PictureRecorder();
    final canvas = Canvas(recorder);
    canvas.drawRect(
      Rect.fromLTWH(0, 0, widthPx.toDouble(), height.toDouble()),
      Paint()..color = const Color(0xFFFFFFFF),
    );
    for (final op in ops) {
      op.paint(canvas, this);
    }
    final picture = recorder.endRecording();
    final image = await picture.toImage(widthPx, height);
    final byteData = await image.toByteData(format: ui.ImageByteFormat.rawRgba);
    image.dispose();
    for (final op in ops) {
      op.dispose();
    }
    if (byteData == null) {
      return RenderedImage(
        rgba: Uint8List(widthPx * height * 4),
        width: widthPx,
        height: height,
      );
    }
    return RenderedImage(
      rgba: byteData.buffer.asUint8List(),
      width: widthPx,
      height: height,
    );
  }

  static Future<Uint8List> rgbaToPng(RenderedImage rendered) async {
    final completer = Completer<ui.Image>();
    ui.decodeImageFromPixels(
      rendered.rgba,
      rendered.width,
      rendered.height,
      ui.PixelFormat.rgba8888,
      completer.complete,
    );
    final image = await completer.future;
    final data = await image.toByteData(format: ui.ImageByteFormat.png);
    image.dispose();
    if (data == null) {
      throw StateError('Could not encode mess slip preview');
    }
    return data.buffer.asUint8List();
  }

  Future<List<int>> _escPosRaster(
    RenderedImage rendered, {
    required int charsPerLine,
    required int feedLines,
    bool autoCut = true,
    bool fullCut = true,
  }) async {
    /* Floyd–Steinberg is CPU-heavy — keep it off the UI isolate. */
    final raster = await compute(
      encodeRgbaIsolate,
      EncodeRgbaArgs(
        rgba: rendered.rgba,
        width: rendered.width,
        height: rendered.height,
        brightValue: 128,
      ),
    );
    final lines = feedLines.clamp(0, 20);
    final out = EscPosEncoder(charsPerLine: charsPerLine)
      ..init()
      ..raw(raster);
    /* Feed lines always apply (with or without auto-cut / cutter hardware).
     * 0 → no blank space; N → exactly N lines. Cut is optional and separate. */
    out.feed(lines);
    if (autoCut) {
      try {
        out.cut(full: fullCut, feedToCutter: 0);
      } catch (_) {
        /* Printers without a cutter ignore cut bytes; feed already applied. */
      }
    }
    return out.bytes;
  }

  /* Pixel columns like [WoosimTicket] — not space-padded monospace text. */
  /* Font/gap sizes stay thermal-dense; only column widths track paper px. */
  Future<RenderedImage> renderTicket(
    ThermalTicket ticket,
    int widthPx, {
    required PrinterPaperSize paperSize,
    String? logoPath,
    bool useAssetLogoFallback = false,
  }) async {
    final profile = profileFor(paperSize);
    final hPad = profile.marginLeft;
    final qtyW = profile.qtyColumnWidth;
    final rateW = profile.rateColumnWidth;
    final amountW = profile.amountColumnWidth;
    final shopSize = profile.shopFontSize;
    final bodySize = profile.bodyFontSize;
    final bannerSize = profile.bannerFontSize;
    final lineGap = profile.lineGap;
    final sectionGap = profile.sectionGap;
    final contentW = widthPx - hPad * 2;
    final itemColW =
        (contentW - qtyW - rateW - amountW).clamp(40.0, contentW);

    TextStyle style({
      double? size,
      FontWeight weight = FontWeight.w500,
    }) => AppFonts.printBody(
      fontSize: size ?? bodySize,
      weight: weight,
      height: 1.15,
    );

    final logoImage = await loadLogo(
      logoPath: logoPath,
      widthPx: widthPx,
      useAssetLogoFallback: useAssetLogoFallback,
      widthFraction: profile.logoWidthFraction,
    );

    final ops = <_PaintOp>[];
    var y = 8.0;

    if (logoImage != null) {
      final lw = logoImage.width.toDouble();
      final lh = logoImage.height.toDouble();
      ops.add(_PaintOp.image(logoImage, (widthPx - lw) / 2, y));
      y += lh + 4;
    }

    for (var i = 0; i < ticket.shopLines.length; i++) {
      y = _paintCentered(
        ops,
        ticket.shopLines[i],
        style(
          size: i == 0 ? shopSize : bodySize,
          weight: i == 0 ? FontWeight.w700 : FontWeight.w500,
        ),
        widthPx: widthPx,
        maxWidth: contentW,
        y: y,
        gap: lineGap,
      );
    }

    for (final line in ticket.metaLines) {
      y = _paintLeft(
        ops,
        line,
        style(),
        left: hPad,
        maxWidth: contentW,
        y: y,
        gap: lineGap,
      );
    }

    y = _paintCentered(
      ops,
      ticket.copyBanner,
      style(size: bannerSize, weight: FontWeight.w500),
      widthPx: widthPx,
      maxWidth: contentW,
      y: y + lineGap,
      gap: sectionGap,
    );

    y = _paintRule(ops, left: hPad, width: contentW, y: y);

    y = _paintColumns(
      ops,
      left: hPad,
      y: y,
      gap: lineGap,
      cells: [
        _Col(ticket.colItem, itemColW, TextAlign.left, FontWeight.w700),
        _Col(ticket.colQty, qtyW, TextAlign.center, FontWeight.w700),
        _Col(ticket.colRate, rateW, TextAlign.center, FontWeight.w700),
        _Col(ticket.colAmount, amountW, TextAlign.right, FontWeight.w700),
      ],
      style: style(),
    );

    y = _paintRule(ops, left: hPad, width: contentW, y: y);

    for (final item in ticket.items) {
      y = _paintLeft(
        ops,
        item.name,
        style(weight: FontWeight.w500),
        left: hPad,
        maxWidth: contentW,
        y: y + lineGap,
        gap: 0,
      );
      y = _paintColumns(
        ops,
        left: hPad,
        y: y,
        gap: sectionGap,
        cells: [
          _Col('', itemColW, TextAlign.left, FontWeight.w500),
          _Col(item.qty, qtyW, TextAlign.center, FontWeight.w500),
          _Col(item.rate, rateW, TextAlign.center, FontWeight.w500),
          _Col(item.amount, amountW, TextAlign.right, FontWeight.w500),
        ],
        style: style(),
      );
    }

    y = _paintRule(ops, left: hPad, width: contentW, y: y);

    for (final pair in ticket.pairs) {
      y = _paintPair(
        ops,
        left: pair.$1,
        right: pair.$2,
        style: style(weight: FontWeight.w600),
        leftPad: hPad,
        maxWidth: contentW,
        y: y,
        gap: sectionGap,
      );
    }

    y = _paintRule(ops, left: hPad, width: contentW, y: y);

    if (ticket.terms.trim().isNotEmpty) {
      y = _paintCentered(
        ops,
        ticket.terms.trim(),
        /* Same family as Powered by, slightly larger. */
        style(size: bodySize * 0.95, weight: FontWeight.w400),
        widthPx: widthPx,
        maxWidth: contentW,
        y: y + lineGap,
        gap: sectionGap,
      );
    }

    final qr = ticket.qrPayload?.trim() ?? '';
    if (qr.isNotEmpty) {
      final qrSize = profile.qrSizeFor(widthPx);
      y += sectionGap;
      ops.add(
        _PaintOp.qr(qr, (widthPx - qrSize) / 2, y, qrSize),
      );
      /* Extra gap so Powered by is not tight under the QR. */
      y += qrSize + sectionGap + lineGap * 2;
    }

    for (final line in ticket.footerLines) {
      y = _paintCentered(
        ops,
        line,
        /* Powered by / website — normal weight, slightly smaller. */
        style(size: bodySize * 0.82, weight: FontWeight.w400),
        widthPx: widthPx,
        maxWidth: contentW,
        y: y,
        gap: lineGap,
      );
    }

    final height = (y + 16).ceil().clamp(24, 12000);
    final recorder = ui.PictureRecorder();
    final canvas = Canvas(recorder);
    canvas.drawRect(
      Rect.fromLTWH(0, 0, widthPx.toDouble(), height.toDouble()),
      Paint()..color = const Color(0xFFFFFFFF),
    );
    for (final op in ops) {
      op.paint(canvas, this);
    }
    final picture = recorder.endRecording();
    final image = await picture.toImage(widthPx, height);
    final byteData = await image.toByteData(format: ui.ImageByteFormat.rawRgba);
    image.dispose();
    for (final op in ops) {
      op.dispose();
    }
    if (byteData == null) {
      return RenderedImage(
        rgba: Uint8List(widthPx * height * 4),
        width: widthPx,
        height: height,
      );
    }
    return RenderedImage(
      rgba: byteData.buffer.asUint8List(),
      width: widthPx,
      height: height,
    );
  }

  /* KOT slip — title + meta + ITEMS/QTY columns (invoice paper metrics). */
  Future<RenderedImage> renderKot(
    KotSlipLayout layout,
    int widthPx, {
    required PrinterPaperSize paperSize,
  }) async {
    final profile = profileFor(paperSize);
    final hPad = profile.marginLeft;
    final qtyW = profile.qtyColumnWidth.clamp(36.0, 72.0);
    final titleSize = profile.shopFontSize * 0.95;
    final bodySize = profile.bodyFontSize * 1.12;
    final lineGap = profile.lineGap;
    final sectionGap = profile.sectionGap;
    final contentW = widthPx - hPad * 2;
    final itemColW = (contentW - qtyW).clamp(40.0, contentW);

    TextStyle style({
      double? size,
      FontWeight weight = FontWeight.w500,
    }) => AppFonts.printBody(
      fontSize: size ?? bodySize,
      weight: weight,
      height: 1.2,
    );

    final ops = <_PaintOp>[];
    var y = 10.0;

    y = _paintCentered(
      ops,
      layout.title,
      style(size: titleSize, weight: FontWeight.w700),
      widthPx: widthPx,
      maxWidth: contentW,
      y: y,
      gap: sectionGap,
    );

    y = _paintRule(ops, left: hPad, width: contentW, y: y);

    for (final line in layout.metaLines) {
      y = _paintLeft(
        ops,
        line,
        style(),
        left: hPad,
        maxWidth: contentW,
        y: y,
        gap: lineGap,
      );
    }

    y = _paintRule(ops, left: hPad, width: contentW, y: y);

    y = _paintColumns(
      ops,
      left: hPad,
      y: y,
      gap: lineGap,
      cells: [
        _Col(layout.colItem, itemColW, TextAlign.left, FontWeight.w700),
        _Col(layout.colQty, qtyW, TextAlign.right, FontWeight.w700),
      ],
      style: style(),
    );

    y = _paintRule(ops, left: hPad, width: contentW, y: y);

    for (final item in layout.items) {
      y = _paintColumns(
        ops,
        left: hPad,
        y: y + lineGap,
        gap: sectionGap,
        cells: [
          _Col(item.name, itemColW, TextAlign.left, FontWeight.w500),
          _Col(item.qty, qtyW, TextAlign.right, FontWeight.w700),
        ],
        style: style(),
      );
    }

    y = _paintRule(ops, left: hPad, width: contentW, y: y);

    final height = (y + 16).ceil().clamp(24, 12000);
    final recorder = ui.PictureRecorder();
    final canvas = Canvas(recorder);
    canvas.drawRect(
      Rect.fromLTWH(0, 0, widthPx.toDouble(), height.toDouble()),
      Paint()..color = const Color(0xFFFFFFFF),
    );
    for (final op in ops) {
      op.paint(canvas, this);
    }
    final picture = recorder.endRecording();
    final image = await picture.toImage(widthPx, height);
    final byteData = await image.toByteData(format: ui.ImageByteFormat.rawRgba);
    image.dispose();
    for (final op in ops) {
      op.dispose();
    }
    if (byteData == null) {
      return RenderedImage(
        rgba: Uint8List(widthPx * height * 4),
        width: widthPx,
        height: height,
      );
    }
    return RenderedImage(
      rgba: byteData.buffer.asUint8List(),
      width: widthPx,
      height: height,
    );
  }

  double _paintCentered(
    List<_PaintOp> ops,
    String text,
    TextStyle style, {
    required int widthPx,
    required double maxWidth,
    required double y,
    required double gap,
  }) {
    final painter = TextPainter(
      text: TextSpan(text: text, style: style),
      textAlign: TextAlign.center,
      textDirection: TextDirection.ltr,
      locale: const Locale('hi', 'IN'),
    )..layout(maxWidth: maxWidth);
    final left = (widthPx - painter.width) / 2;
    ops.add(_PaintOp.text(painter, left, y));
    return y + painter.height + gap;
  }

  double _paintLeft(
    List<_PaintOp> ops,
    String text,
    TextStyle style, {
    required double left,
    required double maxWidth,
    required double y,
    required double gap,
  }) {
    final painter = TextPainter(
      text: TextSpan(text: text, style: style),
      textAlign: TextAlign.left,
      textDirection: TextDirection.ltr,
      locale: const Locale('hi', 'IN'),
    )..layout(maxWidth: maxWidth);
    ops.add(_PaintOp.text(painter, left, y));
    return y + painter.height + gap;
  }

  double _paintPair(
    List<_PaintOp> ops, {
    required String left,
    required String right,
    required TextStyle style,
    required double leftPad,
    required double maxWidth,
    required double y,
    required double gap,
  }) {
    final rightPainter = TextPainter(
      text: TextSpan(text: right, style: style),
      textAlign: TextAlign.right,
      textDirection: TextDirection.ltr,
      locale: const Locale('hi', 'IN'),
    )..layout();
    final leftMax = (maxWidth - rightPainter.width - 8).clamp(20.0, maxWidth);
    final leftPainter = TextPainter(
      text: TextSpan(text: left, style: style),
      textAlign: TextAlign.left,
      textDirection: TextDirection.ltr,
      locale: const Locale('hi', 'IN'),
    )..layout(maxWidth: leftMax);
    final rowH = leftPainter.height > rightPainter.height
        ? leftPainter.height
        : rightPainter.height;
    ops.add(_PaintOp.text(leftPainter, leftPad, y));
    ops.add(
      _PaintOp.text(
        rightPainter,
        leftPad + maxWidth - rightPainter.width,
        y,
      ),
    );
    return y + rowH + gap;
  }

  double _paintColumns(
    List<_PaintOp> ops, {
    required double left,
    required double y,
    required double gap,
    required List<_Col> cells,
    required TextStyle style,
  }) {
    var x = left;
    var rowH = 0.0;
    for (final cell in cells) {
      final painter = TextPainter(
        text: TextSpan(
          text: cell.text,
          style: style.copyWith(fontWeight: cell.weight),
        ),
        textAlign: cell.align,
        textDirection: TextDirection.ltr,
        locale: const Locale('hi', 'IN'),
      )..layout(maxWidth: cell.width);
      final dx = switch (cell.align) {
        TextAlign.center => x + (cell.width - painter.width) / 2,
        TextAlign.right || TextAlign.end => x + cell.width - painter.width,
        _ => x,
      };
      ops.add(_PaintOp.text(painter, dx, y));
      if (painter.height > rowH) rowH = painter.height;
      x += cell.width;
    }
    return y + rowH + gap;
  }

  double _paintRule(
    List<_PaintOp> ops, {
    required double left,
    required double width,
    required double y,
  }) {
    ops.add(_PaintOp.rule(left, y + 2, width));
    return y + 6;
  }

  Future<RenderedImage> render(
    String text,
    int widthPx, {
    PrinterPaperSize paperSize = PrinterPaperSize.mm58,
    String? qrPayload,
    String? logoPath,
    String? qrMarker,
    bool useAssetLogoFallback = false,
    double fontSizeScale = 1,
  }) async {
    final profile = profileFor(paperSize);
    final bodySize = profile.bodyFontSize * fontSizeScale.clamp(0.8, 1.6);
    final lineHeight = 1.15;

    final logoImage = await loadLogo(
      logoPath: logoPath,
      widthPx: widthPx,
      useAssetLogoFallback: useAssetLogoFallback,
      widthFraction: profile.logoWidthFraction,
    );
    final logoDrawH = logoImage == null
        ? 0.0
        : logoImage.height.toDouble() + 12;

    final marker = qrMarker?.trim() ?? '';
    final hasInlineQr =
        marker.isNotEmpty &&
        text.contains(marker) &&
        qrPayload != null &&
        qrPayload.trim().isNotEmpty;

    String topText = text;
    String bottomText = '';
    if (hasInlineQr) {
      final parts = text.split(marker);
      topText = parts.first.replaceAll(RegExp(r'\n+$'), '\n');
      bottomText = parts.length > 1
          ? parts.sublist(1).join(marker).replaceFirst(RegExp(r'^\n+'), '')
          : '';
    } else {
      if (marker.isNotEmpty) {
        topText = text.replaceAll(marker, '');
      }
    }

    /* Shared with invoice / bill raster — Poppins + Indic fallbacks. */
    final style = AppFonts.printBody(
      fontSize: bodySize,
      height: lineHeight,
    );
    final boldStyle = AppFonts.printBold(
      fontSize: bodySize,
      height: lineHeight,
    );

    /* KOT (scaled) — first line is title, paint truly centered + larger. */
    final emphasizeTitle = fontSizeScale > 1.05;
    var titleLine = '';
    var bodyText = topText;
    if (emphasizeTitle) {
      final lines = topText.split('\n');
      if (lines.isNotEmpty) {
        titleLine = lines.first.trim();
        bodyText = lines.skip(1).join('\n');
      }
    }

    final titleStyle = AppFonts.printBold(
      fontSize: bodySize * 1.18,
      height: lineHeight,
    );
    final titlePainter = titleLine.isEmpty
        ? null
        : (TextPainter(
            text: TextSpan(text: titleLine, style: titleStyle),
            textAlign: TextAlign.center,
            textDirection: TextDirection.ltr,
            locale: const Locale('hi', 'IN'),
          )..layout(maxWidth: widthPx - 16.0));

    final topPainter = TextPainter(
      text: TextSpan(text: bodyText, style: style),
      textAlign: TextAlign.left,
      textDirection: TextDirection.ltr,
      locale: const Locale('hi', 'IN'),
    )..layout(maxWidth: widthPx - 16.0);

    TextPainter? bottomPainter;
    if (bottomText.trim().isNotEmpty) {
      bottomPainter = TextPainter(
        text: TextSpan(text: bottomText, style: boldStyle),
        textAlign: TextAlign.left,
        textDirection: TextDirection.ltr,
        locale: const Locale('hi', 'IN'),
      )..layout(maxWidth: widthPx - 16.0);
    }

    /* Same QR budget as [renderTicket]. */
    final qrSize = profile.qrSizeFor(widthPx);
    final qrData = qrPayload?.trim() ?? '';
    final drawQr = qrData.isNotEmpty && (hasInlineQr || marker.isEmpty);
    final qrGap = drawQr ? qrSize + 24 : 0.0;
    final bottomH = bottomPainter?.height ?? 0.0;
    final titleH = titlePainter == null ? 0.0 : titlePainter.height + 6;
    final height =
        (logoDrawH + titleH + topPainter.height + qrGap + bottomH + 24)
            .ceil()
            .clamp(24, 12000);

    final recorder = ui.PictureRecorder();
    final canvas = Canvas(recorder);
    canvas.drawRect(
      Rect.fromLTWH(0, 0, widthPx.toDouble(), height.toDouble()),
      Paint()..color = const Color(0xFFFFFFFF),
    );

    var y = 12.0;
    if (logoImage != null) {
      final lw = logoImage.width.toDouble();
      final lh = logoImage.height.toDouble();
      final left = (widthPx - lw) / 2;
      canvas.drawImage(logoImage, Offset(left, y), Paint());
      y += lh + 8;
      logoImage.dispose();
    }

    if (titlePainter != null) {
      final titleLeft = (widthPx - titlePainter.width) / 2;
      titlePainter.paint(canvas, Offset(titleLeft, y));
      y += titlePainter.height + 6;
    }

    topPainter.paint(canvas, Offset(8, y));
    y += topPainter.height + 8;

    if (drawQr) {
      receiptRasterizerDrawQr(
        canvas,
        qrData,
        left: (widthPx - qrSize) / 2,
        top: y,
        size: qrSize,
      );
      y += qrSize + 12;
    }

    bottomPainter?.paint(canvas, Offset(8, y));

    final picture = recorder.endRecording();
    final image = await picture.toImage(widthPx, height);
    final byteData = await image.toByteData(format: ui.ImageByteFormat.rawRgba);
    image.dispose();
    if (byteData == null) {
      return RenderedImage(
        rgba: Uint8List(widthPx * height * 4),
        width: widthPx,
        height: height,
      );
    }
    return RenderedImage(
      rgba: byteData.buffer.asUint8List(),
      width: widthPx,
      height: height,
    );
  }

  Future<ui.Image?> loadLogo({
    required String? logoPath,
    required int widthPx,
    required bool useAssetLogoFallback,
    double widthFraction = 0.45,
  }) async {
    final targetWidth =
        (widthPx * widthFraction).round().clamp(48, widthPx - 32);
    final filePath = logoPath?.trim() ?? '';
    if (filePath.isNotEmpty && !kIsWeb) {
      try {
        final file = File(filePath);
        if (await file.exists()) {
          final bytes = await file.readAsBytes();
          return await decodeImage(bytes, targetWidth);
        }
      } catch (_) {}
    }
    if (!useAssetLogoFallback) return null;
    try {
      final data = await rootBundle.load(AppAssets.receiptLogo);
      return await decodeImage(data.buffer.asUint8List(), targetWidth);
    } catch (_) {
      try {
        final data = await rootBundle.load(AppAssets.appLogo);
        return await decodeImage(data.buffer.asUint8List(), targetWidth);
      } catch (_) {
        return null;
      }
    }
  }

  Future<ui.Image?> decodeImage(Uint8List bytes, int targetWidth) async {
    final codec = await ui.instantiateImageCodec(
      bytes,
      targetWidth: targetWidth,
    );
    final frame = await codec.getNextFrame();
    return frame.image;
  }

  void receiptRasterizerDrawQr(
    Canvas canvas,
    String data, {
    required double left,
    required double top,
    required double size,
  }) {
    canvas.save();
    canvas.translate(left, top);
    QrPainter(
      data: data,
      version: QrVersions.auto,
      errorCorrectionLevel: QrErrorCorrectLevel.L,
      gapless: true,
      eyeStyle: const QrEyeStyle(
        eyeShape: QrEyeShape.square,
        color: Color(0xFF000000),
      ),
      dataModuleStyle: const QrDataModuleStyle(
        dataModuleShape: QrDataModuleShape.square,
        color: Color(0xFF000000),
      ),
    ).paint(canvas, Size(size, size));
    canvas.restore();
  }
}

class RenderedImage {
  const RenderedImage({
    required this.rgba,
    required this.width,
    required this.height,
  });

  final Uint8List rgba;
  final int width;
  final int height;
}

class _Col {
  const _Col(this.text, this.width, this.align, this.weight);

  final String text;
  final double width;
  final TextAlign align;
  final FontWeight weight;
}

enum _PaintKind { text, rule, image, qr }

class _PaintOp {
  _PaintOp._({
    required this.kind,
    this.painter,
    this.image,
    this.qrData,
    required this.x,
    required this.y,
    this.w = 0,
    this.h = 0,
  });

  factory _PaintOp.text(TextPainter painter, double x, double y) =>
      _PaintOp._(kind: _PaintKind.text, painter: painter, x: x, y: y);

  factory _PaintOp.rule(double x, double y, double width) =>
      _PaintOp._(kind: _PaintKind.rule, x: x, y: y, w: width, h: 1);

  factory _PaintOp.image(ui.Image image, double x, double y) =>
      _PaintOp._(kind: _PaintKind.image, image: image, x: x, y: y);

  factory _PaintOp.qr(String data, double x, double y, double size) =>
      _PaintOp._(kind: _PaintKind.qr, qrData: data, x: x, y: y, w: size, h: size);

  final _PaintKind kind;
  final TextPainter? painter;
  final ui.Image? image;
  final String? qrData;
  final double x;
  final double y;
  final double w;
  final double h;

  void paint(Canvas canvas, ReceiptRasterizer host) {
    switch (kind) {
      case _PaintKind.text:
        painter?.paint(canvas, Offset(x, y));
      case _PaintKind.rule:
        canvas.drawRect(
          Rect.fromLTWH(x, y, w, h < 1 ? 1 : h),
          Paint()..color = const Color(0xFF000000),
        );
      case _PaintKind.image:
        if (image != null) {
          canvas.drawImage(image!, Offset(x, y), Paint());
        }
      case _PaintKind.qr:
        host.receiptRasterizerDrawQr(
          canvas,
          qrData ?? '',
          left: x,
          top: y,
          size: w,
        );
    }
  }

  void dispose() {
    painter?.dispose();
    image?.dispose();
  }
}
