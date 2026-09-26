package com.pos_billingwala.Model;

/**
 * Unified mess report row — paper coupon vs QR token are separate sources.
 */
public class MessReportItem {

    public static final String SOURCE_COUPON = "COUPON";
    public static final String SOURCE_QR = "QR";

    private String source;
    private String memberName;
    private String messType;
    private String dateTime;
    private String detail; // coupon label or token code
    private String memberId;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isQr() {
        return SOURCE_QR.equalsIgnoreCase(source);
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

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String displayType() {
        String meal = messType != null ? messType.trim() : "";
        if (isQr()) {
            return meal.isEmpty() ? "QR Token" : ("QR · " + meal);
        }
        return meal.isEmpty() ? "Coupon" : ("Coupon · " + meal);
    }
}
