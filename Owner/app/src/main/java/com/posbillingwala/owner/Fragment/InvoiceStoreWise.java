package com.posbillingwala.owner.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Adapter.StoreAdapter;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Extra.SimpleDividerItemDecoration;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.LicenseResponse;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.FragmentInvoiceStoreWiseBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InvoiceStoreWise extends Fragment {

    public static Activity activity;
    public FragmentInvoiceStoreWiseBinding binding;
    List<LicenseResponse> licenseResponseList = new ArrayList<>();
    String saleDate;

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentInvoiceStoreWiseBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        activity = getActivity();

        Bundle bundle = getArguments();
        if (bundle != null) {
            saleDate = bundle.getString("saleDate");
        }
        if (saleDate == null || saleDate.isEmpty()) {
            saleDate = "totalSale";
        }
        if (saleDate.equalsIgnoreCase("totalSale")) {
            binding.heading.setText("Total Sale — All Stores");
        } else {
            binding.heading.setText("Today Sale — All Stores");
        }

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                    ((MainActivity) activity).loadFragment(new Home(), false);
                    return true;
                }
                return false;
            }
        });

        binding.backToHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new Home(), false);
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DetectConnection.checkInternetConnection(activity)) {
            getStoreWise();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    public void getStoreWise() {

        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().getStoreWise(MainActivity.userId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    licenseResponseList = response.body().getLicenseResponseList();
                    if (licenseResponseList != null && !licenseResponseList.isEmpty()) {
                        MainActivity.setOutletCounts(licenseResponseList.size());
                        String storeCount = response.body().getStoreCount();
                        if (storeCount == null || storeCount.isEmpty()) {
                            storeCount = String.valueOf(licenseResponseList.size());
                        }
                        CharSequence current = binding.heading.getText();
                        if (current != null && !current.toString().contains("(")) {
                            binding.heading.setText(current + " (" + storeCount + ")");
                        }

                        StoreAdapter adapter = new StoreAdapter(activity, licenseResponseList, saleDate);
                        binding.recyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
                        binding.recyclerView.setAdapter(adapter);
                        if (binding.recyclerView.getItemDecorationCount() == 0) {
                            binding.recyclerView.addItemDecoration(new SimpleDividerItemDecoration(activity));
                        }
                        binding.recyclerView.setHasFixedSize(true);

                        binding.linearLayout.setVisibility(View.VISIBLE);
                        binding.noDataFound.setVisibility(View.GONE);

                    } else {
                        binding.linearLayout.setVisibility(View.GONE);
                        binding.noDataFound.setVisibility(View.VISIBLE);
                    }
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Log.e("storeError", t.getMessage());
            }
        });

    }

}
