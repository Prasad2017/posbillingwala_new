import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/features/masters/domain/product_units.dart';
import 'package:pos_billingwala_v2/features/print/domain/kot_slip_layout.dart';
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

  String rupee(num value) => '₹${money.format(value)}';

  String qtyLabel(num qty, {String? unit}) =>
      ProductUnits.formatQty(qty.toDouble(), unit: unit);

  /* Unicode-safe thermal bytes via ticket layout → bitmap (matches preview). */
  Future<List<int>> billPrintBytes({
    required Invoice invoice,
    required List<InvoiceItem> items,
    String? shopName,
    bool duplicate = false,
  }) {
    final logoPath = settings.logoUse ? shopProfile.logoLocalPath : null;
    return rasterizer.encodeTicket(
      ticket(
        invoice: invoice,
        items: items,
        shopName: shopName,
        duplicate: duplicate,
      ),
      settings: settings,
      logoPath: logoPath,
      useAssetLogoFallback: false,
    );
  }

  Future<List<int>> kotPrintBytes(KotTicket ticket) {
    return rasterizer.encodeKot(
      kotLayout(ticket),
      settings: settings,
      feedLinesOverride: settings.kotFeedLines,
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

  String? upiUriFor(Invoice invoice, {double? payableOverride}) {
    if (!settings.paymentUse || !shopProfile.hasUpiId) return null;
    final amount = payableOverride != null && payableOverride > 0
        ? payableOverride
        : (invoice.totalAmount > 0
              ? invoice.totalAmount
              : (invoice.upiAmount > 0 ? invoice.upiAmount : 0.0));
    /* Android PaymentUpiQrHelper: QR only when payable amount > 0. */
    if (amount <= 0) return null;
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
    final header = shopProfile.headerLines(maxChars: settings.charsPerLine);
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
      final name = invoice.customerName?.trim() ?? '';
      final mobile = invoice.customerMobile?.trim() ?? '';
      final email = invoice.customerEmail?.trim() ?? '';
      final address = invoice.customerAddress?.trim() ?? '';
      if (name.isNotEmpty) meta.add('${labels.customerName}: $name');
      if (mobile.isNotEmpty) meta.add('${labels.customerMobile}: $mobile');
      if (email.isNotEmpty) meta.add('${labels.customerEmail}: $email');
      if (address.isNotEmpty) meta.add('${labels.customerAddress}: $address');
    }

    final lines = [
      for (final item in items)
        ThermalLine(
          name: item.productName,
          qty: qtyLabel(item.productQuantity, unit: item.productUnit),
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
    var taxTotal = 0.0;
    if (gstOn) {
      final cgstAmt = invoice.subTotal * cgstPct / 100;
      final sgstAmt = invoice.subTotal * sgstPct / 100;
      taxTotal = cgstAmt + sgstAmt;
      if (cgstPct > 0) {
        pairs.add((
          'CGST @${money.format(cgstPct)}%',
          rupee(cgstAmt),
        ));
      }
      if (sgstPct > 0) {
        pairs.add((
          'SGST @${money.format(sgstPct)}%',
          rupee(sgstAmt),
        ));
      }
    } else if (invoice.totalGstAmount > 0) {
      taxTotal = invoice.totalGstAmount;
      final half = invoice.totalGstAmount / 2;
      pairs.add(('CGST', rupee(half)));
      pairs.add(('SGST', rupee(half)));
    }
    final discountValue = _moneyComponent(
      invoice.discount,
      invoice.discountType,
      invoice.subTotal,
    );
    final packingValue = _moneyComponent(
      invoice.packingCharge,
      invoice.packingChargeType,
      invoice.subTotal,
    );
    if (discountValue > 0) {
      pairs.add((labels.discount, rupee(discountValue)));
    }
    if (packingValue > 0) {
      pairs.add((labels.packing, rupee(packingValue)));
    }
    /* TOTAL must match shown GST lines (sub + tax − discount + packing). */
    final payable = gstOn || invoice.totalGstAmount > 0
        ? (invoice.subTotal + taxTotal + packingValue - discountValue)
              .clamp(0, double.infinity)
              .ceilToDouble()
        : invoice.totalAmount.ceilToDouble();

    return ticketFromLabels(
      labels: labels,
      shopLines: shopLines,
      metaLines: meta,
      duplicate: duplicate,
      items: lines,
      pairs: pairs,
      totalLabel: labels.totalAmount,
      totalValue: rupee(payable),
      footerLines: [labels.poweredBy, labels.website],
      terms: settings.invoiceTerms,
      qrPayload: upiUriFor(invoice, payableOverride: payable),
    );
  }

  double _moneyComponent(double value, String type, double base) {
    if (value <= 0) return 0;
    return type.toLowerCase().startsWith('p') ? base * value / 100 : value;
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

  /* Invoice-style KOT columns for preview + thermal raster. */
  KotSlipLayout kotLayout(KotTicket ticket) {
    return KotSlipLayout(
      title: labels.kot,
      metaLines: [
        'KOT: ${ticket.kot.kotNumber}',
        '${labels.date}: ${receiptBuilderDate.format(ticket.kot.createdAt)}',
        'Table No: ${ticket.kot.tableNumber}',
        'Round: ${ticket.roundNumber}',
        ticket.kot.kitchenName,
      ],
      colItem: 'ITEMS',
      colQty: labels.qty,
      items: [
        for (final item in ticket.items)
          KotSlipItem(
            name: item.productName,
            qty: 'X${qtyLabel(item.productQuantity, unit: item.productUnit)}',
          ),
      ],
    );
  }

  String kotText(KotTicket ticket) {
    final width = settings.charsPerLine;
    final layout = kotLayout(ticket);
    final buf = StringBuffer()
      ..writeln(receiptBuilderCenter(layout.title, width))
      ..writeln('-' * width);
    for (final line in layout.metaLines) {
      buf.writeln(line);
    }
    buf.writeln('-' * width);
    buf.writeln(kotItemLine(layout.colItem, layout.colQty, width));
    buf.writeln('-' * width);
    for (final item in layout.items) {
      buf.writeln(kotItemLine(item.name, item.qty, width));
    }
    buf.writeln('-' * width);
    return buf.toString();
  }

  String receiptBuilderCenter(String value, int width) {
    if (value.runes.length >= width) return value;
    final pad = width - value.runes.length;
    final left = pad ~/ 2;
    return (' ' * left) + value;
  }

  /* Name left, qty right — truncate name so qty always sits on the right edge. */
  String kotItemLine(String name, String qty, int width) {
    final qtyLen = qty.runes.length;
    final maxName = (width - qtyLen - 1).clamp(1, width);
    var left = name.trim();
    if (left.runes.length > maxName) {
      left = String.fromCharCodes(left.runes.take(maxName));
    }
    final gap = width - left.runes.length - qtyLen;
    return '$left${' ' * gap.clamp(1, width)}$qty';
  }

  String pair(String left, String right, int width) {
    return kotItemLine(left, right, width);
  }
}
