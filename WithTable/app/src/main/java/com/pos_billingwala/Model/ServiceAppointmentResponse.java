package com.pos_billingwala.Model;

/** Local service appointment row (salon family). */
public class ServiceAppointmentResponse {

    private String appointmentId;
    private String localAppointmentId;
    private String productId;
    private String productName;
    private String customerName;
    private String customerMobile;
    private String appointmentAt;
    private String notes;
    private String appointmentStatus;
    private String staffId;
    private String staffName;
    private String appointmentNetworkStatus;

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getLocalAppointmentId() {
        return localAppointmentId;
    }

    public void setLocalAppointmentId(String localAppointmentId) {
        this.localAppointmentId = localAppointmentId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerMobile() {
        return customerMobile;
    }

    public void setCustomerMobile(String customerMobile) {
        this.customerMobile = customerMobile;
    }

    public String getAppointmentAt() {
        return appointmentAt;
    }

    public void setAppointmentAt(String appointmentAt) {
        this.appointmentAt = appointmentAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getAppointmentStatus() {
        return appointmentStatus;
    }

    public void setAppointmentStatus(String appointmentStatus) {
        this.appointmentStatus = appointmentStatus;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getAppointmentNetworkStatus() {
        return appointmentNetworkStatus;
    }

    public void setAppointmentNetworkStatus(String appointmentNetworkStatus) {
        this.appointmentNetworkStatus = appointmentNetworkStatus;
    }
}
