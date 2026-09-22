package com.pos_billingwala.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MessQrInfo {
    @SerializedName("publicToken")
    @Expose
    public String publicToken;
    @SerializedName("status")
    @Expose
    public String status;
    @SerializedName("qrUrl")
    @Expose
    public String qrUrl;
    @SerializedName("messLabel")
    @Expose
    public String messLabel;
    @SerializedName("branchLabel")
    @Expose
    public String branchLabel;
    @SerializedName("printDeviceId")
    @Expose
    public String printDeviceId;
    @SerializedName("createdAt")
    @Expose
    public String createdAt;
}
