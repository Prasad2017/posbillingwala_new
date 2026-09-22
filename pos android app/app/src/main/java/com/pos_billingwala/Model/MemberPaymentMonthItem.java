package com.pos_billingwala.Model;

public class MemberPaymentMonthItem {
    public String yearMonth;
    public String messAmount;
    public String paidAmount;
    public String pendingAmount;
    public String status;

    public MemberPaymentMonthItem() {
    }

    public MemberPaymentMonthItem(String yearMonth, String messAmount, String paidAmount,
                                  String pendingAmount, String status) {
        this.yearMonth = yearMonth;
        this.messAmount = messAmount;
        this.paidAmount = paidAmount;
        this.pendingAmount = pendingAmount;
        this.status = status;
    }
}
