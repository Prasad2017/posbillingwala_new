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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Adapter.BranchComparisonAdapter;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.BranchComparisonResponse;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.FragmentBranchComparisonBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BranchComparison extends Fragment {

    private Activity activity;
    private FragmentBranchComparisonBinding binding;
    private final List<BranchComparisonResponse> branches = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentBranchComparisonBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        activity = getActivity();

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                navigateHome();
                return true;
            }
            return false;
        });

        binding.backToHome.setOnClickListener(v -> navigateHome());
        return view;
    }

    private void navigateHome() {
        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
        ((MainActivity) activity).loadFragment(new Home(), false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DetectConnection.checkInternetConnection(activity)) {
            loadComparison();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    @SuppressLint("SetTextI18n")
    private void loadComparison() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().getBranchComparison(MainActivity.userId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                pDialog.dismiss();
                branches.clear();
                if (response.isSuccessful() && response.body() != null
                        && response.body().getBranchComparisonList() != null) {
                    branches.addAll(response.body().getBranchComparisonList());
                }
                if (!branches.isEmpty()) {
                    binding.heading.setText("Branch Comparison (" + branches.size() + ")");
                    binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                    binding.recyclerView.setAdapter(new BranchComparisonAdapter(branches));
                    binding.recyclerView.setVisibility(View.VISIBLE);
                    binding.noDataFound.setVisibility(View.GONE);
                } else {
                    binding.recyclerView.setVisibility(View.GONE);
                    binding.noDataFound.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                Log.e("BranchComparison", t.getMessage(), t);
            }
        });
    }
}
