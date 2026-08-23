package com.posbillingwala.admin.Fragment;

import android.annotation.SuppressLint;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Adapter.LicenseAdapter;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.CustomerResponse;
import com.posbillingwala.admin.Model.LicenseResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, UseCompatLoadingForDrawables, StaticFieldLeak")
public class CustomerDetails extends Fragment {


    public static Activity activity;
    public static String customerId;
    View view;
    List<CustomerResponse> customerResponseList = new ArrayList<>();
    List<LicenseResponse> licenseResponseList = new ArrayList<>();
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindViews({R.id.customerName, R.id.customerMobileNumber, R.id.customerAddress, R.id.customerShopName})
    List<TextInputEditText> textInputEditTexts;
    LicenseAdapter licenseAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_customer_details, container, false);
        ButterKnife.bind(this, view);

        activity = getActivity();
        MainActivity.title.setText("Customer Details");

        Bundle bundle = getArguments();
        if (bundle != null) {
            customerId = bundle.getString("customerId");
        }

        MainActivity.back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new AllCustomerList(), false);
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
                    ((MainActivity) activity).loadFragment(new AllCustomerList(), false);
                    return true;
                }
                return false;
            }
        });

        return view;

    }

    @OnClick({R.id.updateCustomer})
    public void onClick(View view) {
        if (view.getId() == R.id.updateCustomer) {
            if (textInputEditTexts.get(0).getText().toString().length() > 0) {
                if (textInputEditTexts.get(1).getText().toString().length() > 0) {
                    if (textInputEditTexts.get(2).getText().toString().length() > 0) {
                        if (textInputEditTexts.get(3).getText().toString().length() > 0) {
                            updateCustomerDetails();
                        } else {
                            textInputEditTexts.get(3).setError("Please fill this");
                        }
                    } else {
                        textInputEditTexts.get(2).setError("Please fill this");
                    }
                } else {
                    textInputEditTexts.get(1).setError("Please fill this");
                }
            } else {
                textInputEditTexts.get(0).setError("Please fill this");
            }
        }
    }

    private void updateCustomerDetails() {

        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().updateCustomerDetails(customerId, textInputEditTexts.get(0).getText().toString(), textInputEditTexts.get(1).getText().toString(),
                textInputEditTexts.get(2).getText().toString(), textInputEditTexts.get(3).getText().toString());
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                        Toast.makeText(activity, "" + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        getCustomerDetails();
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
        ((MainActivity) activity).lockUnlockDrawer(1);
        MainActivity.drawerLayout.closeDrawers();
        if (DetectConnection.checkInternetConnection(activity)) {
            getCustomerDetails();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void getCustomerDetails() {

        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        customerResponseList.clear();
        licenseResponseList.clear();

        Call<AllApiResponse> call = Api.getClient().getCustomerDetails(customerId);
        call.enqueue(new Callback<AllApiResponse>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    customerResponseList = response.body().getCustomerResponseList();
                    if (customerResponseList.size() > 0) {

                        textInputEditTexts.get(0).setText("" + customerResponseList.get(0).getName());
                        textInputEditTexts.get(1).setText("" + customerResponseList.get(0).getContactNumber());
                        textInputEditTexts.get(2).setText("" + customerResponseList.get(0).getAddress());
                        textInputEditTexts.get(3).setText("" + customerResponseList.get(0).getShopName());

                        licenseResponseList = customerResponseList.get(0).getLicenseResponseList();

                        if (licenseResponseList.size() > 0) {
                            licenseAdapter = new LicenseAdapter(activity, licenseResponseList, customerId);
                            recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                            recyclerView.setAdapter(licenseAdapter);
                            licenseAdapter.notifyDataSetChanged();
                            recyclerView.setHasFixedSize(true);
                        }
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

}