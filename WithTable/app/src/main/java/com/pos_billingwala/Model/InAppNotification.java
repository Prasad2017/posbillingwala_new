package com.pos_billingwala.Model;

public class InAppNotification {
    public String id;
    public String type;
    public String title;
    public String body;
    public String url;
    public long createdAt;
    public boolean read;
    /** Optional key to avoid duplicate daily reminders (e.g. license_expiring:2026-09-06). */
    public String dedupeKey;

    public InAppNotification() {
    }

    public InAppNotification(String id, String type, String title, String body,
                             String url, long createdAt, boolean read, String dedupeKey) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.body = body;
        this.url = url;
        this.createdAt = createdAt;
        this.read = read;
        this.dedupeKey = dedupeKey;
    }
}
