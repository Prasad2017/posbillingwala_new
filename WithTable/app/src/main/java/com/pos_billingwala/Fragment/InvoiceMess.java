package com.pos_billingwala.Fragment;

import com.pos_billingwala.Extra.PopupUi;
import android.content.Intent;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.Adapter.MessInvoiceAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.MemberResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentInvoiceMessBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cn.pedant.SweetAlert.SweetAlertDialog;

@SuppressLint("StaticFieldLeak, ClickableViewAccessibility, NonConstantResourceId, NotifyDataSetChanged, SetTextI18n")
public class InvoiceMess extends Fragment implements View.OnClickListener {

    public static Activity activity;
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    List<CompanyResponse> companyResponseList = new ArrayList<>();
    List<MemberResponse> memberResponseList = new ArrayList<>();
    List<MemberResponse> searchMemberResponseList = new ArrayList<>();
    PopupWindow mypopupWindow;
    MessInvoiceAdapter messInvoiceAdapter;
    FragmentInvoiceMessBinding binding;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentInvoiceMessBinding.inflate(inflater, container, false);
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
                    ((MainActivity) activity).navigateBack();
                    return true;
                }
                return false;
            }
        });

        binding.searchMessMember.setSelection(binding.searchMessMember.getText().toString().length());

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
        binding.menuIcon.setOnClickListener(this);

        return view;

    }

    public void searchMessMember(String memberData) {

        searchMemberResponseList.clear();
        if (!memberData.isEmpty()) {
            for (int i = 0; i < memberResponseList.size(); i++)
                if ((memberResponseList.get(i).getMemberName() + memberResponseList.get(i).getMemberMobileNumber()).toLowerCase().contains(memberData.toLowerCase().trim())) {
                    searchMemberResponseList.add(memberResponseList.get(i));
                }
        } else {
            Toast.makeText(activity, getString(R.string.toast_no_search_found_all_data_may_be_showing), Toast.LENGTH_SHORT).show();
            searchMemberResponseList = new ArrayList<>();
            searchMemberResponseList.addAll(memberResponseList);
        }

        messInvoiceAdapter = new MessInvoiceAdapter(activity, searchMemberResponseList);
        binding.recyclerView.setAdapter(messInvoiceAdapter);
        messInvoiceAdapter.notifyDataSetChanged();
        // messInvoiceAdapter.notifyItemInserted(searchMemberResponseList.size() - 1);

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


    public void setMemberListPassword(ImageView imageView) {
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

    public void setPopUpWindow() {

        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.mess_menu_dialog, null);
        mypopupWindow = PopupUi.create(activity, view);

        LinearLayout memberListLayout = view.findViewById(R.id.memberListLayout);
        LinearLayout scanVerifyLayout = view.findViewById(R.id.scanVerifyLayout);

        memberListLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mypopupWindow.dismiss();
                setMemberListPassword(binding.menuIcon);
            }
        });

        scanVerifyLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mypopupWindow.dismiss();
                activity.startActivity(new Intent(activity, com.pos_billingwala.Activity.MessTokenScanActivity.class));
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
        SweetAlertDialog loader = ListLoader.show(activity);
        try {
            Date c = Calendar.getInstance().getTime();
            System.out.println("Current time => " + c);
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            String paymentDate = df.format(c);

            memberResponseList.clear();
            memberResponseList = posBillingWalaDatabase.getMemberPaymentList(paymentDate);
            if (!memberResponseList.isEmpty()) {

                messInvoiceAdapter = new MessInvoiceAdapter(activity, memberResponseList);
                binding.recyclerView.setAdapter(messInvoiceAdapter);
                messInvoiceAdapter.notifyDataSetChanged();
                //  messInvoiceAdapter.notifyItemInserted(memberResponseList.size() - 1);

                binding.messOrderLayout.setVisibility(View.VISIBLE);
                binding.noDataFound.setVisibility(View.GONE);
            } else {
                binding.messOrderLayout.setVisibility(View.GONE);
                binding.noDataFound.setVisibility(View.VISIBLE);
            }
        } finally {
            ListLoader.dismiss(loader);
        }
    }

}