package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Adapter.SalesAdapter;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.InvoiceSaleResponse;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentCustomerSalesBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerSales extends Fragment {

    Activity activity;
    FragmentCustomerSalesBinding binding;
    String customerId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCustomerSalesBinding.inflate(inflater, container, false);
        activity = getActivity();
        MainActivity.title.setText("Sales");

        Bundle bundle = getArguments();
        if (bundle != null) {
            customerId = bundle.getString("customerId");
        }

        MainActivity.back.setOnClickListener(v -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            CustomerDetails details = new CustomerDetails();
            Bundle b = new Bundle();
            b.putString("customerId", customerId);
            details.setArguments(b);
            ((MainActivity) activity).loadFragment(details, true);
        });

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(1);
        if (DetectConnection.checkInternetConnection(activity)) {
            loadSales();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void loadSales() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().getCustomerSales(customerId, "");
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                AllApiResponse body = response.body();
                List<InvoiceSaleResponse> list = body != null ? body.getInvoiceResponseList() : null;
                if (list == null) {
                    list = new ArrayList<>();
                }
                String bills = body != null && body.getBillCount() != null ? body.getBillCount() : String.valueOf(list.size());
                String net = body != null && body.getNetSales() != null ? body.getNetSales() : "0";
                binding.salesSummary.setText("Bills: " + bills + "  ·  Net: ₹ " + net);
                binding.emptySales.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                binding.recyclerView.setAdapter(new SalesAdapter(list));
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                binding.emptySales.setVisibility(View.VISIBLE);
            }
        });
    }
}
