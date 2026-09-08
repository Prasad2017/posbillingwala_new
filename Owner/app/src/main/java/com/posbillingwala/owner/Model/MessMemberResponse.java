package com.posbillingwala.owner.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MessMemberResponse {

    @SerializedName("memberId")
    @Expose
    public String memberId;
    @SerializedName("licenceId")
    @Expose
    public String licenceId;
    @SerializedName("memberName")
    @Expose
    public String memberName;
    @SerializedName("memberMobileNumber")
    @Expose
    public String memberMobileNumber;
    @SerializedName("memberAltenetMobileNumber")
    @Expose
    public String memberAltenetMobileNumber;
    @SerializedName("memberAddress")
    @Expose
    public String memberAddress;
    @SerializedName("registrationNo")
    @Expose
    public String registrationNo;
    @SerializedName("memberType")
    @Expose
    public String memberType;
    @SerializedName("rollNo")
    @Expose
    public String rollNo;
    @SerializedName("college")
    @Expose
    public String college;
    @SerializedName("studentYear")
    @Expose
    public String studentYear;
    @SerializedName("company")
    @Expose
    public String company;
    @SerializedName("memberStatus")
    @Expose
    public String memberStatus;
    @SerializedName("memberNetworkStatus")
    @Expose
    public String memberNetworkStatus;

    @SerializedName("paymentId")
    @Expose
    public String paymentId;
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
    @SerializedName("paymentNetworkStatus")
    @Expose
    public String paymentNetworkStatus;
    @SerializedName("paymentStatus")
    @Expose
    public String paymentStatus;

    public String getMemberId() { return memberId; }
    public String getLicenceId() { return licenceId; }
    public String getMemberName() { return memberName; }
    public String getMemberMobileNumber() { return memberMobileNumber; }
    public String getMemberAltenetMobileNumber() { return memberAltenetMobileNumber; }
    public String getMemberAddress() { return memberAddress; }
    public String getRegistrationNo() { return registrationNo; }
    public String getMemberType() { return memberType; }
    public String getRollNo() { return rollNo; }
    public String getCollege() { return college; }
    public String getStudentYear() { return studentYear; }
    public String getCompany() { return company; }
    public String getMemberStatus() { return memberStatus; }
    public String getPaymentId() { return paymentId; }
    public String getPaymentMessAmount() { return paymentMessAmount; }
    public String getPaymentPaidAmount() { return paymentPaidAmount; }
    public String getMessTotalDays() { return messTotalDays; }
    public String getPaymentDate() { return paymentDate; }
    public String getPaymentStatus() { return paymentStatus; }
}
