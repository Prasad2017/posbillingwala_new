package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.InvoiceMessListAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.MemberResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentInvoiceMessMemberReportListBinding;

import java.util.ArrayList;
import java.util.List;


public class InvoiceMessMemberReportList extends Fragment implements View.OnClickListener {

    public static Activity activity;
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    List<MemberResponse> memberResponseList = new ArrayList<>();
    String memberId;
    FragmentInvoiceMessMemberReportListBinding binding;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentInvoiceMessMemberReportListBinding.inflate(inflater, container, false);
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
                    ((MainActivity) getActivity()).removeCurrentFragmentAndMoveBack();
                    ((MainActivity) getActivity()).loadFragment(new InvoiceMessReport(), true);
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
            ((MainActivity) getActivity()).removeCurrentFragmentAndMoveBack();
            ((MainActivity) getActivity()).loadFragment(new InvoiceMessReport(), true);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) getActivity()).lockUnlockDrawer(1);
        getInvoiceMemberList();

    }

    public void getInvoiceMemberList() {

        memberResponseList = posBillingWalaDatabase.getInvoiceMemberList();
        if (!memberResponseList.isEmpty()) {
            InvoiceMessListAdapter invoiceMessListAdapter = new InvoiceMessListAdapter(activity, memberResponseList);
            binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
            binding.recyclerView.setAdapter(invoiceMessListAdapter);
            invoiceMessListAdapter.notifyDataSetChanged();
            // invoiceMessListAdapter.notifyItemInserted(memberResponseList.size() - 1);

            binding.linearLayout.setVisibility(View.VISIBLE);
            binding.noDataFound.setVisibility(View.GONE);
        } else {
            binding.linearLayout.setVisibility(View.GONE);
            binding.noDataFound.setVisibility(View.VISIBLE);
        }

    }
}