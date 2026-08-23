package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.DetectConnection;
import com.pos_billingwala.NetworkToOffline.UserSynchronizeData;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentReportSettingBinding;

public class ReportSetting extends Fragment implements View.OnClickListener {


    public static Activity activity;
    public static POSBillingWalaDatabase posBillingWalaDatabase;
    public static FragmentReportSettingBinding binding;
    View view;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentReportSettingBinding.inflate(inflater, container, false);
        view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here

        activity = getActivity();

        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                    ((MainActivity) activity).loadFragment(new UserSetting(), true);
                    return true;
                }
                return false;
            }
        });

        initViews();

        return view;
    }

    public void initViews() {

        binding.backToSetting.setOnClickListener(this);
        binding.invoiceWiseReportLayout.setOnClickListener(this);
        binding.invoiceTableWiseReportLayout.setOnClickListener(this);
        binding.invoiceTakeAwayWiseReportLayout.setOnClickListener(this);
        binding.invoicePaymentWiseReportLayout.setOnClickListener(this);
        binding.invoiceMessWiseReportLayout.setOnClickListener(this);
        binding.productWiseReportLayout.setOnClickListener(this);
        binding.saleWiseReportLayout.setOnClickListener(this);
        binding.clearInvoiceLayout.setOnClickListener(this);
        binding.invoiceMemberPaymentWiseReportLayout.setOnClickListener(this);
        binding.expenseWiseReportLayout.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToSetting) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new UserSetting(), true);
        } else if (id == R.id.invoiceWiseReportLayout) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new InvoiceReport(), true);
        } else if (id == R.id.invoiceTableWiseReportLayout) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new InvoiceTableReport(), true);
        } else if (id == R.id.invoiceTakeAwayWiseReportLayout) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new InvoiceTakeAwayReport(), true);
        } else if (id == R.id.invoiceMessWiseReportLayout) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new InvoiceMessReport(), true);
        } else if (id == R.id.invoicePaymentWiseReportLayout) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new InvoicePaymentModeWiseReport(), true);
        } else if (id == R.id.invoiceMemberPaymentWiseReportLayout) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new InvoiceMessMemberReportList(), true);
        } else if (id == R.id.productWiseReportLayout) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new InvoiceProductReport(), true);
        } else if (id == R.id.saleWiseReportLayout) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new SaleReport(), true);
        } else if (id == R.id.expenseWiseReportLayout) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new InvoiceExpenseReport(), true);
        } else if (id == R.id.clearInvoiceLayout) {
            int unsynced = posBillingWalaDatabase.countUnsyncedInvoices();
            if (unsynced > 0) {
                Toast.makeText(activity,
                        unsynced + " unsynced bill(s). Upload to cloud first — clear blocked to protect data.",
                        Toast.LENGTH_LONG).show();
                if (DetectConnection.checkInternetConnection(activity)) {
                    new UserSynchronizeData(activity);
                } else {
                    DetectConnection.noInternetConnection(activity);
                }
                return;
            }
            posBillingWalaDatabase.clearInvoice();
            Toast.makeText(activity, getString(R.string.toast_invoice_cleared), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
    }
}