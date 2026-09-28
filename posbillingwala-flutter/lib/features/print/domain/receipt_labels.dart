import 'package:pos_billingwala_v2/language/locale_catalog.dart';

/* Thermal receipt labels matching WithTable `strings_ui.xml`. */
class ReceiptLabels {
  const ReceiptLabels({
    required this.originalCopy,
    required this.duplicateCopy,
    required this.item,
    required this.qty,
    required this.rate,
    required this.amount,
    required this.subTotal,
    required this.discount,
    required this.packing,
    required this.totalAmount,
    required this.grandTotal,
    required this.poweredBy,
    required this.website,
    required this.customerName,
    required this.customerMobile,
    required this.customerEmail,
    required this.customerAddress,
    required this.date,
    required this.time,
    required this.kot,
    required this.billNo,
    required this.table,
    required this.waiter,
    required this.type,
    required this.totalItems,
    required this.taxInvoice,
    required this.thankYou,
    required this.amountInWords,
    required this.srNo,
  });

  final String originalCopy;
  final String duplicateCopy;
  final String item;
  final String qty;
  final String rate;
  final String amount;
  final String subTotal;
  final String discount;
  final String packing;
  final String totalAmount;
  final String grandTotal;
  final String poweredBy;
  final String website;
  final String customerName;
  final String customerMobile;
  final String customerEmail;
  final String customerAddress;
  final String date;
  final String time;
  final String kot;
  final String billNo;
  final String table;
  final String waiter;
  final String type;
  final String totalItems;
  final String taxInvoice;
  final String thankYou;
  final String amountInWords;
  final String srNo;

  factory ReceiptLabels.fromLang(String lang) {
    String t(String key) => LocaleCatalog.get(lang, key);
    String or(String key, String fallback) {
      final v = t(key).trim();
      if (v.isEmpty || v == key || v.startsWith('ui_') || v.startsWith('print_')) {
        return fallback;
      }
      return v;
    }

    return ReceiptLabels(
      originalCopy: t('ui__original_copy_'),
      duplicateCopy: t('ui__duplicate_copy_'),
      item: or('ui_item', 'Item'),
      qty: or('ui_qty', 'Qty'),
      rate: or('ui_rate', 'Rate'),
      amount: or('ui_amount', 'Amount'),
      subTotal: or('ui_sub_total', 'Sub Total'),
      discount: or('ui_discount', 'Discount'),
      packing: or('ui_packing', 'Packing'),
      totalAmount: or('ui_total_amount', 'Total Amount'),
      grandTotal: or('ui_grand_total', 'Grand Total'),
      poweredBy: t('print_powered_by'),
      website: t('print_powered_by_website'),
      customerName: or('ui_customer_name', 'Name'),
      customerMobile: or('ui_customer_mobile', 'Mobile'),
      customerEmail: or('ui_customer_email', 'Email'),
      customerAddress: or('ui_customer_address', 'Address'),
      date: or('ui_date', 'Date'),
      time: or('ui_time', 'Time'),
      kot: t('ui_kot_print'),
      billNo: or('ui_bill_no', 'Bill No'),
      table: or('ui_table', 'Table'),
      waiter: or('ui_waiter', 'Waiter'),
      type: or('ui_order_type', 'Type'),
      totalItems: or('ui_total_items', 'Total Items'),
      taxInvoice: or('ui_tax_invoice', 'TAX INVOICE'),
      thankYou: or('ui_thank_visit_again', 'Thank You! Visit Again!'),
      amountInWords: or('ui_amount_in_words', 'Amount in Words'),
      srNo: or('ui_sr_no', 'Sr'),
    );
  }
}
