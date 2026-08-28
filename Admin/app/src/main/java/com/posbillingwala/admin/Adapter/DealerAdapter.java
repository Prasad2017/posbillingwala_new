package com.posbillingwala.admin.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Extra.LicenseStatusHelper;
import com.posbillingwala.admin.Fragment.DealerDetails;
import com.posbillingwala.admin.Fragment.DealerProfile;
import com.posbillingwala.admin.Fragment.DealerReport;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.DealerResponse;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.DealerListBinding;

import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DealerAdapter extends RecyclerView.Adapter<DealerAdapter.MyViewHolder> {

    Context context;
    List<DealerResponse> dealerResponseList;

    public DealerAdapter(Context context, List<DealerResponse> dealerResponseList) {
        this.context = context;
        this.dealerResponseList = dealerResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        DealerListBinding binding = DealerListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        DealerResponse dealerResponse = dealerResponseList.get(position);

        holder.binding.dealerName.setText(
                dealerResponse.getName() != null ? dealerResponse.getName() : "—");
        holder.binding.dealerNumber.setText(
                dealerResponse.getContactNumber() != null ? dealerResponse.getContactNumber() : "—");
        holder.binding.dealerAddress.setText(
                dealerResponse.getAddress() != null ? dealerResponse.getAddress() : "");
        holder.binding.dealerAadhaarNumber.setText(
                "Customers: " + (dealerResponse.getTotalCustomer() != null
                        ? dealerResponse.getTotalCustomer() : "0"));

        if (holder.binding.dealerInitials != null) {
            holder.binding.dealerInitials.setText(initials(dealerResponse.getName()));
        }

        if (holder.binding.dealerStatusBadge != null) {
            holder.binding.dealerStatusBadge.setVisibility(View.VISIBLE);
            LicenseStatusHelper.applyDealerBadge(
                    holder.binding.dealerStatusBadge, dealerResponse.isActiveDealer());
        }

        holder.binding.editDealer.setOnClickListener(v -> openDetails(dealerResponse));

        holder.itemView.setOnClickListener(v -> openDetails(dealerResponse));
        holder.binding.cardView.setOnClickListener(v -> openDetails(dealerResponse));

        holder.binding.deleteDealer.setOnClickListener(v -> {
            boolean active = dealerResponse.isActiveDealer();
            String action = active ? "deactivate" : "activate";
            new AlertDialog.Builder(context)
                    .setTitle("Confirm")
                    .setMessage(active
                            ? "Deactivate this dealer? They will not be able to log in."
                            : "Reactivate this dealer?")
                    .setPositiveButton("Yes", (d, w) -> updateStatus(dealerResponse, action, position))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        // Long-press still opens report via item long click below

        holder.itemView.setOnLongClickListener(v -> {
            DealerReport report = new DealerReport();
            Bundle bundle = new Bundle();
            bundle.putString("dealerId", dealerResponse.getId());
            bundle.putString("dealerName", dealerResponse.getName());
            report.setArguments(bundle);
            ((MainActivity) context).loadFragment(report, true);
            return true;
        });
    }

    private void openDetails(DealerResponse dealerResponse) {
        DealerDetails details = new DealerDetails();
        Bundle bundle = new Bundle();
        bundle.putString("dealerId", dealerResponse.getId());
        bundle.putString("dealerName", dealerResponse.getName());
        bundle.putString("dealerMobile", dealerResponse.getContactNumber());
        bundle.putString("dealerEmail", dealerResponse.getEmail());
        bundle.putString("dealerAddress", dealerResponse.getAddress());
        bundle.putString("dealerAadhaar", dealerResponse.getAadharNumber());
        bundle.putBoolean("dealerActive", dealerResponse.isActiveDealer());
        details.setArguments(bundle);
        ((MainActivity) context).loadFragment(details, true);
    }

    private static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "D";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            String p = parts[0];
            return p.length() >= 2 ? p.substring(0, 2).toUpperCase() : p.toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private void updateStatus(DealerResponse dealer, String action, int position) {
        SweetAlertDialog pDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2563EB"));
        pDialog.setTitleText("Updating");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().updateDealerStatus(dealer.getId(), action);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                if (response.isSuccessful() && response.body() != null && "1".equals(response.body().getStatus())) {
                    Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    dealer.setIsActive("activate".equals(action) ? "1" : "0");
                    notifyItemChanged(position);
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Update failed";
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Toast.makeText(context, "Unable to update dealer status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return dealerResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private final DealerListBinding binding;

        public MyViewHolder(@NonNull DealerListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
