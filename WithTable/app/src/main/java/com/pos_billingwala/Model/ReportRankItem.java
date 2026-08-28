package com.pos_billingwala.Model;

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

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(String totalSales) {
        this.totalSales = totalSales;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String displayName() {
        if (customerName != null && !customerName.trim().isEmpty()) {
            return customerName.trim();
        }
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        if (shopName != null && !shopName.trim().isEmpty()) {
            return shopName.trim();
        }
        if (label != null && !label.trim().isEmpty()) {
            return label.trim();
        }
        return "—";
    }

    public String displayValue() {
        if (totalSales != null) {
            return totalSales;
        }
        if (amount != null) {
            return amount;
        }
        if (count != null) {
            return count;
        }
        if (total != null) {
            return total;
        }
        return "0";
    }
}
