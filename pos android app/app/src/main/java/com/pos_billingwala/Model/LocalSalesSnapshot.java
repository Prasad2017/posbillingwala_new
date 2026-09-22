package com.pos_billingwala.Model;

import java.util.ArrayList;
import java.util.List;

public class LocalSalesSnapshot {

    private String periodLabel = "";
    private String branchLabel = "";
    private float totalSales;
    private float netSales;
    private int billCount;
    private float avgBill;
    private String totalSalesTrend = "0%";
    private String netSalesTrend = "0%";
    private String billCountTrend = "0%";
    private String avgBillTrend = "0%";
    private List<ReportRankItem> salesTrend = new ArrayList<>();
    private List<ReportRankItem> topCustomers = new ArrayList<>();
    private List<InvoiceResponse> recentInvoices = new ArrayList<>();

    public String getPeriodLabel() {
        return periodLabel;
    }

    public void setPeriodLabel(String periodLabel) {
        this.periodLabel = periodLabel;
    }

    public String getBranchLabel() {
        return branchLabel;
    }

    public void setBranchLabel(String branchLabel) {
        this.branchLabel = branchLabel;
    }

    public float getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(float totalSales) {
        this.totalSales = totalSales;
    }

    public float getNetSales() {
        return netSales;
    }

    public void setNetSales(float netSales) {
        this.netSales = netSales;
    }

    public int getBillCount() {
        return billCount;
    }

    public void setBillCount(int billCount) {
        this.billCount = billCount;
    }

    public float getAvgBill() {
        return avgBill;
    }

    public void setAvgBill(float avgBill) {
        this.avgBill = avgBill;
    }

    public String getTotalSalesTrend() {
        return totalSalesTrend;
    }

    public void setTotalSalesTrend(String totalSalesTrend) {
        this.totalSalesTrend = totalSalesTrend;
    }

    public String getNetSalesTrend() {
        return netSalesTrend;
    }

    public void setNetSalesTrend(String netSalesTrend) {
        this.netSalesTrend = netSalesTrend;
    }

    public String getBillCountTrend() {
        return billCountTrend;
    }

    public void setBillCountTrend(String billCountTrend) {
        this.billCountTrend = billCountTrend;
    }

    public String getAvgBillTrend() {
        return avgBillTrend;
    }

    public void setAvgBillTrend(String avgBillTrend) {
        this.avgBillTrend = avgBillTrend;
    }

    public List<ReportRankItem> getSalesTrend() {
        return salesTrend;
    }

    public void setSalesTrend(List<ReportRankItem> salesTrend) {
        this.salesTrend = salesTrend;
    }

    public List<ReportRankItem> getTopCustomers() {
        return topCustomers;
    }

    public void setTopCustomers(List<ReportRankItem> topCustomers) {
        this.topCustomers = topCustomers;
    }

    public List<InvoiceResponse> getRecentInvoices() {
        return recentInvoices;
    }

    public void setRecentInvoices(List<InvoiceResponse> recentInvoices) {
        this.recentInvoices = recentInvoices;
    }
}
