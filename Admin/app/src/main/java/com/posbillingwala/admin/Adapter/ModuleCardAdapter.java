package com.posbillingwala.admin.Adapter;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.LicenseResponse;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.ItemModuleCardBinding;

import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ModuleCardAdapter extends RecyclerView.Adapter<ModuleCardAdapter.Holder> {

    private final Activity activity;
    private final List<LicenseResponse> items;

    public ModuleCardAdapter(Activity activity, List<LicenseResponse> items) {
        this.activity = activity;
        this.items = items;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemModuleCardBinding binding = ItemModuleCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        LicenseResponse lic = items.get(position);
        String branch = lic.getBranchLabel();
        if (branch == null || branch.isEmpty()) {
            branch = "owner".equalsIgnoreCase(lic.getUserType()) ? "Main Store" : "Branch";
        }
        holder.binding.moduleBranch.setText(branch + " · " + nullToDash(lic.getLicenseKey()));
        holder.binding.switchFastBilling.setChecked(isOn(lic.getFastBilling()));
        holder.binding.switchDineIn.setChecked(isOn(lic.getDineIn()));
        holder.binding.switchTakeAway.setChecked(isOn(lic.getTakeAway()));
        holder.binding.switchMess.setChecked(isOn(lic.getMess()));
        holder.binding.saveModules.setOnClickListener(v -> save(lic, holder));
    }

    private void save(LicenseResponse lic, Holder holder) {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Saving");
        pDialog.setCancelable(false);
        pDialog.show();
        Api.getClient().updateLicenseModules(
                lic.getLicensesId(),
                holder.binding.switchFastBilling.isChecked() ? "1" : "0",
                holder.binding.switchTakeAway.isChecked() ? "1" : "0",
                holder.binding.switchDineIn.isChecked() ? "1" : "0",
                holder.binding.switchMess.isChecked() ? "1" : "0"
        ).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                if (response.isSuccessful() && response.body() != null
                        && "1".equals(response.body().getStatus())) {
                    lic.setFastBilling(holder.binding.switchFastBilling.isChecked() ? "1" : "0");
                    lic.setTakeAway(holder.binding.switchTakeAway.isChecked() ? "1" : "0");
                    lic.setDineIn(holder.binding.switchDineIn.isChecked() ? "1" : "0");
                    lic.setMess(holder.binding.switchMess.isChecked() ? "1" : "0");
                    Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Update failed";
                    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Toast.makeText(activity, "Unable to save modules.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static boolean isOn(String value) {
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    private static String nullToDash(String v) {
        return v == null || v.trim().isEmpty() ? "-" : v.trim();
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemModuleCardBinding binding;

        Holder(ItemModuleCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
