package com.posbillingwala.admin.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class WebsiteContactItem {
    @SerializedName("id") @Expose private String id;
    @SerializedName("name") @Expose private String name;
    @SerializedName("email") @Expose private String email;
    @SerializedName("subject") @Expose private String subject;
    @SerializedName("message") @Expose private String message;
    @SerializedName("contactStatus") @Expose private String contactStatus;
    @SerializedName("status") @Expose private String status;
    @SerializedName("createdAt") @Expose private String createdAt;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    public String getContactStatus() {
        return contactStatus != null && !contactStatus.isEmpty() ? contactStatus : status;
    }
    public String getCreatedAt() { return createdAt; }
}
