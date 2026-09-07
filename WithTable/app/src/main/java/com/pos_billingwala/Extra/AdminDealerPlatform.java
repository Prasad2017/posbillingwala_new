package com.pos_billingwala.Extra;

/**
 * Admin / Dealer / Owner platform map (sibling Android apps + Laravel web admin).
 * Phase 17: documentation facade — POS stays storefront; platform apps manage licences/catalog.
 */
public final class AdminDealerPlatform {

    public static final String APP_POS = "WithTable (POS)";
    public static final String APP_OWNER = "Owner";
    public static final String APP_DEALER = "Dealer";
    public static final String APP_ADMIN = "Admin";
    public static final String WEB_ADMIN = "admin.posbillingwala.com (Laravel)";

    private AdminDealerPlatform() {
    }

    public static String roleSummary() {
        return APP_ADMIN + ": dealers, customers, licences, crash inbox, CMS\n"
                + APP_DEALER + ": customer + licence register/renew, catalog setup\n"
                + APP_OWNER + ": multi-branch sales/catalog view\n"
                + APP_POS + ": offline billing storefront\n"
                + WEB_ADMIN + ": web ops + Website CMS";
    }

    public static String moduleSummary() {
        return roleSummary()
                + "\nAuth: Bearer api_tokens across apps"
                + "\nCatalog Excel: Owner/Dealer/Admin CatalogImportExportHelper"
                + "\nPer-licence modules: Admin/Dealer ModuleCardAdapter → updateLicenseModules"
                + "\nBusiness template: Admin/Dealer/Owner set → company_business_templates"
                + " (also Admin/Dealer insertNewLicence / insertCustomer; Laravel web_admin)"
                + " → POS getBusinessTemplate on Fetch (p31)"
                + "\nOwner OutletOpsHub: template, appointments, deposits, staff";
    }
}
