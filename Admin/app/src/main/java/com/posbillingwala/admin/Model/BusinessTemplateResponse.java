package com.posbillingwala.admin.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Licence business template row from Admin getBusinessTemplate.php.
 */
public class BusinessTemplateResponse {

    @SerializedName("licenceId")
    @Expose
    public String licenceId;

    @SerializedName("businessType")
    @Expose
    public String businessType;

    @SerializedName("businessTemplateId")
    @Expose
    public String businessTemplateId;

    @SerializedName("businessTemplateJson")
    @Expose
    public String businessTemplateJson;

    public String getLicenceId() {
        return licenceId;
    }

    public String getBusinessType() {
        return businessType != null ? businessType : "restaurant";
    }

    public String getBusinessTemplateId() {
        return businessTemplateId != null ? businessTemplateId : "restaurant_default";
    }

    public String getBusinessTemplateJson() {
        return businessTemplateJson != null ? businessTemplateJson : "";
    }
}
