package com.posbillingwala.owner.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Extra.ReportUiHelper;
import com.posbillingwala.owner.Extra.RowDividerUi;
import com.posbillingwala.owner.Fragment.UserProfile;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.LicenseResponse;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.LicenseListBinding;

import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LicenseAdapter extends RecyclerView.Adapter<LicenseAdapter.MyViewHolder> {

    private final Context context;
    private final List<LicenseResponse> licenseResponseList;

    public LicenseAdapter(Context context, List<LicenseResponse> licenseResponseList) {
        this.context = context;
        this.licenseResponseList = licenseResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use ViewBinding to inflate the layout
        LicenseListBinding binding = LicenseListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        LicenseResponse licenseResponse = licenseResponseList.get(position);

        StringBuilder header = new StringBuilder();
        if (licenseResponse.getShopName1() != null && !licenseResponse.getShopName1().trim().isEmpty()) {
            header.append(licenseResponse.getShopName1().trim());
        }
        String branch = licenseResponse.getBranchLabel();
        if (branch != null && !branch.trim().isEmpty()) {
            if (header.length() > 0) {
                header.append(" · ");
            }
            header.append(branch.trim());
        }
        if (licenseResponse.getCompanyAddress() != null && !licenseResponse.getCompanyAddress().trim().isEmpty()) {
            if (header.length() > 0) {
                header.append("\n");
            }
            header.append(licenseResponse.getCompanyAddress().trim());
        }
        if (licenseResponse.getPhoneNo1() != null && !licenseResponse.getPhoneNo1().trim().isEmpty()) {
            header.append("\n").append(licenseResponse.getPhoneNo1().trim());
            if (licenseResponse.getPhoneNo2() != null && !licenseResponse.getPhoneNo2().trim().isEmpty()) {
                header.append(", ").append(licenseResponse.getPhoneNo2().trim());
            }
        }
        holder.binding.shopAddress.setText(header.length() > 0 ? header.toString() : "Outlet");
        holder.binding.licenseKey.setText(licenseResponse.getLicenseKey());
        holder.binding.licenseKey.setTextIsSelectable(true);
        holder.binding.licenseValidity.setText(licenseResponse.getLicenseValidity() + " Days");
        holder.binding.licenseType.setText(licenseResponse.getLicenseType());
        String registration = licenseResponse.getRegistrationDate();
        holder.binding.registrationDate.setText(registration != null && registration.length() >= 10
                ? registration.substring(0, 10) : (registration != null ? registration : "-"));
        holder.binding.expiryDate.setText(licenseResponse.getExpiryDate());
        holder.binding.amount.setText(ReportUiHelper.money(licenseResponse.getAmount()));
        RowDividerUi.bindLastItem(holder.binding.licenseDivider, position, getItemCount());

        holder.binding.totalSaleData.setOnCheckedChangeListener(null);
        holder.binding.todaySaleData.setOnCheckedChangeListener(null);

        holder.binding.totalSaleData.setChecked("1".equalsIgnoreCase(licenseResponse.getTotalSaleData()));
        holder.binding.todaySaleData.setChecked("1".equalsIgnoreCase(licenseResponse.getTodaySaleData()));

        holder.binding.updateSaleData.setOnClickListener(v -> {
            String total = holder.binding.totalSaleData.isChecked() ? "1" : "0";
            String today = holder.binding.todaySaleData.isChecked() ? "1" : "0";
            updateSaleData(licenseResponse, total, today);
        });
    }

    public void updateSaleData(LicenseResponse licenseResponse, String totalSaleData, String todaySaleData) {
        SweetAlertDialog pDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().updateSaleData(licenseResponse.getLicensesId(), totalSaleData, todaySaleData);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    if ("1".equalsIgnoreCase(response.body().getStatus())) {
                        Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        ((MainActivity) context).removeCurrentFragmentAndMoveBack();
                        ((MainActivity) context).loadFragment(new UserProfile(), true);
                    } else {
                        Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return licenseResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private final LicenseListBinding binding;

        public MyViewHolder(@NonNull LicenseListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
