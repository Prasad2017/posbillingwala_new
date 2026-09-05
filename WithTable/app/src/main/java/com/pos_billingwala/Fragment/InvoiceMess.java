package com.pos_billingwala.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Activity.MessMealSessionsActivity;
import com.pos_billingwala.Activity.MessMealTokenTodayActivity;
import com.pos_billingwala.Activity.MessQrManagementActivity;
import com.pos_billingwala.Activity.MessTokenScanActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.Extra.MessMealTokenPrintWorker;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentInvoiceMessBinding;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("StaticFieldLeak, ClickableViewAccessibility, NonConstantResourceId, NotifyDataSetChanged, SetTextI18n")
public class InvoiceMess extends Fragment implements View.OnClickListener {

    public static Activity activity;
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    List<CompanyResponse> companyResponseList = new ArrayList<>();
    FragmentInvoiceMessBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentInvoiceMessBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();
        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

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
        binding.memberListLayout.setOnClickListener(this);
        binding.scanVerifyLayout.setOnClickListener(this);
        binding.qrManagementLayout.setOnClickListener(this);
        binding.todayTokensLayout.setOnClickListener(this);
        binding.mealSessionsLayout.setOnClickListener(this);
        binding.walkInTokenLayout.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.homeCardView) {
            ((MainActivity) activity).navigateBack();
        } else if (id == R.id.memberListLayout) {
            setMemberListPassword();
        } else if (id == R.id.scanVerifyLayout) {
            activity.startActivity(new Intent(activity, MessTokenScanActivity.class));
        } else if (id == R.id.qrManagementLayout) {
            activity.startActivity(new Intent(activity, MessQrManagementActivity.class));
        } else if (id == R.id.todayTokensLayout) {
            activity.startActivity(new Intent(activity, MessMealTokenTodayActivity.class));
        } else if (id == R.id.mealSessionsLayout) {
            activity.startActivity(new Intent(activity, MessMealSessionsActivity.class));
        } else if (id == R.id.walkInTokenLayout) {
            ((MainActivity) activity).loadFragment(new CreatePos(), true);
        }
    }

    public void setMemberListPassword() {
        View content = LayoutInflater.from(activity).inflate(R.layout.report_password_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, content, false);

        TextView continueToReport = content.findViewById(R.id.continueToReport);
        TextView dismissReport = content.findViewById(R.id.dismissReport);
        TextInputEditText reportPin = content.findViewById(R.id.reportPin);
        TextView detailsTxt = content.findViewById(R.id.details);
        detailsTxt.setText("Member List Password");

        dismissReport.setOnClickListener(v -> sheet.dismiss());

        continueToReport.setOnClickListener(v -> {
            String pin;
            if (MainActivity.reportPin != null) {
                pin = MainActivity.reportPin;
            } else {
                pin = "9082";
            }

            if (reportPin.getText().toString().equalsIgnoreCase(pin)) {
                sheet.dismiss();
                ((MainActivity) activity).loadFragment(new MessMemberList(), true);
            } else {
                reportPin.requestFocus();
                reportPin.setError("Enter correct pin");
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        getCompanyDetails();
        MessMealTokenPrintWorker.recoverPendingFromServer(activity);
    }

    public void getCompanyDetails() {
        companyResponseList.clear();
        companyResponseList = posBillingWalaDatabase.getCompanyDetails();
        if (companyResponseList.isEmpty()) {
            Toast.makeText(activity, getString(R.string.toast_please_fill_shop_details), Toast.LENGTH_SHORT).show();
            ((MainActivity) activity).loadFragment(new CompanyDetailSetting(), true);
        }
    }
}
