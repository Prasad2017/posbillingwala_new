import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_auto_connect.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_cut_type.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_paper_profile.dart';
import 'package:shared_preferences/shared_preferences.dart';

export 'printer_cut_type.dart' show PrinterCutType, PrinterCutTypeX;
export 'printer_paper_profile.dart'
    show PrinterPaperProfile, PrinterPaperSize, PrinterPaperSizeX;

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

  /* Bill/KOT may be Bluetooth, USB, or Network (LAN/Wi‑Fi ESC/POS). */
  static PosPrinterTransport fromLocalStorage(String? value) {
    return fromStorage(value);
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
    this.paperSize = PrinterPaperSize.mm58,
    this.kotPaperSize = PrinterPaperSize.mm58,
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
    this.feedLines = 0,
    this.kotFeedLines = 0,
    this.supportsAutoCut = true,
    this.cutType = PrinterCutType.defaultCut,
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
    this.printFastBill = false,
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

  /* When true, send ESC/POS cut after feed if capability allows. */
  final bool supportsAutoCut;
  final PrinterCutType cutType;
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
  final bool printFastBill;
  final int kotCopies;

  int get charsPerLine => charsPerLineFor(isKot: false);

  int get kotCharsPerLine => charsPerLineFor(isKot: true);

  PrinterPaperProfile profileFor({required bool isKot}) =>
      PrinterPaperProfile.of(paperSizeFor(isKot: isKot));

  PrinterPaperProfile get billProfile => profileFor(isKot: false);

  PrinterPaperProfile get kotProfile => profileFor(isKot: true);

  int charsPerLineFor({required bool isKot}) =>
      profileFor(isKot: isKot).charsPerLine;

  PrinterPaperSize paperSizeFor({required bool isKot}) =>
      isKot ? kotPaperSize : paperSize;

  PosPrinterTransport transportFor({required bool isKot}) {
    return isKot ? kotTransport : billTransport;
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
    bool? supportsAutoCut,
    PrinterCutType? cutType,
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
    bool? printFastBill,
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
      supportsAutoCut: supportsAutoCut ?? this.supportsAutoCut,
      cutType: cutType ?? this.cutType,
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
      printFastBill: printFastBill ?? this.printFastBill,
      kotCopies: kotCopies ?? this.kotCopies,
    );
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
  static const autoCutKey = 'printer_supports_auto_cut';
  static const cutTypeKey = 'printer_cut_type';
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
  static const printFastBillKey = 'printer_print_fast_bill';
  static const kotCopiesKey = 'printer_kot_copies';
  static const pendingUploadKey = 'printer_pending_upload';

  Future<bool> isPendingUpload() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(pendingUploadKey) ?? false;
  }

  Future<void> setPendingUpload(bool pending) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(pendingUploadKey, pending);
  }

  Future<PrinterSettings> load() async {
    final prefs = await SharedPreferences.getInstance();
    final paper = PrinterPaperSizeX.fromDb(prefs.getString(paperKey));
    final kotPaperRaw = prefs.getString(kotPaperKey);
    final kotPaper = kotPaperRaw == null
        ? paper
        : PrinterPaperSizeX.fromDb(kotPaperRaw);
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
      feedLines: prefs.getInt(feedKey) ?? 0,
      kotFeedLines: prefs.getInt(kotFeedKey) ?? prefs.getInt(feedKey) ?? 0,
      supportsAutoCut: prefs.getBool(autoCutKey) ?? true,
      cutType: PrinterCutTypeX.fromStorage(prefs.getString(cutTypeKey)),
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
      printFastBill: prefs.getBool(printFastBillKey) ?? false,
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
    await prefs.setString(paperKey, settings.paperSize.dbValue);
    await prefs.setString(kotPaperKey, settings.kotPaperSize.dbValue);
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
    await prefs.setBool(autoCutKey, settings.supportsAutoCut);
    await prefs.setString(cutTypeKey, settings.cutType.storageValue);
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
    await prefs.setBool(printFastBillKey, settings.printFastBill);
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
    /* Restore last saved BT/USB link app-wide after settings hydrate. */
    unawaited(PrinterAutoConnect.ensureSavedPrinters(state));
  }

  Future<bool> isPendingUpload() => store.isPendingUpload();

  Future<void> setPendingUpload(bool pending) =>
      store.setPendingUpload(pending);

  /* Local edits: [fromCloud] false marks pending so sync won't overwrite. */
  Future<void> update(
    PrinterSettings settings, {
    bool fromCloud = false,
  }) async {
    final previous = state;
    await store.save(settings);
    state = settings;
    if (!fromCloud) {
      await store.setPendingUpload(true);
    }
    final endpointsChanged =
        previous.billBluetoothAddress != settings.billBluetoothAddress ||
        previous.kotBluetoothAddress != settings.kotBluetoothAddress ||
        previous.billUsbIdentifier != settings.billUsbIdentifier ||
        previous.kotUsbIdentifier != settings.kotUsbIdentifier ||
        previous.billTransport != settings.billTransport ||
        previous.kotTransport != settings.kotTransport;
    if (fromCloud || endpointsChanged) {
      unawaited(PrinterAutoConnect.ensureSavedPrinters(settings));
    }
  }
}
