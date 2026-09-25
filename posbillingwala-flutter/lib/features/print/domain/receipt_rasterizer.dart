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
import 'package:pos_billingwala_v2/features/print/domain/print_image_encoder.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/thermal_ticket.dart';
import 'package:qr_flutter/qr_flutter.dart';

/* Renders receipt Unicode text (any language + ₹) to ESC/POS raster bytes, */
/* same approach as Android layout → bitmap → [PrintImage]. */
class ReceiptRasterizer {
  const ReceiptRasterizer();

  /* Android effective widths: 2″ → 48*8=384, 3″ → 72*8=576. */
  static int widthPxFor(PrinterPaperSize size) =>
      size == PrinterPaperSize.inch3 ? 576 : 384;

  /* Scale WoosimTicket (mm×3.78 preview) metrics onto printer pixel width. */
  static double previewScale(PrinterPaperSize size) {
    final widthMm = size == PrinterPaperSize.inch3 ? 72.0 : 48.0;
    return widthPxFor(size) / (widthMm * 3.78);
  }

  Future<List<int>> encodeTicket(
    ThermalTicket ticket, {
    required PrinterSettings settings,
    String? logoPath,
    int? feedLinesOverride,
    bool useAssetLogoFallback = false,
  }) async {
    final width = widthPxFor(settings.paperSize);
    final rendered = await renderTicket(
      ticket,
      width,
      paperSize: settings.paperSize,
      logoPath: logoPath,
      useAssetLogoFallback: useAssetLogoFallback,
    );
    return _escPosRaster(
      rendered,
      charsPerLine: settings.charsPerLine,
      feedLines: feedLinesOverride ?? settings.feedLines,
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
    return _escPosRaster(
      rendered,
      charsPerLine: settings.charsPerLine,
      feedLines: feedLinesOverride ?? settings.feedLines,
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
    final width = widthPxFor(settings.paperSize);
    final rendered = await renderMessLayout(
      layout,
      width,
      paperSize: settings.paperSize,
      logoPath: logoPath,
      useAssetLogoFallback: useAssetLogoFallback,
    );
    return _escPosRaster(
      rendered,
      charsPerLine: settings.charsPerLine,
      feedLines: feedLinesOverride ?? settings.feedLines,
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
    final is2Inch = paperSize != PrinterPaperSize.inch3;
    final hPad = is2Inch ? 6.0 : 8.0;
    /* Match invoice / [renderTicket] absolute pt sizes. */
    final shopSize = is2Inch ? 20.0 : 24.0;
    final bodySize = is2Inch ? 17.0 : 20.0;
    final bannerSize = is2Inch ? 15.0 : 17.0;
    final lineGap = is2Inch ? 2.0 : 2.5;
    final sectionGap = is2Inch ? 3.5 : 4.5;
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
      widthFraction: 0.28,
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
      final qrSize = (widthPx * 0.42).clamp(100.0, is2Inch ? 160.0 : 200.0);
      y += sectionGap;
      ops.add(_PaintOp.qr(qr, (widthPx - qrSize) / 2, y, qrSize));
      y += qrSize + sectionGap;
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

  List<int> _escPosRaster(
    RenderedImage rendered, {
    required int charsPerLine,
    required int feedLines,
  }) {
    final raster = PrintImageEncoder.encodeRgba(
      rgba: rendered.rgba,
      width: rendered.width,
      height: rendered.height,
      brightValue: 128,
    );
    final out = EscPosEncoder(charsPerLine: charsPerLine)
      ..init()
      ..raw(raster)
      ..feed(feedLines);
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
    final is2Inch = paperSize != PrinterPaperSize.inch3;
    final colScale = previewScale(paperSize) * 0.72;
    final hPad = is2Inch ? 6.0 : 8.0;
    final rateW = (is2Inch ? 48.0 : 64.0) * colScale;
    final amountW = (is2Inch ? 56.0 : 72.0) * colScale;
    /* Absolute pt — full previewScale (~2.1×) made print look oversized. */
    final shopSize = is2Inch ? 20.0 : 24.0;
    final bodySize = is2Inch ? 17.0 : 20.0;
    final bannerSize = is2Inch ? 15.0 : 17.0;
    final lineGap = is2Inch ? 2.0 : 2.5;
    final sectionGap = is2Inch ? 3.5 : 4.5;
    final contentW = widthPx - hPad * 2;
    final itemColW = (contentW - rateW - amountW).clamp(40.0, contentW);

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
      widthFraction: 0.28,
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
          _Col('X${item.qty}', itemColW, TextAlign.left, FontWeight.w500),
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
        style(size: bodySize * 0.92),
        widthPx: widthPx,
        maxWidth: contentW,
        y: y + lineGap,
        gap: sectionGap,
      );
    }

    final qr = ticket.qrPayload?.trim() ?? '';
    if (qr.isNotEmpty) {
      final qrSize = (widthPx * 0.42).clamp(100.0, is2Inch ? 160.0 : 200.0);
      y += sectionGap;
      ops.add(
        _PaintOp.qr(qr, (widthPx - qrSize) / 2, y, qrSize),
      );
      y += qrSize + sectionGap;
    }

    for (final line in ticket.footerLines) {
      y = _paintCentered(
        ops,
        line,
        style(),
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
    PrinterPaperSize paperSize = PrinterPaperSize.inch2,
    String? qrPayload,
    String? logoPath,
    String? qrMarker,
    bool useAssetLogoFallback = false,
  }) async {
    final is2Inch = paperSize != PrinterPaperSize.inch3;
    /* Match [renderTicket] / bill print absolute pt sizes. */
    final bodySize = is2Inch ? 17.0 : 20.0;
    final lineHeight = 1.15;

    final logoImage = await loadLogo(
      logoPath: logoPath,
      widthPx: widthPx,
      useAssetLogoFallback: useAssetLogoFallback,
      widthFraction: 0.28,
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

    final topPainter = TextPainter(
      text: TextSpan(text: topText, style: style),
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
    final qrSize = (widthPx * 0.42).clamp(100.0, is2Inch ? 160.0 : 200.0);
    final qrData = qrPayload?.trim() ?? '';
    final drawQr = qrData.isNotEmpty && (hasInlineQr || marker.isEmpty);
    final qrGap = drawQr ? qrSize + 24 : 0.0;
    final bottomH = bottomPainter?.height ?? 0.0;
    final height = (logoDrawH + topPainter.height + qrGap + bottomH + 24)
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
