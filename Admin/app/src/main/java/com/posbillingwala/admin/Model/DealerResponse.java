package com.posbillingwala.admin.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DealerResponse {

    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("email")
    @Expose
    private String email;
    @SerializedName("contact_number")
    @Expose
    private String contactNumber;
    @SerializedName("aadhar_number")
    @Expose
    private String aadharNumber;
    @SerializedName("address")
    @Expose
    private String address;
    @SerializedName("is_active")
    @Expose
    private String isActive;
    @SerializedName("totalCustomer")
    @Expose
    private String totalCustomer;
    @SerializedName("joiningDate")
    @Expose
    private String joiningDate;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getAadharNumber() {
        return aadharNumber;
    }

    public void setAadharNumber(String aadharNumber) {
        this.aadharNumber = aadharNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public boolean isActiveDealer() {
        return isActive == null || isActive.isEmpty() || "1".equals(isActive);
    }

    public String getTotalCustomer() {
        return totalCustomer;
    }

    public String getJoiningDate() {
        return joiningDate;
    }
}
