package com.pos_billingwala.Extra;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Fragment.Inventory;
import com.pos_billingwala.Model.ComboItemResponse;
import com.pos_billingwala.Model.InventoryResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Model.ProductCartResponse;
import com.pos_billingwala.R;

import java.util.List;
import java.util.Random;

/**
 * Inventory / stock engine over the live {@code inventory} table.
 * Sale deduct matches BluetoothPrint behaviour: if a product has an inventory row,
 * append a new balance row (afterSale − sold). Combos deduct component products.
 * <p>
 * Phase 15: facade + FeatureEngine gate — no schema rewrite.
 */
public final class InventoryStockEngine {

    /** Matches {@code getLowInventoryList} threshold. */
    public static final int LOW_STOCK_THRESHOLD = 6;

    private InventoryStockEngine() {
    }

    public static boolean isEnabled(Context context) {
        return FeatureEngine.isEnabled(context, FeatureFlags.INVENTORY);
    }

    /**
     * Soft gate for Inventory UI. Returns false if the caller should stop.
     */
    public static boolean ensureEnabled(Activity activity) {
        if (activity == null) {
            return false;
        }
        if (isEnabled(activity)) {
            return true;
        }
        Toast.makeText(activity, R.string.setting_hint_inventory, Toast.LENGTH_SHORT).show();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).navigateBack();
        } else {
            activity.finish();
        }
        return false;
    }

    public static boolean openHub(Activity activity) {
        if (activity == null || !(activity instanceof MainActivity)) {
            return false;
        }
        if (!isEnabled(activity)) {
            Toast.makeText(activity, R.string.setting_hint_inventory, Toast.LENGTH_SHORT).show();
            return false;
        }
        ((MainActivity) activity).loadFragment(new Inventory(), true);
        return true;
    }

    /**
     * Legacy printer toggle {@code productQuantityUpdate} — stored in settings but not
     * historically checked on sale. Exposed for future hardening; deduct today ignores it.
     */
    public static boolean isProductQuantityUpdateFlagOn(POSBillingWalaDatabase db) {
        if (db == null) {
            return false;
        }
        List<PrinterSettingResponse> list = db.getPrinterSettingDetails();
        if (list == null || list.isEmpty()) {
            return false;
        }
        return CompanyFeatureToggles.isOn(list.get(0).getProductQuantityUpdate());
    }

    public static Integer availableQty(POSBillingWalaDatabase db, String productId) {
        InventoryResponse latest = latestForProduct(db, productId);
        if (latest == null || latest.getAfterSaleInventoryQuantity() == null) {
            return null;
        }
        try {
            return Integer.parseInt(latest.getAfterSaleInventoryQuantity().trim());
        } catch (Exception e) {
            return null;
        }
    }

    public static InventoryResponse latestForProduct(POSBillingWalaDatabase db, String productId) {
        if (db == null || productId == null || productId.trim().isEmpty()) {
            return null;
        }
        List<InventoryResponse> list = db.getInventoryDetails(productId);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    public static boolean isLowStock(POSBillingWalaDatabase db, String productId) {
        Integer qty = availableQty(db, productId);
        return qty != null && qty < LOW_STOCK_THRESHOLD;
    }

    /**
     * Deduct stock for a full cart snapshot during invoice save.
     * No-op when inventory feature is off for the current template.
     */
    public static void deductCartOnSale(Context context, POSBillingWalaDatabase db,
                                        List<ProductCartResponse> cartSnapshot, String inventoryDate) {
        if (db == null || cartSnapshot == null || cartSnapshot.isEmpty()) {
            return;
        }
        if (!isEnabled(context)) {
            return;
        }
        String date = inventoryDate != null ? inventoryDate : "";
        for (ProductCartResponse line : cartSnapshot) {
            if (line == null) {
                continue;
            }
            if (CartItemType.isCombo(line.getCartItemType())) {
                deductComboLine(db, line, date);
            } else {
                deductProduct(db, line.getProductId(), parseQty(line.getProductQuantity()), date);
            }
        }
    }

    public static void deductProduct(POSBillingWalaDatabase db, String productId, int saleQty, String inventoryDate) {
        if (db == null || productId == null || productId.trim().isEmpty() || saleQty == 0) {
            return;
        }
        List<InventoryResponse> inventoryList = db.getInventoryDetails(productId);
        if (inventoryList == null || inventoryList.isEmpty()) {
            return;
        }
        for (InventoryResponse inventoryResponse : inventoryList) {
            try {
                int oldInventoryQty = Integer.parseInt(inventoryResponse.getProductInventoryQuantity());
                int afterSaleInventoryQuantity = Integer.parseInt(inventoryResponse.getAfterSaleInventoryQuantity());
                int totalQty = afterSaleInventoryQuantity - saleQty;
                db.addInventory(
                        productId,
                        String.valueOf(oldInventoryQty),
                        String.valueOf(totalQty),
                        String.valueOf(saleQty),
                        inventoryDate,
                        0,
                        newSyncKey());
            } catch (Exception ignored) {
            }
        }
    }

    private static void deductComboLine(POSBillingWalaDatabase db, ProductCartResponse comboLine, String inventoryDate) {
        int comboQty = Math.max(1, parseQty(comboLine.getProductQuantity()));
        List<ComboItemResponse> components = db.getCartComboItems(comboLine.getCartId());
        if (components == null) {
            return;
        }
        for (ComboItemResponse component : components) {
            if (component.getProductId() == null || component.getProductId().trim().isEmpty()) {
                continue;
            }
            int componentQty = Math.max(1, parseQty(component.getComboItemQuantity()));
            deductProduct(db, component.getProductId(), comboQty * componentQty, inventoryDate);
        }
    }

    /**
     * Stock-in (Add Inventory) — mirrors {@code AddInventory.addInventory()} exactly.
     */
    public static void stockIn(POSBillingWalaDatabase db, String productId, int addQty, String inventoryDate) {
        if (db == null || productId == null || addQty <= 0) {
            return;
        }
        List<InventoryResponse> existing = db.getInventoryDetails(productId);
        if (existing != null && !existing.isEmpty()) {
            try {
                int afterSaleInventoryQuantity = Integer.parseInt(existing.get(0).getAfterSaleInventoryQuantity());
                int totalQty = addQty + afterSaleInventoryQuantity;
                db.addInventory(productId, String.valueOf(totalQty), "0", "0", inventoryDate, 0, newSyncKey());
            } catch (Exception ignored) {
            }
        } else {
            String qty = String.valueOf(addQty);
            db.addInventory(productId, qty, qty, "0", inventoryDate, 0, newSyncKey());
        }
    }

    public static String moduleSummary(Context context) {
        return "Inventory: " + (isEnabled(context) ? "on" : "off")
                + "\nTable: inventory (qty in / after sale / sale qty)"
                + "\nSale deduct: when feature on and product has inventory row"
                + "\nLow stock: afterSale < " + LOW_STOCK_THRESHOLD
                + "\nUI: Settings → Inventory";
    }

    private static int parseQty(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 1;
        }
        try {
            return (int) Float.parseFloat(raw.trim());
        } catch (Exception e) {
            return 1;
        }
    }

    private static String newSyncKey() {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random r = new Random();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(alphabet.charAt(r.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
