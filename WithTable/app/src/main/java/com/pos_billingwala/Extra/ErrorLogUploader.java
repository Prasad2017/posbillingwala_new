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

    /** Uploads queued crash/error logs. Returns how many files were accepted by the server. */
    public static int flushPending(Context context) {
        int uploaded = 0;
        if (context == null) {
            return 0;
        }
        if (!ErrorLogQueue.tryBeginFlush()) {
            return 0;
        }
        try {
            List<File> pending = ErrorLogQueue.listPending(context);
            boolean networkDown = false;
            for (File file : pending) {
                if (networkDown) {
                    break;
                }
                ErrorLogPayload payload = ErrorLogQueue.readFile(file);
                if (payload == null) {
                    ErrorLogQueue.deleteFile(file);
                    continue;
                }
                int result = uploadOne(context, payload);
                if (result == 1) {
                    ErrorLogQueue.deleteFile(file);
                    uploaded++;
                } else if (result < 0) {
                    networkDown = true;
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "flushPending: " + t.getMessage());
        } finally {
            ErrorLogQueue.endFlush();
        }
        return uploaded;
    }

    /** @return 1 saved, 0 keep+continue, -1 network down (stop queue) */
    private static int uploadOne(Context context, ErrorLogPayload p) {
        try {
            String userId = p.get("userId");
            if (userId.isEmpty()) {
                userId = Common.getSavedUserData(context, "userId");
            }
            if (userId.isEmpty()) {
                userId = Common.getSavedUserData(context, "ownerId");
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
                    && "1".equals(response.body().getStatus()) ? 1 : 0;
        } catch (java.io.IOException t) {
            Log.w(TAG, "uploadOne network (will retry): " + t.getMessage());
            return -1;
        } catch (Throwable t) {
            Log.w(TAG, "uploadOne failed (will retry): " + t.getMessage());
            return 0;
        }
    }
}
