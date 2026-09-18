import 'dart:io';
import 'dart:ui' as ui;

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:path_provider/path_provider.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:share_plus/share_plus.dart';

/* Android `BluetoothPrint.convertLayout` analogue: screenshot the ticket widget. */
Future<void> shareTicketWidgetAsImage({
  required GlobalKey boundaryKey,
  required String label,
}) async {
  final boundary =
      boundaryKey.currentContext?.findRenderObject() as RenderRepaintBoundary?;
  if (boundary == null) {
    throw StateError('Bill preview is not ready to share');
  }
  final image = await boundary.toImage(pixelRatio: 2.5);
  final data = await image.toByteData(format: ui.ImageByteFormat.png);
  image.dispose();
  if (data == null) {
    throw StateError('Could not capture bill image');
  }
  await sharePngBytes(data.buffer.asUint8List(), label);
}

Future<void> sharePngBytes(Uint8List bytes, String label) async {
  final safeName =
      '${label.replaceAll(RegExp(r'[^a-zA-Z0-9_-]'), '_').toLowerCase()}_'
      '${DateTime.now().millisecondsSinceEpoch}.png';
  if (kIsWeb) {
    await SharePlus.instance.share(
      ShareParams(
        files: [XFile.fromData(bytes, mimeType: 'image/png', name: safeName)],
        subject: label,
        text: label,
      ),
    );
    return;
  }
  final dir = await getTemporaryDirectory();
  final file = File('${dir.path}/$safeName');
  await file.writeAsBytes(bytes, flush: true);
  await SharePlus.instance.share(
    ShareParams(
      files: [XFile(file.path, mimeType: 'image/png')],
      subject: label,
      text: label,
    ),
  );
}

/* Fallback: paint receipt text to a PNG with the same font stack as */
/* [ReceiptRasterizer] (Unicode / Marathi / Hindi user data). */
Future<void> shareReceiptAsImage({
  required String text,
  String label = 'Invoice',
}) async {
  final bytes = await renderReceiptPng(text);
  await sharePngBytes(bytes, label);
}

Future<Uint8List> renderReceiptPng(String text) async {
  const width = 576.0;
  const pad = 24.0;
  final painter = TextPainter(
    text: TextSpan(
      text: text,
      style: AppFonts.printBody(fontSize: 22, height: 1.28),
    ),
    textDirection: TextDirection.ltr,
    locale: const Locale('hi', 'IN'),
  )..layout(maxWidth: width - pad * 2);

  final height = (painter.height + pad * 2).clamp(200.0, 8000.0);
  final recorder = ui.PictureRecorder();
  final canvas = Canvas(recorder);
  canvas.drawRect(
    Rect.fromLTWH(0, 0, width, height),
    Paint()..color = const Color(0xFFFFFFFF),
  );
  painter.paint(canvas, const Offset(pad, pad));
  final picture = recorder.endRecording();
  final image = await picture.toImage(width.toInt(), height.toInt());
  final data = await image.toByteData(format: ui.ImageByteFormat.png);
  image.dispose();
  picture.dispose();
  return data!.buffer.asUint8List();
}
