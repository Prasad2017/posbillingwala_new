package com.posbillingwala.dealer.Fragment;

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

import com.posbillingwala.dealer.Activity.MainActivity;
import com.posbillingwala.dealer.Extra.DetectConnection;
import com.posbillingwala.dealer.Extra.LicenceValidityTiers;
import com.posbillingwala.dealer.Model.AllApiResponse;
import com.posbillingwala.dealer.R;
import com.posbillingwala.dealer.Retrofit.Api;
import com.posbillingwala.dealer.databinding.FragmentNewLicenceRegistrationBinding;

import java.util.Random;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, UseCompatLoadingForDrawables, StaticFieldLeak")
public class NewLicenceRegistration extends Fragment implements View.OnClickListener {

    public static Activity activity;
    View view;
    String[] licenseValidityList;
    String fastBilling = "0", dineIn = "0", takeAway = "0", mess = "0", customerId, licenceValidity, licenceType, customerName, customerNumber, customerAddress, shopName;
    FragmentNewLicenceRegistrationBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNewLicenceRegistrationBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();
        MainActivity.title.setText("Licence Registration");

        Bundle bundle = getArguments();
        if (bundle != null) {
            customerId = bundle.getString("customerId");
            customerName = bundle.getString("customerName");
            customerNumber = bundle.getString("customerNumber");
            customerAddress = bundle.getString("customerAddress");
            shopName = bundle.getString("shopName");
        }

        binding.customerName.setText(customerName);
        binding.customerNumber.setText(customerNumber);
        binding.customerAddress.setText(customerAddress);
        binding.customerShopName.setText(shopName);

        MainActivity.back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new AllCustomerList(), true);
            }
        });

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                    ((MainActivity) activity).loadFragment(new AllCustomerList(), true);
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

        binding.fastBilling.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    fastBilling = "1";
                } else {
                    fastBilling = "0";
                }
            }
        });

        binding.dineIn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    dineIn = "1";
                } else {
                    dineIn = "0";
                }
            }
        });

        binding.takeAway.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    takeAway = "1";
                } else {
                    takeAway = "0";
                }
            }
        });

        binding.mess.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mess = "1";
                } else {
                    mess = "0";
                }
            }
        });

        binding.submitRegistration.setOnClickListener(this);

        return view;

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.submitRegistration) {
            if (!binding.customerName.getText().toString().isEmpty() && !binding.customerNumber.getText().toString().isEmpty() &&
                    !binding.customerAddress.getText().toString().isEmpty() && !binding.customerShopName.getText().toString().isEmpty() &&
                    !binding.branchName.getText().toString().isEmpty() &&
                    !binding.amount.getText().toString().isEmpty()) {
                if (licenceValidity != null) {
                    submitNewLicenceRegistration();
                } else {
                    Toast.makeText(activity, "Please select licence validity", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(activity, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void submitNewLicenceRegistration() {

        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        String licenseKey = getRandomString(10);

        Call<AllApiResponse> call = Api.getClient().customerNewLicenceRegistration(customerId, binding.customerName.getText().toString(), binding.customerNumber.getText().toString(),
                binding.customerAddress.getText().toString(), binding.customerShopName.getText().toString(), binding.branchName.getText().toString(), licenseKey, licenceValidity,
                licenceType, binding.amount.getText().toString(), fastBilling, dineIn, takeAway, mess);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("true")) {

                        final Dialog dialog = new Dialog(getActivity());
                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
                        dialog.setContentView(R.layout.confirmation_dialog);
                        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                        dialog.setCancelable(false);

                        TextView txtYes = dialog.findViewById(R.id.yes);
                        TextView txtMessage = dialog.findViewById(R.id.message);

                        String message = "Licence Registration completed successfully with license key </br><b><font color='#ff0000'>" + licenseKey + "</font</b>";
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
                sweetAlertDialog.setContentText("Something went wrong!");
                sweetAlertDialog.setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.dismiss();
                    }
                }).show();
            }
        });


    }

    private String getRandomString(final int sizeOfRandomString) {

        String ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM";

        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }

    public void onStart() {
        super.onStart();
        Log.e("onStart", "called");
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(1);
        MainActivity.drawerLayout.closeDrawers();
        if (DetectConnection.checkInternetConnection(activity)) {

        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }
}