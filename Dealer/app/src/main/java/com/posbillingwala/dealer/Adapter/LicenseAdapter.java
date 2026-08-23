package com.posbillingwala.dealer.Adapter;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.dealer.Activity.MainActivity;
import com.posbillingwala.dealer.Extra.LicenceValidityTiers;
import com.posbillingwala.dealer.Fragment.CustomerDetails;
import com.posbillingwala.dealer.Model.AllApiResponse;
import com.posbillingwala.dealer.Model.LicenseResponse;
import com.posbillingwala.dealer.R;
import com.posbillingwala.dealer.Retrofit.Api;
import com.posbillingwala.dealer.databinding.LicenseListBinding;

import java.util.Calendar;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, DefaultLocale")
public class LicenseAdapter extends RecyclerView.Adapter<LicenseAdapter.MyViewHolder> {

    private Context context;
    private List<LicenseResponse> licenseResponseList;
    private String[] licenseTypeList, licenseValidityList;
    private String customerId, licenseValidity, licenseType, licenseKeyStatus;
    private Calendar calendar;
    private DatePickerDialog datePickerDialog;
    private int mYear, mMonth, mDay;

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

    @Override
    public void onBindViewHolder(@NonNull LicenseAdapter.MyViewHolder holder, int position) {
        LicenseResponse licenseResponse = licenseResponseList.get(position);
        holder.binding.licenseKey.setText(licenseResponse.getLicenseKey());
        holder.binding.licenseKey.setTextIsSelectable(true);
        licenseType = licenseResponse.getLicenseType();
        licenseValidity = licenseResponse.getLicenseValidity();
        holder.binding.registrationDate.setText(licenseResponse.getRegistrationDate().substring(0, 10));
        holder.binding.expiryDate.setText(licenseResponse.getExpiryDate());
        holder.binding.currencyType.setText(MainActivity.currency);
        holder.binding.amount.setText(licenseResponse.getAmount());
        holder.binding.licenseValidity.setText(LicenceValidityTiers.displayLabel(licenseValidity));
        String branchLabel = licenseResponse.getBranchLabel();
        if (branchLabel == null || branchLabel.isEmpty()) {
            branchLabel = "owner".equalsIgnoreCase(licenseResponse.getUserType()) ? "Main Store" : "Franchise Branch";
        }
        String shopAddress = "<b>" + branchLabel + "</b><br/><b>Shop Address: </b>" + licenseResponse.getCompanyAddress();
        holder.binding.shopAddress.setText(Html.fromHtml(shopAddress));
        holder.binding.licenseType.setText(licenseType);

        holder.binding.licenseValidity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getLicenseValidity(holder);
            }
        });

        holder.binding.registrationDate.setOnClickListener(null);
        holder.binding.registrationDate.setClickable(false);

        holder.binding.updateLicence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amount = holder.binding.amount.getText().toString();
                if (!amount.isEmpty()) {
                    // P4-4: server computes expiry from remaining/today; registrationDate not used for renew math
                    updateCustomerLicenceDetails(licenseResponse, licenseValidity, licenseType, amount,
                            holder.binding.registrationDate.getText().toString(), licenseKeyStatus);
                } else {
                    holder.binding.amount.setError("Please fill this");
                }
            }
        });

        licenseKeyStatus = licenseResponse.getLicenseStatus();
        if (licenseKeyStatus.equalsIgnoreCase("active")) {
            holder.binding.activeButton.setChecked(true);
            holder.binding.expireButton.setChecked(false);
        } else {
            holder.binding.activeButton.setChecked(false);
            holder.binding.expireButton.setChecked(true);
        }

        holder.binding.licenseKeyStatus.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.activeButton) {
                    licenseKeyStatus = "active";
                } else {
                    licenseKeyStatus = "expire";
                }
            }
        });
    }

    private void getLicenseValidity(MyViewHolder holder) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dynamic_status_dropdown);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        ImageView closeDialog = dialog.findViewById(R.id.closeDialog);
        AutoCompleteTextView autoCompleteTextView = dialog.findViewById(R.id.licenseValidity);
        TextView txtSubmit = dialog.findViewById(R.id.submit);

        try {
            licenseValidityList = context.getResources().getStringArray(R.array.license_validity);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, licenseValidityList);
            autoCompleteTextView.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        closeDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                licenseValidity = LicenceValidityTiers.toDayCount(autoCompleteTextView.getText().toString());
                if (LicenceValidityTiers.isRegularTier(licenseValidity)) {
                    licenseType = "Regular";
                    holder.binding.licenseType.setText("Regular");
                } else {
                    licenseType = "Demo";
                    holder.binding.licenseType.setText("Demo");
                }
            }
        });

        txtSubmit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!autoCompleteTextView.getText().toString().isEmpty()) {
                    licenseValidity = LicenceValidityTiers.toDayCount(autoCompleteTextView.getText().toString());
                    if (LicenceValidityTiers.isRegularTier(licenseValidity)) {
                        licenseType = "Regular";
                        holder.binding.licenseType.setText("Regular");
                    } else {
                        licenseType = "Demo";
                        holder.binding.licenseType.setText("Demo");
                    }
                    holder.binding.licenseValidity.setText(LicenceValidityTiers.displayLabel(licenseValidity));
                    dialog.dismiss();
                } else {
                    Toast.makeText(context, "Please select validity", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }

    private void updateCustomerLicenceDetails(LicenseResponse licenseResponse, String licenseValidity, String licenseType, String amount, String registrationDate, String licenseKeyStatus) {
        SweetAlertDialog pDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().updateCustomerLicenceDetails(licenseResponse.getLicensesId(), licenseValidity, licenseType, amount, registrationDate, licenseKeyStatus);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                        Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_LONG).show();
                        ((MainActivity) context).removeCurrentFragmentAndMoveBack();
                        CustomerDetails customerDetails = new CustomerDetails();
                        Bundle bundle = new Bundle();
                        bundle.putString("customerId", customerId);
                        customerDetails.setArguments(bundle);
                        ((MainActivity) context).loadFragment(customerDetails, true);
                    } else {
                        Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
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

        LicenseListBinding binding;

        public MyViewHolder(@NonNull LicenseListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
