package com.pos_billingwala.Model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Envelope for getHomeBannerList.php. */
public class HomeBannerListResponse {

    @SerializedName("status")
    public String status;

    @SerializedName("bannerResponse")
    public List<HomeBannerResponse> bannerResponse;

    public boolean isSuccess() {
        if (status == null) {
            return false;
        }
        String s = status.trim().toLowerCase();
        return "1".equals(s) || "true".equals(s);
    }
}
