package com.pos_billingwala.Extra;

/**
 * Offline sync architecture facade.
 * Phase 21: local-first writes; upload receivers + WorkManager download chain.
 */
public final class OfflineSyncArchitecture {

    private OfflineSyncArchitecture() {
    }

    public static String uploadPath() {
        return "OfflineToNetworkReceiver / OfflineNetworkData / UserSynchronizeData / OfflineSyncExecutor";
    }

    public static String downloadPath() {
        return "NetworkDataFetcher → Workers (FoodType, Category, Product, Portion, Invoice, Mess, …)";
    }

    public static String statusUi() {
        return "CloudSyncStatus + CloudSyncTracker";
    }

    public static String moduleSummary() {
        return "Upload: " + uploadPath()
                + "\nUniversalPendingUpload: appointments, deposits, tiers, variants, template, staff"
                + "\nDownload: " + downloadPath()
                + "\nUI: " + statusUi()
                + "\nPosConfigCache: per-shop Dynamic config snapshot"
                + "\nConfigApplyGuard: block template apply while cart/payment/print active"
                + "\nRule: mark synced only when API status==1"
                + "\nInvoice pending: InvoicePendingSync"
                + "\nStaff PIN: sha256 hashed (legacy plaintext still verifies)"
                + "\nNever mix branch/shop data; never delete history when modules off";
    }
}
