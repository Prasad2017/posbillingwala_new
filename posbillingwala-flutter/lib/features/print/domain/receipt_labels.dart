import 'package:pos_billingwala_v2/l10n/ui_catalog.dart';

/* Thermal receipt labels matching WithTable `strings_ui.xml`. */
class ReceiptLabels {
  const ReceiptLabels({
    required this.originalCopy,
    required this.duplicateCopy,
    required this.item,
    required this.rate,
    required this.amount,
    required this.subTotal,
    required this.discount,
    required this.packing,
    required this.totalAmount,
    required this.poweredBy,
    required this.website,
    required this.customerName,
    required this.customerMobile,
    required this.customerEmail,
    required this.customerAddress,
    required this.date,
    required this.kot,
  });

  final String originalCopy;
  final String duplicateCopy;
  final String item;
  final String rate;
  final String amount;
  final String subTotal;
  final String discount;
  final String packing;
  final String totalAmount;
  final String poweredBy;
  final String website;
  final String customerName;
  final String customerMobile;
  final String customerEmail;
  final String customerAddress;
  final String date;
  final String kot;

  factory ReceiptLabels.fromLang(String lang) {
    String t(String key) => UiCatalog.get(lang, key);
    return ReceiptLabels(
      originalCopy: t('ui__original_copy_'),
      duplicateCopy: t('ui__duplicate_copy_'),
      item: t('ui_item'),
      rate: t('ui_rate'),
      amount: t('ui_amount'),
      subTotal: t('ui_sub_total'),
      discount: t('ui_discount'),
      packing: t('ui_packing'),
      totalAmount: t('ui_total_amount'),
      poweredBy: t('print_powered_by'),
      website: t('print_powered_by_website'),
      customerName: t('ui_customer_name'),
      customerMobile: t('ui_customer_mobile'),
      customerEmail: t('ui_customer_email'),
      customerAddress: t('ui_customer_address'),
      date: t('ui_date'),
      kot: t('ui_kot_print'),
    );
  }
}
