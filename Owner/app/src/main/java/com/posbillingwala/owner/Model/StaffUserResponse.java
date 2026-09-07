package com.posbillingwala.owner.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/** Outlet staff roster row for Owner (no PIN). */
public class StaffUserResponse {

    @SerializedName("staffId")
    @Expose
    public String staffId;

    @SerializedName("localStaffId")
    @Expose
    public String localStaffId;

    @SerializedName("staffName")
    @Expose
    public String staffName;

    @SerializedName("staffRole")
    @Expose
    public String staffRole;

    @SerializedName("staffActive")
    @Expose
    public String staffActive;

    @SerializedName("staffDeletedStatus")
    @Expose
    public String staffDeletedStatus;

    @SerializedName("createdAt")
    @Expose
    public String createdAt;

    public String getStaffId() {
        return staffId != null ? staffId : "";
    }

    public String getLocalStaffId() {
        return localStaffId != null ? localStaffId : "";
    }

    public String getStaffName() {
        return staffName != null ? staffName : "";
    }

    public String getStaffRole() {
        return staffRole != null && !staffRole.isEmpty() ? staffRole : "cashier";
    }

    public String getStaffActive() {
        return staffActive != null ? staffActive : "1";
    }

    public String getStaffDeletedStatus() {
        return staffDeletedStatus != null ? staffDeletedStatus : "0";
    }

    public boolean isDeleted() {
        return "1".equals(staffDeletedStatus);
    }

    public boolean isActive() {
        return !"0".equals(getStaffActive()) && !isDeleted();
    }
}
