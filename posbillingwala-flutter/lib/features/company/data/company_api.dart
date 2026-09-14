import 'package:dio/dio.dart';
import 'package:pos_billingwala_v2/core/network/api_client.dart';
import 'package:pos_billingwala_v2/core/network/api_response.dart';
import 'package:pos_billingwala_v2/features/company/data/company_dtos.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';

class CompanyApi {
  CompanyApi(this.client);

  final ApiClient client;

  Future<List<CompanyDto>> getCompanyList(String userId) async {
    final data = await companyApiGet(
      ApiEndpoints.getCompanyList,
      query: {'userId': userId},
    );
    return mapJsonList(
      data[ApiResponseKeys.companyResponse],
      CompanyDto.fromJson,
    );
  }

  Future<bool> insertCompanyDetail({
    required String userId,
    required CompanyDto company,
  }) async {
    final data = await companyApiPost(
      ApiEndpoints.insertCompanyDetail,
      fields: {
        'userId': userId,
        'companyLogo': company.companyLogo ?? '',
        'companyName': company.companyName,
        'cashierName': company.cashierName ?? '',
        'companyMobile': company.companyMobile ?? '',
        'companyAddress': company.companyAddress ?? '',
        'shopName1': company.shopName1 ?? '',
        'shopName2': company.shopName2 ?? '',
        'addressLine1': company.addressLine1 ?? '',
        'addressLine2': company.addressLine2 ?? '',
        'addressLine3': company.addressLine3 ?? '',
        'phoneNo1': company.phoneNo1 ?? '',
        'phoneNo2': company.phoneNo2 ?? '',
        'currencyName': company.currencyName ?? '',
        'tableStatus': company.tableStatus ?? '',
        'noOfTable': company.noOfTable ?? '',
        'countryName': company.countryName ?? '',
        'stateName': company.stateName ?? '',
        'gstStatus': company.gstStatus ?? '',
        'gstNumber': company.gstNumber ?? '',
        'panNumber': company.panNumber ?? '',
        'paymentLogo': company.paymentLogo ?? '',
        'companyFssis': company.companyFssis ?? '',
        'shopCGST': company.shopCgst ?? '',
        'shopSGST': company.shopSgst ?? '',
        'openingMinutes': company.openingMinutes ?? '',
        'closingMinutes': company.closingMinutes ?? '',
      },
    );
    return isApiSuccess(data);
  }

  Future<List<CompanyPrinterSettingDto>> getCompanyPrinterSetting(
    String userId,
  ) async {
    final data = await companyApiGet(
      ApiEndpoints.getCompanyPrinterSetting,
      query: {'userId': userId},
    );
    return mapJsonList(
      data[ApiResponseKeys.printerResponse],
      CompanyPrinterSettingDto.fromJson,
    );
  }

  Future<bool> insertCompanyPrinterSetting({
    required String userId,
    required CompanyPrinterSettingDto setting,
  }) async {
    final data = await companyApiPost(
      ApiEndpoints.insertCompanyPrinterSetting,
      fields: {
        'userId': userId,
        'printerName': setting.printerName,
        'KOTPrinterName': setting.kotPrinterName,
        'invoicePrefix': setting.invoicePrefix,
        'invoiceTitle': setting.invoiceTitle,
        'invoiceTermsCondition': setting.invoiceTermsCondition,
        'logoUse': setting.logoUse,
        'paymentUse': setting.paymentUse,
        'customerUse': setting.customerUse,
        'productQuantityUpdate': setting.productQuantityUpdate,
        'duplicateBillUse': setting.duplicateBillUse,
        'bluetoothAddress': setting.bluetoothAddress,
        'bluetoothKOTAddress': setting.bluetoothKotAddress,
        'printerFeedLines': setting.printerFeedLines,
        'KotPrinterFeedLines': setting.kotPrinterFeedLines,
        'kotEnable': setting.kotEnable,
        'kotPrefix': setting.kotPrefix,
        'kotCopies': setting.kotCopies,
        'kotAutoPrint': setting.kotAutoPrint,
        'kotPreview': setting.kotPreview,
      },
    );
    return isApiSuccess(data);
  }

  Future<Map<String, dynamic>> companyApiGet(
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

  Future<Map<String, dynamic>> companyApiPost(
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
