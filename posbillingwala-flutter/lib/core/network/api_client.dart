import 'package:dio/dio.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';
import 'package:pos_billingwala_v2/core/constants/app_config.dart';
import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
import 'package:pos_billingwala_v2/core/logging/error_report_service.dart';
import 'package:pos_billingwala_v2/core/logging/file_log_store.dart';
import 'package:pos_billingwala_v2/core/logging/screen_context.dart';
import 'package:pos_billingwala_v2/core/network/api_response.dart';
import 'package:pos_billingwala_v2/features/auth/data/auth_token_refresh.dart';

/* HTTP client for Billingwala API (form-urlencoded endpoints). */
/* */
/* On 401, silently refreshes the Bearer token (WithTable OkHttp authenticator) */
/* and retries the request once. */
class ApiClient {
  ApiClient({Dio? dio})
    : apiClientDio =
          dio ??
          Dio(
            BaseOptions(
              baseUrl: ApiConstants.baseUrl,
              connectTimeout: const Duration(seconds: 30),
              receiveTimeout: const Duration(seconds: 30),
              headers: const {'Accept': 'application/json'},
              /* Many legacy endpoints return text/html content-type with JSON body. */
              responseType: ResponseType.json,
              /* Treat HTTP 401 as error so the refresh interceptor can retry */
              /* (WithTable OkHttp authenticator). Other 4xx keep legacy body parsing. */
              validateStatus: (status) =>
                  status != null &&
                  status >= 200 &&
                  status < 500 &&
                  status != 401,
            ),
          ) {
    ErrorReportService.bindClient(apiClientDio);
    installLoggingInterceptor();
    installAuthRefreshInterceptor();
  }

  final Dio apiClientDio;

  Dio get dio => apiClientDio;

  void setAuthToken(String? token) {
    if (token == null || token.isEmpty) {
      apiClientDio.options.headers.remove('Authorization');
    } else {
      apiClientDio.options.headers['Authorization'] = 'Bearer $token';
    }
  }

  void setStaffId(String? staffId) {
    if (staffId == null || staffId.isEmpty) {
      apiClientDio.options.headers.remove('X-Pos-Staff-Id');
    } else {
      apiClientDio.options.headers['X-Pos-Staff-Id'] = staffId;
    }
  }

  void installLoggingInterceptor() {
    apiClientDio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          options.extra['screenName'] ??= ScreenContext.screenName;
          options.extra['requestStartedAt'] = DateTime.now().toIso8601String();
          if (!AppConfig.enableLogging) {
            handler.next(options);
            return;
          }
          if (AppConfig.enableVerboseIoLogging) {
            AppLogger.info(
              'API → ${options.method} ${options.uri}\n'
              'screen=${options.extra['screenName']}\n'
              'headers=${_safeHeaders(options.headers)}\n'
              'data=${options.data}',
            );
          } else {
            AppLogger.info(
              'API → ${options.method} ${options.uri} '
              'screen=${options.extra['screenName']}',
            );
          }
          handler.next(options);
        },
        onResponse: (response, handler) {
          final opts = response.requestOptions;
          if (opts.extra['skipErrorReport'] == true ||
              opts.path.contains(ApiEndpoints.reportErrorLog)) {
            handler.next(response);
            return;
          }
          final apiLabel = _apiLabel(opts);
          final payload = _requestPayload(opts);
          final bodyMap = asJsonMap(response.data);
          final businessFail =
              bodyMap.containsKey('status') && !isApiSuccess(bodyMap);
          final message = bodyMap['message']?.toString();

          /* Always persist success + failure API bodies to local file logs. */
          FileLogStore.logApi(
            method: opts.method,
            api: apiLabel,
            request: payload,
            response: response.data,
            statusCode: response.statusCode,
            error: businessFail ? (message ?? 'status!=1') : null,
            screenName: opts.extra['screenName']?.toString(),
          );

          if (AppConfig.enableLogging) {
            if (AppConfig.enableVerboseIoLogging) {
              AppLogger.info(
                'API ← ${response.statusCode} ${opts.uri}\n'
                'screen=${opts.extra['screenName']}\n'
                'body=${response.data}',
              );
            } else if (businessFail) {
              AppLogger.warning(
                'API ← ${response.statusCode} ${opts.uri} FAIL ${message ?? ''}',
              );
            } else {
              AppLogger.info('API ← ${response.statusCode} ${opts.uri} OK');
            }
          }

          if (businessFail) {
            ErrorReportService.reportApiFailure(
              method: opts.method,
              apiUrl: opts.uri.toString(),
              httpStatus: response.statusCode,
              requestBody: payload,
              responseBody: response.data,
              errorMessage: message ?? 'API returned status 0',
              screenName: opts.extra['screenName']?.toString(),
            );
          }
          handler.next(response);
        },
        onError: (error, handler) {
          final opts = error.requestOptions;
          if (opts.extra['skipErrorReport'] == true ||
              opts.path.contains(ApiEndpoints.reportErrorLog)) {
            handler.next(error);
            return;
          }
          final apiLabel = _apiLabel(opts);
          final payload = _requestPayload(opts);
          if (AppConfig.enableLogging) {
            AppLogger.error(
              'API ✕ ${error.response?.statusCode ?? '-'} '
              '${opts.uri}\n'
              'screen=${opts.extra['screenName']}\n'
              'body=${error.response?.data}\n'
              'message=${error.message}',
              error,
            );
          }
          FileLogStore.logApi(
            method: opts.method,
            api: apiLabel,
            request: payload,
            response: error.response?.data,
            statusCode: error.response?.statusCode,
            error: error.message,
            screenName: opts.extra['screenName']?.toString(),
          );
          ErrorReportService.reportApiFailure(
            method: opts.method,
            apiUrl: opts.uri.toString(),
            httpStatus: error.response?.statusCode,
            requestBody: payload,
            responseBody: error.response?.data,
            errorMessage: error.message ?? 'network error',
            screenName: opts.extra['screenName']?.toString(),
          );
          handler.next(error);
        },
      ),
    );
  }

  static String _apiLabel(RequestOptions opts) {
    final segments = opts.uri.path.split('/').where((s) => s.isNotEmpty);
    final name = segments.isEmpty ? '' : segments.last;
    if (name.isNotEmpty) {
      return '$name (${opts.uri})';
    }
    return opts.uri.toString();
  }

  static Object? _requestPayload(RequestOptions opts) {
    Object? body = opts.data;
    if (body is FormData) {
      final fields = <String, dynamic>{};
      for (final e in body.fields) {
        /* Redact huge base64 logos from error/log payloads. */
        if (e.key == 'companyLogo' && e.value.length > 200) {
          fields[e.key] = '(logo:${e.value.length} chars)';
        } else {
          fields[e.key] = e.value;
        }
      }
      for (final f in body.files) {
        fields[f.key] = '(file:${f.value.filename ?? 'unknown'})';
      }
      body = fields;
    }
    return {
      'url': opts.uri.toString(),
      'method': opts.method,
      'headers': _safeHeaders(opts.headers),
      'query': opts.queryParameters,
      'body': body,
    };
  }

  static Map<String, dynamic> _safeHeaders(Map<String, dynamic> headers) {
    final copy = Map<String, dynamic>.from(headers);
    final auth = copy['Authorization'];
    if (auth is String && auth.isNotEmpty) {
      copy['Authorization'] = 'Bearer ***';
    }
    return copy;
  }

  void installAuthRefreshInterceptor() {
    apiClientDio.interceptors.add(
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
            final auth = apiClientDio.options.headers['Authorization'];
            if (auth != null) {
              opts.headers['Authorization'] = auth;
            }
            final response = await apiClientDio.fetch<dynamic>(opts);
            return handler.resolve(response);
          } catch (e) {
            return handler.next(error);
          }
        },
      ),
    );
  }
}
