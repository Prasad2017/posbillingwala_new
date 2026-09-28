import 'package:pos_billingwala_v2/features/print/domain/receipt_labels.dart';

/* Structured bill — layout adapts to 58mm / 80mm / A4 via paper size. */
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
    this.metaPairs = const [],
    this.totalItemsLine = '',
    this.grandTotalLabel = '',
    this.grandTotalValue = '',
    this.upiLine = '',
    this.terms = '',
    this.qrPayload,
    this.customerName = '',
    this.customerMobile = '',
    this.customerAddress = '',
    this.amountInWords = '',
    this.taxInvoiceTitle = 'TAX INVOICE',
  });

  final List<String> shopLines;
  /* Stacked meta (best for 58mm). */
  final List<String> metaLines;
  /* Two-column meta rows for 80mm / A4 when non-empty. */
  final List<(String, String)> metaPairs;
  final String copyBanner;
  final String colItem;
  final String colQty;
  final String colRate;
  final String colAmount;
  final List<ThermalLine> items;
  final List<(String, String)> pairs;
  final List<String> footerLines;
  final String totalItemsLine;
  final String grandTotalLabel;
  final String grandTotalValue;
  final String upiLine;
  final String terms;
  final String? qrPayload;
  final String customerName;
  final String customerMobile;
  final String customerAddress;
  final String amountInWords;
  final String taxInvoiceTitle;

  static const upiQrMarker = '<<<UPI_QR>>>';

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
    final qtyW = 6;
    final rateW = 8;
    final amtW = 8;
    final itemW = width - qtyW - rateW - amtW;
    buf.writeln(
      columns(
        [colItem, colQty, colRate, colAmount],
        [itemW, qtyW, rateW, amtW],
      ),
    );
    buf.writeln('-' * width);
    for (final item in items) {
      buf.writeln(
        columns(
          [item.name, item.qty, item.rate, item.amount],
          [itemW, qtyW, rateW, amtW],
        ),
      );
    }
    buf.writeln('-' * width);
    if (totalItemsLine.trim().isNotEmpty) {
      buf.writeln(totalItemsLine.trim());
    }
    for (final pair in pairs) {
      buf.writeln(thermalTicketPair(pair.$1, pair.$2, width));
    }
    if (grandTotalLabel.trim().isNotEmpty) {
      buf.writeln(
        thermalTicketPair(grandTotalLabel, grandTotalValue, width),
      );
    }
    buf.writeln('-' * width);
    if (upiLine.trim().isNotEmpty) {
      buf.writeln(center(upiLine.trim(), width));
    }
    if (terms.trim().isNotEmpty) {
      buf.writeln(center(terms.trim(), width));
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
  List<(String, String)> metaPairs = const [],
  String totalItemsLine = '',
  String grandTotalLabel = '',
  String grandTotalValue = '',
  String upiLine = '',
  String terms = '',
  String? qrPayload,
  String customerName = '',
  String customerMobile = '',
  String customerAddress = '',
  String amountInWords = '',
}) {
  return ThermalTicket(
    shopLines: shopLines,
    metaLines: metaLines,
    metaPairs: metaPairs,
    copyBanner: duplicate ? labels.duplicateCopy : labels.originalCopy,
    colItem: labels.item,
    colQty: labels.qty,
    colRate: labels.rate,
    colAmount: labels.amount,
    items: items,
    pairs: pairs,
    footerLines: footerLines,
    totalItemsLine: totalItemsLine,
    grandTotalLabel: grandTotalLabel,
    grandTotalValue: grandTotalValue,
    upiLine: upiLine,
    terms: terms,
    qrPayload: qrPayload,
    customerName: customerName,
    customerMobile: customerMobile,
    customerAddress: customerAddress,
    amountInWords: amountInWords,
    taxInvoiceTitle: labels.taxInvoice,
  );
}
