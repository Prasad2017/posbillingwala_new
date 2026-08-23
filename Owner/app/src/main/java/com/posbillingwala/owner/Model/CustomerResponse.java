package com.posbillingwala.owner.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CustomerResponse {

    @SerializedName("id")
    @Expose
    public String id;
    @SerializedName("name")
    @Expose
    public String name;
    @SerializedName("email")
    @Expose
    public String email;
    @SerializedName("contact_number")
    @Expose
    public String contactNumber;
    @SerializedName("aadhar_number")
    @Expose
    public String aadharNumber;
    @SerializedName("address")
    @Expose
    public String address;
    @SerializedName("shopName")
    @Expose
    public String shopName;
    @SerializedName("reportPin")
    @Expose
    public String reportPin;
    @SerializedName("licensesResponse")
    @Expose
    public List<LicenseResponse> licenseResponseList;


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

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getReportPin() {
        return reportPin;
    }

    public void setReportPin(String reportPin) {
        this.reportPin = reportPin;
    }

    public List<LicenseResponse> getLicenseResponseList() {
        return licenseResponseList;
    }

    public void setLicenseResponseList(List<LicenseResponse> licenseResponseList) {
        this.licenseResponseList = licenseResponseList;
    }
}
