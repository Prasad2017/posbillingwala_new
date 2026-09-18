import 'package:dio/dio.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';
import 'package:pos_billingwala_v2/core/network/api_client.dart';
import 'package:pos_billingwala_v2/core/network/api_response.dart';
import 'package:pos_billingwala_v2/shared/models/catalog_dtos.dart';
import 'package:pos_billingwala_v2/shared/models/combo_dtos.dart';

class MastersApi {
  MastersApi(this.client);

  final ApiClient client;

  Future<List<FoodTypeDto>> fetchFoodTypes() async {
    final data = await mastersApiGet(ApiEndpoints.getFoodTypeList);
    return mapJsonList(
      data[ApiResponseKeys.foodTypeResponse],
      FoodTypeDto.fromJson,
    );
  }

  Future<List<CategoryDto>> fetchCategories(String userId) async {
    final data = await mastersApiGet(
      ApiEndpoints.getCategoryList,
      query: {'userId': userId},
    );
    return mapJsonList(
      data[ApiResponseKeys.categoryResponse],
      CategoryDto.fromJson,
    );
  }

  Future<List<ProductDto>> fetchProducts(String userId) async {
    final data = await mastersApiGet(
      ApiEndpoints.getProductList,
      query: {'userId': userId},
    );
    return mapJsonList(
      data[ApiResponseKeys.productResponse],
      ProductDto.fromJson,
    );
  }

  Future<List<PortionDto>> fetchPortions(String userId) async {
    final data = await mastersApiGet(
      ApiEndpoints.getPortionList,
      query: {'userId': userId},
    );
    return mapJsonList(
      data[ApiResponseKeys.portionResponse],
      PortionDto.fromJson,
    );
  }

  Future<List<PosTableDto>> fetchPosTables(String userId) async {
    final data = await mastersApiGet(
      ApiEndpoints.getPosTableList,
      query: {'userId': userId},
    );
    return mapJsonList(
      data[ApiResponseKeys.posTableResponse],
      PosTableDto.fromJson,
    );
  }

  Future<List<SubcategoryDto>> fetchSubcategories(String userId) async {
    final data = await mastersApiGet(
      ApiEndpoints.getSubcategoryList,
      query: {'userId': userId},
    );
    return mapJsonList(
      data[ApiResponseKeys.subcategoryResponse],
      SubcategoryDto.fromJson,
    );
  }

  Future<List<DiningAreaDto>> fetchDiningAreas(String userId) async {
    final data = await mastersApiGet(
      ApiEndpoints.getDiningAreaList,
      query: {'userId': userId},
    );
    return mapJsonList(
      data[ApiResponseKeys.diningAreaResponse],
      DiningAreaDto.fromJson,
    );
  }

  Future<List<TableTypeDto>> fetchTableTypes(String userId) async {
    final data = await mastersApiGet(
      ApiEndpoints.getTableTypeList,
      query: {'userId': userId},
    );
    return mapJsonList(
      data[ApiResponseKeys.tableTypeResponse],
      TableTypeDto.fromJson,
    );
  }

  Future<List<PortionMasterDto>> fetchPortionMasters(String userId) async {
    final data = await mastersApiGet(
      ApiEndpoints.getPortionMasterList,
      query: {'userId': userId},
    );
    return mapJsonList(
      data[ApiResponseKeys.portionMasterResponse],
      PortionMasterDto.fromJson,
    );
  }

  Future<bool> insertSubcategory({
    required String userId,
    required String categoryId,
    required String categoryNetworkStatus,
    required String subcategoryName,
    required String subcategoryNetworkStatus,
    String subcategoryDeletedStatus = '0',
    String subcategorySortOrder = '0',
  }) async {
    final data = await mastersApiPost(
      ApiEndpoints.insertSubcategory,
      fields: {
        'userId': userId,
        'categoryId': categoryId,
        'categoryNetworkStatus': categoryNetworkStatus,
        'subcategoryName': subcategoryName,
        'subcategoryDeletedStatus': subcategoryDeletedStatus,
        'subcategoryNetworkStatus': subcategoryNetworkStatus,
        'subcategorySortOrder': subcategorySortOrder,
      },
    );
    return isApiSuccess(data);
  }

  Future<bool> insertDiningArea({
    required String userId,
    required String areaName,
    String areaSortOrder = '0',
    String areaActive = '1',
    String areaNetworkStatus = '',
  }) async {
    final data = await mastersApiPost(
      ApiEndpoints.insertDiningArea,
      fields: {
        'userId': userId,
        'areaName': areaName,
        'areaSortOrder': areaSortOrder,
        'areaActive': areaActive,
        'areaNetworkStatus': areaNetworkStatus,
      },
    );
    return isApiSuccess(data);
  }

  Future<bool> insertTableType({
    required String userId,
    required String tableTypeName,
    String tableTypeSortOrder = '0',
    String tableTypeActive = '1',
    String tableTypeNetworkStatus = '',
  }) async {
    final data = await mastersApiPost(
      ApiEndpoints.insertTableType,
      fields: {
        'userId': userId,
        'tableTypeName': tableTypeName,
        'tableTypeSortOrder': tableTypeSortOrder,
        'tableTypeActive': tableTypeActive,
        'tableTypeNetworkStatus': tableTypeNetworkStatus,
      },
    );
    return isApiSuccess(data);
  }

  Future<bool> insertPosTable({
    required String userId,
    required String tableNumber,
    required String tableName,
    String capacity = '4',
    String areaId = '0',
    String tableActive = '1',
    String sortOrder = '0',
    String posTableNetworkStatus = '',
  }) async {
    final data = await mastersApiPost(
      ApiEndpoints.insertPosTable,
      fields: {
        'userId': userId,
        'tableNumber': tableNumber,
        'tableName': tableName,
        'capacity': capacity,
        'areaId': areaId,
        'tableActive': tableActive,
        'sortOrder': sortOrder,
        'posTableNetworkStatus': posTableNetworkStatus,
      },
    );
    return isApiSuccess(data);
  }

  Future<List<ComboDto>> fetchCombos(String userId) async {
    final data = await mastersApiGet(
      ApiEndpoints.getComboList,
      query: {'userId': userId},
    );
    return mapJsonList(data[ApiResponseKeys.comboResponse], ComboDto.fromJson);
  }

  Future<List<ComboItemDto>> fetchComboItems(String userId) async {
    final data = await mastersApiGet(
      ApiEndpoints.getComboItemList,
      query: {'userId': userId},
    );
    final raw =
        data[ApiResponseKeys.comboItemResponse] ?? data['comboItemResponse'];
    return mapJsonList(raw, ComboItemDto.fromJson);
  }

  Future<bool> insertCategory({
    required String userId,
    required String categoryName,
    required String categoryNetworkStatus,
    String categoryDeletedStatus = '0',
    String foodTypeCode = '',
    String categorySortOrder = '0',
  }) async {
    final data = await mastersApiPost(
      ApiEndpoints.insertCategory,
      fields: {
        'userId': userId,
        'categoryName': categoryName,
        'categoryDeletedStatus': categoryDeletedStatus,
        'categoryNetworkStatus': categoryNetworkStatus,
        'foodTypeCode': foodTypeCode,
        'categorySortOrder': categorySortOrder,
      },
    );
    return isApiSuccess(data);
  }

  Future<bool> insertProduct({
    required String userId,
    required String categoryId,
    required String categoryName,
    required String productCode,
    required String productName,
    required String productPrice,
    String productMrp = '0',
    required String productUnit,
    required String productCgst,
    required String productSgst,
    required String productNetworkStatus,
    String productDeletedStatus = '0',
    String subcategoryId = '0',
    String subcategoryNetworkStatus = '',
    String openPrice = '0',
    String priceIncludesGst = '0',
    String productImage = '',
  }) async {
    final data = await mastersApiPost(
      ApiEndpoints.insertProduct,
      fields: {
        'userId': userId,
        'categoryId': categoryId,
        'categoryName': categoryName,
        'productCode': productCode,
        'productName': productName,
        'productPrice': productPrice,
        'productMrp': productMrp,
        'productUnit': productUnit,
        'productCGST': productCgst,
        'productSGST': productSgst,
        'productNetworkStatus': productNetworkStatus,
        'productDeletedStatus': productDeletedStatus,
        'subcategoryId': subcategoryId,
        'subcategoryNetworkStatus': subcategoryNetworkStatus,
        'openPrice': openPrice,
        'priceIncludesGst': priceIncludesGst,
        'productImage': productImage,
      },
    );
    return isApiSuccess(data);
  }

  Future<bool> insertPortion({
    required String userId,
    required String productId,
    required String productNetworkStatus,
    required String portionName,
    required String portionPrice,
    required String portionSortOrder,
    required String portionNetworkStatus,
    String portionDeletedStatus = '0',
    String portionMasterId = '0',
    String portionMasterNetworkStatus = '',
  }) async {
    final data = await mastersApiPost(
      ApiEndpoints.insertPortion,
      fields: {
        'userId': userId,
        'productId': productId,
        'productNetworkStatus': productNetworkStatus,
        'portionName': portionName,
        'portionPrice': portionPrice,
        'portionSortOrder': portionSortOrder,
        'portionDeletedStatus': portionDeletedStatus,
        'portionNetworkStatus': portionNetworkStatus,
        'portionMasterId': portionMasterId,
        'portionMasterNetworkStatus': portionMasterNetworkStatus,
      },
    );
    return isApiSuccess(data);
  }

  Future<bool> insertPortionMaster({
    required String userId,
    required String portionName,
    String portionMasterDeletedStatus = '0',
    String portionMasterNetworkStatus = '',
  }) async {
    final data = await mastersApiPost(
      ApiEndpoints.insertPortionMaster,
      fields: {
        'userId': userId,
        'portionName': portionName,
        'portionMasterDeletedStatus': portionMasterDeletedStatus,
        'portionMasterNetworkStatus': portionMasterNetworkStatus,
      },
    );
    return isApiSuccess(data);
  }

  Future<bool> insertCombo({
    required String userId,
    required String comboName,
    required String comboCode,
    required String comboPrice,
    required String comboCgst,
    required String comboSgst,
    required String comboWithGstPrice,
    required String comboNetworkStatus,
    String comboActiveStatus = '1',
    String comboDeletedStatus = '0',
    String comboSortOrder = '0',
  }) async {
    final data = await mastersApiPost(
      ApiEndpoints.insertCombo,
      fields: {
        'userId': userId,
        'comboName': comboName,
        'comboCode': comboCode,
        'comboPrice': comboPrice,
        'comboCGST': comboCgst,
        'comboSGST': comboSgst,
        'comboWithGSTPrice': comboWithGstPrice,
        'comboActiveStatus': comboActiveStatus,
        'comboDeletedStatus': comboDeletedStatus,
        'comboNetworkStatus': comboNetworkStatus,
        'comboSortOrder': comboSortOrder,
      },
    );
    return isApiSuccess(data);
  }

  Future<bool> insertComboItem({
    required String userId,
    required String comboId,
    required String comboNetworkStatus,
    required String productId,
    required String productNetworkStatus,
    required String portionId,
    required String portionNetworkStatus,
    required String comboItemQuantity,
    required String comboItemSortOrder,
    required String comboItemNetworkStatus,
    String comboItemDeletedStatus = '0',
  }) async {
    final data = await mastersApiPost(
      ApiEndpoints.insertComboItem,
      fields: {
        'userId': userId,
        'comboId': comboId,
        'comboNetworkStatus': comboNetworkStatus,
        'productId': productId,
        'productNetworkStatus': productNetworkStatus,
        'portionId': portionId,
        'portionNetworkStatus': portionNetworkStatus,
        'comboItemQuantity': comboItemQuantity,
        'comboItemSortOrder': comboItemSortOrder,
        'comboItemDeletedStatus': comboItemDeletedStatus,
        'comboItemNetworkStatus': comboItemNetworkStatus,
      },
    );
    return isApiSuccess(data);
  }

  Future<Map<String, dynamic>> mastersApiGet(
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

  Future<Map<String, dynamic>> mastersApiPost(
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
