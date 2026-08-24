package com.posbillingwala.owner.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Fragment.OrderInvoice;
import com.posbillingwala.owner.Model.LicenseResponse;
import com.posbillingwala.owner.databinding.StoreDataListBinding;

import java.util.List;

@SuppressLint("SetTextI18n")
public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.MyViewHolder> {

    private final Context context;
    private final List<LicenseResponse> licenseResponseList;
    private final String saleDate;

    public StoreAdapter(Context context, List<LicenseResponse> licenseResponseList, String saleDate) {
        this.context = context;
        this.licenseResponseList = licenseResponseList;
        this.saleDate = saleDate;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use ViewBinding to inflate the layout
        StoreDataListBinding binding = StoreDataListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        LicenseResponse licenseResponse = licenseResponseList.get(position);

        String branchLabel = licenseResponse.getBranchLabel();
        if (branchLabel == null || branchLabel.isEmpty()) {
            branchLabel = "owner".equalsIgnoreCase(licenseResponse.getUserType()) ? "Main Store" : "Franchise Branch";
        }
        String deviceName = licenseResponse.getAndroidDeviceName();
        if (deviceName == null || deviceName.isEmpty()) {
            deviceName = "Not bound";
        }
        String status = licenseResponse.getLicenseStatus() != null ? licenseResponse.getLicenseStatus() : "";
        String expiry = licenseResponse.getExpiryDate() != null ? licenseResponse.getExpiryDate() : "";

        String shopTitle = licenseResponse.getShopName1();
        if (shopTitle == null || shopTitle.trim().isEmpty()) {
            shopTitle = branchLabel;
        }
        String phoneLine = "";
        if (licenseResponse.getPhoneNo1() != null && !licenseResponse.getPhoneNo1().trim().isEmpty()) {
            phoneLine = "<br/><b>Phone:</b> " + licenseResponse.getPhoneNo1().trim();
            if (licenseResponse.getPhoneNo2() != null && !licenseResponse.getPhoneNo2().trim().isEmpty()) {
                phoneLine += ", " + licenseResponse.getPhoneNo2().trim();
            }
        }
        String storeDetails = "<b>" + shopTitle + "</b><br/>" +
                "<b>Branch:</b> " + branchLabel + "<br/>" +
                "<b>Address:</b> " + licenseResponse.getCompanyAddress() + phoneLine + "<br/>" +
                "<b>Key:</b> " + licenseResponse.getLicenseKey() + "<br/>" +
                "<b>Device:</b> " + deviceName + "<br/>" +
                "<b>Status:</b> " + status + " · Exp: " + expiry;

        holder.binding.shopAddress.setText(Html.fromHtml(storeDetails));
        if (saleDate.equalsIgnoreCase("totalSale")) {
            holder.binding.totalSale.setText(licenseResponse.getCurrencyName() + " " + licenseResponse.getTotalSale());
        } else if (saleDate.equalsIgnoreCase("todaySale")) {
            holder.binding.totalSale.setText(licenseResponse.getCurrencyName() + " " + licenseResponse.getTodaySale());
        }

        holder.binding.linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) context).removeCurrentFragmentAndMoveBack();
                OrderInvoice orderInvoice = new OrderInvoice();
                Bundle bundle = new Bundle();
                bundle.putString("pageName", "store");
                bundle.putString("licenceId", licenseResponse.getLicensesId());
                bundle.putString("saleDate", saleDate);
                orderInvoice.setArguments(bundle);
                ((MainActivity) context).loadFragment(orderInvoice, true);
            }
        });
    }

    @Override
    public int getItemCount() {
        return licenseResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private final StoreDataListBinding binding;

        public MyViewHolder(@NonNull StoreDataListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
