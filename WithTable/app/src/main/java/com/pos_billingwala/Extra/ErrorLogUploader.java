package com.pos_billingwala.Extra;

import android.content.Context;
import android.util.Log;

import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Retrofit.Api;

import java.io.File;
import java.util.List;

import retrofit2.Response;

/**
 * Uploads pending error logs. Failures are swallowed — never affects POS.
 * Skips reporting upload failures to Observability (loop guard).
 */
public final class ErrorLogUploader {

    private static final String TAG = "POS_ERR_UPLOAD";
    public static final String INGEST_PATH = "reportErrorLog.php";

    private ErrorLogUploader() {
    }

    public static boolean isIngestUrl(String url) {
        if (url == null) {
            return false;
        }
        return url.contains(INGEST_PATH);
    }

    static void flushPending(Context context) {
        if (context == null) {
            return;
        }
        if (!ErrorLogQueue.tryBeginFlush()) {
            return;
        }
        try {
            List<File> pending = ErrorLogQueue.listPending(context);
            for (File file : pending) {
                ErrorLogPayload payload = ErrorLogQueue.readFile(file);
                if (payload == null) {
                    ErrorLogQueue.deleteFile(file);
                    continue;
                }
                if (uploadOne(context, payload)) {
                    ErrorLogQueue.deleteFile(file);
                } else {
                    // Stop on first network failure; retry later.
                    break;
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "flushPending: " + t.getMessage());
        } finally {
            ErrorLogQueue.endFlush();
        }
    }

    private static boolean uploadOne(Context context, ErrorLogPayload p) {
        try {
            String userId = p.get("userId");
            if (userId.isEmpty()) {
                userId = Common.getSavedUserData(context, "userId");
            }
            Response<AllApiResponse> response = Api.getClient(context).reportErrorLog(
                    userId,
                    p.get("fingerprint"),
                    p.get("error_type"),
                    p.get("severity"),
                    p.get("error_category"),
                    p.get("summary"),
                    p.get("app_type"),
                    p.get("app_version"),
                    p.get("customer_id"),
                    p.get("shop_name"),
                    p.get("branch_label"),
                    p.get("device_name"),
                    p.get("device_id"),
                    p.get("user_label"),
                    p.get("screen_name"),
                    p.get("activity_name"),
                    p.get("fragment_name"),
                    p.get("user_action"),
                    p.get("what_happened"),
                    p.get("user_flow"),
                    p.get("breadcrumbs"),
                    p.get("api_method"),
                    p.get("api_url"),
                    p.get("http_status"),
                    p.get("request_body"),
                    p.get("response_body"),
                    p.get("request_size"),
                    p.get("response_size"),
                    p.get("request_duration_ms"),
                    p.get("printer_type"),
                    p.get("printer_model"),
                    p.get("printer_connection"),
                    p.get("print_operation"),
                    p.get("original_error_message"),
                    p.get("original_exception_class"),
                    p.get("original_stack_trace"),
                    p.get("original_error_code"),
                    p.get("original_api_response")
            ).execute();
            return response.isSuccessful()
                    && response.body() != null
                    && "1".equals(response.body().getStatus());
        } catch (Throwable t) {
            Log.w(TAG, "uploadOne failed (will retry): " + t.getMessage());
            return false;
        }
    }
}
