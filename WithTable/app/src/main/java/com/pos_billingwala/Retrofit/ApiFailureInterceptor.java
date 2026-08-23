package com.pos_billingwala.Retrofit;

import com.pos_billingwala.Extra.Observability;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

/**
 * Logs every failed API call to Crashlytics with URL, request, response, and screen context.
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

        try {
            Response response = chain.proceed(rebuilt);
            if (!response.isSuccessful()) {
                String responseSnapshot = snapshotResponseBody(response);
                Observability.logApiFailure(
                        request.method(),
                        sanitizeUrl(request.url()),
                        apiNameFromUrl(request.url()),
                        response.code(),
                        "HTTP " + response.code() + " " + response.message(),
                        requestSnapshot,
                        responseSnapshot
                );
            }
            return response;
        } catch (IOException e) {
            Observability.logApiFailure(
                    request.method(),
                    sanitizeUrl(request.url()),
                    apiNameFromUrl(request.url()),
                    0,
                    e.getClass().getSimpleName() + ": " + safeMessage(e.getMessage()),
                    requestSnapshot,
                    null
            );
            throw e;
        }
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

    private static String safeMessage(String message) {
        return message != null ? message.trim() : "no message";
    }
}
