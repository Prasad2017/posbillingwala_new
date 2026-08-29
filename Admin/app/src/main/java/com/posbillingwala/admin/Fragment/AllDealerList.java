package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.data.PieEntry;
import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Adapter.DealerAdapter;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Extra.ReportUiHelper;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.DealerResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentAllCustomerListBinding;

import java.util.ArrayList;
import java.util.Arrays;
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
    List<DealerResponse> filteredDealers = new ArrayList<>();
    String statusFilter = "ALL";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAllCustomerListBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();
        MainActivity.title.setText("Dealers");

        if (binding.searchCustomer != null) {
            binding.searchCustomer.setHint("Search dealer name or mobile");
            ((View) binding.searchCustomer.getParent()).setVisibility(View.VISIBLE);
            binding.searchCustomer.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    applyFilters();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
        if (binding.chipAll != null) {
            binding.chipTrial.setVisibility(View.GONE);
            binding.chipExpired.setText("Inactive");
            binding.chipAll.setOnClickListener(v -> { statusFilter = "ALL"; highlightDealerChips(); applyFilters(); });
            binding.chipActive.setOnClickListener(v -> { statusFilter = "ACTIVE"; highlightDealerChips(); applyFilters(); });
            binding.chipExpired.setOnClickListener(v -> { statusFilter = "INACTIVE"; highlightDealerChips(); applyFilters(); });
            highlightDealerChips();
        }
        if (binding.emptyCustomers != null) {
            binding.emptyCustomers.setText("No dealers found.\nUse + to add a dealer.");
        }
        if (binding.fabAddCustomer != null) {
            binding.fabAddCustomer.setOnClickListener(v ->
                    ((MainActivity) activity).navigateDetail(new AddDealer(), "Add Dealer"));
        }
        if (binding.dealerCharts != null) {
            binding.dealerCharts.setVisibility(View.VISIBLE);
        }

        return view;
    }

    private void highlightDealerChips() {
        styleChip(binding.chipAll, "ALL".equals(statusFilter));
        styleChip(binding.chipActive, "ACTIVE".equals(statusFilter));
        styleChip(binding.chipExpired, "INACTIVE".equals(statusFilter));
    }

    private void styleChip(android.widget.TextView chip, boolean selected) {
        if (chip == null) return;
        chip.setBackgroundResource(selected ? R.drawable.bg_month_chip : R.drawable.bg_card);
        chip.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(),
                selected ? R.color.colorPrimary : R.color.colorTextSecondary));
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(0);
        MainActivity.drawerLayout.closeDrawers();
        if (DetectConnection.checkInternetConnection(activity)) {
            getDealerList();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void applyFilters() {
        String q = binding.searchCustomer != null && binding.searchCustomer.getText() != null
                ? binding.searchCustomer.getText().toString().trim().toLowerCase() : "";
        filteredDealers.clear();
        int all = 0, active = 0, inactive = 0;
        for (DealerResponse d : dealerResponseList) {
            if (d == null) continue;
            all++;
            if (d.isActiveDealer()) active++;
            else inactive++;
            boolean statusOk = "ALL".equals(statusFilter)
                    || ("ACTIVE".equals(statusFilter) && d.isActiveDealer())
                    || ("INACTIVE".equals(statusFilter) && !d.isActiveDealer());
            if (!statusOk) continue;
            String name = d.getName() != null ? d.getName().toLowerCase() : "";
            String mobile = d.getContactNumber() != null ? d.getContactNumber().toLowerCase() : "";
            if (q.isEmpty() || name.contains(q) || mobile.contains(q)) {
                filteredDealers.add(d);
            }
        }
        if (binding.chipAll != null) {
            binding.chipAll.setText("All (" + all + ")");
            binding.chipActive.setText("Active (" + active + ")");
            binding.chipExpired.setText("Inactive (" + inactive + ")");
        }
        bindDealerCharts(all, active, inactive);
        dealerAdapter = new DealerAdapter(activity, filteredDealers);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        binding.recyclerView.setAdapter(dealerAdapter);
        binding.emptyCustomers.setVisibility(filteredDealers.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void bindDealerCharts(int total, int active, int inactive) {
        if (binding.dealerCharts == null) return;
        int customers = 0;
        for (DealerResponse d : dealerResponseList) {
            if (d == null) continue;
            try {
                customers += Integer.parseInt(ReportUiHelper.nz(d.getTotalCustomer()));
            } catch (Exception ignored) {
            }
        }
        ReportUiHelper.bindKpi(binding.kpi1, "Total", String.valueOf(total), "");
        ReportUiHelper.bindKpi(binding.kpi2, "Active", String.valueOf(active),
                total > 0 ? Math.round(active * 100f / total) + "%" : "0%");
        ReportUiHelper.bindKpi(binding.kpi3, "Inactive", String.valueOf(inactive),
                total > 0 ? Math.round(inactive * 100f / total) + "%" : "0%");
        ReportUiHelper.bindKpi(binding.kpi4, "Customers", String.valueOf(customers), "");
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(active, "Active"));
        entries.add(new PieEntry(inactive, "Inactive"));
        ReportUiHelper.setupDonut(binding.chartDonut, entries,
                Arrays.asList(Color.parseColor("#16A34A"), Color.parseColor("#6B7280")),
                String.valueOf(total));
        ReportUiHelper.fillLegend(binding.legendContainer,
                new String[]{"Active", "Inactive"},
                new String[]{String.valueOf(active), String.valueOf(inactive)},
                new String[]{
                        total > 0 ? String.valueOf(Math.round(active * 100f / total)) : "0",
                        total > 0 ? String.valueOf(Math.round(inactive * 100f / total)) : "0"
                },
                new int[]{Color.parseColor("#16A34A"), Color.parseColor("#6B7280")});
    }

    private void getDealerList() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2563EB"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        dealerResponseList.clear();

        Call<AllApiResponse> call = Api.getClient().getDealerList();
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getDealerResponseList() != null) {
                    dealerResponseList = new ArrayList<>(response.body().getDealerResponseList());
                    applyFilters();
                } else {
                    binding.emptyCustomers.setVisibility(View.VISIBLE);
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE);
                sweetAlertDialog.setTitleText("Oops...");
                sweetAlertDialog.setContentText("Unable to load dealers. Please try again.");
                sweetAlertDialog.setCancelClickListener(SweetAlertDialog::dismiss).show();
            }
        });
    }
}
