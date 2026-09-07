package com.pos_billingwala.Extra;

import android.app.Activity;
import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.dynamic.ModuleRegistry;
import com.pos_billingwala.Fragment.CreatePos;
import com.pos_billingwala.Model.ProductPortionResponse;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.R;

/**
 * Repair job facade (Dynamic Phase 08).
 * Job note + device serial into cart portionName; appointments when APPOINTMENTS is on.
 */
public final class RepairModule {

    public static final String JOB_PREFIX = "Repair: ";

    public interface JobCallback {
        void onReady(ProductPortionResponse noteAsPortion);
    }

    private RepairModule() {
    }

    public static boolean isEnabled(Context context) {
        String type = BusinessTypes.normalize(BusinessSession.getBusinessType(context));
        return BusinessTypes.REPAIR.equals(type);
    }

    public static boolean usesAppointments(Context context) {
        return isEnabled(context) && FeatureEngine.isEnabled(context, FeatureFlags.APPOINTMENTS);
    }

    public static boolean supportsSerial(Context context) {
        return isEnabled(context);
    }

    public static boolean shouldPromptJob(Context context) {
        return isEnabled(context);
    }

    public static BillingMode preferredBillingMode() {
        return BillingMode.FAST;
    }

    public static void showHub(Activity activity, POSBillingWalaDatabase db) {
        if (activity == null) {
            return;
        }
        java.util.List<String> items = new java.util.ArrayList<>();
        items.add(activity.getString(R.string.repair_hub_new_bill));
        items.add(activity.getString(R.string.repair_hub_appointments));
        items.add(activity.getString(R.string.repair_hub_recent));
        String[] labels = items.toArray(new String[0]);
        BottomSheetUi.showSingleChoice(activity, activity.getString(R.string.repair_hub_title),
                labels, -1, true, index -> {
                    if (index == 0) {
                        openFastBilling(activity);
                    } else if (index == 1) {
                        if (db != null) {
                            SalonAppointmentModule.showUpcomingDialog(activity, db);
                        }
                    } else if (index == 2) {
                        showRecentNotes(activity, db);
                    }
                });
    }

    private static void openFastBilling(Activity activity) {
        if (!(activity instanceof MainActivity)) {
            return;
        }
        CreatePos createPos = new CreatePos();
        android.os.Bundle bundle = new android.os.Bundle();
        bundle.putString("tableNumber", "FS" + System.currentTimeMillis() % 1000);
        bundle.putString("cartOrderStatus", BillingMode.FAST.getWireValue());
        createPos.setArguments(bundle);
        ((MainActivity) activity).loadFragment(createPos, true);
    }

    private static void showRecentNotes(Activity activity, POSBillingWalaDatabase db) {
        if (db == null) {
            return;
        }
        java.util.List<String> notes = db.listRecentInvoicePortionNotes(JOB_PREFIX, 30);
        if (notes.isEmpty()) {
            Toast.makeText(activity, R.string.appointment_none, Toast.LENGTH_SHORT).show();
            return;
        }
        BottomSheetUi.showSingleChoice(activity, activity.getString(R.string.repair_hub_recent),
                notes.toArray(new String[0]), -1, true, index -> { });
    }

    public static void showJobDialog(Activity activity, ProductResponse product,
                                     ProductPortionResponse existingPortion,
                                     JobCallback callback) {
        if (activity == null || product == null || callback == null) {
            return;
        }
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, pad / 2);

        EditText customer = new EditText(activity);
        customer.setHint(activity.getString(R.string.repair_customer_hint));
        customer.setSingleLine(true);

        EditText device = new EditText(activity);
        device.setHint(activity.getString(R.string.repair_device_hint));
        device.setSingleLine(true);

        EditText serial = new EditText(activity);
        serial.setHint(activity.getString(R.string.repair_serial_hint));
        serial.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        serial.setSingleLine(true);

        EditText issue = new EditText(activity);
        issue.setHint(activity.getString(R.string.repair_issue_hint));
        issue.setMinLines(2);

        box.addView(customer);
        box.addView(device);
        box.addView(serial);
        box.addView(issue);

        new AlertDialog.Builder(activity)
                .setTitle(R.string.repair_job_title)
                .setView(box)
                .setNegativeButton(R.string.custom_order_skip, (d, w) -> callback.onReady(existingPortion))
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String cust = text(customer);
                    String issueText = text(issue);
                    if (cust.isEmpty() && issueText.isEmpty()) {
                        Toast.makeText(activity, R.string.repair_details_required, Toast.LENGTH_SHORT).show();
                        showJobDialog(activity, product, existingPortion, callback);
                        return;
                    }
                    callback.onReady(buildJobPortion(product, existingPortion, cust, text(device),
                            text(serial), issueText));
                })
                .show();
    }

    private static String text(EditText edit) {
        return edit.getText() != null ? edit.getText().toString().trim() : "";
    }

    private static ProductPortionResponse buildJobPortion(ProductResponse product,
                                                          ProductPortionResponse existing,
                                                          String customer, String device,
                                                          String serial, String issue) {
        StringBuilder label = new StringBuilder(JOB_PREFIX);
        if (!customer.isEmpty()) {
            label.append(customer);
        } else {
            label.append(product.getProductName());
        }
        if (!device.isEmpty()) {
            label.append(" | ").append(device);
        }
        if (!serial.isEmpty()) {
            label.append(" | S/N ").append(serial);
        }
        if (!issue.isEmpty()) {
            label.append(" | ").append(issue);
        }
        ProductPortionResponse p = existing != null ? existing : new ProductPortionResponse();
        if (existing == null) {
            p.setPortionId("repair:" + System.currentTimeMillis());
            p.setPortionPrice(product.getProductPrice());
        }
        String base = existing != null && existing.getPortionName() != null
                ? existing.getPortionName() + " · " : "";
        p.setPortionName(base + label);
        p.setProductId(product.getProductId());
        return p;
    }

    public static String moduleSummary(Context context) {
        return "RepairModule: " + (isEnabled(context) ? "on" : "off")
                + " · appointments=" + usesAppointments(context)
                + " · serial=" + supportsSerial(context)
                + " · registry=" + ModuleRegistry.REPAIR;
    }
}
