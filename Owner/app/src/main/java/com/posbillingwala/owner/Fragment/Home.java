package com.posbillingwala.owner.Fragment;

import static com.posbillingwala.owner.Utils.RequestCodes.directory_path;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
import com.posbillingwala.owner.Extra.ReportUiHelper;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.BranchComparisonResponse;
import com.posbillingwala.owner.Model.CustomerResponse;
import com.posbillingwala.owner.Model.LicenseResponse;
import com.github.mikephil.charting.data.PieEntry;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.FragmentHomeBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    private final Handler homeClockHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat homeDateTimeFormat =
            new SimpleDateFormat("EEE, dd MMM yyyy  hh:mm:ss a", Locale.getDefault());
    private final Runnable homeClockRunnable = new Runnable() {
        @Override
        public void run() {
            updateHomeDateTime();
            homeClockHandler.postDelayed(this, 1000);
        }
    };

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
                    getProfile();
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

        binding.storeWiseLayout.setOnClickListener(v -> openStoreWise("totalSale"));
        binding.outletsCard.setOnClickListener(v -> openStoreWise("totalSale"));

        updateNetworkStatusDot();
        refreshOutletSummary();
        return view;
    }

    private void openStoreWise(String saleDate) {
        InvoiceStoreWise fragment = new InvoiceStoreWise();
        Bundle args = new Bundle();
        args.putString("saleDate", saleDate);
        fragment.setArguments(args);
        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
        ((MainActivity) activity).loadFragment(fragment, true);
    }

    private void openHomeFragment(Fragment fragment) {
        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
        ((MainActivity) activity).loadFragment(fragment, true);
    }

    private void openSalesDashboard(String branchId) {
        SalesDashboard fragment = new SalesDashboard();
        if (branchId != null && !branchId.trim().isEmpty()) {
            Bundle args = new Bundle();
            args.putString("branchId", branchId);
            fragment.setArguments(args);
        }
        openHomeFragment(fragment);
    }

    private void openOutletInvoices(String licenceId) {
        OrderInvoice fragment = new OrderInvoice();
        Bundle bundle = new Bundle();
        bundle.putString("pageName", "store");
        bundle.putString("licenceId", licenceId);
        bundle.putString("saleDate", "totalSale");
        fragment.setArguments(bundle);
        openHomeFragment(fragment);
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
            getProfile();
        } else {
            DetectConnection.noInternetConnection(activity);
        }

        requestPermission();
        updateNetworkStatusDot();
    }

    @Override
    public void onResume() {
        super.onResume();
        startHomeClock();
        updateNetworkStatusDot();
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopHomeClock();
        if (adView != null) {
            adView.pause();
        }
    }

    @Override
    public void onDestroyView() {
        stopHomeClock();
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroyView();
    }

    private void startHomeClock() {
        stopHomeClock();
        homeClockHandler.post(homeClockRunnable);
    }

    private void stopHomeClock() {
        homeClockHandler.removeCallbacks(homeClockRunnable);
    }

    private void updateHomeDateTime() {
        if (binding == null || binding.homeDateTime == null) {
            return;
        }
        binding.homeDateTime.setText(homeDateTimeFormat.format(new Date()));
        refreshOutletSummary();
    }

    private void refreshOutletSummary() {
        if (binding == null || !isAdded()) {
            return;
        }
        int outlets = Math.max(MainActivity.branchCount, MainActivity.licenseCount);
        String greeting = getGreeting();
        if (outlets <= 1) {
            binding.ownerShopName.setText(greeting + " · " + getString(R.string.home_outlet_summary_one));
        } else {
            binding.ownerShopName.setText(greeting + " · " + getString(R.string.home_outlet_summary, outlets, outlets));
        }
        if (binding.totalOutlets != null) {
            binding.totalOutlets.setText(String.valueOf(Math.max(0, outlets)));
        }
        if (binding.compareBranchesLayout != null) {
            binding.compareBranchesLayout.setVisibility(outlets > 1 ? View.VISIBLE : View.GONE);
        }
        if (binding.storeWiseLayout != null) {
            binding.storeWiseLayout.setVisibility(outlets >= 1 ? View.VISIBLE : View.GONE);
        }
    }

    private String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) {
            return getString(R.string.good_morning);
        } else if (hour >= 12 && hour < 17) {
            return getString(R.string.good_afternoon);
        } else if (hour >= 17 && hour < 21) {
            return getString(R.string.good_evening);
        } else {
            return getString(R.string.good_night);
        }
    }

    private void updateNetworkStatusDot() {
        if (binding == null || binding.networkStatusDot == null || activity == null) {
            return;
        }
        boolean online = DetectConnection.checkInternetConnection(activity);
        int color = ContextCompat.getColor(activity, online ? R.color.green_700 : R.color.red_900);
        if (binding.networkStatusDot.getBackground() != null) {
            binding.networkStatusDot.getBackground().mutate().setTint(color);
        }
        binding.networkStatusDot.setContentDescription(
                getString(online ? R.string.status_online : R.string.status_offline));
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
                if (binding == null || !isAdded()) {
                    dismissLoader();
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    List<CustomerResponse> list = response.body().getCustomerResponseList();
                    customerResponseList = list != null ? list : new ArrayList<>();
                    if (!customerResponseList.isEmpty()) {
                        CustomerResponse customer = customerResponseList.get(0);
                        String businessName = customer.getShopName();
                        if (businessName == null || businessName.trim().isEmpty()) {
                            businessName = customer.getName();
                        }
                        if (businessName != null && !businessName.trim().isEmpty()) {
                            binding.shopName.setText(businessName.trim());
                        }
                        Common.saveUserData(activity, "shopName",
                                customer.getShopName() != null ? customer.getShopName() : "");
                        Common.saveUserData(activity, "customerName",
                                customer.getName() != null ? customer.getName() : "");
                        List<LicenseResponse> licenses = customer.getLicenseResponseList();
                        int licenseCount = licenses != null ? licenses.size() : 0;
                        if (licenseCount > 0) {
                            MainActivity.setOutletCounts(licenseCount);
                        }
                        MainActivity.reportPin = customer.getReportPin();
                        Common.saveUserData(activity, "reportPin", "" + customer.getReportPin());
                        refreshOutletSummary();
                    }
                }
                getTotalCount();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                dismissLoader();
                Log.e("serverError", t.getMessage());
            }
        });
    }

    public void getTotalCount() {
        Call<AllApiResponse> call = Api.getClient().getTotalCount(MainActivity.userId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if ("true".equalsIgnoreCase(response.body().getStatus())) {
                        if (binding != null) {
                            binding.totalCategory.setText(response.body().getTotalCategory());
                            binding.totalProduct.setText(response.body().getTotalProduct());
                            binding.totalSale.setText(ReportUiHelper.money(response.body().getTotalSale()));
                            binding.todaySale.setText(ReportUiHelper.money(response.body().getTodaySale()));
                            bindSalesAndCatalogCharts(response.body());
                        }
                        String branchCount = response.body().getBranchCount();
                        int branches = 0;
                        try {
                            branches = branchCount != null ? Integer.parseInt(branchCount) : 0;
                        } catch (NumberFormatException ignored) {
                        }
                        MainActivity.setOutletCounts(branches);
                        refreshOutletSummary();
                        loadBranchMixChart();
                    } else if (binding != null) {
                        binding.totalCategory.setText("0");
                        binding.totalProduct.setText("0");
                        binding.totalSale.setText(ReportUiHelper.money(0f));
                        binding.todaySale.setText(ReportUiHelper.money(0f));
                    }
                }
                dismissLoader();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                dismissLoader();
                Log.e("serverError", t.getMessage());
            }
        });
    }

    private void dismissLoader() {
        if (pDialog != null && pDialog.isShowing()) {
            pDialog.dismiss();
        }
        if (binding != null) {
            binding.swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void bindSalesAndCatalogCharts(AllApiResponse body) {
        if (binding == null || body == null) {
            return;
        }
        float today = ReportUiHelper.parseAmount(body.getTodaySale());
        float total = ReportUiHelper.parseAmount(body.getTotalSale());
        float earlier = Math.max(0f, total - today);
        int todayColor = Color.parseColor("#4862b7");
        int earlierColor = Color.parseColor("#C7D2FE");
        java.util.List<PieEntry> salesEntries = new ArrayList<>();
        String salesCenter;
        if (today <= 0f && earlier <= 0f) {
            salesEntries.add(new PieEntry(1f, getString(R.string.home_no_chart_data)));
            salesCenter = getString(R.string.home_no_chart_data);
            ReportUiHelper.setupDonut(binding.chartSalesMix, salesEntries,
                    Arrays.asList(Color.parseColor("#E5E7EB")), salesCenter);
            binding.chartSalesMix.setHighlightPerTapEnabled(false);
            ReportUiHelper.fillLegend(binding.legendSalesMix,
                    new String[]{getString(R.string.home_today_sales), getString(R.string.home_earlier_sales)},
                    new String[]{ReportUiHelper.money(0f), ReportUiHelper.money(0f)},
                    new String[]{"0", "0"},
                    new int[]{todayColor, earlierColor});
        } else {
            if (today > 0f) {
                salesEntries.add(new PieEntry(today, getString(R.string.home_today_sales), "today"));
            }
            if (earlier > 0f) {
                salesEntries.add(new PieEntry(earlier, getString(R.string.home_earlier_sales), "earlier"));
            }
            salesCenter = ReportUiHelper.money(total);
            java.util.List<Integer> salesColorList = new ArrayList<>();
            String[] salesLabels = new String[salesEntries.size()];
            String[] salesCounts = new String[salesEntries.size()];
            String[] salesPct = new String[salesEntries.size()];
            int[] salesColors = new int[salesEntries.size()];
            for (int i = 0; i < salesEntries.size(); i++) {
                PieEntry pe = salesEntries.get(i);
                int color = "today".equals(pe.getData()) ? todayColor : earlierColor;
                salesColorList.add(color);
                salesLabels[i] = pe.getLabel();
                salesCounts[i] = ReportUiHelper.money(pe.getValue());
                salesPct[i] = ReportUiHelper.percentOf(pe.getValue(), total);
                salesColors[i] = color;
            }
            ReportUiHelper.setupDonut(binding.chartSalesMix, salesEntries, salesColorList, salesCenter);
            ReportUiHelper.fillLegend(binding.legendSalesMix, salesLabels, salesCounts, salesPct, salesColors,
                    index -> binding.chartSalesMix.highlightValue(index, 0));
        }
        ReportUiHelper.bindDonutSelection(binding.chartSalesMix, binding.salesMixDetail,
                binding.legendSalesMix, salesCenter, total, true, index -> {
                    PieEntry pe = pieEntryAt(binding.chartSalesMix, index);
                    Object data = pe != null ? pe.getData() : null;
                    if ("today".equals(data) || (pe != null && getString(R.string.home_today_sales).equals(pe.getLabel()))) {
                        openStoreWise("todaySale");
                    } else {
                        openHomeFragment(new SalesOverview());
                    }
                });

        float categories = ReportUiHelper.parseAmount(body.getTotalCategory());
        float products = ReportUiHelper.parseAmount(body.getTotalProduct());
        float catalogTotal = categories + products;
        int catColor = Color.parseColor("#2563EB");
        int prodColor = Color.parseColor("#16A34A");
        java.util.List<PieEntry> catalogEntries = new ArrayList<>();
        String catalogCenter;
        if (catalogTotal <= 0f) {
            catalogEntries.add(new PieEntry(1f, getString(R.string.home_no_chart_data)));
            catalogCenter = "0";
            ReportUiHelper.setupDonut(binding.chartCatalogMix, catalogEntries,
                    Arrays.asList(Color.parseColor("#E5E7EB")), catalogCenter);
            binding.chartCatalogMix.setHighlightPerTapEnabled(false);
        } else {
            if (categories > 0f) {
                catalogEntries.add(new PieEntry(categories, getString(R.string.home_total_category), "category"));
            }
            if (products > 0f) {
                catalogEntries.add(new PieEntry(products, getString(R.string.home_total_products), "product"));
            }
            java.util.List<Integer> catalogColorList = new ArrayList<>();
            String[] catLabels = new String[catalogEntries.size()];
            String[] catCounts = new String[catalogEntries.size()];
            String[] catPct = new String[catalogEntries.size()];
            int[] catColors = new int[catalogEntries.size()];
            for (int i = 0; i < catalogEntries.size(); i++) {
                PieEntry pe = catalogEntries.get(i);
                int color = "product".equals(pe.getData()) ? prodColor : catColor;
                catalogColorList.add(color);
                catLabels[i] = pe.getLabel();
                catCounts[i] = String.valueOf(Math.round(pe.getValue()));
                catPct[i] = ReportUiHelper.percentOf(pe.getValue(), catalogTotal);
                catColors[i] = color;
            }
            catalogCenter = String.valueOf(Math.round(catalogTotal));
            ReportUiHelper.setupDonut(binding.chartCatalogMix, catalogEntries, catalogColorList, catalogCenter);
            ReportUiHelper.fillLegend(binding.legendCatalogMix, catLabels, catCounts, catPct, catColors,
                    index -> binding.chartCatalogMix.highlightValue(index, 0));
        }
        if (catalogTotal <= 0f) {
            ReportUiHelper.fillLegend(binding.legendCatalogMix,
                    new String[]{getString(R.string.home_total_category), getString(R.string.home_total_products)},
                    new String[]{body.getTotalCategory(), body.getTotalProduct()},
                    new String[]{"0", "0"},
                    new int[]{catColor, prodColor});
        }
        ReportUiHelper.bindDonutSelection(binding.chartCatalogMix, binding.catalogMixDetail,
                binding.legendCatalogMix, catalogCenter, catalogTotal, false, index -> {
                    PieEntry pe = pieEntryAt(binding.chartCatalogMix, index);
                    Object data = pe != null ? pe.getData() : null;
                    if ("product".equals(data) || (pe != null && getString(R.string.home_total_products).equals(pe.getLabel()))) {
                        openHomeFragment(new AllCustomerProductList());
                    } else {
                        openHomeFragment(new AddCustomerProductCategory());
                    }
                });
    }

    private void loadBranchMixChart() {
        if (activity == null || MainActivity.userId == null) {
            return;
        }
        Api.getClient().getBranchComparison(MainActivity.userId).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (binding == null || !isAdded()) {
                    return;
                }
                List<BranchComparisonResponse> branches = response.body() != null
                        ? response.body().getBranchComparisonList() : null;
                bindBranchMixChart(branches);
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.e("branchMix", "" + t.getMessage());
            }
        });
    }

    private void bindBranchMixChart(List<BranchComparisonResponse> branches) {
        if (binding == null) {
            return;
        }
        int[] palette = new int[]{
                Color.parseColor("#4862b7"),
                Color.parseColor("#16A34A"),
                Color.parseColor("#EA580C"),
                Color.parseColor("#7C3AED"),
                Color.parseColor("#0891B2"),
                Color.parseColor("#DC2626"),
                Color.parseColor("#CA8A04"),
                Color.parseColor("#64748B")
        };
        java.util.List<PieEntry> entries = new ArrayList<>();
        java.util.List<Integer> colors = new ArrayList<>();
        java.util.List<String> labels = new ArrayList<>();
        java.util.List<String> counts = new ArrayList<>();
        float grand = 0f;
        if (branches != null) {
            for (BranchComparisonResponse b : branches) {
                grand += ReportUiHelper.parseAmount(b.getTotalSale());
            }
        }
        if (branches == null || branches.isEmpty() || grand <= 0f) {
            entries.add(new PieEntry(1f, getString(R.string.home_no_chart_data)));
            ReportUiHelper.setupDonut(binding.chartBranchMix, entries,
                    Arrays.asList(Color.parseColor("#E5E7EB")), getString(R.string.home_no_chart_data));
            ReportUiHelper.fillLegend(binding.legendBranchMix,
                    new String[]{getString(R.string.home_total_outlets)},
                    new String[]{ReportUiHelper.money(0f)},
                    new String[]{"0"},
                    new int[]{palette[0]});
            return;
        }
        int shown = Math.min(branches.size(), palette.length);
        float others = 0f;
        for (int i = 0; i < branches.size(); i++) {
            BranchComparisonResponse b = branches.get(i);
            float sale = ReportUiHelper.parseAmount(b.getTotalSale());
            if (sale <= 0f) {
                continue;
            }
            if (i < shown - 1 || branches.size() <= palette.length) {
                String name = b.getShopName1();
                if (name == null || name.trim().isEmpty()) {
                    name = b.getBranchLabel() != null ? b.getBranchLabel() : ("Outlet " + (i + 1));
                }
                entries.add(new PieEntry(sale, name, b));
                colors.add(palette[entries.size() - 1]);
                labels.add(name);
                counts.add(ReportUiHelper.money(sale));
            } else {
                others += sale;
            }
        }
        if (others > 0f) {
            entries.add(new PieEntry(others, "Others"));
            colors.add(palette[palette.length - 1]);
            labels.add("Others");
            counts.add(ReportUiHelper.money(others));
        }
        if (entries.isEmpty()) {
            entries.add(new PieEntry(1f, getString(R.string.home_no_chart_data)));
            colors.add(Color.parseColor("#E5E7EB"));
        }
        String branchCenter = ReportUiHelper.money(grand);
        ReportUiHelper.setupDonut(binding.chartBranchMix, entries, colors, branchCenter);
        String[] percents = new String[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            percents[i] = ReportUiHelper.percentOf(entries.get(i).getValue(), grand);
        }
        int[] colorArr = new int[colors.size()];
        for (int i = 0; i < colors.size(); i++) {
            colorArr[i] = colors.get(i);
        }
        ReportUiHelper.fillLegend(binding.legendBranchMix,
                labels.toArray(new String[0]),
                counts.toArray(new String[0]),
                percents,
                colorArr,
                index -> binding.chartBranchMix.highlightValue(index, 0));
        ReportUiHelper.bindDonutSelection(binding.chartBranchMix, binding.branchMixDetail,
                binding.legendBranchMix, branchCenter, grand, true, index -> {
                    PieEntry pe = pieEntryAt(binding.chartBranchMix, index);
                    if (pe != null && pe.getData() instanceof BranchComparisonResponse) {
                        BranchComparisonResponse b = (BranchComparisonResponse) pe.getData();
                        if (b.getBranchId() != null && !b.getBranchId().isEmpty()) {
                            openOutletInvoices(b.getBranchId());
                            return;
                        }
                    }
                    openHomeFragment(new BranchComparison());
                });
    }

    private PieEntry pieEntryAt(com.github.mikephil.charting.charts.PieChart chart, int index) {
        if (chart == null || chart.getData() == null || chart.getData().getDataSetCount() == 0) {
            return null;
        }
        com.github.mikephil.charting.data.PieDataSet set =
                (com.github.mikephil.charting.data.PieDataSet) chart.getData().getDataSetByIndex(0);
        if (set == null || index < 0 || index >= set.getEntryCount()) {
            return null;
        }
        return set.getEntryForIndex(index);
    }
}
