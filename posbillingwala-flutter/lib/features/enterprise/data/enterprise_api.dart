import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';
import 'package:pos_billingwala_v2/core/network/api_client.dart';
import 'package:pos_billingwala_v2/core/network/api_response.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/purchase/domain/purchase_document.dart';
import 'package:pos_billingwala_v2/features/purchase/domain/vendor.dart';

final enterpriseApiProvider = Provider<EnterpriseApi>((ref) {
  return EnterpriseApi(ref.watch(apiClientProvider));
});

/* Cloud sync for enterprise modules (purchase / CRM / hotel / variants). */
class EnterpriseApi {
  EnterpriseApi(this.client);

  final ApiClient client;

  Future<Map<String, dynamic>> post(
    String path,
    Map<String, dynamic> fields,
  ) async {
    final response = await client.dio.post<dynamic>(
      path,
      data: fields,
      options: Options(contentType: Headers.formUrlEncodedContentType),
    );
    return asJsonMap(response.data);
  }

  Future<bool> saveBusinessType({
    required String userId,
    required String businessType,
  }) async {
    final data = await post(ApiEndpoints.saveBusinessType, {
      'userId': userId,
      'businessType': businessType,
    });
    return isApiSuccess(data);
  }

  Future<bool> saveVendor({
    required String userId,
    required Vendor vendor,
  }) async {
    final data = await post(ApiEndpoints.saveVendor, {
      'userId': userId,
      'clientId': vendor.id,
      'name': vendor.name,
      'gstin': vendor.gstin,
      'contactName': vendor.contactName,
      'mobile': vendor.mobile,
      'email': vendor.email,
      'address': vendor.address,
      'paymentTerms': vendor.paymentTerms,
      'creditLimit': vendor.creditLimit.toStringAsFixed(2),
      'openingBalance': vendor.openingBalance.toStringAsFixed(2),
      'bankDetails': vendor.bankDetails,
      'status': vendor.status,
    });
    return isApiSuccess(data);
  }

  Future<List<Vendor>> fetchVendors(String userId) async {
    final data = await post(ApiEndpoints.getVendorList, {'userId': userId});
    return mapJsonList(data[ApiResponseKeys.vendorResponse], Vendor.fromJson);
  }

  Future<bool> savePurchaseDocument({
    required String userId,
    required PurchaseDocument doc,
  }) async {
    final data = await post(ApiEndpoints.savePurchaseDocument, {
      'userId': userId,
      'clientId': doc.id,
      'docNo': doc.docNo,
      'docType': doc.type.id,
      'docStatus': doc.status.id,
      'vendorId': doc.vendorId,
      'vendorName': doc.vendorName,
      'notes': doc.notes,
      'referenceNo': doc.referenceNo,
      'parentDocId': doc.parentDocId,
      'createdBy': doc.createdBy,
      'linesJson': jsonEncode(doc.lines.map((e) => e.toJson()).toList()),
      'subTotal': doc.subTotal.toStringAsFixed(2),
      'receivedAt': doc.receivedAt?.toIso8601String() ?? '',
    });
    return isApiSuccess(data);
  }

  Future<List<PurchaseDocument>> fetchPurchaseDocuments(String userId) async {
    final data = await post(ApiEndpoints.getPurchaseDocumentList, {
      'userId': userId,
    });
    return mapJsonList(
      data[ApiResponseKeys.purchaseResponse],
      PurchaseDocument.fromJson,
    );
  }

  Future<bool> saveCustomer({
    required String userId,
    required Map<String, dynamic> customer,
  }) async {
    final data = await post(ApiEndpoints.saveCustomer, {
      'userId': userId,
      ...customer,
    });
    return isApiSuccess(data);
  }

  Future<List<Map<String, dynamic>>> fetchCustomers(String userId) async {
    final data = await post(ApiEndpoints.getCustomerList, {'userId': userId});
    final raw = data[ApiResponseKeys.customerResponse];
    if (raw is! List) return const [];
    return raw
        .whereType<Map>()
        .map((e) => Map<String, dynamic>.from(e))
        .toList();
  }

  Future<bool> saveHotelRoom({
    required String userId,
    required Map<String, dynamic> room,
  }) async {
    final data = await post(ApiEndpoints.saveHotelRoom, {
      'userId': userId,
      ...room,
    });
    return isApiSuccess(data);
  }

  Future<List<Map<String, dynamic>>> fetchHotelRooms(String userId) async {
    final data = await post(ApiEndpoints.getHotelRoomList, {'userId': userId});
    final raw = data[ApiResponseKeys.hotelRoomResponse];
    if (raw is! List) return const [];
    return raw
        .whereType<Map>()
        .map((e) => Map<String, dynamic>.from(e))
        .toList();
  }

  Future<bool> saveHotelBooking({
    required String userId,
    required Map<String, dynamic> booking,
  }) async {
    final data = await post(ApiEndpoints.saveHotelBooking, {
      'userId': userId,
      ...booking,
    });
    return isApiSuccess(data);
  }

  Future<List<Map<String, dynamic>>> fetchHotelBookings(String userId) async {
    final data = await post(ApiEndpoints.getHotelBookingList, {
      'userId': userId,
    });
    final raw = data[ApiResponseKeys.hotelBookingResponse];
    if (raw is! List) return const [];
    return raw
        .whereType<Map>()
        .map((e) => Map<String, dynamic>.from(e))
        .toList();
  }

  Future<bool> saveProductVariant({
    required String userId,
    required Map<String, dynamic> variant,
  }) async {
    final data = await post(ApiEndpoints.saveProductVariant, {
      'userId': userId,
      ...variant,
    });
    return isApiSuccess(data);
  }

  Future<List<Map<String, dynamic>>> fetchProductVariants(String userId) async {
    final data = await post(ApiEndpoints.getProductVariantList, {
      'userId': userId,
    });
    final raw = data[ApiResponseKeys.variantResponse];
    if (raw is! List) return const [];
    return raw
        .whereType<Map>()
        .map((e) => Map<String, dynamic>.from(e))
        .toList();
  }

  Future<bool> saveOpsRecord({
    required String userId,
    required String entity,
    required Map<String, dynamic> record,
  }) async {
    final payload = Map<String, dynamic>.from(record)
      ..remove('id')
      ..remove('title')
      ..remove('status')
      ..remove('createdAt')
      ..remove('updatedAt')
      ..remove('organizationId')
      ..remove('branchId');
    final data = await post(ApiEndpoints.saveOpsRecord, {
      'userId': userId,
      'entity': entity,
      'clientId': record['id']?.toString() ?? '',
      'title': (record['title'] ?? record['name'] ?? '').toString(),
      'status': (record['status'] ?? 'ACTIVE').toString(),
      'payloadJson': jsonEncode(payload),
    });
    return isApiSuccess(data);
  }

  Future<List<Map<String, dynamic>>> fetchOpsList(
    String userId,
    String entity,
  ) async {
    final data = await post(ApiEndpoints.getOpsList, {
      'userId': userId,
      'entity': entity,
    });
    final raw = data[ApiResponseKeys.opsResponse];
    if (raw is! List) return const [];
    return raw
        .whereType<Map>()
        .map((e) => Map<String, dynamic>.from(e))
        .toList();
  }

  Future<List<Map<String, dynamic>>> fetchAuditLog(String userId) async {
    final data = await post(ApiEndpoints.getAuditLogList, {
      'userId': userId,
      'limit': '200',
    });
    final raw = data[ApiResponseKeys.auditResponse];
    if (raw is! List) return const [];
    return raw
        .whereType<Map>()
        .map((e) => Map<String, dynamic>.from(e))
        .toList();
  }
}

/* Soft cloud push — never blocks offline saves. */
Future<void> enterprisePushSafe(
  WidgetRef ref,
  Future<bool> Function(EnterpriseApi api, String userId) action,
) async {
  final session = ref.read(authControllerProvider).session;
  if (session == null) return;
  if (!await isDeviceOnline()) return;
  try {
    await action(ref.read(enterpriseApiProvider), session.licenceUserId);
  } catch (_) {
    /* Offline-first: local save already succeeded. */
  }
}
