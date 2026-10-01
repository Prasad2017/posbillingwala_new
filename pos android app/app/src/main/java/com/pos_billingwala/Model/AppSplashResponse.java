package com.pos_billingwala.Model;

import com.google.gson.annotations.SerializedName;

/** Public getAppSplash.php envelope. */
public class AppSplashResponse {

    @SerializedName("status")
    public String status;

    @SerializedName("imageUrl")
    public String imageUrl;

    @SerializedName("legacyUrl")
    public String legacyUrl;

    @SerializedName("images")
    public Images images;

    public boolean isSuccess() {
        if (status == null) {
            return false;
        }
        String s = status.trim().toLowerCase();
        return "1".equals(s) || "true".equals(s);
    }

    public String normalizedImageUrl() {
        return clean(imageUrl);
    }

    /**
     * Older single splash. When the server omits legacyUrl (old API),
     * imageUrl is that single picture.
     */
    public String normalizedLegacyUrl() {
        if (legacyUrl != null) {
            return clean(legacyUrl);
        }
        if (images == null) {
            return normalizedImageUrl();
        }
        return null;
    }

    private static String clean(String raw) {
        if (raw == null) {
            return null;
        }
        String u = raw.trim();
        return u.isEmpty() ? null : u;
    }

    public static class Images {
        @SerializedName("mobilePortrait")
        public String mobilePortrait;

        @SerializedName("mobileLandscape")
        public String mobileLandscape;

        @SerializedName("tabletPortrait")
        public String tabletPortrait;

        @SerializedName("tabletLandscape")
        public String tabletLandscape;

        @SerializedName("webPortrait")
        public String webPortrait;

        @SerializedName("webLandscape")
        public String webLandscape;

        public String urlFor(String slot) {
            if (slot == null) {
                return null;
            }
            switch (slot) {
                case "mobile_portrait":
                    return clean(mobilePortrait);
                case "mobile_landscape":
                    return clean(mobileLandscape);
                case "tablet_portrait":
                    return clean(tabletPortrait);
                case "tablet_landscape":
                    return clean(tabletLandscape);
                case "web_portrait":
                    return clean(webPortrait);
                case "web_landscape":
                    return clean(webLandscape);
                default:
                    return null;
            }
        }
    }
}
