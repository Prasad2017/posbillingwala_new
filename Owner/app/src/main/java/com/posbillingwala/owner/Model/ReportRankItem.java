package com.posbillingwala.owner.Model;

public class ReportRankItem {

    private String customerId;
    private String customerName;
    private String shopName;
    private String totalSales;
    private String label;
    private String count;
    private String amount;
    private String date;
    private String total;
    private String name;
    private String branchId;

    public String getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getShopName() { return shopName; }
    public String getTotalSales() { return totalSales; }
    public String getLabel() { return label; }
    public String getCount() { return count; }
    public String getAmount() { return amount; }
    public String getDate() { return date; }
    public String getTotal() { return total; }
    public String getName() { return name; }
    public String getBranchId() { return branchId; }

    public String displayName() {
        if (shopName != null && !shopName.trim().isEmpty()) return shopName.trim();
        if (customerName != null && !customerName.trim().isEmpty()) return customerName.trim();
        if (name != null && !name.trim().isEmpty()) return name.trim();
        if (label != null && !label.trim().isEmpty()) return label.trim();
        return "—";
    }
}
