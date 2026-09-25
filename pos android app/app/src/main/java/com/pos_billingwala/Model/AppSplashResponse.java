package com.pos_billingwala.Model;

import com.google.gson.annotations.SerializedName;

/** Public getAppSplash.php envelope. */
public class AppSplashResponse {

    @SerializedName("status")
    public String status;

    @SerializedName("imageUrl")
    public String imageUrl;

    public boolean isSuccess() {
        if (status == null) {
            return false;
        }
        String s = status.trim().toLowerCase();
        return "1".equals(s) || "true".equals(s);
    }

    public String normalizedImageUrl() {
        if (imageUrl == null) {
            return null;
        }
        String u = imageUrl.trim();
        return u.isEmpty() ? null : u;
    }
}
