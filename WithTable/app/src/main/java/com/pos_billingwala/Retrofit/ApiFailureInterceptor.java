package com.pos_billingwala.Retrofit;

import com.pos_billingwala.Extra.Observability;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.SSLException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

/**
 * Logs every failed API call to Crashlytics + Logcat with URL, request, response,
 * HTTP status / network reason, and screen context.
 */
public final class ApiFailureInterceptor implements Interceptor {

    /** Max bytes read from request/response bodies for failure logs. */
    private static final long MAX_BODY_BYTES = 65536L;

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String rawBody = readRequestBody(request);
        Request rebuilt = rebuildRequest(request, rawBody);
        String requestSnapshot = ApiLogSanitizer.sanitize(captureForLog(rawBody));
        String apiName = apiNameFromUrl(request.url());
        String safeUrl = sanitizeUrl(request.url());

        try {
            Response response = chain.proceed(rebuilt);
            if (!response.isSuccessful()) {
                String responseSnapshot = snapshotResponseBody(response);
                String reason = buildHttpFailureReason(response.code(), response.message(), responseSnapshot);
                Observability.logApiFailure(
                        request.method(),
                        safeUrl,
                        apiName,
                        response.code(),
                        reason,
                        requestSnapshot,
                        responseSnapshot
                );
            }
            return response;
        } catch (IOException e) {
            Observability.logApiFailure(
                    request.method(),
                    safeUrl,
                    apiName,
                    0,
                    buildNetworkFailureReason(e),
                    requestSnapshot,
                    null
            );
            throw e;
        }
    }

    private static String buildHttpFailureReason(int code, String message, String responseBody) {
        String statusHint;
        if (code >= 500) {
            statusHint = "Server error";
        } else if (code == 401 || code == 403) {
            statusHint = "Auth/permission denied";
        } else if (code == 404) {
            statusHint = "Endpoint not found";
        } else if (code == 408 || code == 504) {
            statusHint = "Gateway/request timeout";
        } else if (code >= 400) {
            statusHint = "Client/request error";
        } else {
            statusHint = "HTTP failure";
        }
        String serverMsg = extractServerMessage(responseBody);
        StringBuilder sb = new StringBuilder();
        sb.append(statusHint)
                .append(" — HTTP ")
                .append(code);
        if (message != null && !message.trim().isEmpty()) {
            sb.append(' ').append(message.trim());
        }
        if (serverMsg != null && !serverMsg.isEmpty()) {
            sb.append(" | server_msg=").append(serverMsg);
        }
        return sb.toString();
    }

    private static String buildNetworkFailureReason(IOException e) {
        if (e instanceof UnknownHostException) {
            return "No internet / DNS failed — cannot resolve host: " + Observability.describeThrowable(e);
        }
        if (e instanceof SocketTimeoutException) {
            return "Request timed out — " + Observability.describeThrowable(e);
        }
        if (e instanceof ConnectException) {
            return "Connection refused — server unreachable: " + Observability.describeThrowable(e);
        }
        if (e instanceof SSLException) {
            return "SSL/TLS handshake failed: " + Observability.describeThrowable(e);
        }
        return "Network I/O failed: " + Observability.describeThrowable(e);
    }

    /** Best-effort extract of message/error fields from JSON body for clearer logs. */
    private static String extractServerMessage(String body) {
        if (body == null || body.isEmpty()) {
            return "";
        }
        String lower = body.toLowerCase();
        String[] keys = {"\"message\"", "\"msg\"", "\"error\"", "\"error_message\"", "\"detail\""};
        for (String key : keys) {
            int idx = lower.indexOf(key);
            if (idx < 0) {
                continue;
            }
            int colon = body.indexOf(':', idx);
            if (colon < 0) {
                continue;
            }
            int startQuote = body.indexOf('"', colon + 1);
            if (startQuote < 0) {
                continue;
            }
            int endQuote = body.indexOf('"', startQuote + 1);
            if (endQuote <= startQuote) {
                continue;
            }
            String value = body.substring(startQuote + 1, endQuote).trim();
            if (!value.isEmpty()) {
                return value.length() > 200 ? value.substring(0, 200) + "…" : value;
            }
        }
        return "";
    }

    private static String readRequestBody(Request request) {
        RequestBody body = request.body();
        if (body == null) {
            return null;
        }
        try {
            Buffer buffer = new Buffer();
            body.writeTo(buffer);
            return buffer.readString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static Request rebuildRequest(Request request, String rawBody) {
        if (rawBody == null) {
            return request;
        }
        RequestBody originalBody = request.body();
        MediaType contentType = originalBody != null ? originalBody.contentType() : null;
        RequestBody copiedBody = RequestBody.create(
                contentType != null ? contentType : MediaType.parse("text/plain"),
                rawBody
        );
        return request.newBuilder().method(request.method(), copiedBody).build();
    }

    private static String captureForLog(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        if (raw.length() <= MAX_BODY_BYTES) {
            return raw;
        }
        return raw.substring(0, (int) MAX_BODY_BYTES) + "…[truncated at 64KB]";
    }

    private static String snapshotResponseBody(Response response) {
        ResponseBody body = response.body();
        if (body == null) {
            return "";
        }
        try {
            ResponseBody peeked = response.peekBody(MAX_BODY_BYTES);
            return ApiLogSanitizer.sanitize(captureForLog(peeked.string()));
        } catch (Exception e) {
            return "";
        }
    }

    private static String apiNameFromUrl(HttpUrl url) {
        if (url == null) {
            return "unknown";
        }
        String path = url.encodedPath();
        if (path == null || path.isEmpty()) {
            return "unknown";
        }
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    private static String sanitizeUrl(HttpUrl url) {
        if (url == null) {
            return "";
        }
        return ApiLogSanitizer.sanitize(url.toString());
    }
}
