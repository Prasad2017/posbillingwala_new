package com.pos_billingwala.Model;

import com.google.gson.annotations.SerializedName;

/** Single admin-uploaded home banner (getHomeBannerList.php). */
public class HomeBannerResponse {

    @SerializedName("bannerId")
    public String bannerId;

    @SerializedName("imageUrl")
    public String imageUrl;

    @SerializedName("sortOrder")
    public String sortOrder;

    public String normalizedImageUrl() {
        if (imageUrl == null) {
            return null;
        }
        String u = imageUrl.trim();
        return u.isEmpty() ? null : u;
    }
}
