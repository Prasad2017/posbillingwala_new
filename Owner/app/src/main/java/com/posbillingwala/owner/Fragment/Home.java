package com.posbillingwala.owner.Fragment;

import static com.posbillingwala.owner.Utils.RequestCodes.directory_path;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Extra.BottomSheetUi;
import com.posbillingwala.owner.Extra.Common;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.CustomerResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.FragmentHomeBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, StaticFieldLeak")
public class Home extends Fragment {

    public static Activity activity;
    public FragmentHomeBinding binding;
    List<CustomerResponse> customerResponseList = new ArrayList<>();
    SweetAlertDialog pDialog;
    InvoiceStoreWise invoiceStoreWise;
    Bundle bundle;
    //AdView
    public AdView adView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        activity = getActivity();

        initAds();
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);

        binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (DetectConnection.checkInternetConnection(activity)) {
                    getTotalCount();
                } else {
                    DetectConnection.noInternetConnection(activity);
                }
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        });

        // Setting up click listeners
        binding.userSettingIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new UserSetting(), true);
            }
        });

        binding.totalSaleLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new SalesOverview(), true);
            }
        });

        binding.todaySaleLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new SalesDashboard(), true);
            }
        });

        binding.compareBranchesLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new BranchComparison(), true);
            }
        });

        return view;
    }

    public void initAds() {
        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(activity, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
                Log.i("Admob", "Admob Initialized." + initializationStatus);
            }
        });

        adView = binding.adView;
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
    public void onStart() {
        super.onStart();
        if (DetectConnection.checkInternetConnection(activity)) {
            getTotalCount();
            getProfile();
        } else {
            DetectConnection.noInternetConnection(activity);
        }

        requestPermission();
    }

    public void createFolder() {
        File myDirectory = new File(directory_path);
        if (!myDirectory.exists()) {
            myDirectory.mkdirs();
        }
        Log.e("fileCreated", "" + myDirectory);
    }

    public void requestPermission() {
        Dexter.withContext(activity)
                .withPermissions(Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_CONNECT
                ).withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            createFolder();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    public void showSettingsDialog() {
        BottomSheetUi.showConfirm(activity, "Need Permissions",
                "This app needs permission to use this feature. You can grant them in app settings.",
                "GOTO SETTINGS", "Cancel", true, this::openSettings);
    }

    // Navigating user to app settings
    public void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }

    public void getProfile() {
        pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().getProfile(MainActivity.userId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    customerResponseList = response.body().getCustomerResponseList();
                    if (customerResponseList.size() > 0) {
                        binding.shopName.setText(customerResponseList.get(0).getName());
                        MainActivity.reportPin = response.body().getCustomerResponseList().get(0).getReportPin();
                        Common.saveUserData(activity, "reportPin", "" + response.body().getCustomerResponseList().get(0).getReportPin());
                    } else {
                        binding.shopName.setText("Hi POS Billingwala");
                    }
                }
                getTotalCount();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Log.e("serverError", t.getMessage());
            }
        });
    }

    public void getTotalCount() {
        Call<AllApiResponse> call = Api.getClient().getTotalCount(MainActivity.userId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("true")) {
                        binding.totalCategory.setText(response.body().getTotalCategory());
                        binding.totalProduct.setText(response.body().getTotalProduct());
                        binding.totalSale.setText(response.body().getTotalSale());
                        binding.todaySale.setText(response.body().getTodaySale());
                        String branchCount = response.body().getBranchCount();
                        int branches = 0;
                        try {
                            branches = branchCount != null ? Integer.parseInt(branchCount) : 0;
                        } catch (NumberFormatException ignored) {
                        }
                        MainActivity.branchCount = Math.max(1, branches);
                        binding.compareBranchesLayout.setVisibility(branches > 1 ? View.VISIBLE : View.GONE);
                    } else {
                        binding.totalCategory.setText("0");
                        binding.totalProduct.setText("0");
                        binding.totalSale.setText("0");
                        binding.todaySale.setText("0");
                    }
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Log.e("serverError", t.getMessage());
            }
        });
    }
}
