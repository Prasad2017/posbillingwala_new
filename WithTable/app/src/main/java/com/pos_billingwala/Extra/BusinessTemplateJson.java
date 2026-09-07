package com.pos_billingwala.Extra;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * JSON contract for business templates (version 1).
 * <pre>
 * {
 *   "version": 1,
 *   "id": "restaurant_default",
 *   "businessType": "restaurant",
 *   "displayName": "Restaurant / Food",
 *   "features": ["fast_billing", "dine_in", ...]
 * }
 * </pre>
 */
public final class BusinessTemplateJson {

    public static final int VERSION = 1;

    private BusinessTemplateJson() {
    }

    public static String toJson(BusinessTemplate template) {
        if (template == null) {
            return "";
        }
        try {
            JSONObject root = new JSONObject();
            root.put("version", VERSION);
            root.put("id", template.getId());
            root.put("businessType", template.getBusinessType());
            root.put("displayName", template.getDisplayName());
            JSONArray features = new JSONArray();
            for (String flag : template.getEnabledFeatures()) {
                features.put(flag);
            }
            root.put("features", features);
            return root.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Parses a template JSON string. Returns null if invalid or empty.
     * Unknown feature keys are kept (forward-compatible).
     */
    public static BusinessTemplate parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            JSONObject root = new JSONObject(json);
            String id = root.optString("id", "").trim();
            String businessType = BusinessTypes.normalize(root.optString("businessType", ""));
            String displayName = root.optString("displayName", "").trim();
            if (id.isEmpty()) {
                return null;
            }
            if (displayName.isEmpty()) {
                displayName = id;
            }
            Set<String> features = new LinkedHashSet<>();
            JSONArray array = root.optJSONArray("features");
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    String flag = array.optString(i, "").trim();
                    if (!flag.isEmpty()) {
                        features.add(flag);
                    }
                }
            } else {
                JSONObject map = root.optJSONObject("featureMap");
                if (map != null) {
                    Iterator<String> keys = map.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        if (map.optBoolean(key, false)) {
                            features.add(key);
                        }
                    }
                }
            }
            return new BusinessTemplate(id, businessType, displayName, features);
        } catch (Exception e) {
            return null;
        }
    }
}
