package com.posbillingwala.admin.Model;

public class CatalogImportError {

    private int row;
    private String productName;
    private String category;
    private String subCategory;
    private String portion;
    private String code;
    private String message;

    public int getRow() {
        return row;
    }

    public String getProductName() {
        return productName;
    }

    public String getCategory() {
        return category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public String getPortion() {
        return portion;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
