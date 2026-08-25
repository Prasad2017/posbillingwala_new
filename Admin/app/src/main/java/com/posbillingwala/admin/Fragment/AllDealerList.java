package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Adapter.DealerAdapter;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.DealerResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentAllCustomerListBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class AllDealerList extends Fragment {

    public static Activity activity;
    View view;
    FragmentAllCustomerListBinding binding;
    DealerAdapter dealerAdapter;
    List<DealerResponse> dealerResponseList = new ArrayList<>();


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAllCustomerListBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();
        MainActivity.title.setText("Dealers");
        if (binding.searchCustomer != null) {
            ((View) binding.searchCustomer.getParent()).setVisibility(View.GONE);
        }
        if (binding.emptyCustomers != null) {
            binding.emptyCustomers.setText("No dealers found.\nUse More → Add Dealer.\nLong-press a dealer for report.");
        }

        return view;

    }

    public void onStart() {
        super.onStart();
        Log.e("onStart", "called");
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(0);
        MainActivity.drawerLayout.closeDrawers();
        if (DetectConnection.checkInternetConnection(activity)) {
            getDealerList();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void getDealerList() {

        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        dealerResponseList.clear();

        Call<AllApiResponse> call = Api.getClient().getDealerList();
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    dealerResponseList = response.body().getDealerResponseList();
                    Log.e("customerResponseList", "" + dealerResponseList.size());
                    if (dealerResponseList.size() > 0) {

                        dealerAdapter = new DealerAdapter(activity, dealerResponseList);
                        binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                        binding.recyclerView.setAdapter(dealerAdapter);
                        dealerAdapter.notifyDataSetChanged();
                        binding.recyclerView.setHasFixedSize(true);
                    }
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE);
                sweetAlertDialog.setTitleText("Oops...");
                sweetAlertDialog.setContentText("Something went wrong!");
                sweetAlertDialog.setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.dismiss();
                    }
                }).show();
            }
        });

    }

}
