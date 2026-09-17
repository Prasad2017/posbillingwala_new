import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/features/masters/domain/product_units.dart';
import 'package:pos_billingwala_v2/features/print/domain/esc_pos_encoder.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/receipt_labels.dart';
import 'package:pos_billingwala_v2/features/print/domain/receipt_rasterizer.dart';
import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';
import 'package:pos_billingwala_v2/features/print/domain/thermal_ticket.dart';

/* Bill / KOT receipt content aligned with Android BluetoothPrint layouts. */
class ReceiptBuilder {
  ReceiptBuilder(
    this.settings, {
    this.shopProfile = const ShopReceiptProfile(),
    ReceiptLabels? labels,
  }) : labels = labels ?? ReceiptLabels.fromLang('en');

  final PrinterSettings settings;
  final ShopReceiptProfile shopProfile;
  final ReceiptLabels labels;
  final money = NumberFormat('#0.00');
  /* Android bill date format. */
  final receiptBuilderDate = DateFormat('yyyy-MM-dd HH:mm:ss');
  final rasterizer = const ReceiptRasterizer();

  /* Marker inserted where UPI QR should appear (terms → QR → footer). */
  static const upiQrMarker = '<<<UPI_QR>>>';

  String rupee(num value) => '₹${money.format(value)}';

  String qtyLabel(num qty, {String? unit}) =>
      ProductUnits.formatQty(qty.toDouble(), unit: unit);

  /* Unicode-safe thermal bytes (any language + ₹) via bitmap, like Android. */
  Future<List<int>> billPrintBytes({
    required Invoice invoice,
    required List<InvoiceItem> items,
    String? shopName,
    bool duplicate = false,
  }) {
    final upiUri = upiUriFor(invoice);
    final logoPath = settings.logoUse ? shopProfile.logoLocalPath : null;
    return rasterizer.encodeText(
      billText(
        invoice: invoice,
        items: items,
        shopName: shopName,
        duplicate: duplicate,
      ),
      settings: settings,
      qrPayload: upiUri,
      logoPath: logoPath,
      qrMarker: upiUri != null ? upiQrMarker : null,
      useAssetLogoFallback: settings.logoUse,
    );
  }

  Future<List<int>> kotPrintBytes(KotTicket ticket) {
    return rasterizer.encodeText(
      kotText(ticket),
      settings: settings,
      feedLinesOverride: settings.kotFeedLines,
      useAssetLogoFallback: false,
    );
  }

  Future<List<int>> rawPrintBytes(String text) {
    return rasterizer.encodeText(
      text,
      settings: settings,
      useAssetLogoFallback: false,
    );
  }

  Future<List<int>> testPrintBytes(String label) {
    final width = settings.charsPerLine;
    final text = StringBuffer()
      ..writeln(receiptBuilderCenter('Billingwala', width))
      ..writeln(receiptBuilderCenter('टेस्ट प्रिंट / Test', width))
      ..writeln('-' * width)
      ..writeln(label)
      ..writeln(receiptBuilderDate.format(DateTime.now()))
      ..writeln(rupee(123.45))
      ..writeln('-' * width)
      ..writeln('नमस्ते · Hello · வணக்கம்');
    return rasterizer.encodeText(
      text.toString(),
      settings: settings,
      useAssetLogoFallback: true,
    );
  }

  String? upiUriFor(Invoice invoice) {
    if (!settings.paymentUse || !shopProfile.hasUpiId) return null;
    final amount = invoice.totalAmount > 0
        ? invoice.totalAmount
        : (invoice.upiAmount > 0 ? invoice.upiAmount : 0.0);
    return buildUpiPayUri(
      upiId: shopProfile.upiId,
      payeeName: shopProfile.shopName1.trim().isNotEmpty
          ? shopProfile.shopName1.trim()
          : (shopProfile.companyName.isNotEmpty
              ? shopProfile.companyName
              : 'Merchant'),
      amount: amount,
      note: invoice.invoiceNumber,
    );
  }

  /* Android ORIGINAL / DUPLICATE bill layout (BluetoothPrint XML). */
  ThermalTicket ticket({
    required Invoice invoice,
    required List<InvoiceItem> items,
    String? shopName,
    bool duplicate = false,
  }) {
    final header = shopProfile.headerLines();
    final shopLines = header.isNotEmpty
        ? header
        : [
            (shopName?.trim().isNotEmpty ?? false)
                ? shopName!.trim()
                : 'Billingwala',
          ];

    final meta = <String>[
      'Bill No: ${invoice.invoiceNumber}',
      '${labels.date}: ${receiptBuilderDate.format(invoice.invoiceDate)}',
    ];
    if (invoice.noOfTable.trim().isNotEmpty) {
      meta.add('Table No: ${invoice.noOfTable}');
    }
    final billedBy = invoice.createdByStaffName.trim();
    if (billedBy.isNotEmpty) {
      meta.add('Billed by: $billedBy');
    }
    if (settings.customerUse) {
      final name = invoice.customerName?.trim();
      final mobile = invoice.customerMobile?.trim();
      final email = invoice.customerEmail?.trim();
      final address = invoice.customerAddress?.trim();
      meta.add(
        '${labels.customerName}: ${name == null || name.isEmpty ? 'NA' : name}',
      );
      meta.add(
        '${labels.customerMobile}: ${mobile == null || mobile.isEmpty ? 'NA' : mobile}',
      );
      if (email != null && email.isNotEmpty) {
        meta.add('${labels.customerEmail}: $email');
      }
      meta.add(
        '${labels.customerAddress}: ${address == null || address.isEmpty ? 'NA' : address}',
      );
    }

    final lines = [
      for (final item in items)
        ThermalLine(
          name: item.productName,
          qty: item.productQuantity,
          rate: money.format(item.productPrice),
          amount: money.format(item.productPrice * item.productQuantity),
        ),
    ];

    final pairs = <(String, String)>[
      (labels.subTotal, rupee(invoice.subTotal)),
    ];
    final cgstPct = double.tryParse(shopProfile.shopCgst.trim()) ?? 0;
    final sgstPct = double.tryParse(shopProfile.shopSgst.trim()) ?? 0;
    final gstOn = shopProfile.gstEnabled && (cgstPct > 0 || sgstPct > 0);
    if (gstOn) {
      if (cgstPct > 0) {
        pairs.add((
          'CGST@${money.format(cgstPct)}%',
          rupee(invoice.subTotal * cgstPct / 100),
        ));
      }
      if (sgstPct > 0) {
        pairs.add((
          'SGST@${money.format(sgstPct)}%',
          rupee(invoice.subTotal * sgstPct / 100),
        ));
      }
    } else if (invoice.totalGstAmount > 0) {
      final half = invoice.totalGstAmount / 2;
      pairs.add(('CGST', rupee(half)));
      pairs.add(('SGST', rupee(half)));
    }
    pairs.add((labels.discount, rupee(invoice.discount)));
    if (invoice.packingCharge > 0) {
      pairs.add((labels.packing, rupee(invoice.packingCharge)));
    }
    pairs.add((labels.totalAmount, rupee(invoice.totalAmount.ceilToDouble())));

    return ticketFromLabels(
      labels: labels,
      shopLines: shopLines,
      metaLines: meta,
      duplicate: duplicate,
      items: lines,
      pairs: pairs,
      footerLines: [labels.poweredBy, labels.website],
      terms: settings.invoiceTerms,
      qrPayload: upiUriFor(invoice),
    );
  }

  String billText({
    required Invoice invoice,
    required List<InvoiceItem> items,
    String? shopName,
    bool duplicate = false,
  }) {
    return ticket(
      invoice: invoice,
      items: items,
      shopName: shopName,
      duplicate: duplicate,
    ).toPlainText(width: settings.charsPerLine);
  }

  /* Latin-1 fallback only. Prefer [billPrintBytes] for Unicode / ₹. */
  List<int> billEscPos({
    required Invoice invoice,
    required List<InvoiceItem> items,
    String? shopName,
  }) {
    final text = billText(invoice: invoice, items: items)
        .replaceAll(upiQrMarker, '')
        .replaceAll('₹', 'Rs.');
    final encoder = EscPosEncoder(charsPerLine: settings.charsPerLine)
      ..init();
    for (final line in text.split('\n')) {
      if (line.trim().isEmpty) continue;
      encoder.text(line);
    }
    encoder.feed(settings.feedLines);
    return encoder.bytes;
  }

  String kotText(KotTicket ticket) {
    final width = settings.charsPerLine;
    final buf = StringBuffer()
      ..writeln(receiptBuilderCenter(labels.kot, width))
      ..writeln(receiptBuilderCenter(ticket.kot.kotNumber, width))
      ..writeln('-' * width)
      ..writeln('KOT: ${ticket.kot.kotNumber}')
      ..writeln('${labels.date}: ${receiptBuilderDate.format(ticket.kot.createdAt)}')
      ..writeln('Table No: ${ticket.kot.tableNumber}')
      ..writeln('Round: ${ticket.roundNumber}')
      ..writeln(ticket.kot.kitchenName)
      ..writeln('-' * width);
    for (final item in ticket.items) {
      buf.writeln(
        pair(item.productName, 'X${qtyLabel(item.productQuantity, unit: item.productUnit)}', width),
      );
    }
    buf.writeln('-' * width);
    return buf.toString();
  }

  /* Latin-1 fallback only. Prefer [kotPrintBytes] for Unicode. */
  List<int> kotEscPos(KotTicket ticket) {
    final encoder = EscPosEncoder(charsPerLine: settings.charsPerLine)
      ..init()
      ..text('KOT', boldStyle: true, center: true)
      ..text(ticket.kot.kotNumber, boldStyle: true, center: true)
      ..separator()
      ..text('Date: ${receiptBuilderDate.format(ticket.kot.createdAt)}')
      ..text('Table No: ${ticket.kot.tableNumber}')
      ..text('Round: ${ticket.roundNumber}')
      ..text(ticket.kot.kitchenName)
      ..separator();
    for (final item in ticket.items) {
      encoder.line(
        item.productName,
        'X${qtyLabel(item.productQuantity, unit: item.productUnit)}',
      );
    }
    encoder
      ..separator()
      ..feed(settings.kotFeedLines);
    return encoder.bytes;
  }

  List<int> testEscPos(String label) {
    return (EscPosEncoder(charsPerLine: settings.charsPerLine)
          ..init()
          ..text('Billingwala', boldStyle: true, center: true)
          ..text('Test print', center: true)
          ..separator()
          ..text(label)
          ..text(receiptBuilderDate.format(DateTime.now()))
          ..separator()
          ..feed(settings.feedLines))
        .bytes;
  }

  String receiptBuilderCenter(String value, int width) {
    if (value.runes.length >= width) return value;
    final pad = width - value.runes.length;
    final left = pad ~/ 2;
    return (' ' * left) + value;
  }

  String pair(String left, String right, int width) {
    final space = width - left.runes.length - right.runes.length;
    final gap = space > 1 ? ' ' * space : ' ';
    return '$left$gap$right';
  }
}
