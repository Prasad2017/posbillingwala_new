import 'package:pos_billingwala_v2/features/print/domain/receipt_labels.dart';

/* Structured bill matching WithTable `activity_bluetooth_print.xml` */
/* (`twoLinearLayout` 48mm / `threeLinearLayout` 72mm). */
class ThermalTicket {
  const ThermalTicket({
    required this.shopLines,
    required this.metaLines,
    required this.copyBanner,
    required this.colItem,
    required this.colQty,
    required this.colRate,
    required this.colAmount,
    required this.items,
    required this.pairs,
    required this.footerLines,
    this.totalLabel = '',
    this.totalValue = '',
    this.closingMessage = '',
    this.terms = '',
    this.qrPayload,
  });

  final List<String> shopLines;
  final List<String> metaLines;
  final String copyBanner;
  final String colItem;
  final String colQty;
  final String colRate;
  final String colAmount;
  final List<ThermalLine> items;
  /* Subtotal / tax / discount / packing — TOTAL is [totalLabel]/[totalValue]. */
  final List<(String, String)> pairs;
  final String totalLabel;
  final String totalValue;
  /* Optional extra line above the QR. Bills leave this empty and use [terms]. */
  final String closingMessage;
  final List<String> footerLines;
  final String terms;
  final String? qrPayload;

  static const upiQrMarker = '<<<UPI_QR>>>';

  /* Column char widths: Qty / Rate / Amount — rest is Item. */
  static const qtyChars = 5;
  static const rateChars = 6;
  static const amountChars = 7;

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
    final itemW = (width - qtyChars - rateChars - amountChars).clamp(8, width);
    final widths = [itemW, qtyChars, rateChars, amountChars];
    buf.writeln(
      columns([colItem, colQty, colRate, colAmount], widths),
    );
    buf.writeln('-' * width);
    for (final item in items) {
      /* Name uses the full line; qty / rate / amount sit on the next. */
      buf.writeln(item.name);
      buf.writeln(columns(['', item.qty, item.rate, item.amount], widths));
    }
    buf.writeln('-' * width);
    for (final pair in pairs) {
      buf.writeln(thermalTicketPair(pair.$1, pair.$2, width));
    }
    if (totalLabel.trim().isNotEmpty || totalValue.trim().isNotEmpty) {
      buf.writeln('=' * width);
      buf.writeln(thermalTicketPair(totalLabel, totalValue, width));
      buf.writeln('=' * width);
    } else {
      buf.writeln('-' * width);
    }
    if (terms.trim().isNotEmpty) {
      buf.writeln(center(terms.trim(), width));
    }
    if (closingMessage.trim().isNotEmpty) {
      buf.writeln(center(closingMessage.trim(), width));
    }
    if (qrPayload != null && qrPayload!.trim().isNotEmpty) {
      buf.writeln(upiQrMarker);
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
  final String qty;
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
  String totalLabel = '',
  String totalValue = '',
  String closingMessage = '',
  String terms = '',
  String? qrPayload,
}) {
  return ThermalTicket(
    shopLines: shopLines,
    metaLines: metaLines,
    copyBanner: duplicate ? labels.duplicateCopy : labels.originalCopy,
    colItem: labels.item,
    colQty: labels.qty,
    colRate: labels.rate,
    colAmount: labels.amount,
    items: items,
    pairs: pairs,
    totalLabel: totalLabel,
    totalValue: totalValue,
    closingMessage: closingMessage,
    footerLines: footerLines,
    terms: terms,
    qrPayload: qrPayload,
  );
}
