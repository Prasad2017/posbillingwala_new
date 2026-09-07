package com.posbillingwala.owner.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/** Cloud service appointment row (salon family) for Owner calendar. */
public class ServiceAppointmentResponse {

    @SerializedName("appointmentId")
    @Expose
    public String appointmentId;

    @SerializedName("localAppointmentId")
    @Expose
    public String localAppointmentId;

    @SerializedName("productId")
    @Expose
    public String productId;

    @SerializedName("productName")
    @Expose
    public String productName;

    @SerializedName("customerName")
    @Expose
    public String customerName;

    @SerializedName("customerMobile")
    @Expose
    public String customerMobile;

    @SerializedName("appointmentAt")
    @Expose
    public String appointmentAt;

    @SerializedName("notes")
    @Expose
    public String notes;

    @SerializedName("appointmentStatus")
    @Expose
    public String appointmentStatus;

    @SerializedName("staffId")
    @Expose
    public String staffId;

    @SerializedName("staffName")
    @Expose
    public String staffName;

    public String getAppointmentAt() {
        return appointmentAt != null ? appointmentAt : "";
    }

    public String getCustomerName() {
        return customerName != null ? customerName : "";
    }

    public String getProductName() {
        return productName != null ? productName : "";
    }

    public String getCustomerMobile() {
        return customerMobile != null ? customerMobile : "";
    }

    public String getAppointmentStatus() {
        return appointmentStatus != null && !appointmentStatus.isEmpty() ? appointmentStatus : "booked";
    }

    public String getStaffName() {
        return staffName != null ? staffName : "";
    }

    public String getNotes() {
        return notes != null ? notes : "";
    }

    public String getAppointmentId() {
        return appointmentId != null ? appointmentId : "";
    }

    public String getLocalAppointmentId() {
        return localAppointmentId != null ? localAppointmentId : "";
    }
}
