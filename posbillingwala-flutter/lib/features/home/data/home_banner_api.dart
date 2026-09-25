import 'package:dio/dio.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';
import 'package:pos_billingwala_v2/core/network/api_client.dart';
import 'package:pos_billingwala_v2/core/utils/json_parsers.dart';

class HomeBanner {
  const HomeBanner({required this.id, required this.imageUrl});

  final String id;
  final String imageUrl;

  factory HomeBanner.fromJson(Map<String, dynamic> json) {
    return HomeBanner(
      id: parseString(json['bannerId']) ?? '',
      imageUrl: parseString(json['imageUrl']) ?? '',
    );
  }
}

class HomeBannerApi {
  HomeBannerApi(this.client);

  final ApiClient client;

  Future<List<HomeBanner>> fetch({required String userId}) async {
    if (userId.trim().isEmpty) return const [];
    try {
      final response = await client.dio.get<dynamic>(
        ApiEndpoints.getHomeBannerList,
        queryParameters: {'userId': userId},
        options: Options(
          validateStatus: (s) => s != null && s < 500,
        ),
      );
      final data = response.data;
      if (data is! Map) return const [];
      final map = Map<String, dynamic>.from(data);
      final status = parseString(map['status'])?.toLowerCase();
      if (status != 'true' && status != '1') return const [];
      final raw = map['bannerResponse'];
      if (raw is! List) return const [];
      return raw
          .whereType<Map>()
          .map((row) => HomeBanner.fromJson(Map<String, dynamic>.from(row)))
          .where((banner) => banner.imageUrl.trim().isNotEmpty)
          .toList();
    } catch (_) {
      return const [];
    }
  }
}
