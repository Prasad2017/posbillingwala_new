package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.CustomerResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;


import java.util.ArrayList;
import java.util.List;

import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class DealerProfile extends Fragment {

    public static Activity activity;
    View view;
    @BindViews({R.id.dealerName, R.id.dealerNumber, R.id.dealerEmail, R.id.dealerAddress, R.id.aadhaarNumber})
    List<TextInputEditText> textInputEditTexts;
    List<CustomerResponse> customerResponseList = new ArrayList<>();
    String dealerId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_dealer_profile, container, false);
        ButterKnife.bind(this, view);

        activity = getActivity();
        MainActivity.title.setText("Profile");

        Bundle bundle = getArguments();
        if (bundle!=null) {
            dealerId = bundle.getString("dealerId");
        }

        MainActivity.back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new AllDealerList(), true);
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
                    ((MainActivity) activity).loadFragment(new AllDealerList(), true);
                    return true;
                }
                return false;
            }
        });


        return view;

    }

    @OnClick({R.id.update})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.update:

                if (textInputEditTexts.get(0).getText().toString().length() > 0 && textInputEditTexts.get(1).getText().toString().length() > 0 &&
                        textInputEditTexts.get(2).getText().toString().length() > 0 && textInputEditTexts.get(3).getText().toString().length() > 0 &&
                        textInputEditTexts.get(4).getText().toString().length() > 0) {
                    updateProfile();
                } else {
                    Toast.makeText(activity, "Please fill all fields", Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }

    private void updateProfile() {

        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().updateDealerProfile(dealerId, textInputEditTexts.get(0).getText().toString(), textInputEditTexts.get(1).getText().toString(),
                textInputEditTexts.get(2).getText().toString(), textInputEditTexts.get(3).getText().toString(), textInputEditTexts.get(4).getText().toString());
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                        Toast.makeText(activity, "" + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        getProfile();
                    } else {
                        Toast.makeText(activity, "" + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Toast.makeText(activity, "" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void onStart() {
        super.onStart();
        Log.e("onStart", "called");
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(0);
        MainActivity.drawerLayout.closeDrawers();
        if (DetectConnection.checkInternetConnection(activity)) {
            getProfile();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void getProfile() {

        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().getProfile(dealerId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    customerResponseList = response.body().getCustomerResponseList();
                    if (customerResponseList.size() > 0) {

                        textInputEditTexts.get(0).setText(customerResponseList.get(0).getName());
                        textInputEditTexts.get(1).setText(customerResponseList.get(0).getContactNumber());
                        textInputEditTexts.get(2).setText(customerResponseList.get(0).getEmail());
                        textInputEditTexts.get(3).setText(customerResponseList.get(0).getAddress());
                        textInputEditTexts.get(4).setText(customerResponseList.get(0).getAadharNumber());

                    }
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                Log.e("profileError", "" + t.getMessage());
                pDialog.dismiss();
            }
        });

    }

}