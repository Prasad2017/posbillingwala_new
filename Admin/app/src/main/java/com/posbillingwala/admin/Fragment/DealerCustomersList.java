package com.posbillingwala.admin.Fragment;

import android.annotation.SuppressLint;
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
import com.posbillingwala.admin.Adapter.CustomerAdapter;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.CustomerResponse;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentAllCustomerListBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n")
public class DealerCustomersList extends Fragment {

    Activity activity;
    FragmentAllCustomerListBinding binding;
    String dealerId;
    String dealerName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAllCustomerListBinding.inflate(inflater, container, false);
        activity = getActivity();
        Bundle args = getArguments();
        if (args != null) {
            dealerId = args.getString("dealerId");
            dealerName = args.getString("dealerName");
        }
        ((MainActivity) activity).setScreenTitle(dealerName != null ? dealerName + " Customers" : "Dealer Customers");
        if (binding.fabAddCustomer != null) {
            binding.fabAddCustomer.setVisibility(View.GONE);
        }
        if (binding.searchCustomer != null && binding.searchCustomer.getParent() != null) {
            ((View) binding.searchCustomer.getParent()).setVisibility(View.GONE);
        }
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        if (DetectConnection.checkInternetConnection(activity)) {
            loadCustomers();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void loadCustomers() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2563EB"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Api.getClient().getDealerCustomerList(dealerId).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                if (!isAdded() || binding == null) return;
                List<CustomerResponse> list = response.isSuccessful() && response.body() != null
                        ? response.body().getCustomerResponseList() : null;
                if (list == null) list = new ArrayList<>();
                binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                binding.recyclerView.setAdapter(new CustomerAdapter(activity, list));
                binding.emptyCustomers.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                binding.emptyCustomers.setText("No customers for this dealer");
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                if (binding != null) {
                    binding.emptyCustomers.setVisibility(View.VISIBLE);
                    binding.emptyCustomers.setText("Unable to load customers");
                }
            }
        });
    }
}
