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
import com.posbillingwala.admin.Adapter.ErrorLogAdapter;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.ErrorLogSummary;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentCrashErrorLogListBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CrashErrorLogList extends Fragment {

    Activity activity;
    FragmentCrashErrorLogListBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCrashErrorLogListBinding.inflate(inflater, container, false);
        activity = getActivity();
        MainActivity.title.setText("Crash & Error Logs");
        MainActivity.back.setOnClickListener(v ->
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack());
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(0);
        if (DetectConnection.checkInternetConnection(activity)) {
            loadLogs();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void loadLogs() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().getErrorLogList(100);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                List<ErrorLogSummary> list = response.body() != null
                        ? response.body().getErrorLogList() : null;
                if (list == null) {
                    list = new ArrayList<>();
                }
                binding.emptyLogs.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                binding.recyclerView.setAdapter(new ErrorLogAdapter(list, item -> {
                    CrashErrorLogDetail detail = CrashErrorLogDetail.newInstance(item.getId());
                    ((MainActivity) activity).loadFragment(detail, true);
                }));
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                binding.emptyLogs.setVisibility(View.VISIBLE);
            }
        });
    }
}
