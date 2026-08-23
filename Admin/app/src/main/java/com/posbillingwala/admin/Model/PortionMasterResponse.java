package com.posbillingwala.admin.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Global portion name catalog (Half, Full, Small…). Price is on product + portion only.
 */
public class PortionMasterResponse {

    @SerializedName("portionMasterId")
    @Expose
    private String portionMasterId;
    @SerializedName("portionName")
    @Expose
    private String portionName;
    @SerializedName("portionMasterDeletedStatus")
    @Expose
    private String portionMasterDeletedStatus;
    @SerializedName("portionMasterNetworkStatus")
    @Expose
    private String portionMasterNetworkStatus;

    public String getPortionMasterId() {
        return portionMasterId;
    }

    public void setPortionMasterId(String portionMasterId) {
        this.portionMasterId = portionMasterId;
    }

    public String getPortionName() {
        return portionName;
    }

    public void setPortionName(String portionName) {
        this.portionName = portionName;
    }

    public String getPortionMasterDeletedStatus() {
        return portionMasterDeletedStatus;
    }

    public void setPortionMasterDeletedStatus(String portionMasterDeletedStatus) {
        this.portionMasterDeletedStatus = portionMasterDeletedStatus;
    }

    public String getPortionMasterNetworkStatus() {
        return portionMasterNetworkStatus;
    }

    public void setPortionMasterNetworkStatus(String portionMasterNetworkStatus) {
        this.portionMasterNetworkStatus = portionMasterNetworkStatus;
    }
}
