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

public class SalesList extends Fragment {
    Activity activity;
    FragmentCustomerSalesBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCustomerSalesBinding.inflate(inflater, container, false);
        activity = getActivity();
        ((MainActivity) activity).setScreenTitle("Sales List");
        MainActivity.back.setOnClickListener(v ->
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack());
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        if (DetectConnection.checkInternetConnection(activity)) {
            loadSales();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void loadSales() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2563EB"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Api.getClient().getRecentInvoices(100, "").enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                if (!isAdded() || binding == null) return;
                AllApiResponse body = response.body();
                List<InvoiceSaleResponse> list = body != null ? body.getInvoiceResponseList() : null;
                if (list == null) list = new ArrayList<>();
                String bills = body != null && body.getBillCount() != null
                        ? body.getBillCount() : String.valueOf(list.size());
                String net = body != null && body.getNetSales() != null ? body.getNetSales() : "0";
                binding.salesSummary.setText("Bills: " + bills + "  ·  Net: ₹ " + net);
                binding.emptySales.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                binding.recyclerView.setAdapter(new SalesAdapter(list, inv -> {
                    SalesDetails d = new SalesDetails();
                    Bundle b = new Bundle();
                    b.putString("invoiceId", inv.getInvoiceId());
                    d.setArguments(b);
                    ((MainActivity) activity).navigateDetail(d, "Sales Details");
                }));
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                if (binding != null) binding.emptySales.setVisibility(View.VISIBLE);
            }
        });
    }
}
