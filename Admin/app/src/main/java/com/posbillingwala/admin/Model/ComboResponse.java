package com.posbillingwala.admin.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ComboResponse {
    @SerializedName("comboId")
    @Expose
    private String comboId;
    @SerializedName("comboName")
    @Expose
    private String comboName;
    @SerializedName("comboPrice")
    @Expose
    private String comboPrice;
    @SerializedName("comboCode")
    @Expose
    private String comboCode;
    @SerializedName("comboActiveStatus")
    @Expose
    private String comboActiveStatus;
    @SerializedName("comboDeletedStatus")
    @Expose
    private String comboDeletedStatus;
    @SerializedName("comboNetworkStatus")
    @Expose
    private String comboNetworkStatus;

    public String getComboId() {
        return comboId;
    }

    public String getComboName() {
        return comboName;
    }

    public String getComboPrice() {
        return comboPrice;
    }

    public String getComboCode() {
        return comboCode;
    }

    public String getComboActiveStatus() {
        return comboActiveStatus;
    }

    public String getComboDeletedStatus() {
        return comboDeletedStatus;
    }

    public String getComboNetworkStatus() {
        return comboNetworkStatus;
    }
}
