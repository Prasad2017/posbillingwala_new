package com.pos_billingwala.Extra;

import android.app.Activity;
import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.ProductPortionResponse;
import com.pos_billingwala.Model.ProductVariantResponse;
import com.pos_billingwala.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Fashion / jewellery variants — separate {@code product_variant} table (not portions).
 * Cart stores variant via portionId prefix {@link #CART_KEY_PREFIX} + display label in portionName.
 */
public final class FashionJewelleryModule {

    public static final String CART_KEY_PREFIX = "var:";

    private FashionJewelleryModule() {
    }

    public static boolean isEnabled(Context context) {
        return FeatureEngine.isEnabled(context, FeatureFlags.VARIANTS);
    }

    public static boolean isFashionFamily(Context context) {
        String type = BusinessTypes.normalize(BusinessSession.getBusinessType(context));
        return BusinessTypes.FASHION.equals(type) || BusinessTypes.JEWELLERY.equals(type);
    }

    public static boolean shouldOfferVariantPicker(Context context, boolean productHasVariants) {
        return isEnabled(context) && productHasVariants;
    }

    public static String displayLabel(ProductVariantResponse v) {
        if (v == null) {
            return "";
        }
        String size = v.getVariantSize() != null ? v.getVariantSize().trim() : "";
        String color = v.getVariantColor() != null ? v.getVariantColor().trim() : "";
        String sku = v.getVariantSku() != null ? v.getVariantSku().trim() : "";
        StringBuilder sb = new StringBuilder();
        if (!size.isEmpty()) {
            sb.append(size);
        }
        if (!color.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" / ");
            }
            sb.append(color);
        }
        if (sb.length() == 0 && !sku.isEmpty()) {
            sb.append(sku);
        } else if (!sku.isEmpty()) {
            sb.append(" (").append(sku).append(")");
        }
        return sb.length() > 0 ? sb.toString() : "Variant";
    }

    public static String cartKey(ProductVariantResponse v) {
        if (v == null || v.getVariantId() == null) {
            return null;
        }
        return CART_KEY_PREFIX + v.getVariantId();
    }

    public static boolean isVariantCartKey(String portionId) {
        return portionId != null && portionId.startsWith(CART_KEY_PREFIX);
    }

    /** Adapt variant → portion shape so CreatePos can reuse the portion picker UI. */
    public static ProductPortionResponse asPseudoPortion(ProductVariantResponse v) {
        ProductPortionResponse p = new ProductPortionResponse();
        if (v == null) {
            return p;
        }
        p.setPortionId(cartKey(v));
        p.setProductId(v.getProductId());
        p.setPortionName(displayLabel(v));
        String price = v.getVariantPrice();
        p.setPortionPrice(price != null && !price.trim().isEmpty() ? price.trim() : "0");
        return p;
    }

    public static List<ProductPortionResponse> asPseudoPortions(List<ProductVariantResponse> variants) {
        List<ProductPortionResponse> list = new ArrayList<>();
        if (variants == null) {
            return list;
        }
        for (ProductVariantResponse v : variants) {
            list.add(asPseudoPortion(v));
        }
        return list;
    }

    public static void showManageVariantsDialog(Activity activity, POSBillingWalaDatabase db, String productId) {
        if (activity == null || db == null || productId == null) {
            return;
        }
        List<ProductVariantResponse> variants = db.getProductVariantList(productId);
        StringBuilder body = new StringBuilder();
        if (variants.isEmpty()) {
            body.append(activity.getString(R.string.variant_none_yet));
        } else {
            for (ProductVariantResponse v : variants) {
                body.append("• ").append(displayLabel(v))
                        .append(" — ").append(MainActivityCurrency(activity)).append(" ")
                        .append(v.getVariantPrice() != null ? v.getVariantPrice() : "0")
                        .append("\n");
            }
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.variant_manage_title)
                .setMessage(body.toString())
                .setPositiveButton(R.string.variant_add, (d, w) -> showAddVariantDialog(activity, db, productId))
                .setNeutralButton(R.string.variant_delete_last, (d, w) -> {
                    if (!variants.isEmpty()) {
                        db.softDeleteProductVariant(variants.get(variants.size() - 1).getVariantId());
                        Toast.makeText(activity, R.string.variant_deleted, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static String MainActivityCurrency(Activity activity) {
        try {
            return com.pos_billingwala.Activity.MainActivity.currencyName != null
                    ? com.pos_billingwala.Activity.MainActivity.currencyName : "₹";
        } catch (Exception e) {
            return "₹";
        }
    }

    private static void showAddVariantDialog(Activity activity, POSBillingWalaDatabase db, String productId) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, pad / 2);
        EditText size = field(activity, activity.getString(R.string.variant_size_hint));
        EditText color = field(activity, activity.getString(R.string.variant_color_hint));
        EditText sku = field(activity, activity.getString(R.string.variant_sku_hint));
        EditText price = field(activity, activity.getString(R.string.variant_price_hint));
        price.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        box.addView(size);
        box.addView(color);
        box.addView(sku);
        box.addView(price);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.variant_add)
                .setView(box)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String p = price.getText() != null ? price.getText().toString().trim() : "";
                    if (p.isEmpty()) {
                        Toast.makeText(activity, R.string.ui_enter_amount, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.addProductVariant(productId,
                            text(size), text(color), text(sku), p);
                    Toast.makeText(activity, R.string.variant_saved, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static EditText field(Activity activity, String hint) {
        EditText e = new EditText(activity);
        e.setHint(hint);
        e.setSingleLine(true);
        return e;
    }

    private static String text(EditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }

    public static String moduleSummary(Context context) {
        return "Variants: " + (isEnabled(context) ? "on (product_variant + cloud sync)" : "off")
                + "\nFashion/Jewellery template: " + (isFashionFamily(context) ? "yes" : "no")
                + "\nCart key prefix: " + CART_KEY_PREFIX + " (not product_portion)";
    }
}
