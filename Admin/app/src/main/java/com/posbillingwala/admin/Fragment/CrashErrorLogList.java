package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Adapter.ErrorLogAdapter;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.ErrorLogSummary;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentCrashErrorLogListBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CrashErrorLogList extends Fragment {

    Activity activity;
    FragmentCrashErrorLogListBinding binding;
    private final List<ErrorLogSummary> allLogs = new ArrayList<>();
    private String typeFilter = "ALL";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCrashErrorLogListBinding.inflate(inflater, container, false);
        activity = getActivity();
        MainActivity.title.setText("Crash & Error Logs");
        MainActivity.back.setOnClickListener(v ->
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack());

        binding.chipAll.setOnClickListener(v -> setFilter("ALL"));
        binding.chipCrash.setOnClickListener(v -> setFilter("CRASH"));
        binding.chipApi.setOnClickListener(v -> setFilter("API"));
        binding.chipPrinter.setOnClickListener(v -> setFilter("PRINTER"));
        binding.chipDatabase.setOnClickListener(v -> setFilter("DATABASE"));
        binding.chipNetwork.setOnClickListener(v -> setFilter("NETWORK"));
        binding.chipDevice.setOnClickListener(v -> setFilter("DEVICE"));
        binding.chipApp.setOnClickListener(v -> setFilter("APPLICATION"));
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

    private void setFilter(String filter) {
        typeFilter = filter;
        highlightChips();
        bindList();
    }

    private void highlightChips() {
        styleChip(binding.chipAll, "ALL".equals(typeFilter));
        styleChip(binding.chipCrash, "CRASH".equals(typeFilter));
        styleChip(binding.chipApi, "API".equals(typeFilter));
        styleChip(binding.chipPrinter, "PRINTER".equals(typeFilter));
        styleChip(binding.chipDatabase, "DATABASE".equals(typeFilter));
        styleChip(binding.chipNetwork, "NETWORK".equals(typeFilter));
        styleChip(binding.chipDevice, "DEVICE".equals(typeFilter));
        styleChip(binding.chipApp, "APPLICATION".equals(typeFilter));
    }

    private void styleChip(TextView chip, boolean selected) {
        if (chip == null) {
            return;
        }
        chip.setBackgroundResource(selected ? R.drawable.bg_month_chip : R.drawable.bg_card);
        chip.setTextColor(ContextCompat.getColor(requireContext(),
                selected ? R.color.colorPrimary : R.color.colorTextSecondary));
    }

    private void loadLogs() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().getErrorLogList(500);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                allLogs.clear();
                List<ErrorLogSummary> list = response.body() != null
                        ? response.body().getErrorLogList() : null;
                if (list != null) {
                    allLogs.addAll(list);
                }
                updateChipCounts();
                highlightChips();
                bindList();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                binding.emptyLogs.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateChipCounts() {
        int crash = 0, api = 0, printer = 0, db = 0, network = 0, app = 0;
        for (ErrorLogSummary e : allLogs) {
            if (matchesFilter(e, "CRASH")) {
                crash++;
            }
            if (matchesFilter(e, "API")) {
                api++;
            }
            if (matchesFilter(e, "PRINTER")) {
                printer++;
            }
            if (matchesFilter(e, "DATABASE")) {
                db++;
            }
            if (matchesFilter(e, "NETWORK")) {
                network++;
            }
            if (matchesFilter(e, "APPLICATION")) {
                app++;
            }
        }
        binding.chipAll.setText("All (" + allLogs.size() + ")");
        binding.chipCrash.setText("Crash (" + crash + ")");
        binding.chipApi.setText("API (" + api + ")");
        binding.chipPrinter.setText("Printer (" + printer + ")");
        binding.chipDatabase.setText("Database (" + db + ")");
        binding.chipNetwork.setText("Network (" + network + ")");
        binding.chipApp.setText("App (" + app + ")");
    }

    private void bindList() {
        List<ErrorLogSummary> filtered = new ArrayList<>();
        for (ErrorLogSummary e : allLogs) {
            if (matchesFilter(e, typeFilter)) {
                filtered.add(e);
            }
        }
        binding.emptyLogs.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        if (filtered.isEmpty()) {
            binding.emptyLogs.setText("ALL".equals(typeFilter)
                    ? "No crash or error logs yet"
                    : "No " + typeFilter.toLowerCase(Locale.US) + " logs yet");
        }
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        binding.recyclerView.setAdapter(new ErrorLogAdapter(filtered, item -> {
            CrashErrorLogDetail detail = CrashErrorLogDetail.newInstance(item.getId());
            ((MainActivity) activity).loadFragment(detail, true);
        }));
    }

    static boolean matchesFilter(ErrorLogSummary e, String filter) {
        if (filter == null || "ALL".equals(filter)) {
            return true;
        }
        String type = nz(e.getErrorType()).toUpperCase(Locale.US);
        String cat = nz(e.getErrorCategory()).toLowerCase(Locale.US);
        String clazz = nz(e.getOriginalExceptionClass()).toLowerCase(Locale.US);
        if ("CRASH".equals(filter)) {
            return type.equals("CRASH") || type.equals("ANR") || type.equals("NATIVE_CRASH")
                    || type.equals("LOW_MEMORY")
                    || clazz.contains("nullpointer") || clazz.contains("outofmemory")
                    || cat.equals("npe") || cat.equals("oom") || cat.equals("java_crash")
                    || cat.equals("native_crash") || cat.equals("anr") || cat.equals("low_memory");
        }
        if ("DEVICE".equals(filter)) {
            return type.equals("DEVICE") || type.equals("LOW_MEMORY")
                    || cat.contains("storage") || cat.contains("thermal") || cat.contains("battery")
                    || cat.contains("memory") || cat.equals("resource_kill");
        }
        if ("NETWORK".equals(filter)) {
            return type.equals("NETWORK") || cat.contains("timeout") || cat.contains("connection")
                    || cat.equals("no_internet") || cat.equals("no_network");
        }
        return type.equals(filter);
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }
}
