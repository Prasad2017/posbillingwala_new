package com.posbillingwala.owner.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Fragment.UserProfile;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.LicenseResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;

import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.posbillingwala.owner.databinding.LicenseListBinding;  // Import the generated binding class

public class LicenseAdapter extends RecyclerView.Adapter<LicenseAdapter.MyViewHolder> {

    private final Context context;
    private final List<LicenseResponse> licenseResponseList;
    private String totalSaleData = "0";
    private String todaySaleData = "0";

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

        String shopAddress = "<b><font color='#ff0000'>Shop Address:</font></b> " + licenseResponse.getCompanyAddress();
        holder.binding.shopAddress.setText(Html.fromHtml(shopAddress));
        holder.binding.licenseKey.setText(licenseResponse.getLicenseKey());
        holder.binding.licenseKey.setTextIsSelectable(true);
        holder.binding.licenseValidity.setText(licenseResponse.getLicenseValidity() + " Days");
        holder.binding.licenseType.setText(licenseResponse.getLicenseType());
        holder.binding.registrationDate.setText(licenseResponse.getRegistrationDate().substring(0, 10));
        holder.binding.expiryDate.setText(licenseResponse.getExpiryDate());
        holder.binding.amount.setText(MainActivity.currency + " " + licenseResponse.getAmount());

        if (licenseResponse.getTotalSaleData().equalsIgnoreCase("1")) {
            holder.binding.totalSaleData.setChecked(true);
            totalSaleData = "1";
        } else {
            holder.binding.totalSaleData.setChecked(false);
            totalSaleData = "0";
        }

        if (licenseResponse.getTodaySaleData().equalsIgnoreCase("1")) {
            holder.binding.todaySaleData.setChecked(true);
            todaySaleData = "1";
        } else {
            holder.binding.todaySaleData.setChecked(false);
            todaySaleData = "0";
        }

        holder.binding.totalSaleData.setOnCheckedChangeListener((buttonView, isChecked) -> {
            totalSaleData = isChecked ? "1" : "0";
        });

        holder.binding.todaySaleData.setOnCheckedChangeListener((buttonView, isChecked) -> {
            todaySaleData = isChecked ? "1" : "0";
        });

        holder.binding.updateSaleData.setOnClickListener(v -> updateSaleData(licenseResponse));
    }

    public void updateSaleData(LicenseResponse licenseResponse) {
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
