package com.posbillingwala.admin.Model;

/**
 * Aggregated dealer metrics for Sales charts (from getDealerReport).
 */
public class DealerPerformance {

    private final String dealerId;
    private final String dealerName;
    private int totalCustomers;
    private int activeCustomers;
    private int activeLicenses;
    private int expiredLicenses;

    public DealerPerformance(String dealerId, String dealerName) {
        this.dealerId = dealerId;
        this.dealerName = dealerName != null ? dealerName : "Dealer";
    }

    public String getDealerId() {
        return dealerId;
    }

    public String getDealerName() {
        return dealerName;
    }

    public int getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(int totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public int getActiveCustomers() {
        return activeCustomers;
    }

    public void setActiveCustomers(int activeCustomers) {
        this.activeCustomers = activeCustomers;
    }

    public int getActiveLicenses() {
        return activeLicenses;
    }

    public void setActiveLicenses(int activeLicenses) {
        this.activeLicenses = activeLicenses;
    }

    public int getExpiredLicenses() {
        return expiredLicenses;
    }

    public void setExpiredLicenses(int expiredLicenses) {
        this.expiredLicenses = expiredLicenses;
    }

    public String shortLabel() {
        String name = dealerName.trim();
        if (name.length() <= 12) {
            return name;
        }
        return name.substring(0, 11) + "…";
    }
}
