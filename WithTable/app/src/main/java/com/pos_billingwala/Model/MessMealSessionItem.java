package com.pos_billingwala.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MessMealSessionItem {
    @SerializedName("sessionId")
    @Expose
    public String sessionId;
    @SerializedName("sessionName")
    @Expose
    public String sessionName;
    @SerializedName("startTime")
    @Expose
    public String startTime;
    @SerializedName("endTime")
    @Expose
    public String endTime;
    @SerializedName("tokenPrefix")
    @Expose
    public String tokenPrefix;
    @SerializedName("isActive")
    @Expose
    public String isActive;
    @SerializedName("menuNotes")
    @Expose
    public String menuNotes;
    @SerializedName("sortOrder")
    @Expose
    public String sortOrder;
}
