package com.pos_billingwala.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MessMealTokenItem {
    @SerializedName("tokenId")
    @Expose
    public String tokenId;
    @SerializedName("tokenNumber")
    @Expose
    public String tokenNumber;
    @SerializedName("registrationNo")
    @Expose
    public String registrationNo;
    @SerializedName("mealSession")
    @Expose
    public String mealSession;
    @SerializedName("date")
    @Expose
    public String date;
    @SerializedName("printStatus")
    @Expose
    public String printStatus;
    @SerializedName("createdAt")
    @Expose
    public String createdAt;
    @SerializedName("printedAt")
    @Expose
    public String printedAt;
    @SerializedName("memberName")
    @Expose
    public String memberName;
}
