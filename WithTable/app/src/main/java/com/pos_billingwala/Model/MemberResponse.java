package com.pos_billingwala.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MemberResponse {

    @SerializedName("memberId")
    @Expose
    public String memberId;
    @SerializedName("memberName")
    @Expose
    public String memberName;
    @SerializedName("memberMobileNumber")
    @Expose
    public String memberMobileNumber;
    @SerializedName("memberAlternetMobileNumber")
    @Expose
    public String memberAlternetMobileNumber;
    @SerializedName("memberAddress")
    @Expose
    public String memberAddress;
    @SerializedName("paymentMessAmount")
    @Expose
    public String paymentMessAmount;
    @SerializedName("paymentPaidAmount")
    @Expose
    public String paymentPaidAmount;
    @SerializedName("messTotalDays")
    @Expose
    public String messTotalDays;
    @SerializedName("paymentDate")
    @Expose
    public String paymentDate;
    @SerializedName("memberNetworkStatus")
    @Expose
    public String memberNetworkStatus;
    @SerializedName("memberStatus")
    @Expose
    public String memberStatus;
    @SerializedName("paymentNetworkStatus")
    @Expose
    public String paymentNetworkStatus;
    @SerializedName("paymentStatus")
    @Expose
    public String paymentStatus;


    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getMemberMobileNumber() {
        return memberMobileNumber;
    }

    public void setMemberMobileNumber(String memberMobileNumber) {
        this.memberMobileNumber = memberMobileNumber;
    }

    public String getMemberAlternetMobileNumber() {
        return memberAlternetMobileNumber;
    }

    public void setMemberAlternetMobileNumber(String memberAlternetMobileNumber) {
        this.memberAlternetMobileNumber = memberAlternetMobileNumber;
    }

    public String getMemberNetworkStatus() {
        return memberNetworkStatus;
    }

    public void setMemberNetworkStatus(String memberNetworkStatus) {
        this.memberNetworkStatus = memberNetworkStatus;
    }

    public String getMemberStatus() {
        return memberStatus;
    }

    public void setMemberStatus(String memberStatus) {
        this.memberStatus = memberStatus;
    }

    public String getMemberAddress() {
        return memberAddress;
    }

    public void setMemberAddress(String memberAddress) {
        this.memberAddress = memberAddress;
    }

    public String getPaymentMessAmount() {
        return paymentMessAmount;
    }

    public void setPaymentMessAmount(String paymentMessAmount) {
        this.paymentMessAmount = paymentMessAmount;
    }

    public String getPaymentPaidAmount() {
        return paymentPaidAmount;
    }

    public void setPaymentPaidAmount(String paymentPaidAmount) {
        this.paymentPaidAmount = paymentPaidAmount;
    }

    public String getMessTotalDays() {
        return messTotalDays;
    }

    public void setMessTotalDays(String messTotalDays) {
        this.messTotalDays = messTotalDays;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentNetworkStatus() {
        return paymentNetworkStatus;
    }

    public void setPaymentNetworkStatus(String paymentNetworkStatus) {
        this.paymentNetworkStatus = paymentNetworkStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}
