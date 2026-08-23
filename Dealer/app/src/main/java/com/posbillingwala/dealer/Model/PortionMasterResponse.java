package com.posbillingwala.dealer.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Portion Master — name only (Half, Full, Small…). Price lives on product + portion.
 */
public class PortionMasterResponse {

    @SerializedName("portionMasterId")
    @Expose
    private String portionMasterId;
    @SerializedName("portionName")
    @Expose
    private String portionName;
    @SerializedName("portionMasterNetworkStatus")
    @Expose
    private String portionMasterNetworkStatus;
    @SerializedName("portionMasterDeletedStatus")
    @Expose
    private String portionMasterDeletedStatus;
    @SerializedName("portionMasterStatus")
    @Expose
    private String portionMasterStatus;

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

    public String getPortionMasterNetworkStatus() {
        return portionMasterNetworkStatus;
    }

    public void setPortionMasterNetworkStatus(String portionMasterNetworkStatus) {
        this.portionMasterNetworkStatus = portionMasterNetworkStatus;
    }

    public String getPortionMasterDeletedStatus() {
        return portionMasterDeletedStatus;
    }

    public void setPortionMasterDeletedStatus(String portionMasterDeletedStatus) {
        this.portionMasterDeletedStatus = portionMasterDeletedStatus;
    }

    public String getPortionMasterStatus() {
        return portionMasterStatus;
    }

    public void setPortionMasterStatus(String portionMasterStatus) {
        this.portionMasterStatus = portionMasterStatus;
    }
}
