package com.posbillingwala.dealer.Model;

/**
 * In-memory product + portion row while adding/editing a product.
 */
public class ProductPortionDraft {

    private String portionId;
    private String portionMasterId;
    private String portionName;
    private String portionPrice;
    private String portionNetworkStatus;
    private int sortOrder;

    public ProductPortionDraft() {
    }

    public ProductPortionDraft(String portionMasterId, String portionName, String portionPrice, int sortOrder) {
        this.portionMasterId = portionMasterId;
        this.portionName = portionName;
        this.portionPrice = portionPrice;
        this.sortOrder = sortOrder;
    }

    public static ProductPortionDraft fromResponse(ProductPortionResponse response) {
        ProductPortionDraft draft = new ProductPortionDraft();
        if (response == null) {
            return draft;
        }
        draft.portionId = response.getPortionId();
        draft.portionMasterId = response.getPortionMasterId();
        draft.portionName = response.getPortionName();
        draft.portionPrice = response.getPortionPrice();
        draft.portionNetworkStatus = response.getPortionNetworkStatus();
        try {
            if (response.getPortionSortOrder() != null && !response.getPortionSortOrder().trim().isEmpty()) {
                draft.sortOrder = Integer.parseInt(response.getPortionSortOrder().trim());
            }
        } catch (NumberFormatException ignored) {
        }
        return draft;
    }

    public String getPortionId() {
        return portionId;
    }

    public void setPortionId(String portionId) {
        this.portionId = portionId;
    }

    public String getPortionMasterId() {
        return portionMasterId;
    }

    public void setPortionMasterId(String portionMasterId) {
        this.portionMasterId = portionMasterId;
    }

    public String getPortionName() {
        return portionName;
    }

    public void setPortionName(String portionName) {
        this.portionName = portionName;
    }

    public String getPortionPrice() {
        return portionPrice;
    }

    public void setPortionPrice(String portionPrice) {
        this.portionPrice = portionPrice;
    }

    public String getPortionNetworkStatus() {
        return portionNetworkStatus;
    }

    public void setPortionNetworkStatus(String portionNetworkStatus) {
        this.portionNetworkStatus = portionNetworkStatus;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
