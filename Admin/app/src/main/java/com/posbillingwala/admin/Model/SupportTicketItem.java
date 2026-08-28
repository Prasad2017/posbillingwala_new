package com.posbillingwala.admin.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SupportTicketItem {
    @SerializedName("id") @Expose private String id;
    @SerializedName("ticketNo") @Expose private String ticketNo;
    @SerializedName("appName") @Expose private String appName;
    @SerializedName("category") @Expose private String category;
    @SerializedName("subject") @Expose private String subject;
    @SerializedName("description") @Expose private String description;
    @SerializedName("status") @Expose private String status;
    @SerializedName("createdAt") @Expose private String createdAt;
    @SerializedName("messages") @Expose private List<SupportMessageItem> messages;

    @SerializedName("shopName") @Expose private String shopName;

    public String getId() { return id; }
    public String getTicketNo() { return ticketNo; }
    public String getAppName() { return appName; }
    public String getCategory() { return category; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getShopName() { return shopName; }
    public List<SupportMessageItem> getMessages() { return messages; }

    public static class SupportMessageItem {
        @SerializedName("id") @Expose private String id;
        @SerializedName("sender") @Expose private String sender;
        @SerializedName("message") @Expose private String message;
        @SerializedName("createdAt") @Expose private String createdAt;
        public String getId() { return id; }
        public String getSender() { return sender; }
        public String getMessage() { return message; }
        public String getCreatedAt() { return createdAt; }
    }
}
