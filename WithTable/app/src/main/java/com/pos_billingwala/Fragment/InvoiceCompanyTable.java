package com.pos_billingwala.Fragment;

import com.pos_billingwala.Extra.PopupUi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.pos_billingwala.Activity.DuplicateBluetoothPrint;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.TableAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.AppExecutors;
import com.pos_billingwala.Extra.AutoFitGridRecyclerView;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Model.CompanyResponse;
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
    View view;
    PopupWindow mypopupWindow;
    FragmentInvoiceCompanyTableBinding binding;

    public static void getCompanyDetails() {
        if (activity == null || posBillingWalaDatabase == null) {
            return;
        }
        final cn.pedant.SweetAlert.SweetAlertDialog loader = ListLoader.show(activity);
        AppExecutors.get().db().execute(() -> {
            List<CompanyResponse> list = posBillingWalaDatabase.getCompanyDetails();
            AppExecutors.get().main(() -> {
                try {
                    if (activity == null) {
                        return;
                    }
                    companyResponseList.clear();
                    if (list != null) {
                        companyResponseList.addAll(list);
                    }
                    if (!companyResponseList.isEmpty()) {
                        if (companyResponseList.get(0).getTableStatus() != null) {
                            if (companyResponseList.get(0).getTableStatus().equalsIgnoreCase("on")) {
                                int noOfTable = Integer.parseInt(companyResponseList.get(0).getNoOfTable());
                                if (noOfTable > 0 && tableRecyclerView != null) {
                                    tableAdapter = new TableAdapter(activity, noOfTable);
                                    // AutoFitGridRecyclerView owns the LayoutManager — do not set GridLayoutManager here
                                    tableRecyclerView.setAdapter(tableAdapter);
                                    tableAdapter.notifyDataSetChanged();
                                    tableRecyclerView.requestLayout();
                                }
                            }
                        }
                    } else {
                        Toast.makeText(activity, activity.getString(R.string.toast_please_fill_shop_details), Toast.LENGTH_SHORT).show();
                        ((MainActivity) activity).loadFragment(new CompanyDetailSetting(), true);
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
        view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here

        activity = getActivity();

        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        tableRecyclerView = binding.tableRecyclerView;

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    ((MainActivity) activity).navigateBack();
                    return true;
                }
                return false;
            }
        });

        binding.homeCardView.setOnClickListener(this);
        binding.menuIcon.setOnClickListener(this);

        return view;
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

        duplicateInvoicePrintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mypopupWindow.dismiss();

                Intent intent = new Intent(activity, DuplicateBluetoothPrint.class);
                intent.putExtra("invoiceRunningStatus", "printBill");
                intent.putExtra("cartOrderStatus", "table_wise");
                activity.startActivity(intent);

            }
        });

        PopupUi.showAsToolbarMenu(mypopupWindow, binding.menuIcon);

    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        getCompanyDetails();

    }

}
