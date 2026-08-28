package com.pos_billingwala.Extra;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Serializable payload for crash/error ingest. */
public final class ErrorLogPayload {

    public final Map<String, String> fields = new LinkedHashMap<>();

    public ErrorLogPayload put(String key, String value) {
        fields.put(key, value != null ? value : "");
        return this;
    }

    public ErrorLogPayload putInt(String key, Integer value) {
        fields.put(key, value != null ? String.valueOf(value) : "");
        return this;
    }

    public String get(String key) {
        String v = fields.get(key);
        return v != null ? v : "";
    }

    public String toJson() {
        try {
            JSONObject obj = new JSONObject();
            for (Map.Entry<String, String> e : fields.entrySet()) {
                obj.put(e.getKey(), e.getValue());
            }
            return obj.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    public static ErrorLogPayload fromJson(String json) {
        ErrorLogPayload p = new ErrorLogPayload();
        if (json == null || json.isEmpty()) {
            return p;
        }
        try {
            JSONObject obj = new JSONObject(json);
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                p.put(k, obj.optString(k, ""));
            }
        } catch (Exception ignored) {
        }
        return p;
    }
}
