package com.pos_billingwala.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MessTokenResponse {

    @SerializedName("tokenId")
    @Expose
    public String tokenId;
    @SerializedName("tokenCode")
    @Expose
    public String tokenCode;
    @SerializedName("memberId")
    @Expose
    public String memberId;
    @SerializedName("memberName")
    @Expose
    public String memberName;
    @SerializedName("memberMobile")
    @Expose
    public String memberMobile;
    @SerializedName("memberType")
    @Expose
    public String memberType;
    @SerializedName("messType")
    @Expose
    public String messType;
    @SerializedName("tokenAmount")
    @Expose
    public String tokenAmount;
    @SerializedName("tokenDate")
    @Expose
    public String tokenDate;
    @SerializedName("verifiedDate")
    @Expose
    public String verifiedDate;
    @SerializedName("tokenNetworkStatus")
    @Expose
    public String tokenNetworkStatus;
    @SerializedName("tokenState")
    @Expose
    public String tokenState;
    @SerializedName("verifyNetworkStatus")
    @Expose
    public String verifyNetworkStatus;
    @SerializedName("tokenStatus")
    @Expose
    public String tokenStatus;
    @SerializedName("verifyStatus")
    @Expose
    public String verifyStatus;

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getTokenCode() {
        return tokenCode;
    }

    public void setTokenCode(String tokenCode) {
        this.tokenCode = tokenCode;
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

    public String getMemberMobile() {
        return memberMobile;
    }

    public void setMemberMobile(String memberMobile) {
        this.memberMobile = memberMobile;
    }

    public String getMemberType() {
        return memberType;
    }

    public void setMemberType(String memberType) {
        this.memberType = memberType;
    }

    public String getMessType() {
        return messType;
    }

    public void setMessType(String messType) {
        this.messType = messType;
    }

    public String getTokenAmount() {
        return tokenAmount;
    }

    public void setTokenAmount(String tokenAmount) {
        this.tokenAmount = tokenAmount;
    }

    public String getTokenDate() {
        return tokenDate;
    }

    public void setTokenDate(String tokenDate) {
        this.tokenDate = tokenDate;
    }

    public String getVerifiedDate() {
        return verifiedDate;
    }

    public void setVerifiedDate(String verifiedDate) {
        this.verifiedDate = verifiedDate;
    }

    public String getTokenNetworkStatus() {
        return tokenNetworkStatus;
    }

    public void setTokenNetworkStatus(String tokenNetworkStatus) {
        this.tokenNetworkStatus = tokenNetworkStatus;
    }

    public String getTokenState() {
        return tokenState != null ? tokenState : tokenStatus;
    }

    public void setTokenState(String tokenState) {
        this.tokenState = tokenState;
    }

    public String getVerifyNetworkStatus() {
        return verifyNetworkStatus;
    }

    public void setVerifyNetworkStatus(String verifyNetworkStatus) {
        this.verifyNetworkStatus = verifyNetworkStatus;
    }

    public String getTokenStatus() {
        return tokenStatus;
    }

    public void setTokenStatus(String tokenStatus) {
        this.tokenStatus = tokenStatus;
    }

    public String getVerifyStatus() {
        return verifyStatus;
    }

    public void setVerifyStatus(String verifyStatus) {
        this.verifyStatus = verifyStatus;
    }
}
