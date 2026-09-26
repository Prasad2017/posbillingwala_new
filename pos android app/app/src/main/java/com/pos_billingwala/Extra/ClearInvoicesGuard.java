package com.pos_billingwala.Extra;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.NetworkToOffline.UserSynchronizeData;
import com.pos_billingwala.R;

/**
 * Password + confirmation gate before permanently clearing local invoices.
 */
public final class ClearInvoicesGuard {

    private ClearInvoicesGuard() {
    }

    public static void requestClear(Activity activity, POSBillingWalaDatabase database) {
        if (activity == null || database == null || activity.isFinishing()) {
            return;
        }

        int unsynced = database.countUnsyncedInvoices();
        if (unsynced > 0) {
            Toast.makeText(activity,
                    unsynced + " unsynced bill(s). Upload to cloud first — clear blocked to protect data.",
                    Toast.LENGTH_LONG).show();
            if (DetectConnection.checkInternetConnection(activity)) {
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).openCloudSyncStatus();
                }
                UserSynchronizeData.start(activity, false);
            } else {
                DetectConnection.noInternetConnection(activity);
            }
            return;
        }

        BottomSheetUi.showConfirm(
                activity,
                activity.getString(R.string.ui_delete_all_invoices_title),
                activity.getString(R.string.ui_delete_all_invoices_confirm),
                "YES",
                "NO",
                true,
                () -> promptPassword(activity, database));
    }

    private static void promptPassword(Activity activity, POSBillingWalaDatabase database) {
        View content = LayoutInflater.from(activity).inflate(R.layout.report_password_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, content, false);

        TextView details = content.findViewById(R.id.details);
        TextView continueToReport = content.findViewById(R.id.continueToReport);
        TextView dismissReport = content.findViewById(R.id.dismissReport);
        TextInputEditText reportPin = content.findViewById(R.id.reportPin);

        if (details != null) {
            details.setText(R.string.ui_delete_all_invoices_pin_title);
        }
        dismissReport.setOnClickListener(v -> sheet.dismiss());
        continueToReport.setOnClickListener(v -> {
            String expected = MainActivity.reportPin != null ? MainActivity.reportPin : "9082";
            String entered = reportPin.getText() != null ? reportPin.getText().toString().trim() : "";
            if (entered.equalsIgnoreCase(expected)) {
                sheet.dismiss();
                database.clearInvoice();
                Toast.makeText(activity, activity.getString(R.string.toast_invoice_cleared), Toast.LENGTH_SHORT).show();
            } else {
                reportPin.requestFocus();
                reportPin.setError(activity.getString(R.string.ui_incorrect_pin));
                Toast.makeText(activity, activity.getString(R.string.ui_incorrect_pin), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
