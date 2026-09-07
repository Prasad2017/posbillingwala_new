package com.pos_billingwala.Extra;

import android.app.Activity;
import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.ProductPriceTierResponse;
import com.pos_billingwala.Model.ProductPortionResponse;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Retail / grocery / wholesale facade.
 * Phase 09: barcode + wholesale qty price tiers; billing reuses {@link BillingMode#FAST}.
 */
public final class RetailGroceryModule {

    public static final String TIER_CART_PREFIX = "tier:";

    private RetailGroceryModule() {
    }

    public static boolean isRetailFamily(Context context) {
        String type = BusinessTypes.normalize(BusinessSession.getBusinessType(context));
        switch (type) {
            case BusinessTypes.RETAIL:
            case BusinessTypes.GROCERY:
            case BusinessTypes.WHOLESALE:
            case BusinessTypes.ELECTRONICS:
            case BusinessTypes.HARDWARE:
            case BusinessTypes.STATIONERY:
            case BusinessTypes.PET_SHOP:
            case BusinessTypes.RENTAL:
                return true;
            default:
                return false;
        }
    }

    public static boolean canBarcode(Context context) {
        return FeatureEngine.isEnabled(context, FeatureFlags.BARCODE);
    }

    public static boolean canWholesalePricing(Context context) {
        return FeatureEngine.isEnabled(context, FeatureFlags.WHOLESALE_PRICING);
    }

    public static boolean canInventory(Context context) {
        return FeatureEngine.isEnabled(context, FeatureFlags.INVENTORY);
    }

    /**
     * True when search Enter / scanner terminator should try exact {@code productCode} add-to-cart.
     * Off for restaurant default (barcode flag not in template).
     */
    public static boolean shouldAutoAddOnBarcodeScan(Context context) {
        return canBarcode(context);
    }

    public static boolean shouldOfferTierPicker(Context context, boolean productHasTiers) {
        return canWholesalePricing(context) && productHasTiers;
    }

    public static BillingMode preferredBillingMode() {
        return BillingMode.FAST;
    }

    public static String displayLabel(ProductPriceTierResponse t) {
        if (t == null) {
            return "";
        }
        String label = t.getTierLabel() != null ? t.getTierLabel().trim() : "";
        String min = t.getMinQty() != null ? t.getMinQty().trim() : "1";
        String price = t.getTierPrice() != null ? t.getTierPrice().trim() : "0";
        if (!label.isEmpty()) {
            return label + " · Qty " + min + "+ @ " + price;
        }
        return "Qty " + min + "+ @ " + price;
    }

    public static ProductPortionResponse asPseudoPortion(ProductPriceTierResponse t) {
        ProductPortionResponse p = new ProductPortionResponse();
        if (t == null) {
            return p;
        }
        p.setPortionId(TIER_CART_PREFIX + (t.getTierId() != null ? t.getTierId() : ""));
        p.setProductId(t.getProductId());
        p.setPortionName(displayLabel(t));
        p.setPortionPrice(t.getTierPrice() != null ? t.getTierPrice() : "0");
        return p;
    }

    public static List<ProductPortionResponse> asPseudoPortions(ProductResponse product,
                                                                 List<ProductPriceTierResponse> tiers) {
        List<ProductPortionResponse> list = new ArrayList<>();
        if (product != null) {
            ProductPortionResponse retail = new ProductPortionResponse();
            retail.setPortionId(TIER_CART_PREFIX + "retail");
            retail.setProductId(product.getProductId());
            String base = product.getProductPrice() != null ? product.getProductPrice() : "0";
            retail.setPortionName("Retail @ " + base);
            retail.setPortionPrice(base);
            list.add(retail);
        }
        if (tiers != null) {
            for (ProductPriceTierResponse t : tiers) {
                list.add(asPseudoPortion(t));
            }
        }
        return list;
    }

    public static void showManageTiersDialog(Activity activity, POSBillingWalaDatabase db, String productId) {
        if (activity == null || db == null || productId == null) {
            return;
        }
        List<ProductPriceTierResponse> tiers = db.getProductPriceTier(productId);
        StringBuilder body = new StringBuilder();
        if (tiers.isEmpty()) {
            body.append(activity.getString(R.string.tier_none_yet));
        } else {
            for (ProductPriceTierResponse t : tiers) {
                body.append("• ").append(displayLabel(t)).append('\n');
            }
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.tier_manage_title)
                .setMessage(body.toString())
                .setPositiveButton(R.string.tier_add, (d, w) -> showAddTierDialog(activity, db, productId))
                .setNeutralButton(R.string.tier_delete_last, (d, w) -> {
                    if (!tiers.isEmpty()) {
                        db.softDeleteProductPriceTier(tiers.get(tiers.size() - 1).getTierId());
                        Toast.makeText(activity, R.string.tier_deleted, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static void showAddTierDialog(Activity activity, POSBillingWalaDatabase db, String productId) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, pad / 2);
        EditText minQty = new EditText(activity);
        minQty.setHint(activity.getString(R.string.tier_min_qty_hint));
        minQty.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        minQty.setSingleLine(true);
        minQty.setText("10");
        EditText price = new EditText(activity);
        price.setHint(activity.getString(R.string.tier_price_hint));
        price.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        price.setSingleLine(true);
        EditText label = new EditText(activity);
        label.setHint(activity.getString(R.string.tier_label_hint));
        label.setSingleLine(true);
        box.addView(minQty);
        box.addView(price);
        box.addView(label);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.tier_add)
                .setView(box)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String p = price.getText() != null ? price.getText().toString().trim() : "";
                    String q = minQty.getText() != null ? minQty.getText().toString().trim() : "1";
                    if (p.isEmpty()) {
                        Toast.makeText(activity, R.string.ui_enter_amount, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String lbl = label.getText() != null ? label.getText().toString().trim() : "";
                    db.addProductPriceTier(productId, q, p, lbl);
                    Toast.makeText(activity, R.string.tier_saved, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public static String moduleSummary(Context context) {
        return "Retail family: " + (isRetailFamily(context) ? "yes" : "no")
                + "\nBarcode: " + (canBarcode(context)
                ? "on (Enter + camera → exact code)" : "off")
                + " · Wholesale tiers: " + (canWholesalePricing(context) ? "on" : "off")
                + "\nBilling: " + preferredBillingMode().getWireValue()
                + " · Inventory: " + (canInventory(context) ? "on" : "off");
    }
}
