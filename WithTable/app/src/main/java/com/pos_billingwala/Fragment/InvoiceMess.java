package com.pos_billingwala.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.pos_billingwala.Adapter.MessInvoiceAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.Extra.MessMealTokenPrintWorker;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.MemberResponse;
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
    List<MemberResponse> memberResponseList = new ArrayList<>();
    List<MemberResponse> searchMemberResponseList = new ArrayList<>();
    MessInvoiceAdapter messInvoiceAdapter;
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

        if (binding.searchMessMember.getText() != null) {
            binding.searchMessMember.setSelection(binding.searchMessMember.getText().toString().length());
        }
        binding.searchMessMember.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchMessMember(s.toString());
            }
        });

        binding.homeCardView.setOnClickListener(this);
        binding.memberListLayout.setOnClickListener(this);
        binding.qrManagementLayout.setOnClickListener(this);
        binding.todayTokensLayout.setOnClickListener(this);
        binding.mealSessionsLayout.setOnClickListener(this);

        return view;
    }

    public void searchMessMember(String memberData) {
        searchMemberResponseList.clear();
        if (!memberData.isEmpty()) {
            for (int i = 0; i < memberResponseList.size(); i++) {
                String name = memberResponseList.get(i).getMemberName() != null
                        ? memberResponseList.get(i).getMemberName() : "";
                String mobile = memberResponseList.get(i).getMemberMobileNumber() != null
                        ? memberResponseList.get(i).getMemberMobileNumber() : "";
                String altMobile = memberResponseList.get(i).getMemberAlternetMobileNumber() != null
                        ? memberResponseList.get(i).getMemberAlternetMobileNumber() : "";
                String regNo = memberResponseList.get(i).getRegistrationNo() != null
                        ? memberResponseList.get(i).getRegistrationNo() : "";
                if ((name + mobile + altMobile + regNo).toLowerCase().contains(memberData.toLowerCase().trim())) {
                    searchMemberResponseList.add(memberResponseList.get(i));
                }
            }
        } else {
            searchMemberResponseList = new ArrayList<>();
            searchMemberResponseList.addAll(memberResponseList);
        }

        messInvoiceAdapter = new MessInvoiceAdapter(activity, searchMemberResponseList);
        binding.recyclerView.setAdapter(messInvoiceAdapter);
        messInvoiceAdapter.notifyDataSetChanged();

        boolean hasResults = !searchMemberResponseList.isEmpty();
        binding.recyclerView.setVisibility(hasResults ? View.VISIBLE : View.GONE);
        binding.noDataFound.setVisibility(hasResults ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.homeCardView) {
            ((MainActivity) activity).navigateBack();
        } else if (id == R.id.memberListLayout) {
            setMemberListPassword();
        } else if (id == R.id.qrManagementLayout) {
            activity.startActivity(new Intent(activity, MessQrManagementActivity.class));
        } else if (id == R.id.todayTokensLayout) {
            activity.startActivity(new Intent(activity, MessMealTokenTodayActivity.class));
        } else if (id == R.id.mealSessionsLayout) {
            activity.startActivity(new Intent(activity, MessMealSessionsActivity.class));
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
        if (!companyResponseList.isEmpty()) {
            getMemberList();
        } else {
            Toast.makeText(activity, getString(R.string.toast_please_fill_shop_details), Toast.LENGTH_SHORT).show();
            ((MainActivity) activity).loadFragment(new CompanyDetailSetting(), true);
        }
    }

    public void getMemberList() {
        memberResponseList.clear();
        memberResponseList = posBillingWalaDatabase.getMemberList();
        String query = binding.searchMessMember.getText() != null
                ? binding.searchMessMember.getText().toString() : "";
        searchMessMember(query);
        binding.messOrderLayout.setVisibility(View.VISIBLE);
    }
}
