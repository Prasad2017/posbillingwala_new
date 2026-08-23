package com.pos_billingwala.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ExpenseResponse {

    @SerializedName("expensesId")
    @Expose
    public String expenseId;
    @SerializedName("expensesName")
    @Expose
    public String expenseName;
    @SerializedName("expensesAmount")
    @Expose
    public String expenseAmount;
    @SerializedName("expensesDate")
    @Expose
    public String expenseDate;
    @SerializedName("expensesNetworkStatus")
    @Expose
    public String expenseNetworkStatus;
    @SerializedName("expensesStatus")
    @Expose
    public String expenseStatus;


    public String getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(String expenseId) {
        this.expenseId = expenseId;
    }

    public String getExpenseName() {
        return expenseName;
    }

    public void setExpenseName(String expenseName) {
        this.expenseName = expenseName;
    }

    public String getExpenseAmount() {
        return expenseAmount;
    }

    public void setExpenseAmount(String expenseAmount) {
        this.expenseAmount = expenseAmount;
    }

    public String getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(String expenseDate) {
        this.expenseDate = expenseDate;
    }

    public String getExpenseNetworkStatus() {
        return expenseNetworkStatus;
    }

    public void setExpenseNetworkStatus(String expenseNetworkStatus) {
        this.expenseNetworkStatus = expenseNetworkStatus;
    }

    public String getExpenseStatus() {
        return expenseStatus;
    }

    public void setExpenseStatus(String expenseStatus) {
        this.expenseStatus = expenseStatus;
    }
}
