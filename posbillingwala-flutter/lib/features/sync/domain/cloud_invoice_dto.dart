import 'package:drift/drift.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/utils/json_parsers.dart';

class CloudInvoiceDto {
  const CloudInvoiceDto({
    required this.invoiceNumber,
    required this.invoiceNetworkStatus,
    required this.invoiceDate,
    this.invoiceType = 'fast_billing',
    this.subTotal = 0,
    this.totalGstAmount = 0,
    this.discount = 0,
    this.discountType = 'Amount',
    this.packingCharge = 0,
    this.packingChargeType = 'Amount',
    this.totalAmount = 0,
    this.paymentMode = 'Cash',
    this.cashAmount = 0,
    this.upiAmount = 0,
    this.noOfTable = '',
    this.customerName,
    this.customerMobile,
    this.customerEmail,
    this.customerAddress,
    this.diningSessionId,
    this.invoiceOrderStatus = 'completed',
    this.itemCount = 0,
    this.organizationId,
    this.branchId,
    this.deviceId,
    this.userId,
  });

  final String invoiceNumber;
  final String invoiceNetworkStatus;
  final DateTime invoiceDate;
  final String invoiceType;
  final double subTotal;
  final double totalGstAmount;
  final double discount;
  final String discountType;
  final double packingCharge;
  final String packingChargeType;
  final double totalAmount;
  final String paymentMode;
  final double cashAmount;
  final double upiAmount;
  final String noOfTable;
  final String? customerName;
  final String? customerMobile;
  final String? customerEmail;
  final String? customerAddress;
  final int? diningSessionId;
  final String invoiceOrderStatus;
  final int itemCount;
  final String? organizationId;
  final String? branchId;
  final String? deviceId;
  final String? userId;

  factory CloudInvoiceDto.fromJson(Map<String, dynamic> json) {
    final date = parseInvoiceDate(json['invoiceDate']) ?? DateTime.now();
    final number = parseString(json['invoiceNumber'])?.trim() ?? '';
    final networkRaw = parseString(json['invoiceNetworkStatus'])?.trim() ?? '';
    return CloudInvoiceDto(
      invoiceNumber: number,
      // Older cloud rows may omit network status — fall back so they still import.
      invoiceNetworkStatus:
          networkRaw.isNotEmpty ? networkRaw : (number.isEmpty ? '' : 'inv_$number'),
      invoiceDate: date,
      invoiceType: parseString(json['invoiceType'])?.trim().isNotEmpty == true
          ? parseString(json['invoiceType'])!.trim()
          : 'fast_billing',
      subTotal: parseCloudMoney(json['subTotal']),
      totalGstAmount: parseCloudMoney(json['totalGSTAmount']),
      discount: parseCloudMoney(json['discount']),
      discountType: parseString(json['discountType']) ?? 'Amount',
      packingCharge: parseCloudMoney(json['packingCharge']),
      packingChargeType: parseString(json['packingChargeType']) ?? 'Amount',
      totalAmount: parseCloudMoney(json['totalAmount']),
      paymentMode: parseString(json['paymentMode']) ?? 'Cash',
      cashAmount: parseCloudMoney(json['cashAmount']),
      upiAmount: parseCloudMoney(json['upiAmount']),
      noOfTable: parseString(json['noOfTable']) ?? '',
      customerName: _emptyToNull(parseString(json['customerName'])),
      customerMobile: _emptyToNull(parseString(json['customerMobile'])),
      customerEmail: _emptyToNull(parseString(json['customerEmail'])),
      customerAddress: _emptyToNull(parseString(json['customerAddress'])),
      diningSessionId: parseInt(json['diningSessionId']),
      invoiceOrderStatus:
          parseString(json['invoiceOrderStatus'])?.trim().isNotEmpty == true
              ? parseString(json['invoiceOrderStatus'])!.trim()
              : 'completed',
      organizationId: _emptyToNull(parseString(json['organizationId'])),
      branchId: _emptyToNull(parseString(json['branchId'])),
      deviceId: _emptyToNull(parseString(json['deviceId'])),
      userId: _emptyToNull(parseString(json['userId'])),
    );
  }

  Map<String, dynamic> toJson() => {
        'invoiceNumber': invoiceNumber,
        'invoiceNetworkStatus': invoiceNetworkStatus,
        'invoiceDate': invoiceDate.toIso8601String(),
        'invoiceType': invoiceType,
        'subTotal': subTotal,
        'totalGSTAmount': totalGstAmount,
        'discount': discount,
        'discountType': discountType,
        'packingCharge': packingCharge,
        'packingChargeType': packingChargeType,
        'totalAmount': totalAmount,
        'paymentMode': paymentMode,
        'cashAmount': cashAmount,
        'upiAmount': upiAmount,
        'noOfTable': noOfTable,
        'customerName': customerName,
        'customerMobile': customerMobile,
        'customerEmail': customerEmail,
        'customerAddress': customerAddress,
        'diningSessionId': diningSessionId,
        'invoiceOrderStatus': invoiceOrderStatus,
        if (organizationId != null) 'organizationId': organizationId,
        if (branchId != null) 'branchId': branchId,
        if (deviceId != null) 'deviceId': deviceId,
        if (userId != null) 'userId': userId,
      };

  InvoicesCompanion toCompanion() {
    return InvoicesCompanion.insert(
      invoiceNumber: invoiceNumber,
      invoiceDate: invoiceDate,
      invoiceType: Value(invoiceType),
      subTotal: Value(subTotal),
      totalGstAmount: Value(totalGstAmount),
      discount: Value(discount),
      discountType: Value(discountType),
      packingCharge: Value(packingCharge),
      packingChargeType: Value(packingChargeType),
      totalAmount: Value(totalAmount),
      paymentMode: Value(paymentMode),
      cashAmount: Value(cashAmount),
      upiAmount: Value(upiAmount),
      invoiceOrderStatus: Value(
        invoiceOrderStatus.isEmpty ? 'completed' : invoiceOrderStatus,
      ),
      invoiceNetworkStatus: invoiceNetworkStatus,
      invoiceSyncStatus: const Value('1'),
      noOfTable: Value(noOfTable),
      customerName: Value(customerName),
      customerMobile: Value(customerMobile),
      customerEmail: Value(customerEmail),
      customerAddress: Value(customerAddress),
      diningSessionId: Value(diningSessionId),
      itemCount: Value(itemCount),
      createdAt: Value(invoiceDate),
      organizationId: Value(organizationId ?? ''),
      branchId: Value(branchId ?? ''),
      deviceId: Value(deviceId ?? ''),
      userId: Value(userId),
    );
  }

  static String? _emptyToNull(String? value) {
    final trimmed = value?.trim();
    if (trimmed == null || trimmed.isEmpty) return null;
    return trimmed;
  }
}

class CloudInvoiceItemDto {
  const CloudInvoiceItemDto({
    required this.invoiceNumber,
    required this.productName,
    required this.invoiceItemNetworkStatus,
    this.productPrice = 0,
    this.productQuantity = 1,
    this.productCgst = 0,
    this.productSgst = 0,
    this.productUnit,
    this.productStatus = 'completed',
    this.invoiceItemType = 'product',
  });

  final String invoiceNumber;
  final String productName;
  final String invoiceItemNetworkStatus;
  final double productPrice;
  final int productQuantity;
  final double productCgst;
  final double productSgst;
  final String? productUnit;
  final String productStatus;
  final String invoiceItemType;

  factory CloudInvoiceItemDto.fromJson(Map<String, dynamic> json) {
    // PHP misnames invoiceProductStatus — it is the line network UUID.
    final network = parseString(json['invoiceProductStatus'])?.trim().isNotEmpty ==
            true
        ? parseString(json['invoiceProductStatus'])!.trim()
        : (parseString(json['invoiceProductNetworkStatus'])?.trim() ?? '');

    return CloudInvoiceItemDto(
      invoiceNumber: parseString(json['invoiceNumber'])?.trim() ?? '',
      productName: parseString(json['productName'])?.trim() ?? '',
      invoiceItemNetworkStatus: network,
      productPrice: parseCloudMoney(json['productPrice']),
      productQuantity: parseInt(json['productQuantity']) ?? 1,
      productCgst: parseCloudMoney(json['productCGST']),
      productSgst: parseCloudMoney(json['productSGST']),
      productUnit: parseString(json['productUnit']),
      productStatus: parseString(json['productStatus']) ?? 'completed',
      invoiceItemType:
          parseString(json['invoiceItemType'])?.toLowerCase() ?? 'product',
    );
  }

  Map<String, dynamic> toJson() => {
        'invoiceNumber': invoiceNumber,
        'productName': productName,
        'invoiceProductStatus': invoiceItemNetworkStatus,
        'productPrice': productPrice,
        'productQuantity': productQuantity,
        'productCGST': productCgst,
        'productSGST': productSgst,
        'productUnit': productUnit,
        'productStatus': productStatus,
        'invoiceItemType': invoiceItemType,
      };

  InvoiceItemsCompanion toCompanion() {
    return InvoiceItemsCompanion.insert(
      invoiceNumber: invoiceNumber,
      productName: Value(productName),
      productPrice: Value(productPrice),
      productQuantity: Value(productQuantity),
      productCgst: Value(productCgst),
      productSgst: Value(productSgst),
      productUnit: Value(productUnit ?? ''),
      productStatus: Value(productStatus),
      invoiceItemType: Value(invoiceItemType),
      invoiceItemNetworkStatus: Value(
        invoiceItemNetworkStatus.isEmpty
            ? 'cloud_${invoiceNumber}_$productName'
            : invoiceItemNetworkStatus,
      ),
      invoiceItemSyncStatus: const Value('1'),
    );
  }
}

/// Cloud invoice combo component line (`getInvoiceComboItemList.php`).
class CloudInvoiceComboItemDto {
  const CloudInvoiceComboItemDto({
    required this.invoiceNumber,
    this.invoiceProductNetworkStatus,
    this.comboId,
    this.comboNetworkStatus,
    this.productId,
    this.productName,
    this.portionId,
    this.portionName,
    this.quantity = 1,
    this.sortOrder = 0,
    this.invoiceComboItemNetworkStatus,
    this.invoiceComboItemStatus = '1',
  });

  final String invoiceNumber;
  final String? invoiceProductNetworkStatus;
  final int? comboId;
  final String? comboNetworkStatus;
  final int? productId;
  final String? productName;
  final int? portionId;
  final String? portionName;
  final int quantity;
  final int sortOrder;
  final String? invoiceComboItemNetworkStatus;
  final String invoiceComboItemStatus;

  factory CloudInvoiceComboItemDto.fromJson(Map<String, dynamic> json) {
    return CloudInvoiceComboItemDto(
      invoiceNumber: parseString(json['invoiceNumber'])?.trim() ?? '',
      invoiceProductNetworkStatus:
          parseString(json['invoiceProductNetworkStatus'])?.trim(),
      comboId: parseInt(json['comboId']),
      comboNetworkStatus: parseString(json['comboNetworkStatus'])?.trim(),
      productId: parseInt(json['productId']),
      productName: parseString(json['productName'])?.trim() ??
          parseString(json['productNameSnapshot'])?.trim(),
      portionId: parseInt(json['portionId']),
      portionName: parseString(json['portionName'])?.trim() ??
          parseString(json['portionNameSnapshot'])?.trim(),
      quantity: parseInt(json['comboItemQuantity']) ??
          parseInt(json['quantity']) ??
          1,
      sortOrder: parseInt(json['sortOrder']) ?? 0,
      invoiceComboItemNetworkStatus:
          parseString(json['invoiceComboItemNetworkStatus'])?.trim(),
      invoiceComboItemStatus:
          parseString(json['invoiceComboItemStatus'])?.trim().isNotEmpty == true
              ? parseString(json['invoiceComboItemStatus'])!.trim()
              : '1',
    );
  }

  InvoiceComboItemsCompanion toCompanion() {
    final network = invoiceComboItemNetworkStatus?.trim().isNotEmpty == true
        ? invoiceComboItemNetworkStatus!.trim()
        : 'ici_${invoiceNumber}_${productId ?? productName ?? sortOrder}';
    return InvoiceComboItemsCompanion.insert(
      invoiceNumber: Value(invoiceNumber),
      invoiceProductNetworkStatus: Value(invoiceProductNetworkStatus),
      comboId: Value(comboId),
      comboNetworkStatus: Value(comboNetworkStatus),
      productId: Value(productId),
      productNameSnapshot: Value(productName),
      portionId: Value(portionId),
      portionNameSnapshot: Value(portionName),
      quantity: Value(quantity <= 0 ? 1 : quantity),
      sortOrder: Value(sortOrder),
      invoiceComboItemNetworkStatus: Value(network),
      invoiceComboItemStatus: Value(
        invoiceComboItemStatus.isEmpty ? '1' : invoiceComboItemStatus,
      ),
    );
  }
}
