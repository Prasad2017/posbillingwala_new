import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/features/masters/domain/product_units.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/receipt_labels.dart';
import 'package:pos_billingwala_v2/features/print/domain/receipt_rasterizer.dart';
import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';
import 'package:pos_billingwala_v2/features/print/domain/thermal_ticket.dart';

/* Bill / KOT receipt — same data; layout fits 58mm / 80mm / A4. */
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
  final dateFmt = DateFormat('dd-MM-yyyy');
  final timeFmt = DateFormat('hh:mm a');
  final receiptBuilderDate = DateFormat('yyyy-MM-dd HH:mm:ss');
  final rasterizer = const ReceiptRasterizer();

  String rupee(num value) => '₹${money.format(value)}';

  String qtyLabel(num qty, {String? unit}) =>
      ProductUnits.formatQty(qty.toDouble(), unit: unit);

  Future<List<int>> billPrintBytes({
    required Invoice invoice,
    required List<InvoiceItem> items,
    String? shopName,
    bool duplicate = false,
  }) {
    final logoPath = settings.logoUse ? shopProfile.logoLocalPath : null;
    /* A4 is preview/share; thermal printers get 80mm layout of same data. */
    final printSettings = settings.paperSize == PrinterPaperSize.a4
        ? settings.copyWith(paperSize: PrinterPaperSize.inch3)
        : settings;
    return rasterizer.encodeTicket(
      ticket(
        invoice: invoice,
        items: items,
        shopName: shopName,
        duplicate: duplicate,
      ),
      settings: printSettings,
      logoPath: logoPath,
      useAssetLogoFallback: false,
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

  String orderTypeLabel(Invoice invoice) {
    final raw = invoice.invoiceType.trim().toLowerCase();
    if (raw.contains('take')) return 'Takeaway';
    if (raw.contains('fast')) return 'Fast Bill';
    if (raw.contains('dine') || invoice.noOfTable.trim().isNotEmpty) {
      return 'Dine-in';
    }
    if (raw.isEmpty) return 'Sale';
    return invoice.invoiceType.trim();
  }

  /* Indian-number words for A4 (English); empty when amount is 0. */
  String amountWords(num value) {
    final n = value.round();
    if (n <= 0) return '';
    return '${_numberToWords(n)} Rupees Only';
  }

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

    final when = invoice.invoiceDate;
    final billNo = invoice.invoiceNumber.trim();
    final table = invoice.noOfTable.trim();
    final waiter = invoice.createdByStaffName.trim();
    final type = orderTypeLabel(invoice);

    final meta = <String>[
      '${labels.billNo}: $billNo',
      '${labels.date}: ${dateFmt.format(when)}',
      '${labels.time}: ${timeFmt.format(when)}',
    ];
    if (table.isNotEmpty) meta.add('${labels.table}: $table');
    if (waiter.isNotEmpty) meta.add('${labels.waiter}: $waiter');
    meta.add('${labels.type}: $type');

    final metaPairs = <(String, String)>[
      (
        '${labels.billNo}: $billNo',
        '${labels.date}: ${dateFmt.format(when)}',
      ),
      (
        '${labels.time}: ${timeFmt.format(when)}',
        table.isNotEmpty ? '${labels.table}: $table' : '${labels.type}: $type',
      ),
    ];
    if (waiter.isNotEmpty) {
      metaPairs.add((
        '${labels.waiter}: $waiter',
        table.isNotEmpty ? '${labels.type}: $type' : '',
      ));
    } else if (table.isNotEmpty) {
      metaPairs.add(('${labels.type}: $type', ''));
    }

    final custName = invoice.customerName?.trim() ?? '';
    final custMobile = invoice.customerMobile?.trim() ?? '';
    final custEmail = invoice.customerEmail?.trim() ?? '';
    final custAddress = invoice.customerAddress?.trim() ?? '';
    if (settings.customerUse) {
      if (custName.isNotEmpty) meta.add('${labels.customerName}: $custName');
      if (custMobile.isNotEmpty) {
        meta.add('${labels.customerMobile}: $custMobile');
      }
      if (custEmail.isNotEmpty) meta.add('${labels.customerEmail}: $custEmail');
      if (custAddress.isNotEmpty) {
        meta.add('${labels.customerAddress}: $custAddress');
      }
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
    if (invoice.discount > 0) {
      pairs.add(('${labels.discount} (-)', '-${rupee(invoice.discount)}'));
    }
    final cgstPct = double.tryParse(shopProfile.shopCgst.trim()) ?? 0;
    final sgstPct = double.tryParse(shopProfile.shopSgst.trim()) ?? 0;
    final gstOn = shopProfile.gstEnabled && (cgstPct > 0 || sgstPct > 0);
    if (gstOn) {
      final taxable = (invoice.subTotal - invoice.discount).clamp(0, double.infinity);
      if (cgstPct > 0 || sgstPct > 0) {
        pairs.add(('Taxable', rupee(taxable)));
      }
      if (cgstPct > 0) {
        pairs.add((
          'CGST (${money.format(cgstPct)}%)',
          rupee(taxable * cgstPct / 100),
        ));
      }
      if (sgstPct > 0) {
        pairs.add((
          'SGST (${money.format(sgstPct)}%)',
          rupee(taxable * sgstPct / 100),
        ));
      }
    } else if (invoice.totalGstAmount > 0) {
      final half = invoice.totalGstAmount / 2;
      pairs.add(('CGST', rupee(half)));
      pairs.add(('SGST', rupee(half)));
    }
    if (invoice.packingCharge > 0) {
      pairs.add((labels.packing, rupee(invoice.packingCharge)));
    }

    final grand = invoice.totalAmount.ceilToDouble();
    final itemCount = items.fold<double>(
      0,
      (s, e) => s + e.productQuantity,
    );

    final upi = shopProfile.hasUpiId && settings.paymentUse
        ? 'UPI: ${shopProfile.upiId}'
        : '';

    final footer = <String>[
      labels.thankYou,
      if (labels.poweredBy.trim().isNotEmpty) labels.poweredBy,
      if (labels.website.trim().isNotEmpty) labels.website,
    ];

    return ticketFromLabels(
      labels: labels,
      shopLines: shopLines,
      metaLines: meta,
      metaPairs: metaPairs,
      duplicate: duplicate,
      items: lines,
      pairs: pairs,
      footerLines: footer,
      totalItemsLine: '${labels.totalItems}: ${qtyLabel(itemCount)}',
      grandTotalLabel: labels.grandTotal,
      grandTotalValue: rupee(grand),
      upiLine: upi,
      terms: settings.invoiceTerms,
      qrPayload: upiUriFor(invoice),
      customerName: custName.isEmpty ? 'Walk-in Customer' : custName,
      customerMobile: custMobile,
      customerAddress: custAddress,
      amountInWords: amountWords(grand),
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

  String kotText(KotTicket ticket) {
    final width = settings.charsPerLine;
    final buf = StringBuffer()
      ..writeln(receiptBuilderCenter(labels.kot, width))
      ..writeln(receiptBuilderCenter(ticket.kot.kotNumber, width))
      ..writeln('-' * width)
      ..writeln('KOT: ${ticket.kot.kotNumber}')
      ..writeln(
        '${labels.date}: ${receiptBuilderDate.format(ticket.kot.createdAt)}',
      )
      ..writeln('Table No: ${ticket.kot.tableNumber}')
      ..writeln('Round: ${ticket.roundNumber}')
      ..writeln(ticket.kot.kitchenName)
      ..writeln('-' * width);
    for (final item in ticket.items) {
      buf.writeln(
        pair(
          item.productName,
          'X${qtyLabel(item.productQuantity, unit: item.productUnit)}',
          width,
        ),
      );
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

  String pair(String left, String right, int width) {
    final space = width - left.runes.length - right.runes.length;
    final gap = space > 1 ? ' ' * space : ' ';
    return '$left$gap$right';
  }

  static String _numberToWords(int n) {
    if (n == 0) return 'Zero';
    const ones = [
      '',
      'One',
      'Two',
      'Three',
      'Four',
      'Five',
      'Six',
      'Seven',
      'Eight',
      'Nine',
      'Ten',
      'Eleven',
      'Twelve',
      'Thirteen',
      'Fourteen',
      'Fifteen',
      'Sixteen',
      'Seventeen',
      'Eighteen',
      'Nineteen',
    ];
    const tens = [
      '',
      '',
      'Twenty',
      'Thirty',
      'Forty',
      'Fifty',
      'Sixty',
      'Seventy',
      'Eighty',
      'Ninety',
    ];
    String underThousand(int x) {
      if (x == 0) return '';
      if (x < 20) return ones[x];
      if (x < 100) {
        final t = tens[x ~/ 10];
        final o = ones[x % 10];
        return o.isEmpty ? t : '$t $o';
      }
      final h = ones[x ~/ 100];
      final rest = underThousand(x % 100);
      return rest.isEmpty ? '$h Hundred' : '$h Hundred $rest';
    }

    final crore = n ~/ 10000000;
    final lakh = (n % 10000000) ~/ 100000;
    final thousand = (n % 100000) ~/ 1000;
    final rem = n % 1000;
    final parts = <String>[];
    if (crore > 0) parts.add('${underThousand(crore)} Crore');
    if (lakh > 0) parts.add('${underThousand(lakh)} Lakh');
    if (thousand > 0) parts.add('${underThousand(thousand)} Thousand');
    if (rem > 0) parts.add(underThousand(rem));
    return parts.join(' ');
  }
}
