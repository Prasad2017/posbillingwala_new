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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.jaredrummler.materialspinner.MaterialSpinner;
import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Extra.LicenceValidityTiers;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;

import java.util.List;
import java.util.Random;

import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, UseCompatLoadingForDrawables, StaticFieldLeak")
public class NewLicenceRegistration extends Fragment {

    public static Activity activity;
    View view;
    @BindViews({R.id.customerName, R.id.customerNumber, R.id.customerAddress, R.id.customerShopName, R.id.branchName, R.id.amount})
    List<TextInputEditText> textInputEditTexts;
    @BindViews({R.id.fastBilling, R.id.dineIn, R.id.takeAway})
    List<CheckBox> checkBoxes;
    @BindViews({R.id.licenseValidity})
    List<MaterialSpinner> autoCompleteTextViews;
    String[] licenseValidityList;
    String fastBilling = "0", dineIn = "0", takeAway = "0", customerId, licenceValidity, licenceType;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_new_licence_registration, container, false);
        ButterKnife.bind(this, view);

        activity = getActivity();
        MainActivity.title.setText("Licence Registration");

        Bundle bundle = getArguments();
        if (bundle != null) {
            customerId = bundle.getString("customerId");
        }

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
            autoCompleteTextViews.get(0).setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        autoCompleteTextViews.get(0).setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                licenceValidity = LicenceValidityTiers.toDayCount(item);
                if (LicenceValidityTiers.isRegularTier(licenceValidity)) {
                    licenceType = "Regular";
                    textInputEditTexts.get(5).setText("");
                } else {
                    licenceType = "Demo";
                    textInputEditTexts.get(5).setText("0");
                }
            }
        });

        checkBoxes.get(0).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    fastBilling = "1";
                } else {
                    fastBilling = "0";
                }
            }
        });

        checkBoxes.get(1).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    dineIn = "1";
                } else {
                    dineIn = "0";
                }
            }
        });

        checkBoxes.get(2).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    takeAway = "1";
                } else {
                    takeAway = "0";
                }
            }
        });


        return view;

    }

    @OnClick({R.id.submitRegistration})
    public void onClick(View view) {
        if (view.getId() == R.id.submitRegistration) {
            if (textInputEditTexts.get(0).getText().toString().length() > 0 && textInputEditTexts.get(1).getText().toString().length() > 0 &&
                    textInputEditTexts.get(2).getText().toString().length() > 0 && textInputEditTexts.get(3).getText().toString().length() > 0 &&
                    textInputEditTexts.get(4).getText().toString().length() > 0 &&
                    textInputEditTexts.get(5).getText().toString().length() > 0) {
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

        Call<AllApiResponse> call = Api.getClient().customerNewLicenceRegistration(customerId, textInputEditTexts.get(0).getText().toString(), textInputEditTexts.get(1).getText().toString(),
                textInputEditTexts.get(2).getText().toString(), textInputEditTexts.get(3).getText().toString(), textInputEditTexts.get(4).getText().toString(), licenseKey, licenceValidity,
                licenceType, textInputEditTexts.get(5).getText().toString(), fastBilling, dineIn, takeAway);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("true")) {

                        final Dialog dialog = new Dialog(getActivity());
                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
                        dialog.setContentView(R.layout.confirmation_dialog);
                        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
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