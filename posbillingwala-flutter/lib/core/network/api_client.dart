import 'package:dio/dio.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';
import 'package:pos_billingwala_v2/features/auth/data/auth_token_refresh.dart';

/// HTTP client for POS Billingwala API (form-urlencoded endpoints).
///
/// On 401, silently refreshes the Bearer token (WithTable OkHttp authenticator)
/// and retries the request once.
class ApiClient {
  ApiClient({Dio? dio})
      : _dio = dio ??
            Dio(
              BaseOptions(
                baseUrl: ApiConstants.baseUrl,
                connectTimeout: const Duration(seconds: 30),
                receiveTimeout: const Duration(seconds: 30),
                headers: const {
                  'Accept': 'application/json',
                },
                // Many legacy endpoints return text/html content-type with JSON body.
                responseType: ResponseType.json,
                // Treat HTTP 401 as error so the refresh interceptor can retry
                // (WithTable OkHttp authenticator). Other 4xx keep legacy body parsing.
                validateStatus: (status) =>
                    status != null &&
                    status >= 200 &&
                    status < 500 &&
                    status != 401,
              ),
            ) {
    _installAuthRefreshInterceptor();
  }

  final Dio _dio;

  Dio get dio => _dio;

  void setAuthToken(String? token) {
    if (token == null || token.isEmpty) {
      _dio.options.headers.remove('Authorization');
    } else {
      _dio.options.headers['Authorization'] = 'Bearer $token';
    }
  }

  void _installAuthRefreshInterceptor() {
    _dio.interceptors.add(
      QueuedInterceptorsWrapper(
        onError: (error, handler) async {
          final status = error.response?.statusCode;
          final path = error.requestOptions.path;
          if (status != 401 ||
              path.contains(ApiEndpoints.refreshAuthToken) ||
              error.requestOptions.extra['authRetry'] == true) {
            return handler.next(error);
          }

          final refreshed = await AuthTokenRefresh.tryRefresh(
            onTokenSaved: setAuthToken,
          );
          if (!refreshed) {
            return handler.next(error);
          }

          try {
            final opts = error.requestOptions;
            opts.extra['authRetry'] = true;
            final auth = _dio.options.headers['Authorization'];
            if (auth != null) {
              opts.headers['Authorization'] = auth;
            }
            final response = await _dio.fetch<dynamic>(opts);
            return handler.resolve(response);
          } catch (e) {
            return handler.next(error);
          }
        },
      ),
    );
  }
}
