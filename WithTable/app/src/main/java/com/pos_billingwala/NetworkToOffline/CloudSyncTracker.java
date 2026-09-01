package com.pos_billingwala.NetworkToOffline;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.Common;
import com.pos_billingwala.Extra.ErrorLogQueue;
import com.pos_billingwala.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Live pending/complete counts for each table that cloud sync uploads.
 */
public final class CloudSyncTracker {

    public static final String KEY_LAST_CLOUD_SYNC_MS = "lastCloudSyncMs";
    public static final String KEY_CATEGORIES = "categories";
    public static final String KEY_SUBCATEGORIES = "subcategories";
    public static final String KEY_PRODUCTS = "products";
    public static final String KEY_PORTION_MASTER = "portion_master";
    public static final String KEY_PORTIONS = "portions";
    public static final String KEY_COMBOS = "combos";
    public static final String KEY_COMBO_ITEMS = "combo_items";
    public static final String KEY_PRINTER = "printer";
    public static final String KEY_COMPANY = "company";
    public static final String KEY_INVOICE_DELETES = "invoice_deletes";
    public static final String KEY_INVOICE_ITEMS = "invoice_items";
    public static final String KEY_INVOICE_COMBO_ITEMS = "invoice_combo_items";
    public static final String KEY_INVOICES = "invoices";
    public static final String KEY_MESS_MEMBERS = "mess_members";
    public static final String KEY_MESS_PAYMENTS = "mess_payments";
    public static final String KEY_MESS_INVOICES = "mess_invoices";
    public static final String KEY_INVENTORY = "inventory";
    public static final String KEY_EXPENSES = "expenses";
    public static final String KEY_MESS_TOKENS = "mess_tokens";
    public static final String KEY_ERROR_LOGS = "error_logs";

    private static final MutableLiveData<Snapshot> LIVE = new MutableLiveData<>();
    private static volatile boolean running;
    private static volatile boolean lastSuccess = true;
    private static volatile int uploadedThisRun;
    private static volatile String currentKey = "";
    private static volatile String currentTitle = "";

    private CloudSyncTracker() {
    }

    @NonNull
    public static LiveData<Snapshot> live() {
        return LIVE;
    }

    public static boolean isRunning() {
        return running;
    }

    public static int uploadedThisRun() {
        return uploadedThisRun;
    }

    public static void beginRun() {
        running = true;
        lastSuccess = true;
        uploadedThisRun = 0;
        currentKey = "";
        currentTitle = "";
    }

    public static void addUploaded(int count) {
        if (count > 0) {
            uploadedThisRun += count;
        }
    }

    public static void setCurrentTable(@Nullable String key, @Nullable String title) {
        currentKey = key != null ? key : "";
        currentTitle = title != null ? title : "";
    }

    public static void endRun(boolean success) {
        running = false;
        lastSuccess = success;
        currentKey = "";
        currentTitle = "";
    }

    public static void recordSuccessfulSync(@NonNull Context context) {
        Common.saveUserData(context.getApplicationContext(), KEY_LAST_CLOUD_SYNC_MS,
                String.valueOf(System.currentTimeMillis()));
    }

    public static void invalidateSticky() {
        LIVE.setValue(null);
    }

    @NonNull
    public static Snapshot refresh(@NonNull Context context) {
        Snapshot snapshot = capture(context.getApplicationContext());
        LIVE.postValue(snapshot);
        return snapshot;
    }

    @NonNull
    public static Snapshot capture(@NonNull Context context) {
        POSBillingWalaDatabase db = new POSBillingWalaDatabase(context);
        List<TableStatus> tables = new ArrayList<>();
        int pendingTotal = 0;
        String activeKey = currentKey;
        for (TableDef def : TABLE_DEFS) {
            int pending = KEY_ERROR_LOGS.equals(def.key)
                    ? ErrorLogQueue.pendingCount(context)
                    : safeCount(def, db);
            pendingTotal += pending;
            tables.add(new TableStatus(def.key, def.labelRes, pending, pending == 0,
                    running && def.key.equals(activeKey)));
        }
        return new Snapshot(running, lastSuccess, uploadedThisRun, pendingTotal, currentTitle, tables);
    }

    private static int safeCount(TableDef def, POSBillingWalaDatabase db) {
        try {
            return def.counter.count(db);
        } catch (Exception e) {
            return 0;
        }
    }

    public static final class Snapshot {
        public final boolean running;
        public final boolean lastSuccess;
        public final int uploadedThisRun;
        public final int pendingTotal;
        public final String currentTitle;
        public final List<TableStatus> tables;

        Snapshot(boolean running, boolean lastSuccess, int uploadedThisRun, int pendingTotal,
                 String currentTitle, List<TableStatus> tables) {
            this.running = running;
            this.lastSuccess = lastSuccess;
            this.uploadedThisRun = uploadedThisRun;
            this.pendingTotal = pendingTotal;
            this.currentTitle = currentTitle != null ? currentTitle : "";
            this.tables = tables;
        }
    }

    public static final class TableStatus {
        public final String key;
        public final int labelRes;
        public final int pending;
        public final boolean complete;
        public final boolean inProgress;

        TableStatus(String key, int labelRes, int pending, boolean complete, boolean inProgress) {
            this.key = key;
            this.labelRes = labelRes;
            this.pending = pending;
            this.complete = complete;
            this.inProgress = inProgress;
        }
    }

    private interface Counter {
        int count(POSBillingWalaDatabase db);
    }

    private static final class TableDef {
        final String key;
        final int labelRes;
        final Counter counter;

        TableDef(String key, int labelRes, Counter counter) {
            this.key = key;
            this.labelRes = labelRes;
            this.counter = counter;
        }
    }

    private static final TableDef[] TABLE_DEFS = new TableDef[]{
            new TableDef(KEY_CATEGORIES, R.string.sync_table_categories,
                    db -> db.countUnsyncedRows(POSBillingWalaDatabase.PRODUCT_CATEGORY_TABLE, "categoryStatus")),
            new TableDef(KEY_SUBCATEGORIES, R.string.sync_table_subcategories,
                    db -> db.countUnsyncedRows(POSBillingWalaDatabase.PRODUCT_SUBCATEGORY_TABLE, "subcategoryStatus")),
            new TableDef(KEY_PRODUCTS, R.string.sync_table_products,
                    db -> db.countUnsyncedRows(POSBillingWalaDatabase.PRODUCT_TABLE, "productStatus")),
            new TableDef(KEY_PORTION_MASTER, R.string.sync_table_portion_master,
                    db -> db.countUnsyncedRows(POSBillingWalaDatabase.PORTION_MASTER_TABLE, "portionMasterStatus")),
            new TableDef(KEY_PORTIONS, R.string.sync_table_portions,
                    db -> db.countUnsyncedRows(POSBillingWalaDatabase.PRODUCT_PORTION_TABLE, "portionStatus")),
            new TableDef(KEY_COMBOS, R.string.sync_table_combos,
                    db -> db.countUnsyncedRows(POSBillingWalaDatabase.COMBO_TABLE, "comboStatus")),
            new TableDef(KEY_COMBO_ITEMS, R.string.sync_table_combo_items,
                    db -> db.countUnsyncedRows(POSBillingWalaDatabase.COMBO_ITEM_TABLE, "comboItemStatus")),
            new TableDef(KEY_PRINTER, R.string.sync_table_printer,
                    db -> db.countUnsyncedRows(POSBillingWalaDatabase.PRINTER_SETTING_TABLE, "settingStatus")),
            new TableDef(KEY_COMPANY, R.string.sync_table_company,
                    db -> db.countUnsyncedRows(POSBillingWalaDatabase.COMPANY_TABLE, "companyStatus")),
            new TableDef(KEY_INVOICE_DELETES, R.string.sync_table_invoice_deletes,
                    db -> db.countAllRows(POSBillingWalaDatabase.INVOICE_PRODUCT_DELETE_QUEUE_TABLE)),
            new TableDef(KEY_INVOICE_ITEMS, R.string.sync_table_invoice_items,
                    db -> db.countUnsyncedRows(POSBillingWalaDatabase.INVOICE_PRODUCT_TABLE, "invoiceProductStatus")),
            new TableDef(KEY_INVOICE_COMBO_ITEMS, R.string.sync_table_invoice_combo_items,
                    db -> db.countUnsyncedRows(POSBillingWalaDatabase.INVOICE_COMBO_ITEM_TABLE, "invoiceComboItemStatus")),
            new TableDef(KEY_INVOICES, R.string.sync_table_invoices,
                    db -> db.countUnsyncedRows(POSBillingWalaDatabase.INVOICE_TABLE, "invoiceStatus")),
            new TableDef(KEY_MESS_MEMBERS, R.string.sync_table_mess_members,
                    db -> db.countUnsyncedRows(POSBillingWalaDatabase.MEMBER_TABLE, "memberStatus")),
            new TableDef(KEY_MESS_PAYMENTS, R.string.sync_table_mess_payments,
                    db -> db.countUnsyncedRows(POSBillingWalaDatabase.MEMBER_PAYMENT_TABLE, "paymentStatus")),
            new TableDef(KEY_MESS_INVOICES, R.string.sync_table_mess_invoices,
                    db -> db.countUnsyncedRows(POSBillingWalaDatabase.MESS_INVOICE_TABLE, "messInvoiceStatus")),
            new TableDef(KEY_INVENTORY, R.string.sync_table_inventory,
                    db -> db.countUnsyncedRows(POSBillingWalaDatabase.INVENTORY_TABLE, "inventoryStatus")),
            new TableDef(KEY_EXPENSES, R.string.sync_table_expenses,
                    db -> db.countUnsyncedRows(POSBillingWalaDatabase.EXPENSES_TABLE, "expensesStatus")),
            new TableDef(KEY_MESS_TOKENS, R.string.sync_table_mess_tokens,
                    POSBillingWalaDatabase::countPendingMessTokens),
            new TableDef(KEY_ERROR_LOGS, R.string.sync_table_error_logs, db -> 0),
    };
}
