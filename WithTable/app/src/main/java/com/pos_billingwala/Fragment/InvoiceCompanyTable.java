package com.pos_billingwala.Fragment;

import com.pos_billingwala.Extra.PopupUi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import com.pos_billingwala.Activity.DuplicateBluetoothPrint;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.TableAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.AppExecutors;
import com.pos_billingwala.Extra.AutoFitGridRecyclerView;
import com.pos_billingwala.Extra.EmptyListUi;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.DiningAreaResponse;
import com.pos_billingwala.Model.PosTableResponse;
import com.pos_billingwala.Model.TableStatus;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentInvoiceCompanyTableBinding;

import java.util.ArrayList;
import java.util.List;


public class InvoiceCompanyTable extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public static List<CompanyResponse> companyResponseList = new ArrayList<>();
    static POSBillingWalaDatabase posBillingWalaDatabase;
    static AutoFitGridRecyclerView tableRecyclerView;
    static TableAdapter tableAdapter;
    private static InvoiceCompanyTable activeInstance;
    View view;
    PopupWindow mypopupWindow;
    FragmentInvoiceCompanyTableBinding binding;
    private String selectedAreaId = null;
    private List<DiningAreaResponse> diningAreas = new ArrayList<>();

    public static void getCompanyDetails() {
        if (activity == null || posBillingWalaDatabase == null) {
            return;
        }
        final InvoiceCompanyTable ui = activeInstance;
        final boolean showLoader = tableAdapter == null;
        final cn.pedant.SweetAlert.SweetAlertDialog loader =
                showLoader ? ListLoader.show(activity) : null;
        AppExecutors.get().db().execute(() -> {
            List<CompanyResponse> list = posBillingWalaDatabase.getCompanyDetails();
            List<PosTableResponse> floorTables = null;
            List<DiningAreaResponse> areas = new ArrayList<>();
            final boolean tableFeatureOn;
            final boolean shopMissing = list == null || list.isEmpty();
            if (!shopMissing
                    && list.get(0).getTableStatus() != null
                    && list.get(0).getTableStatus().equalsIgnoreCase("on")) {
                tableFeatureOn = true;
                floorTables = com.pos_billingwala.Extra.DineInTableHelper
                        .buildFloorTableList(posBillingWalaDatabase);
                List<DiningAreaResponse> loadedAreas = posBillingWalaDatabase.getDiningAreas();
                if (loadedAreas != null) {
                    areas.addAll(loadedAreas);
                }
                if (floorTables == null || floorTables.isEmpty()) {
                    int parsed = 0;
                    try {
                        parsed = Integer.parseInt(list.get(0).getNoOfTable());
                    } catch (Exception ignored) {
                    }
                    if (parsed > 0) {
                        floorTables = new ArrayList<>();
                        for (int i = 1; i <= parsed; i++) {
                            PosTableResponse fallback = new PosTableResponse();
                            fallback.setTableNumber(String.valueOf(i));
                            fallback.setTableName("T" + i);
                            fallback.setCapacity("4");
                            fallback.setDisplayStatus(TableStatus.AVAILABLE);
                            com.pos_billingwala.Extra.DineInTableHelper.enrichTableRuntime(
                                    posBillingWalaDatabase, fallback);
                            floorTables.add(fallback);
                        }
                    }
                }
            } else {
                tableFeatureOn = false;
            }
            final List<PosTableResponse> tablesForUi = floorTables;
            final List<DiningAreaResponse> areasForUi = areas;
            AppExecutors.get().main(() -> {
                try {
                    if (activity == null) {
                        return;
                    }
                    companyResponseList.clear();
                    if (list != null) {
                        companyResponseList.addAll(list);
                    }
                    if (shopMissing) {
                        Toast.makeText(activity, activity.getString(R.string.toast_please_fill_shop_details),
                                Toast.LENGTH_SHORT).show();
                        ((MainActivity) activity).loadFragment(new CompanyDetailSetting(), true);
                        return;
                    }
                    if (!tableFeatureOn || tableRecyclerView == null) {
                        return;
                    }
                    if (tablesForUi != null && !tablesForUi.isEmpty()) {
                        if (tableAdapter != null) {
                            tableAdapter.replaceTables(tablesForUi);
                            if (ui != null) {
                                tableAdapter.setAreaFilter(ui.selectedAreaId);
                            }
                        } else {
                            tableAdapter = new TableAdapter(activity, tablesForUi);
                            if (ui != null) {
                                tableAdapter.setAreaFilter(ui.selectedAreaId);
                            }
                        }
                        if (tableRecyclerView.getAdapter() != tableAdapter) {
                            tableRecyclerView.setAdapter(tableAdapter);
                        }
                        tableRecyclerView.requestLayout();
                        tableRecyclerView.setVisibility(View.VISIBLE);
                        EmptyListUi.bind(ui != null && ui.binding != null ? ui.binding.noDataFound : null, true,
                                R.string.empty_sub_dine_in);
                    } else {
                        if (tableRecyclerView != null) {
                            tableRecyclerView.setVisibility(View.GONE);
                        }
                        EmptyListUi.bind(ui != null && ui.binding != null ? ui.binding.noDataFound : null, false,
                                R.string.empty_sub_dine_in);
                    }
                    if (ui != null) {
                        ui.diningAreas.clear();
                        ui.diningAreas.addAll(areasForUi);
                        ui.renderAreaFilters();
                        ui.renderStatusLegend();
                    }
                } finally {
                    ListLoader.dismiss(loader);
                }
            });
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentInvoiceCompanyTableBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();
        activeInstance = this;

        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        tableRecyclerView = binding.tableRecyclerView;

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                Log.i("tag", "onKey Back listener is working!!!");
                ((MainActivity) activity).navigateBack();
                return true;
            }
            return false;
        });

        binding.homeCardView.setOnClickListener(this);
        binding.menuIcon.setOnClickListener(this);
        renderStatusLegend();

        return view;
    }

    private void renderStatusLegend() {
        if (binding == null || binding.statusLegendRow == null || activity == null) {
            return;
        }
        binding.statusLegendRow.removeAllViews();
        int gap = dp(10);
        for (String[] entry : TableStatus.legendEntries()) {
            LinearLayout item = new LinearLayout(activity);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            itemLp.setMarginEnd(gap);
            item.setLayoutParams(itemLp);

            View dot = new View(activity);
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(8), dp(8));
            dotLp.setMarginEnd(dp(4));
            dot.setLayoutParams(dotLp);
            Drawable base = ContextCompat.getDrawable(activity, R.drawable.bg_table_status_dot);
            if (base != null) {
                Drawable tinted = base.mutate();
                DrawableCompat.setTint(tinted, ContextCompat.getColor(activity, TableStatus.colorRes(entry[0])));
                dot.setBackground(tinted);
            }

            TextView label = new TextView(activity);
            label.setText(entry[1]);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            try {
                label.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(activity, R.font.poppinsregular));
            } catch (Exception ignored) {
            }
            label.setTextColor(ContextCompat.getColor(activity, R.color.grey_60));

            item.addView(dot);
            item.addView(label);
            binding.statusLegendRow.addView(item);
        }
    }

    private void renderAreaFilters() {
        if (binding == null || binding.areaFilterRow == null || activity == null) {
            return;
        }
        binding.areaFilterRow.removeAllViews();
        addAreaChip(null, "All");
        for (DiningAreaResponse area : diningAreas) {
            if (area == null || area.getAreaName() == null) {
                continue;
            }
            addAreaChip(area.getAreaId(), area.getAreaName());
        }
    }

    private void addAreaChip(String areaId, String label) {
        TextView chip = new TextView(activity);
        chip.setText(label);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        try {
            chip.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(activity, R.font.poppinsmedium));
        } catch (Exception ignored) {
        }
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(14), dp(6), dp(14), dp(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(8));
        chip.setLayoutParams(lp);

        boolean selected = (areaId == null && selectedAreaId == null)
                || (areaId != null && areaId.equals(selectedAreaId));
        applyChipStyle(chip, selected);
        chip.setOnClickListener(v -> {
            selectedAreaId = areaId;
            if (tableAdapter != null) {
                tableAdapter.setAreaFilter(selectedAreaId);
            }
            renderAreaFilters();
        });
        binding.areaFilterRow.addView(chip);
    }

    private void applyChipStyle(TextView chip, boolean selected) {
        if (selected) {
            chip.setBackgroundResource(R.drawable.bg_table_area_chip_selected);
            chip.setTextColor(ContextCompat.getColor(activity, R.color.white));
        } else {
            chip.setBackgroundResource(R.drawable.bg_table_area_chip);
            chip.setTextColor(ContextCompat.getColor(activity, R.color.colorTextPrimary));
        }
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.homeCardView) {
            ((MainActivity) activity).navigateBack();
        } else if (id == R.id.menuIcon) {
            setPopUpWindow();
        }
    }

    public void setPopUpWindow() {
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.share_dialog, null);
        mypopupWindow = PopupUi.create(activity, view);

        LinearLayout saveInvoiceLayout = view.findViewById(R.id.saveInvoiceLayout);
        LinearLayout duplicateInvoicePrintLayout = view.findViewById(R.id.duplicateInvoicePrintLayout);

        saveInvoiceLayout.setVisibility(View.GONE);

        duplicateInvoicePrintLayout.setOnClickListener(v -> {
            mypopupWindow.dismiss();
            Intent intent = new Intent(activity, DuplicateBluetoothPrint.class);
            intent.putExtra("invoiceRunningStatus", "printBill");
            intent.putExtra("cartOrderStatus", "table_wise");
            activity.startActivity(intent);
        });

        PopupUi.showAsToolbarMenu(mypopupWindow, binding.menuIcon);
    }

    @Override
    public void onDestroyView() {
        if (activeInstance == this) {
            activeInstance = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
        activeInstance = this;
        ((MainActivity) activity).lockUnlockDrawer(1);
        getCompanyDetails();
    }
}
