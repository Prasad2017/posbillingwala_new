package com.pos_billingwala.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.InvoiceMessMemberPaymentAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Extra.ReportCursorHelper;
import com.pos_billingwala.Model.MemberResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentInvoiceMessMemberPaymentReportBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;

@SuppressLint("SetTextI18n")
public class InvoiceMessMemberPaymentReport extends Fragment implements View.OnClickListener {

    public static Activity activity;
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    List<MemberResponse> memberResponseList = new ArrayList<>();
    String memberId;
    FragmentInvoiceMessMemberPaymentReportBinding binding;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentInvoiceMessMemberPaymentReportBinding.inflate(inflater, container, false);
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
                    ((MainActivity) getActivity()).navigateBack();
                    return true;
                }
                return false;
            }
        });

        Bundle bundle = getArguments();
        if (bundle != null) {
            memberId = bundle.getString("memberId");
        }

        binding.backToSetting.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.backToSetting) {
            ((MainActivity) getActivity()).navigateBack();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) getActivity()).lockUnlockDrawer(1);
        getInvoiceMemberPaymentReportList();

    }


    public void getInvoiceMemberPaymentReportList() {
        SweetAlertDialog loader = ListLoader.show(activity);
        try {
            memberResponseList.clear();
            memberResponseList = posBillingWalaDatabase.getInvoiceMemberPaymentReportList(memberId);

            if (!memberResponseList.isEmpty()) {

                float pendingAmount = 0, paidAmount = 0;
                for (MemberResponse memberResponse : memberResponseList) {
                    paidAmount += ReportCursorHelper.parseAmount(memberResponse.getPaymentPaidAmount());
                }

                pendingAmount = ReportCursorHelper.parseAmount(memberResponseList.get(0).getPaymentMessAmount()) - paidAmount;
                binding.pendingAmount.setText(MainActivity.currencyName + " " + pendingAmount);

                InvoiceMessMemberPaymentAdapter adapter = new InvoiceMessMemberPaymentAdapter(activity, memberResponseList);
                binding.recyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
                binding.recyclerView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                // adapter.notifyItemInserted(memberResponseList.size() - 1);

                binding.linearLayout.setVisibility(View.VISIBLE);
                binding.noDataFound.setVisibility(View.GONE);

            } else {
                binding.linearLayout.setVisibility(View.GONE);
                binding.noDataFound.setVisibility(View.VISIBLE);
            }
        } finally {
            ListLoader.dismiss(loader);
        }
    }
}