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

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Activity.MessMealSessionsActivity;
import com.pos_billingwala.Activity.MessMealTokenTodayActivity;
import com.pos_billingwala.Activity.MessQrManagementActivity;
import com.pos_billingwala.Adapter.MessInvoiceAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.MessMealTokenPrintWorker;
import com.pos_billingwala.Extra.MessModule;
import com.pos_billingwala.Extra.SecurityPermissions;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.MemberResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentInvoiceMessBinding;

import java.util.ArrayList;
import java.util.List;
import com.pos_billingwala.Extra.EmptyListUi;

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

        if (!MessModule.isEnabled(activity)) {
            Toast.makeText(activity, R.string.toast_you_have_not_selected_mess_please_contact, Toast.LENGTH_SHORT).show();
            ((MainActivity) activity).navigateBack();
            return view;
        }

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
        EmptyListUi.bind(binding.noDataFound, hasResults, R.string.empty_sub_mess_invoices);
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
        SecurityPermissions.runAuthorized(activity, SecurityPermissions.MESS_MEMBERS,
                "Member List Password",
                () -> ((MainActivity) activity).loadFragment(new MessMemberList(), true));
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
        List<MemberResponse> allMembers = posBillingWalaDatabase.getMemberList();
        // Alert uses full roster (e.g. unpaid tokens); list shows current-month members only.
        updateMessPaymentAlert(allMembers);

        memberResponseList.clear();
        for (MemberResponse m : allMembers) {
            if (hasCurrentMonthPayment(m)) {
                memberResponseList.add(m);
            }
        }

        String query = binding.searchMessMember.getText() != null
                ? binding.searchMessMember.getText().toString() : "";
        searchMessMember(query);
        binding.messOrderLayout.setVisibility(View.VISIBLE);
    }

    /** Member has a mess payment package (or paid amount) for the current month. */
    private boolean hasCurrentMonthPayment(MemberResponse m) {
        if (m == null) {
            return false;
        }
        float mess = parseFloatSafe(m.getPaymentMessAmount());
        float paid = parseFloatSafe(m.getPaymentPaidAmount());
        if (mess > 0.009f || paid > 0.009f) {
            return true;
        }
        String memberId = m.getMemberId();
        if (memberId == null || memberId.trim().isEmpty()) {
            return false;
        }
        String currentMonth = new java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
                .format(java.util.Calendar.getInstance().getTime());
        return posBillingWalaDatabase.hasMemberPaymentForMonth(memberId, currentMonth);
    }

    private void updateMessPaymentAlert(List<MemberResponse> source) {
        int pendingCount = 0;
        int unpaidWithTokens = 0;
        if (source != null) {
            for (MemberResponse m : source) {
                float mess = parseFloatSafe(m.getPaymentMessAmount());
                float paid = parseFloatSafe(m.getPaymentPaidAmount());
                float pending = Math.max(0f, mess - paid);
                int tokens = parseIntSafe(m.getTokensGenerated());
                int todayTokens = parseIntSafe(m.getTodayTokensGenerated());

                if (pending > 0.009f) {
                    pendingCount++;
                }
                boolean unpaid = mess <= 0.009f || paid <= 0.009f;
                if (unpaid && (tokens > 0 || todayTokens > 0)) {
                    unpaidWithTokens++;
                }
            }
        }

        if (pendingCount == 0 && unpaidWithTokens == 0) {
            binding.messAlertBanner.setVisibility(View.GONE);
            return;
        }

        binding.messAlertBanner.setVisibility(View.VISIBLE);
        if (pendingCount > 0 && unpaidWithTokens > 0) {
            binding.messAlertText.setText(getString(R.string.ui_mess_alert_both, pendingCount, unpaidWithTokens));
        } else if (pendingCount > 0) {
            binding.messAlertText.setText(getString(R.string.ui_mess_alert_pending_only, pendingCount));
        } else {
            binding.messAlertText.setText(getString(R.string.ui_mess_alert_unpaid_tokens, unpaidWithTokens));
        }
    }

    private float parseFloatSafe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0f;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (Exception e) {
            return 0f;
        }
    }

    private int parseIntSafe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
