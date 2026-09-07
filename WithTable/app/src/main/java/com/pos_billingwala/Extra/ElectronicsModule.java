package com.pos_billingwala.Extra;

import android.app.Activity;
import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.pos_billingwala.Extra.dynamic.ModuleRegistry;
import com.pos_billingwala.Model.ProductPortionResponse;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.R;

/**
 * Electronics / mobile facade (Dynamic Phase 08).
 * Serial / IMEI captured into cart portionName (no new cart column) — same pattern as custom orders.
 */
public final class ElectronicsModule {

    public static final String SERIAL_PREFIX = "Serial: ";

    public interface SerialCallback {
        void onReady(ProductPortionResponse noteAsPortion);
    }

    private ElectronicsModule() {
    }

    public static boolean isEnabled(Context context) {
        String type = BusinessTypes.normalize(BusinessSession.getBusinessType(context));
        return BusinessTypes.ELECTRONICS.equals(type)
                || BusinessTypes.HARDWARE.equals(type);
    }

    public static boolean supportsSerial(Context context) {
        return isEnabled(context);
    }

    public static boolean supportsWarranty(Context context) {
        return isEnabled(context);
    }

    public static boolean shouldPromptSerial(Context context) {
        return supportsSerial(context);
    }

    public static BillingMode preferredBillingMode() {
        return BillingMode.FAST;
    }

    public static void showSerialDialog(Activity activity, ProductResponse product,
                                        ProductPortionResponse existingPortion,
                                        SerialCallback callback) {
        if (activity == null || product == null || callback == null) {
            return;
        }
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, pad / 2);

        EditText serial = new EditText(activity);
        serial.setHint(activity.getString(R.string.electronics_serial_hint));
        serial.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        serial.setSingleLine(true);

        EditText warranty = new EditText(activity);
        warranty.setHint(activity.getString(R.string.electronics_warranty_hint));
        warranty.setSingleLine(true);

        box.addView(serial);
        box.addView(warranty);

        new AlertDialog.Builder(activity)
                .setTitle(R.string.electronics_serial_title)
                .setView(box)
                .setNegativeButton(R.string.custom_order_skip, (d, w) -> callback.onReady(existingPortion))
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String s = serial.getText() != null ? serial.getText().toString().trim() : "";
                    String war = warranty.getText() != null ? warranty.getText().toString().trim() : "";
                    if (s.isEmpty()) {
                        Toast.makeText(activity, R.string.electronics_serial_required, Toast.LENGTH_SHORT).show();
                        showSerialDialog(activity, product, existingPortion, callback);
                        return;
                    }
                    callback.onReady(buildSerialPortion(product, existingPortion, s, war));
                })
                .show();
    }

    private static ProductPortionResponse buildSerialPortion(ProductResponse product,
                                                             ProductPortionResponse existing,
                                                             String serial, String warranty) {
        StringBuilder label = new StringBuilder(SERIAL_PREFIX).append(serial);
        if (warranty != null && !warranty.isEmpty()) {
            label.append(" | Warranty ").append(warranty);
        }
        ProductPortionResponse p = existing != null ? existing : new ProductPortionResponse();
        if (existing == null) {
            p.setPortionId("serial:" + System.currentTimeMillis());
            p.setPortionPrice(product.getProductPrice());
        }
        String base = existing != null && existing.getPortionName() != null
                ? existing.getPortionName() + " · " : "";
        p.setPortionName(base + label);
        p.setProductId(product.getProductId());
        return p;
    }

    public static String moduleSummary(Context context) {
        return "ElectronicsModule: " + (isEnabled(context) ? "on" : "off")
                + " · serial=" + supportsSerial(context)
                + " · barcode=" + FeatureEngine.isEnabled(context, FeatureFlags.BARCODE)
                + " · registry=" + ModuleRegistry.ELECTRONICS;
    }
}
