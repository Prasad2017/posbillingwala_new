package com.posbillingwala.admin.Adapter;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.jaredrummler.materialspinner.MaterialSpinner;
import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Extra.BottomSheetUi;
import com.posbillingwala.admin.Extra.LicenceValidityTiers;
import com.posbillingwala.admin.Extra.LicenseStatusHelper;
import com.posbillingwala.admin.Fragment.CustomerDetails;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.LicenseResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.DynamicStatusDropdownBinding;
import com.posbillingwala.admin.databinding.LicenseListBinding;

import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LicenseAdapter extends RecyclerView.Adapter<LicenseAdapter.MyViewHolder> {

    Context context;
    List<LicenseResponse> licenseResponseList;
    String[] licenseValidityList;
    String customerId, licenseValidity, licenseType;

    public LicenseAdapter(Context context, List<LicenseResponse> licenseResponseList, String customerId) {
        this.context = context;
        this.licenseResponseList = licenseResponseList;
        this.customerId = customerId;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LicenseListBinding binding = LicenseListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        LicenseResponse licenseResponse = licenseResponseList.get(position);

        holder.binding.licenseKey.setText("" + licenseResponse.getLicenseKey());
        licenseType = "" + licenseResponse.getLicenseType();
        licenseValidity = "" + licenseResponse.getLicenseValidity();
        String regDate = licenseResponse.getRegistrationDate();
        if (regDate != null && regDate.length() >= 10) {
            holder.binding.registrationDate.setText(regDate.substring(0, 10));
        } else {
            holder.binding.registrationDate.setText(regDate != null ? regDate : "");
        }
        holder.binding.expiryDate.setText("" + licenseResponse.getExpiryDate());
        holder.binding.currencyType.setText(MainActivity.currency);
        holder.binding.amount.setText("" + licenseResponse.getAmount());
        holder.binding.licenseValidity.setText(LicenceValidityTiers.displayLabel(licenseValidity));
        holder.binding.licenseTYpe.setText(licenseType);

        String status = LicenseStatusHelper.displayStatus(licenseResponse);
        LicenseStatusHelper.applyBadge(holder.binding.licenseStatusBadge, status);

        String deviceName = licenseResponse.getAndroidDeviceName();
        String deviceId = licenseResponse.getAndroidDeviceId();
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            holder.binding.deviceInfo.setVisibility(View.VISIBLE);
            holder.binding.deviceInfo.setText("Device: " + (deviceName != null && !deviceName.isEmpty() ? deviceName : "Bound")
                    + "\nID: " + deviceId);
        } else {
            holder.binding.deviceInfo.setVisibility(View.VISIBLE);
            holder.binding.deviceInfo.setText("Device: NOT ACTIVATED");
        }

        String branchLabel = licenseResponse.getBranchLabel();
        if (branchLabel == null || branchLabel.isEmpty()) {
            branchLabel = "owner".equalsIgnoreCase(licenseResponse.getUserType()) ? "Main Store" : "Franchise Branch";
        }
        String shopTitle = licenseResponse.getShopName1();
        if (shopTitle == null || shopTitle.trim().isEmpty()) {
            shopTitle = branchLabel;
        }
        String shopAddress = "<b>" + shopTitle + "</b><br/><b>Branch:</b> " + branchLabel
                + "<br/><b>Shop Address: </b>" + licenseResponse.getCompanyAddress();
        if (licenseResponse.getPhoneNo1() != null && !licenseResponse.getPhoneNo1().trim().isEmpty()) {
            shopAddress += "<br/><b>Phone:</b> " + licenseResponse.getPhoneNo1().trim();
            if (licenseResponse.getPhoneNo2() != null && !licenseResponse.getPhoneNo2().trim().isEmpty()) {
                shopAddress += ", " + licenseResponse.getPhoneNo2().trim();
            }
        }
        holder.binding.shopAddress.setText(Html.fromHtml(shopAddress));

        boolean suspended = LicenseStatusHelper.isSuspended(licenseResponse);
        holder.binding.suspendReactivateLicence.setText(suspended ? "Reactivate" : "Suspend");
        holder.binding.updateLicence.setEnabled(LicenseStatusHelper.canUpgradeOrRenew(licenseResponse));
        holder.binding.updateLicence.setAlpha(LicenseStatusHelper.canUpgradeOrRenew(licenseResponse) ? 1f : 0.4f);

        holder.binding.licenseValidity.setOnClickListener(v -> getLicenseValidity(holder));
        holder.binding.registrationDate.setOnClickListener(null);
        holder.binding.registrationDate.setClickable(false);

        holder.binding.copyLicenseKey.setOnClickListener(v -> {
            String key = licenseResponse.getLicenseKey();
            if (key == null || key.isEmpty()) {
                Toast.makeText(context, "No license key", Toast.LENGTH_SHORT).show();
                return;
            }
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("licenseKey", key));
                Toast.makeText(context, "License key copied", Toast.LENGTH_SHORT).show();
            }
        });

        holder.binding.suspendReactivateLicence.setOnClickListener(v -> {
            String action = LicenseStatusHelper.isSuspended(licenseResponse) ? "reactivate" : "suspend";
            String confirmMsg = "suspend".equals(action)
                    ? "Are you sure you want to suspend this license?\n" + licenseResponse.getLicenseKey()
                    : "Reactivate this license?\n" + licenseResponse.getLicenseKey();
            BottomSheetUi.showConfirm(context, "Confirm", confirmMsg,
                    "Yes", "Cancel", true, () -> updateLicenseStatus(licenseResponse, action));
        });

        holder.binding.updateLicence.setOnClickListener(v -> {
            if (!LicenseStatusHelper.canUpgradeOrRenew(licenseResponse)) {
                Toast.makeText(context, "Suspended licenses cannot be upgraded. Reactivate first.", Toast.LENGTH_LONG).show();
                return;
            }
            if (holder.binding.amount.getText().length() == 0) {
                holder.binding.amount.setError("Please fill this");
                return;
            }
            String amount = holder.binding.amount.getText().toString();
            String currentPlan = LicenceValidityTiers.displayLabel(licenseResponse.getLicenseValidity());
            String newPlan = LicenceValidityTiers.displayLabel(licenseValidity);
            BottomSheetUi.showConfirm(context, "Confirm Upgrade / Renew",
                    "License Key: " + licenseResponse.getLicenseKey()
                            + "\n\nCurrent: " + currentPlan + " (" + licenseResponse.getLicenseType() + ")"
                            + "\nNew: " + newPlan + " (" + licenseType + ")"
                            + "\n\nSame license key will be kept.",
                    "Confirm", "Cancel", true, () -> updateCustomerLicenceDetails(
                            licenseResponse, licenseValidity, licenseType, amount,
                            holder.binding.registrationDate.getText().toString()));
        });
    }

    private void getLicenseValidity(MyViewHolder holder) {

        DynamicStatusDropdownBinding dialogBinding = DynamicStatusDropdownBinding.inflate(LayoutInflater.from(context));
        BottomSheetDialog sheet = BottomSheetUi.showContent(context, dialogBinding.getRoot(), false);
        if (sheet == null) {
            return;
        }

        try {
            licenseValidityList = context.getResources().getStringArray(R.array.license_validity);
            final ArrayAdapter adapter = new ArrayAdapter(context, android.R.layout.simple_spinner_item, licenseValidityList);
            adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
            dialogBinding.licenseValidity.setAdapter(adapter);
            String currentLabel = LicenceValidityTiers.displayLabel(licenseValidity);
            int index = adapter.getPosition(currentLabel);
            if (index >= 0) {
                dialogBinding.licenseValidity.setSelectedIndex(index);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        dialogBinding.closeDialog.setOnClickListener(v -> sheet.dismiss());

        dialogBinding.licenseValidity.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                licenseValidity = LicenceValidityTiers.toDayCount(item);
                if (LicenceValidityTiers.isRegularTier(licenseValidity)) {
                    licenseType = "Regular";
                    holder.binding.licenseTYpe.setText("Regular");
                } else {
                    licenseType = "Demo";
                    holder.binding.licenseTYpe.setText("Demo");
                }
            }
        });

        dialogBinding.submit.setOnClickListener(v -> {
            if (licenseValidity != null && licenseValidity.length() > 0) {
                if (LicenceValidityTiers.isRegularTier(licenseValidity)) {
                    licenseType = "Regular";
                    holder.binding.licenseTYpe.setText("Regular");
                } else {
                    licenseType = "Demo";
                    holder.binding.licenseTYpe.setText("Demo");
                }
                holder.binding.licenseValidity.setText(LicenceValidityTiers.displayLabel(licenseValidity));
                sheet.dismiss();
            } else {
                Toast.makeText(context, "Please select validity", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateLicenseStatus(LicenseResponse licenseResponse, String action) {
        SweetAlertDialog pDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().updateLicenseStatus(licenseResponse.getLicensesId(), action);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                if (response.isSuccessful() && response.body() != null
                        && "1".equals(response.body().getStatus())) {
                    Toast.makeText(context, "" + response.body().getMessage(), Toast.LENGTH_LONG).show();
                    reloadCustomerDetails();
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Unable to update license status.";
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Toast.makeText(context, "Unable to update license status. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCustomerLicenceDetails(LicenseResponse licenseResponse, String licenseValidity, String licenseType, String amount, String registrationDate) {

        SweetAlertDialog pDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().updateCustomerLicenceDetails(licenseResponse.getLicensesId(), licenseValidity, licenseType, amount, registrationDate);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    if ("1".equals(response.body().getStatus())) {
                        Toast.makeText(context, "" + response.body().getMessage(), Toast.LENGTH_LONG).show();
                        reloadCustomerDetails();
                    } else {
                        Toast.makeText(context, "" + response.body().getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Toast.makeText(context, "Unable to upgrade license. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reloadCustomerDetails() {
        ((MainActivity) context).removeCurrentFragmentAndMoveBack();
        CustomerDetails customerDetails = new CustomerDetails();
        Bundle bundle = new Bundle();
        bundle.putString("customerId", "" + customerId);
        customerDetails.setArguments(bundle);
        ((MainActivity) context).loadFragment(customerDetails, true);
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
