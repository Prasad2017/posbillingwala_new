package com.posbillingwala.admin.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.jaredrummler.materialspinner.MaterialSpinner;
import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Extra.LicenceValidityTiers;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentCustomerRegistrationBinding;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


@SuppressLint("SetTextI18n, NonConstantResourceId, UseCompatLoadingForDrawables, StaticFieldLeak")
public class CustomerRegistration extends Fragment implements View.OnClickListener {

    public static Activity activity;
    View view;
    FragmentCustomerRegistrationBinding binding;
    String[] licenseValidityList;
    String fastBilling = "0", dineIn = "0", takeAway = "0", mess = "0", licenceValidity, licenceType;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCustomerRegistrationBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();
        MainActivity.title.setText("Customer Registration");

        MainActivity.back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new Home(), false);
            }
        });

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                    ((MainActivity) activity).loadFragment(new Home(), false);
                    return true;
                }
                return false;
            }
        });

        try {
            licenseValidityList = getResources().getStringArray(R.array.license_validity);
            final ArrayAdapter adapter = new ArrayAdapter(activity, android.R.layout.simple_spinner_item, licenseValidityList);
            adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
            binding.licenseValidity.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        binding.licenseValidity.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                licenceValidity = LicenceValidityTiers.toDayCount(item);
                if (LicenceValidityTiers.isRegularTier(licenceValidity)) {
                    licenceType = "Regular";
                    binding.amount.setText("");
                } else {
                    licenceType = "Demo";
                    binding.amount.setText("0");
                }
            }
        });

        binding.fastBilling.setOnCheckedChangeListener((buttonView, isChecked) -> fastBilling = isChecked ? "1" : "0");
        binding.dineIn.setOnCheckedChangeListener((buttonView, isChecked) -> dineIn = isChecked ? "1" : "0");
        binding.takeAway.setOnCheckedChangeListener((buttonView, isChecked) -> takeAway = isChecked ? "1" : "0");
        binding.mess.setOnCheckedChangeListener((buttonView, isChecked) -> mess = isChecked ? "1" : "0");

        binding.submitRegistration.setOnClickListener(this);

        return view;

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.submitRegistration) {
            if (binding.customerName.getText().toString().length() > 0 && binding.customerNumber.getText().toString().length() > 0 &&
                    binding.customerAddress.getText().toString().length() > 0 && binding.customerShopName.getText().toString().length() > 0 &&
                    binding.amount.getText().toString().length() > 0) {
                if (licenceValidity != null) {
                    if (!DetectConnection.checkInternetConnection(activity)) {
                        DetectConnection.noInternetConnection(activity);
                        return;
                    }
                    submitRegistration();
                } else {
                    Toast.makeText(activity, "Please select licence validity", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(activity, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void submitRegistration() {

        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().customerRegistration(
                binding.customerName.getText().toString(),
                binding.customerNumber.getText().toString(),
                binding.customerAddress.getText().toString(),
                binding.customerShopName.getText().toString(),
                licenceValidity,
                licenceType,
                binding.amount.getText().toString(),
                fastBilling,
                takeAway,
                dineIn,
                mess);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getStatus() != null && response.body().getStatus().equalsIgnoreCase("true")) {

                        String serverKey = response.body().getLicenseKey();
                        if (serverKey == null || serverKey.isEmpty()) {
                            serverKey = "(see customer details)";
                        }

                        final Dialog dialog = new Dialog(getActivity());
                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        dialog.setContentView(R.layout.confirmation_dialog);
                        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        dialog.setCancelable(false);

                        TextView txtYes = dialog.findViewById(R.id.yes);
                        TextView txtMessage = dialog.findViewById(R.id.message);

                        String message = "Registration completed. License key (give to customer):</br><b><font color='#ff0000'>"
                                + serverKey + "</font></b></br>Device status: NOT ACTIVATED";
                        txtMessage.setText(Html.fromHtml(message));

                        txtYes.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                                ((MainActivity) activity).loadFragment(new Home(), false);
                            }
                        });

                        dialog.show();

                    } else {
                        Toast.makeText(activity, "" + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE);
                sweetAlertDialog.setTitleText("Oops...");
                sweetAlertDialog.setContentText("Unable to create customer. Please try again.");
                sweetAlertDialog.setCancelClickListener(SweetAlertDialog::dismiss).show();
            }
        });
    }

    public void onStart() {
        super.onStart();
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(1);
        MainActivity.drawerLayout.closeDrawers();
        if (!DetectConnection.checkInternetConnection(activity)) {
            DetectConnection.noInternetConnection(activity);
        }
    }
}
