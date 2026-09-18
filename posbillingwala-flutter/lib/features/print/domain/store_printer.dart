import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';

class StorePrinter {
  const StorePrinter({
    required this.id,
    required this.printerName,
    required this.connectionType,
    this.ipAddress = '',
    this.port = 9100,
    this.bluetoothAddress = '',
    this.usbIdentifier = '',
    this.usbName = '',
    this.paperSize = '2-Inch',
    this.purpose = 'KOT',
    this.area = 'KITCHEN',
    this.status = 'OFFLINE',
    this.enabled = true,
    this.isDefault = false,
    this.isBackup = false,
    this.primaryPrinterId = '',
    this.deviceId = '',
  });

  final String id;
  final String printerName;
  final String connectionType;
  final String ipAddress;
  final int port;
  final String bluetoothAddress;
  final String usbIdentifier;
  final String usbName;
  final String paperSize;
  final String purpose;
  final String area;
  final String status;
  final bool enabled;
  final bool isDefault;
  final bool isBackup;
  final String primaryPrinterId;
  final String deviceId;

  PrinterPaperSize get paperSizeEnum {
    final n = paperSize.toLowerCase().replaceAll(' ', '');
    if (n.contains('3')) return PrinterPaperSize.inch3;
    return PrinterPaperSize.inch2;
  }

  String get connectionLabel {
    switch (connectionType.toUpperCase()) {
      case 'USB':
        return 'USB';
      case 'WIFI':
      case 'NETWORK':
        return 'Wi-Fi';
      default:
        return 'Bluetooth';
    }
  }

  String get paperSizeLabel =>
      paperSizeEnum == PrinterPaperSize.inch3 ? '3-Inch' : '2-Inch';

  factory StorePrinter.fromJson(Map<String, dynamic> json) {
    String s(Object? v) => v?.toString() ?? '';
    return StorePrinter(
      id: s(json['id']),
      printerName: s(json['printerName']),
      connectionType: s(json['connectionType']),
      ipAddress: s(json['ipAddress']),
      port: int.tryParse(s(json['port'])) ?? 9100,
      bluetoothAddress: s(json['bluetoothAddress']),
      usbIdentifier: s(json['usbIdentifier']),
      usbName: s(json['usbName']),
      paperSize: s(json['paperSize']).isEmpty ? '2-Inch' : s(json['paperSize']),
      purpose: s(json['purpose']).isEmpty ? 'KOT' : s(json['purpose']),
      area: s(json['area']).isEmpty ? 'KITCHEN' : s(json['area']),
      status: s(json['status']),
      enabled: s(json['enabled']) != '0',
      isDefault: s(json['isDefault']) == '1',
      isBackup: s(json['isBackup']) == '1',
      primaryPrinterId: s(json['primaryPrinterId']),
      deviceId: s(json['deviceId']),
    );
  }

  Map<String, dynamic> toForm() => {
    'printerName': printerName,
    'connectionType': connectionType,
    'ipAddress': ipAddress,
    'port': '$port',
    'bluetoothAddress': bluetoothAddress,
    'usbIdentifier': usbIdentifier,
    'usbName': usbName,
    'paperSize': paperSize,
    'purpose': purpose,
    'area': area,
    'deviceId': deviceId,
    'enabled': enabled ? '1' : '0',
    'isDefault': isDefault ? '1' : '0',
    'isBackup': isBackup ? '1' : '0',
    'primaryPrinterId': primaryPrinterId,
  };
}

class PrinterRouteRule {
  const PrinterRouteRule({
    required this.printerId,
    this.documentType = 'KOT',
    this.foodTypeCode = '',
    this.categoryId = 0,
  });

  final String printerId;
  final String documentType;
  final String foodTypeCode;
  final int categoryId;

  factory PrinterRouteRule.fromJson(Map<String, dynamic> json) {
    return PrinterRouteRule(
      printerId: json['printerId']?.toString() ?? '',
      documentType: json['documentType']?.toString() ?? 'KOT',
      foodTypeCode: json['foodTypeCode']?.toString() ?? '',
      categoryId: int.tryParse(json['categoryId']?.toString() ?? '') ?? 0,
    );
  }

  Map<String, dynamic> toJson() => {
    'printerId': printerId,
    'documentType': documentType,
    'foodTypeCode': foodTypeCode,
    'categoryId': categoryId,
  };
}
