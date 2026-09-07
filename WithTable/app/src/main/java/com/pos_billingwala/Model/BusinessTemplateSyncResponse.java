package com.pos_billingwala.Model;

/** Cloud sync payload for shop business template selection. */
public class BusinessTemplateSyncResponse {

    private String businessType;
    private String businessTemplateId;
    private String businessTemplateJson;
    private String templateNetworkStatus;

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getBusinessTemplateId() {
        return businessTemplateId;
    }

    public void setBusinessTemplateId(String businessTemplateId) {
        this.businessTemplateId = businessTemplateId;
    }

    public String getBusinessTemplateJson() {
        return businessTemplateJson;
    }

    public void setBusinessTemplateJson(String businessTemplateJson) {
        this.businessTemplateJson = businessTemplateJson;
    }

    public String getTemplateNetworkStatus() {
        return templateNetworkStatus;
    }

    public void setTemplateNetworkStatus(String templateNetworkStatus) {
        this.templateNetworkStatus = templateNetworkStatus;
    }
}
