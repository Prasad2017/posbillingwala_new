import 'package:pos_billingwala_v2/features/print/domain/receipt_labels.dart';

/* Structured bill matching WithTable `activity_bluetooth_print.xml` */
/* (`twoLinearLayout` 48mm / `threeLinearLayout` 72mm). */
class ThermalTicket {
  const ThermalTicket({
    required this.shopLines,
    required this.metaLines,
    required this.copyBanner,
    required this.colItem,
    required this.colRate,
    required this.colAmount,
    required this.items,
    required this.pairs,
    required this.footerLines,
    this.terms = '',
    this.qrPayload,
  });

  final List<String> shopLines;
  final List<String> metaLines;
  final String copyBanner;
  final String colItem;
  final String colRate;
  final String colAmount;
  final List<ThermalLine> items;
  final List<(String, String)> pairs;
  final List<String> footerLines;
  final String terms;
  final String? qrPayload;

  String toPlainText({required int width}) {
    final buf = StringBuffer();
    for (final line in shopLines) {
      buf.writeln(center(line, width));
    }
    for (final line in metaLines) {
      buf.writeln(line);
    }
    buf.writeln(center(copyBanner, width));
    buf.writeln('-' * width);
    final itemW = width - 16;
    buf.writeln(columns([colItem, colRate, colAmount], [itemW, 8, 8]));
    buf.writeln('-' * width);
    for (final item in items) {
      buf.writeln(item.name);
      buf.writeln(
        columns(
          ['X${item.qty}', item.rate, item.amount],
          [itemW, 8, 8],
        ),
      );
    }
    buf.writeln('-' * width);
    for (final pair in pairs) {
      buf.writeln(thermalTicketPair(pair.$1, pair.$2, width));
    }
    buf.writeln('-' * width);
    if (terms.trim().isNotEmpty) {
      buf.writeln(center(terms.trim(), width));
    }
    if (qrPayload != null && qrPayload!.trim().isNotEmpty) {
      buf.writeln('<<<UPI_QR>>>');
    }
    for (final line in footerLines) {
      buf.writeln(center(line, width));
    }
    return buf.toString();
  }

  static String center(String value, int width) {
    if (value.runes.length >= width) return value;
    final pad = width - value.runes.length;
    return (' ' * (pad ~/ 2)) + value;
  }

  static String thermalTicketPair(String left, String right, int width) {
    final space = width - left.runes.length - right.runes.length;
    final gap = space > 1 ? ' ' * space : ' ';
    return '$left$gap$right';
  }

  static String columns(List<String> values, List<int> widths) {
    final parts = <String>[];
    for (var i = 0; i < values.length; i++) {
      final w = widths[i];
      final v = values[i];
      if (i == 0) {
        parts.add(v.padRight(w));
      } else {
        parts.add(v.padLeft(w));
      }
    }
    return parts.join();
  }
}

class ThermalLine {
  const ThermalLine({
    required this.name,
    required this.qty,
    required this.rate,
    required this.amount,
  });

  final String name;
  final num qty;
  final String rate;
  final String amount;
}

ThermalTicket ticketFromLabels({
  required ReceiptLabels labels,
  required List<String> shopLines,
  required List<String> metaLines,
  required bool duplicate,
  required List<ThermalLine> items,
  required List<(String, String)> pairs,
  required List<String> footerLines,
  String terms = '',
  String? qrPayload,
}) {
  return ThermalTicket(
    shopLines: shopLines,
    metaLines: metaLines,
    copyBanner: duplicate ? labels.duplicateCopy : labels.originalCopy,
    colItem: labels.item,
    colRate: labels.rate,
    colAmount: labels.amount,
    items: items,
    pairs: pairs,
    footerLines: footerLines,
    terms: terms,
    qrPayload: qrPayload,
  );
}
