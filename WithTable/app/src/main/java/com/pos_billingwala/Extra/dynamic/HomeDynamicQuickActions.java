package com.pos_billingwala.Extra.dynamic;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.BillingMode;
import com.pos_billingwala.Extra.DynamicUiEngine;
import com.pos_billingwala.Extra.LicenseModules;
import com.pos_billingwala.Extra.RentalModule;
import com.pos_billingwala.Extra.RepairModule;
import com.pos_billingwala.Extra.SalonAppointmentModule;
import com.pos_billingwala.Extra.dynamicui.UiCodes;
import com.pos_billingwala.Fragment.CreatePos;
import com.pos_billingwala.Fragment.Inventory;
import com.pos_billingwala.R;

/**
 * Additive Home quick actions from QuickActionResolver (Phase 03 deepen).
 * Primary billing tiles stay hardcoded; this row shows weight / stock / appointments / etc.
 */
public final class HomeDynamicQuickActions {

    private HomeDynamicQuickActions() {
    }

    public static void bind(Activity activity,
                            @Nullable View root,
                            @Nullable View inventoryCard,
                            @Nullable POSBillingWalaDatabase db) {
        if (activity == null || root == null) {
            return;
        }
        boolean hasFast = DynamicUiEngine.isQuickActionVisible(activity, UiCodes.QA_FAST_BILLING)
                && NavigationRegistry.isVisible(activity, UiCodes.BILLING);
        boolean showWeight = visible(activity, UiCodes.QA_WEIGHT_BILLING, UiCodes.WEIGHT_BILLING) && !hasFast;
        boolean showStock = visible(activity, UiCodes.QA_STOCK, UiCodes.STOCK);
        boolean showAppt = visible(activity, UiCodes.QA_APPOINTMENTS, UiCodes.APPOINTMENTS);
        boolean showCustom = visible(activity, UiCodes.QA_CUSTOM_ORDER, UiCodes.CUSTOM_ORDERS) && !hasFast;
        boolean showBarcode = visible(activity, UiCodes.QA_BARCODE_SCAN, UiCodes.BARCODE) && !hasFast;
        boolean showRepair = RepairModule.isEnabled(activity);
        boolean showRental = RentalModule.isEnabled(activity);

        if (inventoryCard != null) {
            LicenseModules.setVisible(inventoryCard, showStock);
        }

        LinearLayout chipRow = ensureChipRow(activity, root);
        if (chipRow == null) {
            return;
        }
        chipRow.removeAllViews();
        int added = 0;
        if (showWeight) {
            added += addChip(activity, chipRow, activity.getString(R.string.home_qa_weight),
                    v -> openFastBilling(activity)) ? 1 : 0;
        }
        if (showCustom) {
            added += addChip(activity, chipRow, activity.getString(R.string.home_qa_custom_order),
                    v -> openFastBilling(activity)) ? 1 : 0;
        }
        if (showAppt) {
            added += addChip(activity, chipRow, activity.getString(R.string.home_qa_appointments),
                    v -> {
                        if (db != null) {
                            SalonAppointmentModule.showUpcomingDialog(activity, db);
                        }
                    }) ? 1 : 0;
        }
        if (showStock) {
            added += addChip(activity, chipRow, activity.getString(R.string.home_qa_stock),
                    v -> {
                        if (activity instanceof MainActivity) {
                            ((MainActivity) activity).loadFragment(new Inventory(), true);
                        }
                    }) ? 1 : 0;
        }
        if (showBarcode) {
            added += addChip(activity, chipRow, activity.getString(R.string.home_qa_barcode),
                    v -> openFastBilling(activity)) ? 1 : 0;
        }
        if (showRepair) {
            added += addChip(activity, chipRow, activity.getString(R.string.home_qa_repair),
                    v -> RepairModule.showHub(activity, db)) ? 1 : 0;
        }
        if (showRental) {
            added += addChip(activity, chipRow, activity.getString(R.string.home_qa_rental),
                    v -> RentalModule.showHub(activity, db)) ? 1 : 0;
        }

        View scroll = root.findViewById(R.id.homeExtraQuickActions);
        if (scroll != null) {
            LicenseModules.setVisible(scroll, added > 0);
        }
    }

    private static boolean visible(Context context, String qaCode, String navCode) {
        return DynamicUiEngine.isQuickActionVisible(context, qaCode)
                && NavigationRegistry.isVisible(context, navCode);
    }

    private static void openFastBilling(Activity activity) {
        if (!(activity instanceof MainActivity)) {
            return;
        }
        CreatePos createPos = new CreatePos();
        Bundle bundle = new Bundle();
        bundle.putString("tableNumber", "FS" + randomSuffix());
        bundle.putString("cartOrderStatus", BillingMode.FAST.getWireValue());
        createPos.setArguments(bundle);
        ((MainActivity) activity).loadFragment(createPos, true);
    }

    private static String randomSuffix() {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(3);
        for (int i = 0; i < 3; i++) {
            sb.append(alphabet.charAt((int) (Math.random() * alphabet.length())));
        }
        return sb.toString();
    }

    @Nullable
    private static LinearLayout ensureChipRow(Activity activity, View root) {
        View existing = root.findViewById(R.id.homeExtraQuickActionsRow);
        if (existing instanceof LinearLayout) {
            return (LinearLayout) existing;
        }
        View row2 = root.findViewById(R.id.posBillingRow2);
        View row1 = root.findViewById(R.id.posBillingRow1);
        View anchor = row2 != null && row2.getVisibility() != View.GONE ? row2 : row1;
        if (anchor == null || !(anchor.getParent() instanceof ViewGroup)) {
            return null;
        }
        ViewGroup parent = (ViewGroup) anchor.getParent();
        int index = parent.indexOfChild(anchor);
        float density = activity.getResources().getDisplayMetrics().density;

        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setId(R.id.homeExtraQuickActions);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scrollParams.topMargin = (int) (10 * density);
        scroll.setLayoutParams(scrollParams);
        scroll.setVisibility(View.GONE);

        LinearLayout chipRow = new LinearLayout(activity);
        chipRow.setId(R.id.homeExtraQuickActionsRow);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        chipRow.setGravity(Gravity.CENTER_VERTICAL);
        chipRow.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scroll.addView(chipRow);
        parent.addView(scroll, index + 1);
        return chipRow;
    }

    private static boolean addChip(Activity activity, LinearLayout row, String label,
                                   View.OnClickListener click) {
        if (label == null || label.trim().isEmpty()) {
            return false;
        }
        float density = activity.getResources().getDisplayMetrics().density;
        TextView chip = new TextView(activity);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd((int) (8 * density));
        chip.setLayoutParams(lp);
        chip.setText(label);
        chip.setTextColor(ContextCompat.getColor(activity, R.color.colorPrimary));
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        try {
            chip.setTypeface(ResourcesCompat.getFont(activity, R.font.poppinsmedium));
        } catch (Exception ignored) {
        }
        int padH = (int) (14 * density);
        int padV = (int) (8 * density);
        chip.setPadding(padH, padV, padH, padV);
        chip.setBackgroundResource(R.drawable.bg_button_outline);
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setOnClickListener(click);
        row.addView(chip);
        return true;
    }
}
