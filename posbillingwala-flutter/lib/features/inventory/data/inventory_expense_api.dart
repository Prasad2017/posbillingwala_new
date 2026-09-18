import 'package:dio/dio.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/network/api_client.dart';
import 'package:pos_billingwala_v2/core/network/api_response.dart';
import 'package:pos_billingwala_v2/shared/models/inventory_dtos.dart';

class InventoryExpenseApi {
  InventoryExpenseApi(this.client);

  final ApiClient client;

  Future<bool> uploadInventory({
    required String userId,
    required InventoryMovement row,
  }) async {
    final data = await inventoryExpenseApiPost(
      ApiEndpoints.insertInventory,
      fields: {
        'userId': userId,
        'productId': '${row.productId}',
        'productInventoryQuantity': row.productInventoryQuantity
            .toStringAsFixed(3),
        'afterSaleInventoryQuantity': row.afterSaleInventoryQuantity
            .toStringAsFixed(3),
        'saleInventoryQuantity': row.saleInventoryQuantity.toStringAsFixed(3),
        'movementType': row.movementType,
        'inventoryNote': row.inventoryNote,
        'unitCost': row.unitCost.toStringAsFixed(2),
        'inventoryDate': DateFormat('yyyy-MM-dd').format(row.inventoryDate),
        'inventoryNetworkStatus': row.inventoryNetworkStatus,
      },
    );
    return isApiSuccess(data);
  }

  Future<bool> uploadExpense({
    required String userId,
    required ShopExpense row,
  }) async {
    final data = await inventoryExpenseApiPost(
      ApiEndpoints.insertExpenses,
      fields: {
        'userId': userId,
        'expensesName': row.expensesName,
        'expensesAmount': row.expensesAmount.toStringAsFixed(2),
        'expensesDate': DateFormat('yyyy-MM-dd').format(row.expensesDate),
        'expensesNetworkStatus': row.expensesNetworkStatus,
      },
    );
    return isApiSuccess(data);
  }

  Future<List<InventoryDto>> fetchInventory(String userId) async {
    final data = await inventoryExpenseApiGet(
      ApiEndpoints.getInventoryList,
      query: {'userId': userId},
    );
    return mapJsonList(
      data[ApiResponseKeys.inventoryResponse],
      InventoryDto.fromJson,
    );
  }

  Future<List<ExpenseDto>> fetchExpenses(String userId) async {
    final data = await inventoryExpenseApiGet(
      ApiEndpoints.getExpensesList,
      query: {'userId': userId},
    );
    return mapJsonList(
      data[ApiResponseKeys.expensesResponse],
      ExpenseDto.fromJson,
    );
  }

  Future<Map<String, dynamic>> inventoryExpenseApiGet(
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

  Future<Map<String, dynamic>> inventoryExpenseApiPost(
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
