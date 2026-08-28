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
import com.posbillingwala.admin.Adapter.LicenseAdapter;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.LicenseResponse;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentCustomerDetailsBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Reuses a simple licenses recycler layout pattern via CustomerDetails licenses panel fields
 * are too coupled — use a lightweight dedicated layout based on all-customer list shell.
 */
@SuppressLint("SetTextI18n")
public class DealerLicensesList extends Fragment {

    Activity activity;
    View root;
    androidx.recyclerview.widget.RecyclerView recyclerView;
    android.widget.TextView emptyView;
    String dealerId;
    String dealerName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = getActivity();
        Bundle args = getArguments();
        if (args != null) {
            dealerId = args.getString("dealerId");
            dealerName = args.getString("dealerName");
        }
        ((MainActivity) activity).setScreenTitle(dealerName != null ? dealerName + " Licenses" : "Dealer Licenses");

        android.widget.FrameLayout frame = new android.widget.FrameLayout(requireContext());
        frame.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        frame.setBackgroundColor(android.graphics.Color.parseColor("#F7F9FC"));

        emptyView = new android.widget.TextView(requireContext());
        emptyView.setText("No licenses for this dealer");
        emptyView.setGravity(android.view.Gravity.CENTER);
        emptyView.setPadding(32, 64, 32, 32);
        emptyView.setVisibility(View.GONE);
        frame.addView(emptyView, new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        recyclerView = new androidx.recyclerview.widget.RecyclerView(requireContext());
        recyclerView.setPadding(8, 8, 8, 8);
        recyclerView.setClipToPadding(false);
        frame.addView(recyclerView, new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        root = frame;
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        if (DetectConnection.checkInternetConnection(activity)) {
            loadLicenses();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void loadLicenses() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2563EB"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Api.getClient().getDealerLicenseList(dealerId).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                if (!isAdded()) return;
                List<LicenseResponse> list = response.isSuccessful() && response.body() != null
                        ? response.body().getLicensesResponseList() : null;
                if (list == null) list = new ArrayList<>();
                recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                // LicenseAdapter expects a customerId for renew actions; use empty and list still shows.
                recyclerView.setAdapter(new LicenseAdapter(activity, list, ""));
                emptyView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                emptyView.setVisibility(View.VISIBLE);
                emptyView.setText("Unable to load licenses");
            }
        });
    }
}
