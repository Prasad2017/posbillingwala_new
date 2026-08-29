package com.posbillingwala.dealer.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.github.mikephil.charting.data.PieEntry;
import com.posbillingwala.dealer.Activity.MainActivity;
import com.posbillingwala.dealer.Extra.DetectConnection;
import com.posbillingwala.dealer.Extra.ReportUiHelper;
import com.posbillingwala.dealer.Model.AllApiResponse;
import com.posbillingwala.dealer.R;
import com.posbillingwala.dealer.Retrofit.Api;
import com.posbillingwala.dealer.databinding.FragmentHomeBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, UseCompatLoadingForDrawables, StaticFieldLeak")
public class Home extends Fragment implements View.OnClickListener {

    public static Activity activity;
    View view;
    FragmentHomeBinding binding;
    //AdView
    private AdView adView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        view = binding.getRoot();


        activity = getActivity();
        MainActivity.title.setText("Dashboard");

        initAds();

        binding.customerRegistration.setOnClickListener(this);
        binding.onBoardCustomerList.setOnClickListener(this);

        return view;

    }

    private void initAds() {

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(activity, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
                // on below line displaying a log that admob ads has been initialized.
                Log.i("Admob", "Admob Initialized." + initializationStatus);
            }
        });

        adView = view.findViewById(R.id.ad_view);
        // Create an ad request.
        AdRequest adRequest = new AdRequest.Builder().build();
        // Start loading the ad in the background.
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                Log.e("loadAdError", "" + loadAdError);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
            }

            @Override
            public void onAdSwipeGestureClicked() {
                super.onAdSwipeGestureClicked();
            }
        });

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.customerRegistration) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new CustomerRegistration(), true);
        } else if (view.getId() == R.id.onBoardCustomerList) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new AllCustomerList(), true);
        }
    }

    public void onStart() {
        super.onStart();
        Log.e("onStart", "called");
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(0);
        MainActivity.drawerLayout.closeDrawers();
        if (DetectConnection.checkInternetConnection(activity)) {
            getCustomerCount();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void getCustomerCount() {

        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().getCustomerCount(MainActivity.userId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AllApiResponse body = response.body();
                    String total = ReportUiHelper.nz(body.getTotalCustomer());
                    if ("0".equals(total) && body.getMessage() != null) {
                        total = ReportUiHelper.nz(body.getMessage());
                    }
                    binding.totalCustomer.setText(total);
                    binding.activeCustomer.setText(ReportUiHelper.nz(body.getActiveCustomer()));
                    binding.trialCustomer.setText(ReportUiHelper.nz(body.getTrialCustomer()));
                    binding.expiredCustomer.setText(ReportUiHelper.nz(body.getExpiredCustomer()));
                    List<PieEntry> entries = new ArrayList<>();
                    entries.add(new PieEntry(parse(body.getActiveCustomer()), "Active"));
                    entries.add(new PieEntry(parse(body.getTrialCustomer()), "Trial"));
                    entries.add(new PieEntry(parse(body.getExpiredCustomer()), "Expired"));
                    ReportUiHelper.setupDonut(binding.chartDonut, entries,
                            Arrays.asList(Color.parseColor("#16A34A"), Color.parseColor("#EA580C"), Color.parseColor("#DC2626")),
                            total);
                    ReportUiHelper.fillLegend(binding.legendContainer,
                            new String[]{"Active", "Trial", "Expired"},
                            new String[]{body.getActiveCustomer(), body.getTrialCustomer(), body.getExpiredCustomer()},
                            new String[]{body.getActivePercent(), body.getTrialPercent(), body.getExpiredPercent()},
                            new int[]{Color.parseColor("#16A34A"), Color.parseColor("#EA580C"), Color.parseColor("#DC2626")});
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


    private float parse(String v) {
        try {
            return Float.parseFloat(ReportUiHelper.nz(v));
        } catch (Exception e) {
            return 0f;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (adView != null) {
            adView.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (adView != null) {
            adView.destroy();
        }
    }


}