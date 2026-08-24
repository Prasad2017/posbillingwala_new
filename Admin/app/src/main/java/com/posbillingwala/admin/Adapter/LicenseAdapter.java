package com.posbillingwala.admin.Adapter;

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
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.jaredrummler.materialspinner.MaterialSpinner;
import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Extra.LicenceValidityTiers;
import com.posbillingwala.admin.Fragment.CustomerDetails;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.LicenseResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;

import java.util.Calendar;
import java.util.List;

import butterknife.BindViews;
import butterknife.ButterKnife;
import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LicenseAdapter extends RecyclerView.Adapter<LicenseAdapter.MyViewHolder> {

    Context context;
    List<LicenseResponse> licenseResponseList;
    String[] licenseTypeList, licenseValidityList;
    String customerId, licenseValidity, licenseType;
    Calendar calender;
    DatePickerDialog datePickerDialog;
    private int mYear, mMonth, mDay;

    public LicenseAdapter(Context context, List<LicenseResponse> licenseResponseList, String customerId) {
        this.context = context;
        this.licenseResponseList = licenseResponseList;
        this.customerId = customerId;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.license_list, parent, false);
        return new MyViewHolder(itemView);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        LicenseResponse licenseResponse = licenseResponseList.get(position);

        holder.textViews.get(0).setText("" + licenseResponse.getLicenseKey());
        licenseType = "" + licenseResponse.getLicenseType();
        licenseValidity = "" + licenseResponse.getLicenseValidity();
        holder.textViews.get(1).setText("" + licenseResponse.getRegistrationDate().substring(0, 10));
        holder.textViews.get(2).setText("" + licenseResponse.getExpiryDate());
        holder.textViews.get(3).setText(MainActivity.currency);
        holder.textInputEditTexts.get(0).setText("" + licenseResponse.getAmount());
        holder.textViews.get(5).setText(LicenceValidityTiers.displayLabel(licenseValidity));
        String branchLabel = licenseResponse.getBranchLabel();
        if (branchLabel == null || branchLabel.isEmpty()) {
            branchLabel = "owner".equalsIgnoreCase(licenseResponse.getUserType()) ? "Main Store" : "Franchise Branch";
        }
        String shopAddress = "<b>" + branchLabel + "</b><br/><b>Shop Address: </b>" + licenseResponse.getCompanyAddress();
        holder.textViews.get(6).setText(Html.fromHtml(shopAddress));
        holder.textViews.get(7).setText(licenseType);


        holder.textViews.get(5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getLicenseValidity(holder);
            }
        });

        holder.textViews.get(1).setOnClickListener(null);
        holder.textViews.get(1).setClickable(false);

        holder.textViews.get(4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.textInputEditTexts.get(0).getText().length() > 0) {
                    String amount = holder.textInputEditTexts.get(0).getText().toString();
                    // P4-4: server computes expiry from remaining/today; registrationDate not used for renew math
                    updateCustomerLicenceDetails(licenseResponse, licenseValidity, licenseType, "" + amount, holder.textViews.get(1).getText().toString());
                } else {
                    holder.textInputEditTexts.get(0).setError("Please fill this");
                }
            }
        });

    }

    private void getLicenseValidity(MyViewHolder holder) {

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.dynamic_status_dropdown);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        ImageView closeDialog = dialog.findViewById(R.id.closeDialog);
        MaterialSpinner licenseValiditySpinner = dialog.findViewById(R.id.licenseValidity);
        TextView txtSubmit = dialog.findViewById(R.id.submit);

        try {
            licenseValidityList = context.getResources().getStringArray(R.array.license_validity);
            final ArrayAdapter adapter = new ArrayAdapter(context, android.R.layout.simple_spinner_item, licenseValidityList);
            adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
            licenseValiditySpinner.setAdapter(adapter);
            String currentLabel = LicenceValidityTiers.displayLabel(licenseValidity);
            int index = adapter.getPosition(currentLabel);
            if (index >= 0) {
                licenseValiditySpinner.setSelectedIndex(index);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        closeDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        licenseValiditySpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                licenseValidity = LicenceValidityTiers.toDayCount(item);
                if (LicenceValidityTiers.isRegularTier(licenseValidity)) {
                    licenseType = "Regular";
                    holder.textViews.get(7).setText("Regular");
                } else {
                    licenseType = "Demo";
                    holder.textViews.get(7).setText("Demo");
                }
            }
        });

        txtSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (licenseValidity != null && licenseValidity.length() > 0) {
                    if (LicenceValidityTiers.isRegularTier(licenseValidity)) {
                        licenseType = "Regular";
                        holder.textViews.get(7).setText("Regular");
                    } else {
                        licenseType = "Demo";
                        holder.textViews.get(7).setText("Demo");
                    }
                    holder.textViews.get(5).setText(LicenceValidityTiers.displayLabel(licenseValidity));
                    dialog.dismiss();
                } else {
                    Toast.makeText(context, "Please select validity", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);

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
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                        Toast.makeText(context, "" + response.body().getMessage(), Toast.LENGTH_LONG).show();
                        ((MainActivity) context).removeCurrentFragmentAndMoveBack();
                        CustomerDetails customerDetails = new CustomerDetails();
                        Bundle bundle = new Bundle();
                        bundle.putString("customerId", "" + customerId);
                        customerDetails.setArguments(bundle);
                        ((MainActivity) context).loadFragment(customerDetails, true);
                    } else {
                        Toast.makeText(context, "" + response.body().getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Toast.makeText(context, "" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


    }

    @Override
    public int getItemCount() {
        return licenseResponseList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        @BindViews({R.id.licenseKey, R.id.registrationDate, R.id.expiryDate, R.id.currencyType,
                R.id.updateLicence, R.id.licenseValidity, R.id.shopAddress, R.id.licenseTYpe})
        List<TextView> textViews;
        @BindViews({R.id.amount})
        List<TextInputEditText> textInputEditTexts;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

        }
    }
}
