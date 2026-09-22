package com.pos_billingwala.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MessSessionCount {
    @SerializedName("sessionName")
    @Expose
    public String sessionName;
    @SerializedName("generated")
    @Expose
    public int generated;
    @SerializedName("printed")
    @Expose
    public int printed;
    @SerializedName("pending")
    @Expose
    public int pending;
    @SerializedName("failed")
    @Expose
    public int failed;
}
