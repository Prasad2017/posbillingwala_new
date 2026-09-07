package com.pos_billingwala.Extra.dynamic;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.pos_billingwala.Activity.CompanyPrinterSetting;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.BillingMode;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.Extra.MessModule;
import com.pos_billingwala.Extra.dynamicui.UiCodes;
import com.pos_billingwala.Fragment.CreatePos;
import com.pos_billingwala.Fragment.Expenses;
import com.pos_billingwala.Fragment.Inventory;
import com.pos_billingwala.Fragment.InvoiceCompanyTable;
import com.pos_billingwala.Fragment.InvoiceTakeAway;
import com.pos_billingwala.Fragment.MasterData;
import com.pos_billingwala.Fragment.ProductMaster;
import com.pos_billingwala.Fragment.ReportsHub;
import com.pos_billingwala.Fragment.UserSetting;
import com.pos_billingwala.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Soft "More screens" sheet from NavigationRegistry (Phase 03 close-out).
 * Additive — does not replace Home tiles or introduce BottomNavigationView.
 */
public final class DynamicNavigationSheet {

    private DynamicNavigationSheet() {
    }

    public static void show(Activity activity) {
        if (!(activity instanceof MainActivity)) {
            return;
        }
        List<String> codes = NavigationRegistry.visibleCodes(activity);
        if (codes == null || codes.isEmpty()) {
            return;
        }
        List<String> labels = new ArrayList<>();
        List<String> targets = new ArrayList<>();
        for (String code : codes) {
            if (UiCodes.HOME.equals(code)) {
                continue;
            }
            String title = titleFor(activity, code);
            if (title == null) {
                continue;
            }
            labels.add(title);
            targets.add(code);
        }
        if (labels.isEmpty()) {
            return;
        }
        BottomSheetUi.showSingleChoice(activity, activity.getString(R.string.nav_more_title),
                labels.toArray(new String[0]), -1, true, index -> {
                    if (index < 0 || index >= targets.size()) {
                        return;
                    }
                    open(activity, targets.get(index));
                });
    }

    @Nullable
    private static String titleFor(Activity activity, String code) {
        if (UiCodes.BILLING.equals(code) || UiCodes.WEIGHT_BILLING.equals(code)
                || UiCodes.BARCODE.equals(code)) {
            return activity.getString(R.string.fast_billing);
        }
        if (UiCodes.TABLE_MANAGEMENT.equals(code) || UiCodes.ORDERS.equals(code)) {
            return activity.getString(R.string.dine_in);
        }
        if (UiCodes.TAKE_AWAY.equals(code)) {
            return activity.getString(R.string.take_away);
        }
        if (UiCodes.MESS.equals(code)) {
            return "Mess";
        }
        if (UiCodes.PRODUCTS.equals(code) || UiCodes.SERVICES.equals(code)) {
            return activity.getString(R.string.ui_products);
        }
        if (UiCodes.STOCK.equals(code) || UiCodes.INVENTORY.equals(code)) {
            return activity.getString(R.string.ui_inventory);
        }
        if (UiCodes.REPORTS.equals(code)) {
            return activity.getString(R.string.ui_reports_hub);
        }
        if (UiCodes.SETTINGS.equals(code)) {
            return activity.getString(R.string.setting_shop_details);
        }
        if (UiCodes.MASTER_DATA.equals(code) || UiCodes.APPOINTMENTS.equals(code)
                || UiCodes.CUSTOM_ORDERS.equals(code)) {
            return activity.getString(R.string.setting_master_data);
        }
        if (UiCodes.PRINTER.equals(code)) {
            return activity.getString(R.string.setting_printer_details);
        }
        if (UiCodes.EXPENSES.equals(code)) {
            return activity.getString(R.string.setting_expense);
        }
        return code;
    }

    private static void open(Activity activity, String code) {
        MainActivity main = (MainActivity) activity;
        if (!NavigationRegistry.isVisible(activity, code)) {
            ScreenRegistry.guardOpen(activity, code);
            return;
        }
        if (UiCodes.BILLING.equals(code) || UiCodes.WEIGHT_BILLING.equals(code)
                || UiCodes.BARCODE.equals(code)) {
            CreatePos createPos = new CreatePos();
            Bundle bundle = new Bundle();
            bundle.putString("tableNumber", "FS" + (System.currentTimeMillis() % 1000));
            bundle.putString("cartOrderStatus", BillingMode.FAST.getWireValue());
            createPos.setArguments(bundle);
            main.loadFragment(createPos, true);
            return;
        }
        if (UiCodes.TABLE_MANAGEMENT.equals(code) || UiCodes.ORDERS.equals(code)) {
            main.loadFragment(new InvoiceCompanyTable(), true);
            return;
        }
        if (UiCodes.TAKE_AWAY.equals(code)) {
            main.loadFragment(new InvoiceTakeAway(), true);
            return;
        }
        if (UiCodes.MESS.equals(code)) {
            MessModule.openHub(activity);
            return;
        }
        if (UiCodes.PRODUCTS.equals(code) || UiCodes.SERVICES.equals(code)) {
            main.loadFragment(new ProductMaster(), true);
            return;
        }
        if (UiCodes.STOCK.equals(code) || UiCodes.INVENTORY.equals(code)) {
            main.loadFragment(new Inventory(), true);
            return;
        }
        if (UiCodes.REPORTS.equals(code)) {
            main.loadFragment(new ReportsHub(), true);
            return;
        }
        if (UiCodes.MASTER_DATA.equals(code) || UiCodes.APPOINTMENTS.equals(code)
                || UiCodes.CUSTOM_ORDERS.equals(code)) {
            main.loadFragment(new MasterData(), true);
            return;
        }
        if (UiCodes.EXPENSES.equals(code)) {
            main.loadFragment(new Expenses(), true);
            return;
        }
        if (UiCodes.PRINTER.equals(code)) {
            activity.startActivity(new android.content.Intent(activity, CompanyPrinterSetting.class));
            return;
        }
        if (UiCodes.SETTINGS.equals(code)) {
            main.loadFragment(new UserSetting(), true);
        }
    }
}
