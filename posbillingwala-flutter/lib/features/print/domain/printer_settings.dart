import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

/* Android stores logo/payment/customer as on/off; Flutter also accepts 1/0. */
bool printerFlagOn(String? value) {
  final v = (value ?? '').trim().toLowerCase();
  return v == '1' || v == 'on' || v == 'true' || v == 'yes';
}

String printerFlagValue(bool on) => on ? 'on' : 'off';

/* How the bill/KOT printer is reached. Works with any ESC/POS model. */
enum PosPrinterTransport { bluetooth, usb, network }

extension PosPrinterTransportX on PosPrinterTransport {
  String get label {
    switch (this) {
      case PosPrinterTransport.bluetooth:
        return 'Bluetooth';
      case PosPrinterTransport.usb:
        return 'USB';
      case PosPrinterTransport.network:
        return 'Network';
    }
  }

  static PosPrinterTransport fromStorage(String? value) {
    switch (value) {
      case 'usb':
      case 'USB':
        return PosPrinterTransport.usb;
      case 'network':
      case 'WIFI':
      case 'NETWORK':
        return PosPrinterTransport.network;
      case 'bluetooth':
      case 'BLUETOOTH':
      case 'BT':
      default:
        return PosPrinterTransport.bluetooth;
    }
  }

  /* Bill/KOT company printers are Bluetooth or USB only. */
  static PosPrinterTransport fromLocalStorage(String? value) {
    return fromStorage(value) == PosPrinterTransport.usb
        ? PosPrinterTransport.usb
        : PosPrinterTransport.bluetooth;
  }

  String get dbValue {
    switch (this) {
      case PosPrinterTransport.usb:
        return 'USB';
      case PosPrinterTransport.network:
        return 'WIFI';
      case PosPrinterTransport.bluetooth:
        return 'BLUETOOTH';
    }
  }

  String get storageValue {
    switch (this) {
      case PosPrinterTransport.bluetooth:
        return 'bluetooth';
      case PosPrinterTransport.usb:
        return 'usb';
      case PosPrinterTransport.network:
        return 'network';
    }
  }
}

class PrinterSettings {
  const PrinterSettings({
    this.paperSize = PrinterPaperSize.inch2,
    this.kotPaperSize = PrinterPaperSize.inch2,
    this.billTransport = PosPrinterTransport.bluetooth,
    this.kotTransport = PosPrinterTransport.bluetooth,
    this.billBluetoothAddress = '',
    this.kotBluetoothAddress = '',
    this.billUsbIdentifier = '',
    this.kotUsbIdentifier = '',
    this.billUsbName = '',
    this.kotUsbName = '',
    this.networkHost = '',
    this.networkPort = 9100,
    this.feedLines = 3,
    this.kotFeedLines = 3,
    this.autoShareOnSave = true,
    this.invoiceTitle = '',
    this.invoiceTerms = '',
    this.invoicePrefix = 'PB',
    this.kotPrefix = 'KOT',
    this.customerUse = false,
    this.paymentUse = false,
    this.duplicateBillUse = false,
    this.logoUse = false,
    this.kotEnable = true,
    this.productQuantityUpdate = true,
    this.kotAutoPrint = false,
    this.kotPreview = true,
    this.kotCopies = 1,
  });

  final PrinterPaperSize paperSize;
  final PrinterPaperSize kotPaperSize;
  final PosPrinterTransport billTransport;
  final PosPrinterTransport kotTransport;
  final String billBluetoothAddress;
  final String kotBluetoothAddress;

  /* Android USB id `vendorId:productId`, or desktop serial path. */
  final String billUsbIdentifier;
  final String kotUsbIdentifier;
  final String billUsbName;
  final String kotUsbName;
  final String networkHost;
  final int networkPort;
  final int feedLines;
  final int kotFeedLines;
  final bool autoShareOnSave;
  final String invoiceTitle;
  final String invoiceTerms;
  final String invoicePrefix;
  final String kotPrefix;
  final bool customerUse;
  final bool paymentUse;
  final bool duplicateBillUse;
  final bool logoUse;
  final bool kotEnable;
  final bool productQuantityUpdate;
  final bool kotAutoPrint;
  final bool kotPreview;
  final int kotCopies;

  int get charsPerLine => charsPerLineFor(isKot: false);

  int get kotCharsPerLine => charsPerLineFor(isKot: true);

  int charsPerLineFor({required bool isKot}) =>
      paperSizeFor(isKot: isKot) == PrinterPaperSize.inch3 ? 48 : 32;

  PrinterPaperSize paperSizeFor({required bool isKot}) =>
      isKot ? kotPaperSize : paperSize;

  PosPrinterTransport transportFor({required bool isKot}) {
    final t = isKot ? kotTransport : billTransport;
    return t == PosPrinterTransport.usb
        ? PosPrinterTransport.usb
        : PosPrinterTransport.bluetooth;
  }

  String bluetoothFor({required bool isKot}) =>
      (isKot ? kotBluetoothAddress : billBluetoothAddress).trim();

  String usbIdFor({required bool isKot}) =>
      (isKot ? kotUsbIdentifier : billUsbIdentifier).trim();

  String usbNameFor({required bool isKot}) =>
      (isKot ? kotUsbName : billUsbName).trim();

  PrinterSettings copyWith({
    PrinterPaperSize? paperSize,
    PrinterPaperSize? kotPaperSize,
    PosPrinterTransport? billTransport,
    PosPrinterTransport? kotTransport,
    String? billBluetoothAddress,
    String? kotBluetoothAddress,
    String? billUsbIdentifier,
    String? kotUsbIdentifier,
    String? billUsbName,
    String? kotUsbName,
    String? networkHost,
    int? networkPort,
    int? feedLines,
    int? kotFeedLines,
    bool? autoShareOnSave,
    String? invoiceTitle,
    String? invoiceTerms,
    String? invoicePrefix,
    String? kotPrefix,
    bool? customerUse,
    bool? paymentUse,
    bool? duplicateBillUse,
    bool? logoUse,
    bool? kotEnable,
    bool? productQuantityUpdate,
    bool? kotAutoPrint,
    bool? kotPreview,
    int? kotCopies,
  }) {
    return PrinterSettings(
      paperSize: paperSize ?? this.paperSize,
      kotPaperSize: kotPaperSize ?? this.kotPaperSize,
      billTransport: billTransport ?? this.billTransport,
      kotTransport: kotTransport ?? this.kotTransport,
      billBluetoothAddress: billBluetoothAddress ?? this.billBluetoothAddress,
      kotBluetoothAddress: kotBluetoothAddress ?? this.kotBluetoothAddress,
      billUsbIdentifier: billUsbIdentifier ?? this.billUsbIdentifier,
      kotUsbIdentifier: kotUsbIdentifier ?? this.kotUsbIdentifier,
      billUsbName: billUsbName ?? this.billUsbName,
      kotUsbName: kotUsbName ?? this.kotUsbName,
      networkHost: networkHost ?? this.networkHost,
      networkPort: networkPort ?? this.networkPort,
      feedLines: feedLines ?? this.feedLines,
      kotFeedLines: kotFeedLines ?? this.kotFeedLines,
      autoShareOnSave: autoShareOnSave ?? this.autoShareOnSave,
      invoiceTitle: invoiceTitle ?? this.invoiceTitle,
      invoiceTerms: invoiceTerms ?? this.invoiceTerms,
      invoicePrefix: invoicePrefix ?? this.invoicePrefix,
      kotPrefix: kotPrefix ?? this.kotPrefix,
      customerUse: customerUse ?? this.customerUse,
      paymentUse: paymentUse ?? this.paymentUse,
      duplicateBillUse: duplicateBillUse ?? this.duplicateBillUse,
      logoUse: logoUse ?? this.logoUse,
      kotEnable: kotEnable ?? this.kotEnable,
      productQuantityUpdate:
          productQuantityUpdate ?? this.productQuantityUpdate,
      kotAutoPrint: kotAutoPrint ?? this.kotAutoPrint,
      kotPreview: kotPreview ?? this.kotPreview,
      kotCopies: kotCopies ?? this.kotCopies,
    );
  }
}

enum PrinterPaperSize { inch2, inch3 }

extension PrinterPaperSizeX on PrinterPaperSize {
  String get dbValue => this == PrinterPaperSize.inch3 ? '3-Inch' : '2-Inch';

  static PrinterPaperSize fromDb(String? raw) {
    final n = (raw ?? '').toLowerCase().replaceAll(' ', '');
    if (n.contains('3')) return PrinterPaperSize.inch3;
    return PrinterPaperSize.inch2;
  }
}

class PrinterSettingsStore {
  static Future<PrinterSettings> Function(PrinterSettings prefs)? dbOverlay;
  static Future<void> Function(PrinterSettings settings)? dbPersist;

  static const paperKey = 'printer_paper_size';
  static const kotPaperKey = 'printer_kot_paper_size';
  static const billTransportKey = 'printer_bill_transport';
  static const kotTransportKey = 'printer_kot_transport';
  static const billMacKey = 'printer_bill_mac';
  static const kotMacKey = 'printer_kot_mac';
  static const billUsbKey = 'printer_bill_usb';
  static const kotUsbKey = 'printer_kot_usb';
  static const billUsbNameKey = 'printer_bill_usb_name';
  static const kotUsbNameKey = 'printer_kot_usb_name';
  static const hostKey = 'printer_network_host';
  static const portKey = 'printer_network_port';
  static const feedKey = 'printer_feed_lines';
  static const kotFeedKey = 'printer_kot_feed_lines';
  static const autoShareKey = 'printer_auto_share';
  static const invoiceTitleKey = 'printer_invoice_title';
  static const invoiceTermsKey = 'printer_invoice_terms';
  static const invoicePrefixKey = 'printer_invoice_prefix';
  static const kotPrefixKey = 'printer_kot_prefix';
  static const customerUseKey = 'printer_customer_use';
  static const paymentUseKey = 'printer_payment_use';
  static const duplicateKey = 'printer_duplicate_bill';
  static const logoUseKey = 'printer_logo_use';
  static const kotEnableKey = 'printer_kot_enable';
  static const qtyUpdateKey = 'printer_qty_update';
  static const kotAutoKey = 'printer_kot_auto';
  static const kotPreviewKey = 'printer_kot_preview';
  static const kotCopiesKey = 'printer_kot_copies';

  Future<PrinterSettings> load() async {
    final prefs = await SharedPreferences.getInstance();
    final paper = prefs.getString(paperKey) == '3-Inch'
        ? PrinterPaperSize.inch3
        : PrinterPaperSize.inch2;
    final kotPaperRaw = prefs.getString(kotPaperKey);
    final kotPaper = kotPaperRaw == null
        ? paper
        : (kotPaperRaw == '3-Inch'
              ? PrinterPaperSize.inch3
              : PrinterPaperSize.inch2);
    var loaded = PrinterSettings(
      paperSize: paper,
      kotPaperSize: kotPaper,
      billTransport: PosPrinterTransportX.fromLocalStorage(
        prefs.getString(billTransportKey),
      ),
      kotTransport: PosPrinterTransportX.fromLocalStorage(
        prefs.getString(kotTransportKey),
      ),
      billBluetoothAddress: prefs.getString(billMacKey) ?? '',
      kotBluetoothAddress: prefs.getString(kotMacKey) ?? '',
      billUsbIdentifier: prefs.getString(billUsbKey) ?? '',
      kotUsbIdentifier: prefs.getString(kotUsbKey) ?? '',
      billUsbName: prefs.getString(billUsbNameKey) ?? '',
      kotUsbName: prefs.getString(kotUsbNameKey) ?? '',
      networkHost: prefs.getString(hostKey) ?? '',
      networkPort: prefs.getInt(portKey) ?? 9100,
      feedLines: prefs.getInt(feedKey) ?? 3,
      kotFeedLines: prefs.getInt(kotFeedKey) ?? prefs.getInt(feedKey) ?? 3,
      autoShareOnSave: prefs.getBool(autoShareKey) ?? true,
      invoiceTitle: prefs.getString(invoiceTitleKey) ?? '',
      invoiceTerms: prefs.getString(invoiceTermsKey) ?? '',
      invoicePrefix: prefs.getString(invoicePrefixKey) ?? 'PB',
      kotPrefix: prefs.getString(kotPrefixKey) ?? 'KOT',
      customerUse: prefs.getBool(customerUseKey) ?? false,
      paymentUse: prefs.getBool(paymentUseKey) ?? false,
      duplicateBillUse: prefs.getBool(duplicateKey) ?? false,
      logoUse: prefs.getBool(logoUseKey) ?? false,
      kotEnable: prefs.getBool(kotEnableKey) ?? true,
      productQuantityUpdate: prefs.getBool(qtyUpdateKey) ?? true,
      kotAutoPrint: prefs.getBool(kotAutoKey) ?? false,
      kotPreview: prefs.getBool(kotPreviewKey) ?? true,
      kotCopies: prefs.getInt(kotCopiesKey) ?? 1,
    );
    final overlay = dbOverlay;
    if (overlay != null) {
      loaded = await overlay(loaded);
    }
    return loaded;
  }

  Future<void> save(PrinterSettings settings) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(
      paperKey,
      settings.paperSize == PrinterPaperSize.inch3 ? '3-Inch' : '2-Inch',
    );
    await prefs.setString(
      kotPaperKey,
      settings.kotPaperSize == PrinterPaperSize.inch3 ? '3-Inch' : '2-Inch',
    );
    await prefs.setString(
      billTransportKey,
      settings.billTransport.storageValue,
    );
    await prefs.setString(kotTransportKey, settings.kotTransport.storageValue);
    await prefs.setString(billMacKey, settings.billBluetoothAddress.trim());
    await prefs.setString(kotMacKey, settings.kotBluetoothAddress.trim());
    await prefs.setString(billUsbKey, settings.billUsbIdentifier.trim());
    await prefs.setString(kotUsbKey, settings.kotUsbIdentifier.trim());
    await prefs.setString(billUsbNameKey, settings.billUsbName.trim());
    await prefs.setString(kotUsbNameKey, settings.kotUsbName.trim());
    await prefs.setString(hostKey, settings.networkHost.trim());
    await prefs.setInt(portKey, settings.networkPort);
    await prefs.setInt(feedKey, settings.feedLines);
    await prefs.setInt(kotFeedKey, settings.kotFeedLines);
    await prefs.setBool(autoShareKey, settings.autoShareOnSave);
    await prefs.setString(invoiceTitleKey, settings.invoiceTitle.trim());
    await prefs.setString(invoiceTermsKey, settings.invoiceTerms.trim());
    await prefs.setString(
      invoicePrefixKey,
      settings.invoicePrefix.trim().isEmpty
          ? 'PB'
          : settings.invoicePrefix.trim(),
    );
    await prefs.setString(
      kotPrefixKey,
      settings.kotPrefix.trim().isEmpty ? 'KOT' : settings.kotPrefix.trim(),
    );
    await prefs.setBool(customerUseKey, settings.customerUse);
    await prefs.setBool(paymentUseKey, settings.paymentUse);
    await prefs.setBool(duplicateKey, settings.duplicateBillUse);
    await prefs.setBool(logoUseKey, settings.logoUse);
    await prefs.setBool(kotEnableKey, settings.kotEnable);
    await prefs.setBool(qtyUpdateKey, settings.productQuantityUpdate);
    await prefs.setBool(kotAutoKey, settings.kotAutoPrint);
    await prefs.setBool(kotPreviewKey, settings.kotPreview);
    await prefs.setInt(kotCopiesKey, settings.kotCopies);
    final persist = dbPersist;
    if (persist != null) {
      await persist(settings);
    }
  }
}

final printerSettingsProvider =
    NotifierProvider<PrinterSettingsController, PrinterSettings>(
      PrinterSettingsController.new,
    );

class PrinterSettingsController extends Notifier<PrinterSettings> {
  final store = PrinterSettingsStore();

  @override
  PrinterSettings build() {
    Future.microtask(reload);
    return const PrinterSettings();
  }

  Future<void> reload() async {
    state = await store.load();
  }

  Future<void> update(PrinterSettings settings) async {
    await store.save(settings);
    state = settings;
  }
}
