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
 * Rental deposit / return facade (Dynamic Phase 08).
 * Return date + deposit into cart portionName; reuses custom-order deposit ledger when amount set.
 */
public final class RentalModule {

    public static final String RENTAL_PREFIX = "Rental: ";

    public interface RentalCallback {
        void onReady(ProductPortionResponse noteAsPortion);
    }

    private RentalModule() {
    }

    public static boolean isEnabled(Context context) {
        String type = BusinessTypes.normalize(BusinessSession.getBusinessType(context));
        return BusinessTypes.RENTAL.equals(type);
    }

    public static boolean usesDeposits(Context context) {
        return isEnabled(context);
    }

    public static boolean shouldPromptRental(Context context) {
        return isEnabled(context);
    }

    public static BillingMode preferredBillingMode() {
        return BillingMode.FAST;
    }

    public static void showRentalDialog(Activity activity, ProductResponse product,
                                        ProductPortionResponse existingPortion,
                                        RentalCallback callback) {
        if (activity == null || product == null || callback == null) {
            return;
        }
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, pad / 2);

        EditText customer = new EditText(activity);
        customer.setHint(activity.getString(R.string.rental_customer_hint));
        customer.setSingleLine(true);

        EditText returnDate = new EditText(activity);
        returnDate.setHint(activity.getString(R.string.rental_return_hint));
        returnDate.setSingleLine(true);

        EditText deposit = new EditText(activity);
        deposit.setHint(activity.getString(R.string.rental_deposit_hint));
        deposit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        deposit.setSingleLine(true);

        EditText notes = new EditText(activity);
        notes.setHint(activity.getString(R.string.rental_notes_hint));
        notes.setMinLines(2);

        box.addView(customer);
        box.addView(returnDate);
        box.addView(deposit);
        box.addView(notes);

        new AlertDialog.Builder(activity)
                .setTitle(R.string.rental_dialog_title)
                .setView(box)
                .setNegativeButton(R.string.custom_order_skip, (d, w) -> callback.onReady(existingPortion))
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String cust = text(customer);
                    String due = text(returnDate);
                    if (cust.isEmpty() && due.isEmpty()) {
                        Toast.makeText(activity, R.string.rental_details_required, Toast.LENGTH_SHORT).show();
                        showRentalDialog(activity, product, existingPortion, callback);
                        return;
                    }
                    callback.onReady(buildRentalPortion(activity, product, existingPortion,
                            cust, due, text(deposit), text(notes)));
                })
                .show();
    }

    public static void showDepositLedger(Activity activity) {
        CakeBakeryModule.showDepositLedger(activity);
    }

    public static void showHub(Activity activity, POSBillingWalaDatabase db) {
        if (activity == null) {
            return;
        }
        String[] labels = new String[]{
                activity.getString(R.string.rental_hub_new_bill),
                activity.getString(R.string.rental_hub_deposits),
                activity.getString(R.string.rental_hub_recent)
        };
        BottomSheetUi.showSingleChoice(activity, activity.getString(R.string.rental_hub_title),
                labels, -1, true, index -> {
                    if (index == 0) {
                        openFastBilling(activity);
                    } else if (index == 1) {
                        showDepositLedger(activity);
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
        java.util.List<String> notes = db.listRecentInvoicePortionNotes(RENTAL_PREFIX, 30);
        if (notes.isEmpty()) {
            Toast.makeText(activity, R.string.deposit_ledger_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        BottomSheetUi.showSingleChoice(activity, activity.getString(R.string.rental_hub_recent),
                notes.toArray(new String[0]), -1, true, index -> { });
    }

    private static String text(EditText edit) {
        return edit.getText() != null ? edit.getText().toString().trim() : "";
    }

    private static ProductPortionResponse buildRentalPortion(Activity activity,
                                                             ProductResponse product,
                                                             ProductPortionResponse existing,
                                                             String customer, String returnDue,
                                                             String dep, String notes) {
        StringBuilder label = new StringBuilder(RENTAL_PREFIX);
        if (!customer.isEmpty()) {
            label.append(customer);
        } else {
            label.append(product.getProductName());
        }
        if (!returnDue.isEmpty()) {
            label.append(" | Return ").append(returnDue);
        }
        if (!dep.isEmpty()) {
            label.append(" | Dep ").append(dep);
        }
        if (!notes.isEmpty()) {
            label.append(" | ").append(notes);
        }
        ProductPortionResponse p = existing != null ? existing : new ProductPortionResponse();
        if (existing == null) {
            p.setPortionId("rental:" + System.currentTimeMillis());
            p.setPortionPrice(product.getProductPrice());
        }
        String base = existing != null && existing.getPortionName() != null
                ? existing.getPortionName() + " · " : "";
        p.setPortionName(base + label);
        p.setProductId(product.getProductId());

        if (activity != null && !dep.isEmpty()) {
            try {
                new POSBillingWalaDatabase(activity).addCustomOrderDeposit(
                        product.getProductName(),
                        customer.isEmpty() ? notes : customer,
                        dep,
                        returnDue,
                        "");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return p;
    }

    public static String moduleSummary(Context context) {
        return "RentalModule: " + (isEnabled(context) ? "on" : "off")
                + " · deposits=" + usesDeposits(context)
                + " · registry=" + ModuleRegistry.RENTAL;
    }
}
