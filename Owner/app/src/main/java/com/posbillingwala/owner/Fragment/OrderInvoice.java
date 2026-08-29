package com.posbillingwala.owner.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Adapter.InvoiceAdapter;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.InvoiceResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.posbillingwala.owner.databinding.FragmentOrderInvoiceBinding;

public class OrderInvoice extends Fragment {

    public static Activity activity;
    public FragmentOrderInvoiceBinding binding;
    List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
    InvoiceAdapter adapter;
    String pageName, licenceId, saleDate;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentOrderInvoiceBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        activity = getActivity();

        Bundle bundle = getArguments();
        if (bundle != null) {
            pageName = bundle.getString("pageName");
            licenceId = bundle.getString("licenceId");
            saleDate = bundle.getString("saleDate");
        }

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                    if (pageName.equalsIgnoreCase("setting")) {
                        ((MainActivity) activity).loadFragment(new UserSetting(), true);
                    } else {
                        InvoiceStoreWise invoiceStoreWise = new InvoiceStoreWise();
                        Bundle bundle = new Bundle();
                        bundle.putString("saleDate", saleDate);
                        invoiceStoreWise.setArguments(bundle);
                        ((MainActivity) activity).loadFragment(invoiceStoreWise, true);
                    }
                    return true;
                }
                return false;
            }
        });

        // Set up backToSetting button click listener
        binding.backToSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                if (pageName.equalsIgnoreCase("setting")) {
                    ((MainActivity) activity).loadFragment(new UserSetting(), true);
                } else {
                    InvoiceStoreWise invoiceStoreWise = new InvoiceStoreWise();
                    Bundle bundle = new Bundle();
                    bundle.putString("saleDate", saleDate);
                    invoiceStoreWise.setArguments(bundle);
                    ((MainActivity) activity).loadFragment(invoiceStoreWise, true);
                }
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DetectConnection.checkInternetConnection(activity)) {
            getInvoiceList();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    public void getInvoiceList() {

        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call;
        if (licenceId != null && saleDate != null) {
            call = Api.getClient().getInvoiceLicenceIdWiseList(licenceId, saleDate);
        } else {
            call = Api.getClient().getInvoiceList(MainActivity.userId);
        }
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    invoiceResponseList.clear();
                    List<InvoiceResponse> invoices = response.body() != null
                            ? response.body().getInvoiceResponseList() : null;
                    invoiceResponseList = invoices != null ? invoices : new ArrayList<>();
                    if (!invoiceResponseList.isEmpty()) {

                        adapter = new InvoiceAdapter(activity, invoiceResponseList);
                        binding.recyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
                        binding.recyclerView.setHasFixedSize(true);
                        binding.recyclerView.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                        adapter.notifyItemInserted(invoiceResponseList.size() - 1);

                        binding.recyclerView.setVisibility(View.VISIBLE);
                        binding.noDataFound.setVisibility(View.GONE);

                    } else {
                        binding.recyclerView.setVisibility(View.GONE);
                        binding.noDataFound.setVisibility(View.VISIBLE);
                    }

                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Log.e("serverError", t.getMessage());
            }
        });

    }
}
