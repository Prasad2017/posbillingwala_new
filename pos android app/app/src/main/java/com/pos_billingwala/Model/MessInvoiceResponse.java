package com.pos_billingwala.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MessInvoiceResponse {

    @SerializedName("invoiceId")
    @Expose
    public String invoiceId;
    @SerializedName("memberId")
    @Expose
    public String memberId;
    @SerializedName("memberName")
    @Expose
    public String memberName;
    @SerializedName("messType")
    @Expose
    public String messType;
    @SerializedName("messInvoiceDate")
    @Expose
    public String messInvoiceDate;
    @SerializedName("messInvoiceNetworkStatus")
    @Expose
    public String messInvoiceNetworkStatus;
    @SerializedName("messInvoiceStatus")
    @Expose
    public String messInvoiceStatus;


    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

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

    public String getMessType() {
        return messType;
    }

    public void setMessType(String messType) {
        this.messType = messType;
    }

    public String getMessInvoiceDate() {
        return messInvoiceDate;
    }

    public void setMessInvoiceDate(String messInvoiceDate) {
        this.messInvoiceDate = messInvoiceDate;
    }

    public String getMessInvoiceNetworkStatus() {
        return messInvoiceNetworkStatus;
    }

    public void setMessInvoiceNetworkStatus(String messInvoiceNetworkStatus) {
        this.messInvoiceNetworkStatus = messInvoiceNetworkStatus;
    }

    public String getMessInvoiceStatus() {
        return messInvoiceStatus;
    }

    public void setMessInvoiceStatus(String messInvoiceStatus) {
        this.messInvoiceStatus = messInvoiceStatus;
    }
}
