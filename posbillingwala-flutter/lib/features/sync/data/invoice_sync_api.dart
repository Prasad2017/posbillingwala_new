import 'package:dio/dio.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/network/api_client.dart';
import 'package:pos_billingwala_v2/core/network/api_response.dart';
import 'package:pos_billingwala_v2/core/utils/json_parsers.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_invoice_dto.dart';

class SalesReportSummary {
  const SalesReportSummary({
    required this.billCount,
    required this.totalSales,
    required this.subTotal,
    required this.gstTotal,
    required this.discountTotal,
    required this.cashTotal,
    required this.upiTotal,
    required this.posCount,
    required this.takeawayCount,
    required this.tableCount,
    required this.avgBill,
  });

  final int billCount;
  final double totalSales;
  final double subTotal;
  final double gstTotal;
  final double discountTotal;
  final double cashTotal;
  final double upiTotal;
  final int posCount;
  final int takeawayCount;
  final int tableCount;
  final double avgBill;

  factory SalesReportSummary.fromJson(Map<String, dynamic> json) {
    return SalesReportSummary(
      billCount: parseInt(json['billCount']) ?? 0,
      totalSales: parseMoney(json['totalSales']),
      subTotal: parseMoney(json['subTotal']),
      gstTotal: parseMoney(json['gstTotal']),
      discountTotal: parseMoney(json['discountTotal']),
      cashTotal: parseMoney(json['cashTotal']),
      upiTotal: parseMoney(json['upiTotal']),
      posCount: parseInt(json['posCount']) ?? 0,
      takeawayCount: parseInt(json['takeawayCount']) ?? 0,
      tableCount: parseInt(json['tableCount']) ?? 0,
      avgBill: parseMoney(json['avgBill']),
    );
  }
}

class InvoiceSyncApi {
  InvoiceSyncApi(this.client);

  final ApiClient client;

  Future<bool> uploadInvoice({
    required String userId,
    required Invoice invoice,
  }) async {
    final data = await invoiceSyncApiPost(
      ApiEndpoints.insertInvoice,
      fields: {
        'userId': userId,
        'noOfTable': invoice.noOfTable,
        'invoiceNumber': invoice.invoiceNumber,
        'customerName': invoice.customerName ?? '',
        'customerMobile': invoice.customerMobile ?? '',
        'customerEmail': invoice.customerEmail ?? '',
        'customerAddress': invoice.customerAddress ?? '',
        'subTotal': invoice.subTotal.toStringAsFixed(2),
        'totalGSTAmount': invoice.totalGstAmount.toStringAsFixed(2),
        'discount': invoice.discount.toStringAsFixed(2),
        'discountType': invoice.discountType,
        'packingCharge': invoice.packingCharge.toStringAsFixed(2),
        'packingChargeType': invoice.packingChargeType,
        'totalAmount': invoice.totalAmount.toStringAsFixed(2),
        'paymentMode': invoice.paymentMode,
        'cashAmount': invoice.cashAmount.toStringAsFixed(2),
        'upiAmount': invoice.upiAmount.toStringAsFixed(2),
        'diningSessionId': '${invoice.diningSessionId ?? ''}',
        'billPrintStatus': invoice.billPrintStatus,
        'invoiceDate': DateFormat(
          'yyyy-MM-dd HH:mm:ss',
        ).format(invoice.invoiceDate),
        'invoiceType': invoice.invoiceType,
        'invoiceOrderStatus': invoice.invoiceOrderStatus,
        'invoiceNetworkStatus': invoice.invoiceNetworkStatus,
        'organizationId': invoice.organizationId,
        'branchId': invoice.branchId,
        'deviceId': invoice.deviceId,
        if (invoice.createdByStaffId != null)
          'createdByStaffId': '${invoice.createdByStaffId}',
        if (invoice.createdByStaffName.trim().isNotEmpty)
          'createdByStaffName': invoice.createdByStaffName,
      },
    );
    return isApiSuccess(data);
  }

  Future<bool> uploadInvoiceItem({
    required InvoiceItem item,
    String? comboNetworkStatus,
  }) async {
    final network = item.invoiceItemNetworkStatus?.trim().isNotEmpty == true
        ? item.invoiceItemNetworkStatus!
        : 'item${item.invoiceItemId}';
    final data = await invoiceSyncApiPost(
      ApiEndpoints.insertInvoiceProduct,
      fields: {
        'invoiceNumber': item.invoiceNumber,
        'productName': item.productName,
        'productPrice': item.productPrice.toStringAsFixed(2),
        'productUnit': item.productUnit ?? '',
        'productCGST': item.productCgst.toStringAsFixed(2),
        'productSGST': item.productSgst.toStringAsFixed(2),
        'productQuantity': '${item.productQuantity}',
        'productStatus': item.productStatus,
        'invoiceProductNetworkStatus': network,
        'portionId': item.portionId == null ? '' : '${item.portionId}',
        'portionName': item.portionName ?? '',
        'snapshotProductName':
            item.snapshotProductName?.trim().isNotEmpty == true
            ? item.snapshotProductName!
            : item.productName,
        'snapshotLinePrice': (item.snapshotLinePrice ?? item.productPrice)
            .toStringAsFixed(2),
        'invoiceItemType': item.invoiceItemType.trim().isEmpty
            ? 'PRODUCT'
            : item.invoiceItemType.toUpperCase(),
        'comboId': item.comboId == null ? '' : '${item.comboId}',
        'comboNetworkStatus': comboNetworkStatus ?? '',
        'snapshotComboComponents': item.snapshotComboComponents ?? '',
        'organizationId': item.organizationId,
        'branchId': item.branchId,
        'deviceId': item.deviceId,
      },
    );
    return isApiSuccess(data);
  }

  /* Uploads one invoice combo component row (WithTable `saveInvoiceComboItem`). */
  Future<bool> uploadInvoiceComboItem({required InvoiceComboItem item}) async {
    final network =
        item.invoiceComboItemNetworkStatus?.trim().isNotEmpty == true
        ? item.invoiceComboItemNetworkStatus!
        : 'ici_${item.invoiceComboItemId}';
    final data = await invoiceSyncApiPost(
      ApiEndpoints.insertInvoiceComboItem,
      fields: {
        'invoiceNumber': item.invoiceNumber ?? '',
        'invoiceProductNetworkStatus': item.invoiceProductNetworkStatus ?? '',
        'comboNetworkStatus': item.comboNetworkStatus ?? '',
        'productId': item.productId == null ? '' : '${item.productId}',
        'productNetworkStatus': '',
        'productName': item.productNameSnapshot ?? '',
        'portionId': item.portionId == null ? '' : '${item.portionId}',
        'portionNetworkStatus': '',
        'portionName': item.portionNameSnapshot ?? '',
        'quantity': '${item.quantity}',
        'sortOrder': '${item.sortOrder}',
        'invoiceComboItemNetworkStatus': network,
      },
    );
    return isApiSuccess(data);
  }

  Future<bool> deleteInvoiceProduct({
    required String invoiceProductNetworkStatus,
  }) async {
    final data = await invoiceSyncApiPost(
      ApiEndpoints.deleteInvoiceProduct,
      fields: {'invoiceProductNetworkStatus': invoiceProductNetworkStatus},
    );
    return isApiSuccess(data);
  }

  Future<List<CloudInvoiceDto>> fetchInvoices(
    String userId, {
    String? invoiceDate,
    String? startDate,
    String? endDate,
    bool staffScope = false,
  }) async {
    final data = await invoiceSyncApiGet(
      ApiEndpoints.getInvoiceList,
      query: {
        'userId': userId,
        if (invoiceDate != null && invoiceDate.isNotEmpty)
          'invoiceDate': invoiceDate,
        if (startDate != null && startDate.isNotEmpty) 'startDate': startDate,
        if (endDate != null && endDate.isNotEmpty) 'endDate': endDate,
        /* Sync omits this — always full licence. Reports pass true for staff. */
        if (staffScope) 'staffScope': '1',
      },
    );
    return mapJsonList(
      data[ApiResponseKeys.invoiceResponse],
      CloudInvoiceDto.fromJson,
    );
  }

  /* Web reports: range sales summary + invoice headers from cloud. */
  Future<({SalesReportSummary summary, List<CloudInvoiceDto> invoices})>
  fetchPosSalesReport({
    required String userId,
    required String startDate,
    required String endDate,
    bool staffScope = false,
  }) async {
    final data = await invoiceSyncApiGet(
      ApiEndpoints.getPosSalesReport,
      query: {
        'userId': userId,
        'startDate': startDate,
        'endDate': endDate,
        if (staffScope) 'staffScope': '1',
      },
    );
    final status = '${data[ApiResponseKeys.status] ?? ''}'.toLowerCase();
    if (status != '1' && status != 'true') {
      throw StateError(
        '${data[ApiResponseKeys.message] ?? 'Failed to load sales report'}',
      );
    }
    return (
      summary: SalesReportSummary.fromJson(data),
      invoices: mapJsonList(
        data[ApiResponseKeys.invoiceResponse],
        CloudInvoiceDto.fromJson,
      ),
    );
  }

  Future<List<CloudInvoiceItemDto>> fetchInvoiceItems(String userId) async {
    final data = await invoiceSyncApiGet(
      ApiEndpoints.getInvoiceProductList,
      query: {'userId': userId},
    );
    return mapJsonList(
      data[ApiResponseKeys.invoiceProductResponse],
      CloudInvoiceItemDto.fromJson,
    );
  }

  Future<List<CloudInvoiceComboItemDto>> fetchInvoiceComboItems(
    String userId,
  ) async {
    final data = await invoiceSyncApiGet(
      ApiEndpoints.getInvoiceComboItemList,
      query: {'userId': userId},
    );
    return mapJsonList(
      data[ApiResponseKeys.invoiceComboItemResponse] ??
          data[ApiResponseKeys.comboItemResponse],
      CloudInvoiceComboItemDto.fromJson,
    );
  }

  Future<Map<String, dynamic>> invoiceSyncApiGet(
    String path, {
    Map<String, dynamic>? query,
  }) async {
    final response = await client.dio.get<dynamic>(
      path,
      queryParameters: query,
      options: Options(responseType: ResponseType.json),
    );
    return asJsonMap(response.data);
  }

  Future<Map<String, dynamic>> invoiceSyncApiPost(
    String path, {
    required Map<String, dynamic> fields,
  }) async {
    final response = await client.dio.post<dynamic>(
      path,
      data: FormData.fromMap(fields),
      options: Options(responseType: ResponseType.json),
    );
    return asJsonMap(response.data);
  }
}
