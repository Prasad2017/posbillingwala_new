import 'package:dio/dio.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';
import 'package:pos_billingwala_v2/core/network/api_client.dart';
import 'package:pos_billingwala_v2/core/utils/json_parsers.dart';

class HomeSalesOverview {
  const HomeSalesOverview({
    required this.primarySales,
    required this.todaySales,
    required this.monthSales,
    required this.allTimeSales,
    this.primarySalesTrend = '0',
    this.todaySalesTrend = '0',
    this.totalSubcategory = 0,
    this.totalProduct = 0,
    this.totalCombo = 0,
    this.period = 'today',
    this.primarySalesLabel = 'total_sales',
  });

  final double primarySales;
  final double todaySales;
  final double monthSales;
  final double allTimeSales;
  final String primarySalesTrend;
  final String todaySalesTrend;
  final int totalSubcategory;
  final int totalProduct;
  final int totalCombo;
  final String period;
  final String primarySalesLabel;

  factory HomeSalesOverview.fromJson(Map<String, dynamic> json) {
    return HomeSalesOverview(
      primarySales: parseMoney(json['primarySales']),
      todaySales: parseMoney(json['todaySales']),
      monthSales: parseMoney(json['monthSales']),
      allTimeSales: parseMoney(json['allTimeSales']),
      primarySalesTrend: parseString(json['primarySalesTrend']) ?? '0',
      todaySalesTrend: parseString(json['todaySalesTrend']) ?? '0',
      totalSubcategory: parseInt(json['totalSubcategory']) ?? 0,
      totalProduct: parseInt(json['totalProduct']) ?? 0,
      totalCombo: parseInt(json['totalCombo']) ?? 0,
      period: parseString(json['period']) ?? 'today',
      primarySalesLabel: parseString(json['primarySalesLabel']) ?? 'total_sales',
    );
  }
}

class HomeSalesApi {
  HomeSalesApi(this._client);

  final ApiClient _client;

  /// Android [ApiInterface.getHomeSalesOverview] — period: `today` | `month`.
  Future<HomeSalesOverview?> fetchOverview({
    required String userId,
    String period = 'today',
  }) async {
    if (userId.trim().isEmpty) return null;
    try {
      final response = await _client.dio.get<dynamic>(
        ApiEndpoints.getHomeSalesOverview,
        queryParameters: {
          'userId': userId,
          'period': period == 'month' ? 'month' : 'today',
        },
        options: Options(
          contentType: Headers.formUrlEncodedContentType,
          validateStatus: (s) => s != null && s < 500,
        ),
      );
      final data = response.data;
      if (data is! Map) return null;
      final map = Map<String, dynamic>.from(data);
      final status = parseString(map['status'])?.toLowerCase();
      if (status != 'true' && status != '1') return null;
      return HomeSalesOverview.fromJson(map);
    } catch (_) {
      return null;
    }
  }
}
