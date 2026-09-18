import 'dart:io';
import 'dart:ui' as ui;

import 'package:flutter/foundation.dart';
import 'package:flutter/painting.dart';
import 'package:flutter/services.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/features/print/domain/esc_pos_encoder.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_image_encoder.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:qr_flutter/qr_flutter.dart';

/* Renders receipt Unicode text (any language + ₹) to ESC/POS raster bytes, */
/* same approach as Android layout → bitmap → [PrintImage]. */
class ReceiptRasterizer {
  const ReceiptRasterizer();

  /* Android effective widths: 2″ → 48*8=384, 3″ → 72*8=576. */
  static int widthPxFor(PrinterPaperSize size) =>
      size == PrinterPaperSize.inch3 ? 576 : 384;

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
      qrPayload: qrPayload,
      logoPath: logoPath,
      qrMarker: qrMarker,
      useAssetLogoFallback: useAssetLogoFallback,
    );
    final raster = PrintImageEncoder.encodeRgba(
      rgba: rendered.rgba,
      width: rendered.width,
      height: rendered.height,
      brightValue: 128,
    );

    final out = EscPosEncoder(charsPerLine: settings.charsPerLine)
      ..init()
      ..raw(raster)
      ..feed(feedLinesOverride ?? settings.feedLines);
    return out.bytes;
  }

  Future<RenderedImage> render(
    String text,
    int widthPx, {
    String? qrPayload,
    String? logoPath,
    String? qrMarker,
    bool useAssetLogoFallback = false,
  }) async {
    final logoImage = await loadLogo(
      logoPath: logoPath,
      widthPx: widthPx,
      useAssetLogoFallback: useAssetLogoFallback,
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

    /* Shared with invoice preview — Poppins + Indic fallbacks so user data */
    /* (EN / MR / HI / …) prints identically on Android, iOS, and web share. */
    final style = AppFonts.printBody();
    final boldStyle = AppFonts.printBold();

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

    final qrSize = (widthPx * 0.55).clamp(120.0, 280.0);
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
  }) async {
    final targetWidth = (widthPx * 0.45).round().clamp(64, widthPx - 32);
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
