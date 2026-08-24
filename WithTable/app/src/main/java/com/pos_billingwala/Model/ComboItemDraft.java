package com.pos_billingwala.Model;

import com.pos_billingwala.Extra.ComboValidator;

public class ComboItemDraft {

    private String comboItemId;
    private String productId;
    private String productName;
    private String portionId;
    private String portionName;
    private String quantity;
    private String comboItemNetworkStatus;
    private int sortOrder;

    public ComboItemDraft() {
        this.quantity = "1";
    }

    public static ComboItemDraft fromResponse(ComboItemResponse response) {
        ComboItemDraft draft = new ComboItemDraft();
        if (response == null) {
            return draft;
        }
        draft.comboItemId = response.getComboItemId();
        draft.productId = response.getProductId();
        draft.productName = response.getProductName();
        draft.portionId = response.getPortionId();
        draft.portionName = response.getPortionName();
        draft.quantity = response.getComboItemQuantity();
        draft.comboItemNetworkStatus = response.getComboItemNetworkStatus();
        try {
            if (response.getComboItemSortOrder() != null && !response.getComboItemSortOrder().trim().isEmpty()) {
                draft.sortOrder = Integer.parseInt(response.getComboItemSortOrder().trim());
            }
        } catch (NumberFormatException ignored) {
        }
        return draft;
    }

    public String getComboItemId() {
        return comboItemId;
    }

    public void setComboItemId(String comboItemId) {
        this.comboItemId = comboItemId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getPortionId() {
        return portionId;
    }

    public void setPortionId(String portionId) {
        this.portionId = portionId;
    }

    public String getPortionName() {
        return portionName;
    }

    public void setPortionName(String portionName) {
        this.portionName = portionName;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getComboItemNetworkStatus() {
        return comboItemNetworkStatus;
    }

    public void setComboItemNetworkStatus(String comboItemNetworkStatus) {
        this.comboItemNetworkStatus = comboItemNetworkStatus;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getDisplayLabel() {
        return ComboValidator.comboItemDisplayName(productName, portionName, quantity);
    }

    public String itemKey() {
        String portion = portionId != null ? portionId : "";
        return (productId != null ? productId : "") + "|" + portion;
    }
}
