package com.pos_billingwala.Fragment;

import static android.app.Activity.RESULT_OK;
import static com.pos_billingwala.Utils.RequestCodes.directory_path;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.pos_billingwala.Activity.Login;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.BuildConfig;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.AppExecutors;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.Extra.BusinessHours;
import com.pos_billingwala.Extra.Common;
import com.pos_billingwala.Extra.LocalSalesAnalytics;
import com.pos_billingwala.Extra.LicenceExpiredUi;
import com.pos_billingwala.Extra.LicenseModules;
import com.pos_billingwala.Extra.LicenseValidator;
import com.pos_billingwala.Extra.DetectConnection;
import com.pos_billingwala.Extra.ErrorLogQueue;
import com.pos_billingwala.Extra.TabletUi;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.LocalSalesSnapshot;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Retrofit.Api;
import com.pos_billingwala.NetworkToOffline.CloudSyncNav;
import com.pos_billingwala.NetworkToOffline.NetworkDataFetcher;
import com.pos_billingwala.NetworkToOffline.Receiver.LicenceKeyReceiver;
import com.pos_billingwala.NetworkToOffline.Receiver.OfflineToNetworkReceiver;
import com.pos_billingwala.NetworkToOffline.UserSynchronizeData;
import com.pos_billingwala.Print.KOTWoosimPrnMng;
import com.pos_billingwala.Print.PrinterConnectionHelper;
import com.pos_billingwala.Print.WoosimPrnMng;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentHomeBinding;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Response;


@SuppressLint("SetTextI18n, Range, StaticFieldLeak, NonConstantResourceId")
public class Home extends Fragment implements View.OnClickListener {

    private static final int SALES_FILTER_TODAY = 0;
    private static final int SALES_FILTER_MONTH = 1;

    public static Activity activity;
    public static View fastBilling, tableBilling, takeAwayBilling, messBilling;
    public static View posBillingRow1, posBillingRow2;
    public static CardView totalSalesCardView, todaySalesCardView;
    /** When true, next onStart skips heavy DB/Bluetooth (used when Home is only a back-stack seed). */
    public static boolean deferHeavyWorkForNextStart = false;
    public final ActivityResultLauncher<Intent> storageActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                //here we will handle the result of our intent
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    //Android is 11(R) or above
                    if (Environment.isExternalStorageManager()) {
                        //Manage External Storage Permission is granted
                    } else {
                        //Manage External Storage Permission is denied
                    }
                }
            }
    );
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    LicenceKeyReceiver licenceKeyReceiver;
    OfflineToNetworkReceiver offlineToNetworkReceiver;
    private final BroadcastReceiver networkStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateOnlineStatusUi();
            if (DetectConnection.checkInternetConnection(context)) {
                ErrorLogQueue.flushAsync();
            }
        }
    };
    List<PrinterSettingResponse> printerSettingResponseList = new ArrayList<>();
    FragmentHomeBinding binding;

    private int salesPeriodFilter = SALES_FILTER_TODAY;

    BluetoothAdapter bluetoothAdapter;
    private final Handler homeClockHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat homeDateTimeFormat =
            new SimpleDateFormat("EEE, dd MMM yyyy • hh:mm:ss a", Locale.getDefault());
    private int lastGreetingHour = -1;
    private Bitmap cachedStoreLogo;
    private String cachedStoreLogoRaw;
    private List<CompanyResponse> cachedCompanyDetails;
    private final Runnable homeClockRunnable = new Runnable() {
        @Override
        public void run() {
            updateHomeDateTime();
            homeClockHandler.postDelayed(this, 1000);
        }
    };
    private final Runnable deferredPrinterConnectRunnable = this::getPrinterSettingDetails;

    private static final long LICENCE_CHECK_INTERVAL_MS = 60_000L;
    private static volatile long lastLicenceCheckAtMs = 0L;

    public static long getUnitBetweenDates(Date startDate, Date endDate, TimeUnit unit) {
        long timeDiff = endDate.getTime() - startDate.getTime();
        return unit.convert(timeDiff, TimeUnit.MILLISECONDS);
    }

    /**
     * Validates licence off the UI thread (RSA verify + optional DB). Throttled so returning
     * to Home after every fragment pop does not hitch the main thread.
     */
    public static void totalLicenceDays() {
        if (activity == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastLicenceCheckAtMs < LICENCE_CHECK_INTERVAL_MS) {
            return;
        }
        lastLicenceCheckAtMs = now;

        final android.content.Context appContext = activity.getApplicationContext();
        final String expireDate = MainActivity.LicenceKeyExpireDate != null
                ? MainActivity.LicenceKeyExpireDate : "";

        AppExecutors.get().io().execute(() -> {
            boolean shouldLogout = false;
            try {
                if (LicenseValidator.hasStoredPayload(appContext)) {
                    POSBillingWalaDatabase db = new POSBillingWalaDatabase(appContext);
                    LicenseValidator.ValidationResult result = LicenseValidator.validate(appContext, db);
                    shouldLogout = result == null || !result.valid;
                } else if (!expireDate.equalsIgnoreCase("")) {
                    Date c = Calendar.getInstance().getTime();
                    SimpleDateFormat todayDF = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String todayDate = todayDF.format(c);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                    Date startDate = dateFormat.parse(todayDate);
                    Date endDate = dateFormat.parse(expireDate);
                    long numberOfDays = getUnitBetweenDates(startDate, endDate, TimeUnit.DAYS);
                    // Valid through end of expiry day (aligned with server P4-1)
                    shouldLogout = numberOfDays < 0;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (Exception e) {
                Log.e("Home", "totalLicenceDays", e);
            }
            if (shouldLogout) {
                AppExecutors.get().main(Home::forceLogoutToLogin);
            }
        });
    }

    private static void forceLogoutToLogin() {
        if (activity == null) {
            return;
        }
        Common.saveUserData(activity, "userId", "");
        File file1 = new File("data/data/" + activity.getPackageName() + "/shared_prefs/" + Common.SHARED_PREF + ".xml");
        if (file1.exists()) {
            file1.delete();
        }
        Intent intent = new Intent(activity, Login.class);
        intent.putExtra(LicenceExpiredUi.EXTRA_SHOW_LICENCE_EXPIRED, true);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void setValidationUI() {
        applyModuleVisibility();
        if (activity == null) {
            return;
        }
        AppExecutors.get().io().execute(() -> {
            boolean restored = LicenseModules.hydrateMissingFlagsFromPayload(activity);
            if (restored) {
                AppExecutors.get().main(Home::applyModuleVisibility);
            }
        });
    }

    private static void applyModuleVisibility() {
        boolean showFast = LicenseModules.isEnabled(MainActivity.fastBilling);
        boolean showDineIn = LicenseModules.isEnabled(MainActivity.dineIn);
        boolean showTakeAway = LicenseModules.isEnabled(MainActivity.takeAway);
        boolean showMess = LicenseModules.isEnabled(MainActivity.mess);
        boolean tablet = activity != null && TabletUi.isTablet(activity);

        LicenseModules.setVisible(fastBilling, showFast);
        LicenseModules.setVisible(tableBilling, showDineIn);
        LicenseModules.setVisible(takeAwayBilling, showTakeAway);
        LicenseModules.setVisible(messBilling, showMess);
        LicenseModules.setVisible(posBillingRow1,
                showFast || showDineIn || (tablet && (showTakeAway || showMess)));
        LicenseModules.setVisible(posBillingRow2, !tablet && (showTakeAway || showMess));

        LicenseModules.setVisible(totalSalesCardView, LicenseModules.isEnabled(MainActivity.totalSaleData));
        LicenseModules.setVisible(todaySalesCardView, LicenseModules.isEnabled(MainActivity.todaySaleData));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here

        activity = getActivity();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(activity, getString(R.string.toast_bluetooth_is_not_supported_on_this_devic), Toast.LENGTH_LONG).show();
        }

        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);
        // Receivers registered in onResume / unregistered in onPause (single registration only)
        licenceKeyReceiver = new LicenceKeyReceiver();
        offlineToNetworkReceiver = new OfflineToNetworkReceiver();

        updateHomeHeader();

        binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (DetectConnection.checkInternetConnection(activity)) {
                    LicenceKeyReceiver.getLicenceKeyData(activity, true);
                    fetchHomeSalesFromCloud(true);
                } else {
                    getTotalCount();
                }
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        });

        initViews();
        applyTabletHomeLayout();
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        setValidationUI();

        return view;

    }

    private boolean enableBluetooth() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, return false indicating Bluetooth cannot be enabled
            requestBluetoothPermission();
            return false;
        } else {
            // Permission granted, proceed to enable Bluetooth if necessary
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
                return false; // Bluetooth is being enabled, return false until user responds
            } else {
                Log.e("enableBluetooth: ", "Bluetooth is already enabled");
                return true; // Bluetooth is already enabled
            }
        }
    }

    private void requestBluetoothPermission() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    1001);
        } else {
            // Permission already granted, proceed
            enableBluetooth();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed to enable Bluetooth
                enableBluetooth();
            } else {
                // Permission denied, show a message or handle accordingly
                Toast.makeText(activity, getString(R.string.toast_bluetooth_permission_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }


    public void initViews() {
        fastBilling = view.findViewById(R.id.fastBilling);
        tableBilling = view.findViewById(R.id.tableBilling);
        takeAwayBilling = view.findViewById(R.id.takeAwayBilling);
        messBilling = view.findViewById(R.id.messBilling);
        posBillingRow1 = view.findViewById(R.id.posBillingRow1);
        posBillingRow2 = view.findViewById(R.id.posBillingRow2);
        totalSalesCardView = view.findViewById(R.id.totalSalesCardView);
        todaySalesCardView = view.findViewById(R.id.todaySalesCardView);

        binding.userSettingIcon.setOnClickListener(this);
        binding.fastBilling.setOnClickListener(this);
        binding.tableBilling.setOnClickListener(this);
        binding.takeAwayBilling.setOnClickListener(this);
        binding.messBilling.setOnClickListener(this);
        binding.productCardView.setOnClickListener(this);
        binding.comboCardView.setOnClickListener(this);
        binding.subcategoryCardView.setOnClickListener(this);
        binding.hideShowTotalSale.setOnClickListener(this);
        binding.hideShowTodaySale.setOnClickListener(this);
        binding.fetchDataLayout.setOnClickListener(this);
        binding.synchronizeLayout.setOnClickListener(this);
        binding.catalogViewAll.setOnClickListener(this);
        binding.salesPeriodFilter.setOnClickListener(this);
        totalSalesCardView.setOnClickListener(this);
        todaySalesCardView.setOnClickListener(this);
        applySalesPeriodLabels(salesPeriodFilter);

    }

    /** On tablet, show all billing mode tiles in one landscape-friendly row. */
    private void applyTabletHomeLayout() {
        if (activity == null || !TabletUi.isTablet(activity)) {
            return;
        }
        LinearLayout row1 = binding.posBillingRow1;
        LinearLayout row2 = binding.posBillingRow2;
        if (row1 != null && row2 != null) {
            float density = activity.getResources().getDisplayMetrics().density;
            int gap = (int) (6 * density);
            moveBillingTileToRow(binding.takeAwayBilling, row2, row1, gap);
            moveBillingTileToRow(binding.messBilling, row2, row1, gap);
            row2.setVisibility(View.GONE);
        }
        applyTabletHomeStatsLayout();
    }

    /** Place sales KPIs and catalog counts side-by-side on tablet. */
    private void applyTabletHomeStatsLayout() {
        LinearLayout salesBlock = binding.homeSalesBlock;
        LinearLayout catalogBlock = binding.homeCatalogBlock;
        if (salesBlock == null || catalogBlock == null) {
            return;
        }
        ViewGroup parent = (ViewGroup) salesBlock.getParent();
        if (parent == null || catalogBlock.getParent() != parent) {
            return;
        }
        int salesIndex = parent.indexOfChild(salesBlock);
        int catalogIndex = parent.indexOfChild(catalogBlock);
        if (salesIndex < 0 || catalogIndex < 0 || catalogIndex != salesIndex + 1) {
            return;
        }
        parent.removeView(salesBlock);
        parent.removeView(catalogBlock);

        float density = activity.getResources().getDisplayMetrics().density;
        int gap = (int) (12 * density);

        LinearLayout statsRow = new LinearLayout(activity);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setBaselineAligned(false);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = gap;
        statsRow.setLayoutParams(rowParams);

        LinearLayout.LayoutParams blockParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        salesBlock.setLayoutParams(blockParams);
        catalogBlock.setLayoutParams(blockParams);
        catalogBlock.setPadding(gap, 0, 0, 0);

        statsRow.addView(salesBlock);
        statsRow.addView(catalogBlock);
        parent.addView(statsRow, salesIndex);
    }

    private static void moveBillingTileToRow(View tile, ViewGroup from, LinearLayout to, int gapPx) {
        if (tile == null || from == null || to == null || tile.getParent() != from) {
            return;
        }
        from.removeView(tile);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.setMarginStart(gapPx);
        tile.setLayoutParams(params);
        to.addView(tile);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.userSettingIcon) {
            ((MainActivity) activity).loadFragment(new UserSetting(), true);
        } else if (id == R.id.fastBilling) {
            if (LicenseModules.isEnabled(MainActivity.fastBilling)) {
                CreatePos createPos = new CreatePos();
                Bundle bundle = new Bundle();
                bundle.putString("tableNumber", "FS" + getRandomString(3));
                bundle.putString("cartOrderStatus", "fast_billing");
                createPos.setArguments(bundle);
                ((MainActivity) activity).loadFragment(createPos, true);
            } else {
                Toast.makeText(activity, getString(R.string.toast_you_have_not_selected_fast_billing_pleas), Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.tableBilling) {
            if (LicenseModules.isEnabled(MainActivity.dineIn)) {
                ((MainActivity) activity).loadFragment(new InvoiceCompanyTable(), true);
            } else {
                Toast.makeText(activity, getString(R.string.toast_you_have_not_selected_dinein_please_cont), Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.takeAwayBilling) {
            if (LicenseModules.isEnabled(MainActivity.takeAway)) {
                ((MainActivity) activity).loadFragment(new InvoiceTakeAway(), true);
            } else {
                Toast.makeText(activity, getString(R.string.toast_you_have_not_selected_take_away_please_c), Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.messBilling) {
            if (LicenseModules.isEnabled(MainActivity.mess)) {
                ((MainActivity) activity).loadFragment(new InvoiceMess(), true);
            } else {
                Toast.makeText(activity, getString(R.string.toast_you_have_not_selected_mess_please_contact), Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.subcategoryCardView) {
            ((MainActivity) activity).loadFragment(new AddSubcategory(), true);
        } else if (id == R.id.productCardView) {
            ((MainActivity) activity).loadFragment(new ProductMaster(), true);
        } else if (id == R.id.comboCardView) {
            ((MainActivity) activity).loadFragment(new ComboMaster(), true);
        } else if (id == R.id.catalogViewAll) {
            ((MainActivity) activity).loadFragment(new ProductMaster(), true);
        } else if (id == R.id.salesPeriodFilter) {
            showSalesPeriodMenu();
        } else if (id == R.id.hideShowTotalSale) {
            if (binding.totalSale.getTransformationMethod().equals(PasswordTransformationMethod.getInstance())) {
                binding.hideShowTotalSale.setImageResource(R.drawable.ic_hide);
                binding.totalSale.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                binding.hideShowTotalSale.setImageResource(R.drawable.ic_show);
                binding.totalSale.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        } else if (id == R.id.hideShowTodaySale) {
            if (binding.todaySaleAmount.getTransformationMethod().equals(PasswordTransformationMethod.getInstance())) {
                binding.hideShowTodaySale.setImageResource(R.drawable.ic_hide);
                binding.todaySaleAmount.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                binding.hideShowTodaySale.setImageResource(R.drawable.ic_show);
                binding.todaySaleAmount.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        } else if (id == R.id.fetchDataLayout) {
            confirmFetchData();
        } else if (id == R.id.synchronizeLayout) {
            CloudSyncNav.openFromUi(activity);
        } else if (id == R.id.totalSalesCardView) {
            if (LicenseModules.isEnabled(MainActivity.totalSaleData)) {
                ((MainActivity) activity).loadFragment(new SalesOverview(), true);
            }
        } else if (id == R.id.todaySalesCardView) {
            if (LicenseModules.isEnabled(MainActivity.todaySaleData)) {
                ((MainActivity) activity).loadFragment(new SalesDashboard(), true);
            }
        }

    }

    public void confirmFetchData() {

        BottomSheetUi.showConfirm(
                activity,
                getString(R.string.toast_do_you_want_to_confirm_to_fetch_from_clo),
                getString(R.string.toast_local_data_will_be_replaced_with_cloud_d),
                "YES",
                "NO",
                false,
                () -> {
                    if (DetectConnection.checkInternetConnection(activity)) {
                        NetworkDataFetcher.resetAndFetchAllData(activity, posBillingWalaDatabase);
                    } else {
                        DetectConnection.noInternetConnection(activity);
                    }
                });
    }

    public void confirmSynchronizeData() {

        BottomSheetUi.showConfirm(
                activity,
                getString(R.string.toast_do_you_want_to_confirm_to_send_on_cloud),
                getString(R.string.toast_your_offline_data_will_be_send_to_the_cl),
                "YES",
                "NO",
                false,
                () -> {
                    if (DetectConnection.checkInternetConnection(activity)) {
                        UserSynchronizeData.start(activity);
                    } else {
                        DetectConnection.noInternetConnection(activity);
                    }
                });
    }

    public String getRandomString(final int sizeOfRandomString) {

        String ALLOWED_CHARACTERS = "0123456789";

        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(0);
        if (deferHeavyWorkForNextStart) {
            deferHeavyWorkForNextStart = false;
            return;
        }
        if (!MainActivity.userId.equalsIgnoreCase("")) {
            totalLicenceDays();
        }
        requestPermissionsOnce();
        getTotalCount();
        getLowInventoryList();
        // Defer Bluetooth so first paint / dashboard bind stay smooth
        AppExecutors.get().removeMainCallbacks(deferredPrinterConnectRunnable);
        AppExecutors.get().postMainDelayed(deferredPrinterConnectRunnable, 600);
    }

    public void getPrinterSettingDetails() {
        AppExecutors.get().runDbThenMain(this, () -> {
            printerSettingResponseList = posBillingWalaDatabase.getPrinterSettingDetails();
        }, () -> {
            if (printerSettingResponseList == null || printerSettingResponseList.isEmpty()) {
                return;
            }
            final String bluetoothAddress = printerSettingResponseList.get(0).getBluetoothAddress() != null
                    ? printerSettingResponseList.get(0).getBluetoothAddress() : "";
            final String bluetoothKOTAddress = printerSettingResponseList.get(0).getBluetoothKOTAddress() != null
                    ? printerSettingResponseList.get(0).getBluetoothKOTAddress() : "";
            // Bluetooth enable prompt stays on main; connect off main to avoid Home hitch
            final boolean btOk = enableBluetooth();
            if (!btOk) {
                return;
            }
            AppExecutors.get().io().execute(() -> {
                try {
                    PrinterConnectionHelper.autoConnectBillPrinter(activity, bluetoothAddress);
                    PrinterConnectionHelper.autoConnectKotPrinter(activity, bluetoothKOTAddress);
                } catch (Exception e) {
                    Log.e("Home", "getPrinterSettingDetails connect", e);
                }
            });
        });
    }

    public void createFolder() {

        File myDirectory = new File(directory_path);
        if (!myDirectory.exists()) {
            myDirectory.mkdirs();
        }

    }

    @SuppressLint("Recycle")
    public void getTotalCount() {
        if (activity == null || posBillingWalaDatabase == null) {
            return;
        }
        final int filter = salesPeriodFilter;
        final String trendVsYesterday = getString(R.string.home_vs_yesterday);
        final String trendVsLastMonth = getString(R.string.home_vs_last_month);
        AppExecutors.get().db().execute(() -> {
            Date c = Calendar.getInstance().getTime();
            SimpleDateFormat todayDF = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat monthDF = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            String todayDate = todayDF.format(c);
            String monthPrefix = monthDF.format(c);

            String currencyName = MainActivity.currencyName;
            String totalSubcategory = "0";
            String totalProduct = "0";
            String totalCombo = "0";
            String totalSaleText = MainActivity.currencyName + " 0.00";
            String todaySaleText = MainActivity.currencyName + " 0.00";
            String totalSalesTrendText = "";
            String todaySalesTrendText = "";

            SQLiteDatabase database = null;
            Cursor cursor = null;
            try {
                database = posBillingWalaDatabase.getReadableDatabase();
                cursor = database.rawQuery("SELECT currencyName FROM " + POSBillingWalaDatabase.COMPANY_TABLE, null);
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndex("currencyName"));
                    if (name != null) {
                        String[] separated = name.trim().split(":");
                        try {
                            currencyName = separated[1];
                        } catch (Exception e) {
                            currencyName = "\u20B9";
                        }
                    } else {
                        currencyName = "\u20B9";
                    }
                }
                cursor.close();

                cursor = database.rawQuery("SELECT COUNT(subcategoryId) as totalSubcategory FROM " + POSBillingWalaDatabase.PRODUCT_SUBCATEGORY_TABLE + " WHERE subcategoryDeletedStatus = '0'", null);
                if (cursor.moveToNext()) {
                    totalSubcategory = cursor.getString(cursor.getColumnIndex("totalSubcategory"));
                }
                cursor.close();

                cursor = database.rawQuery("SELECT COUNT(productId) as totalProduct FROM " + POSBillingWalaDatabase.PRODUCT_TABLE + " WHERE categoryName !='' AND productDeletedStatus='0'", null);
                if (cursor.moveToNext()) {
                    totalProduct = cursor.getString(cursor.getColumnIndex("totalProduct"));
                }
                cursor.close();

                cursor = database.rawQuery("SELECT COUNT(comboId) as totalCombo FROM " + POSBillingWalaDatabase.COMBO_TABLE + " WHERE IFNULL(comboDeletedStatus,'0')='0'", null);
                if (cursor.moveToNext()) {
                    totalCombo = cursor.getString(cursor.getColumnIndex("totalCombo"));
                }
                cursor.close();

                if (filter == SALES_FILTER_MONTH) {
                    cursor = database.rawQuery("SELECT SUM(totalAmount) as totalAmount FROM " + POSBillingWalaDatabase.INVOICE_TABLE
                            + " WHERE invoiceDate LIKE ? AND " + POSBillingWalaDatabase.notRefundedClause(),
                            new String[]{monthPrefix + "%"});
                } else {
                    cursor = database.rawQuery("SELECT SUM(totalAmount) as totalAmount FROM " + POSBillingWalaDatabase.INVOICE_TABLE
                            + " WHERE " + POSBillingWalaDatabase.notRefundedClause(), null);
                }
                if (cursor.moveToNext()) {
                    float totalSaleAmount = parseAmountSafe(cursor.getString(cursor.getColumnIndex("totalAmount")));
                    totalSaleText = formatDisplayAmount(currencyName, totalSaleAmount);
                }
                cursor.close();

                cursor = database.rawQuery("SELECT SUM(totalAmount) as totalAmount FROM " + POSBillingWalaDatabase.INVOICE_TABLE
                        + " WHERE invoiceDate LIKE ? AND " + POSBillingWalaDatabase.notRefundedClause(),
                        new String[]{"%" + todayDate + "%"});
                if (cursor.moveToNext()) {
                    float todaySaleAmount = parseAmountSafe(cursor.getString(cursor.getColumnIndex("totalAmount")));
                    todaySaleText = formatDisplayAmount(currencyName, todaySaleAmount);
                }
                cursor.close();

                LocalSalesAnalytics analytics = new LocalSalesAnalytics(activity);
                LocalSalesSnapshot monthSnapshot = analytics.loadMonthlyOverview();
                LocalSalesSnapshot todaySnapshot = analytics.loadTodayDashboard();
                totalSalesTrendText = formatTrendLine(
                        trendVsLastMonth,
                        monthSnapshot.getTotalSalesTrend());
                todaySalesTrendText = formatTrendLine(
                        trendVsYesterday,
                        todaySnapshot.getTotalSalesTrend());
                if (filter == SALES_FILTER_TODAY) {
                    totalSalesTrendText = formatTrendLine(
                            trendVsYesterday,
                            todaySnapshot.getTotalSalesTrend());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Exception ignored) {
                    }
                }
                if (database != null) {
                    try {
                        database.close();
                    } catch (Exception ignored) {
                    }
                }
            }

            final String finalCurrency = currencyName;
            final String finalSubcategory = totalSubcategory != null ? totalSubcategory : "0";
            final String finalProduct = totalProduct != null ? totalProduct : "0";
            final String finalCombo = totalCombo != null ? totalCombo : "0";
            final String finalTotalSale = totalSaleText;
            final String finalTodaySale = todaySaleText;
            final String finalTotalSalesTrend = totalSalesTrendText;
            final String finalTodaySalesTrend = todaySalesTrendText;

            AppExecutors.get().main(() -> {
                if (!isAdded() || binding == null) {
                    return;
                }
                MainActivity.currencyName = finalCurrency;
                Common.saveUserData(activity, "currencyName", finalCurrency);
                binding.totalSubcategory.setText(finalSubcategory);
                binding.totalProduct.setText(finalProduct);
                binding.totalCombo.setText(finalCombo);
                binding.totalSale.setText(finalTotalSale);
                binding.todaySaleAmount.setText(finalTodaySale);
                applyTrendText(binding.totalSalesTrend, finalTotalSalesTrend);
                applyTrendText(binding.todaySalesTrend, finalTodaySalesTrend);
                applySalesPeriodLabels(filter);
                if (DetectConnection.checkInternetConnection(activity)) {
                    fetchHomeSalesFromCloud(false);
                }
            });
        });
    }

    private void fetchHomeSalesFromCloud(boolean forceRefresh) {
        if (activity == null || !DetectConnection.checkInternetConnection(activity)) {
            return;
        }
        if (MainActivity.userId == null || MainActivity.userId.trim().isEmpty()) {
            return;
        }
        final int filter = salesPeriodFilter;
        final String period = filter == SALES_FILTER_MONTH ? "month" : "today";
        final String trendVsYesterday = getString(R.string.home_vs_yesterday);
        final String trendVsLastMonth = getString(R.string.home_vs_last_month);
        AppExecutors.get().io().execute(() -> {
            try {
                Call<AllApiResponse> call = Api.getClient(activity)
                        .getHomeSalesOverview(MainActivity.userId, period);
                Response<AllApiResponse> response = call.execute();
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }
                AllApiResponse body = response.body();
                if (body.status == null
                        || (!"true".equalsIgnoreCase(body.status) && !"1".equals(body.status))) {
                    return;
                }
                final String currency = MainActivity.currencyName != null
                        ? MainActivity.currencyName : "\u20B9";
                final String primaryText = formatDisplayAmount(currency,
                        parseAmountSafe(body.primarySales));
                final String todayText = formatDisplayAmount(currency,
                        parseAmountSafe(body.todaySales));
                final String primaryTrend = formatTrendLine(
                        filter == SALES_FILTER_MONTH ? trendVsLastMonth : trendVsYesterday,
                        normalizeTrendRaw(body.primarySalesTrend));
                final String todayTrend = formatTrendLine(
                        trendVsYesterday,
                        normalizeTrendRaw(body.todaySalesTrend));
                final String subcategory = body.totalSubcategory != null ? body.totalSubcategory : null;
                final String product = body.totalProduct != null ? body.totalProduct : null;
                final String combo = body.totalCombo != null ? body.totalCombo : null;

                AppExecutors.get().main(() -> {
                    if (!isAdded() || binding == null) {
                        return;
                    }
                    binding.totalSale.setText(primaryText);
                    binding.todaySaleAmount.setText(todayText);
                    applyTrendText(binding.totalSalesTrend, primaryTrend);
                    applyTrendText(binding.todaySalesTrend, todayTrend);
                    if (subcategory != null) {
                        binding.totalSubcategory.setText(subcategory);
                    }
                    if (product != null) {
                        binding.totalProduct.setText(product);
                    }
                    if (combo != null) {
                        binding.totalCombo.setText(combo);
                    }
                    applySalesPeriodLabels(filter);
                });
            } catch (Exception e) {
                Log.e("Home", "fetchHomeSalesFromCloud", e);
                if (forceRefresh) {
                    AppExecutors.get().main(() -> getTotalCount());
                }
            }
        });
    }

    private static String normalizeTrendRaw(String trendRaw) {
        if (trendRaw == null || trendRaw.trim().isEmpty()) {
            return "0%";
        }
        String value = trendRaw.trim();
        if (value.endsWith("%")) {
            return value.startsWith("+") || value.startsWith("-") ? value : "+" + value;
        }
        try {
            float pct = Float.parseFloat(value);
            return String.format(Locale.US, "%+.0f%%", pct);
        } catch (Exception ignored) {
            return value.contains("%") ? value : value + "%";
        }
    }

    private void applySalesPeriodLabels(int filter) {
        if (binding == null) {
            return;
        }
        if (binding.totalSalesLabel != null) {
            binding.totalSalesLabel.setText(getString(filter == SALES_FILTER_MONTH
                    ? R.string.home_monthly_sales_label
                    : R.string.home_total_sales_label));
        }
        if (binding.todaySalesLabel != null) {
            binding.todaySalesLabel.setText(getString(R.string.home_today_sales_label));
        }
    }

    private static float parseAmountSafe(String raw) {
        try {
            if (raw == null || raw.trim().isEmpty()) {
                return 0f;
            }
            return Float.parseFloat(raw.trim());
        } catch (Exception ignored) {
            return 0f;
        }
    }

    private String formatDisplayAmount(String currency, float amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        String prefix = currency != null && !currency.trim().isEmpty() ? currency.trim() : "\u20B9";
        return prefix + " " + formatter.format(amount);
    }

    private String formatTrendLine(String template, String trendRaw) {
        if (trendRaw == null || trendRaw.trim().isEmpty()) {
            trendRaw = "0%";
        }
        String arrow = trendRaw.startsWith("-") ? "\u2193" : "\u2191";
        String value = trendRaw.startsWith("+") || trendRaw.startsWith("-")
                ? trendRaw.substring(1) : trendRaw;
        return String.format(Locale.getDefault(), template, arrow, value);
    }

    private void applyTrendText(android.widget.TextView view, String text) {
        if (view == null || activity == null) {
            return;
        }
        view.setText(text);
        boolean down = text.contains("\u2193");
        view.setTextColor(ContextCompat.getColor(activity,
                down ? R.color.statusExpired : R.color.statusActive));
    }

    private void showSalesPeriodMenu() {
        if (binding == null || activity == null) {
            return;
        }
        PopupMenu menu = new PopupMenu(activity, binding.salesPeriodFilter);
        menu.getMenu().add(0, SALES_FILTER_TODAY, 0, R.string.home_filter_today);
        menu.getMenu().add(0, SALES_FILTER_MONTH, 1, R.string.home_filter_this_month);
        menu.setOnMenuItemClickListener(item -> {
            int selected = item.getItemId();
            if (selected != salesPeriodFilter) {
                salesPeriodFilter = selected;
                binding.salesPeriodFilter.setText(getString(selected == SALES_FILTER_MONTH
                        ? R.string.home_filter_this_month
                        : R.string.home_filter_today));
                applySalesPeriodLabels(selected);
                getTotalCount();
            }
            return true;
        });
        menu.show();
    }

    private boolean permissionsRequestedThisSession;

    private void requestPermissionsOnce() {
        if (permissionsRequestedThisSession || activity == null) {
            return;
        }
        permissionsRequestedThisSession = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestNewPermission();
        } else {
            requestPermission();
        }
    }

    public void requestNewPermission() {
        Dexter.withContext(activity)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_CONNECT
                ).withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            createFolder();
                        } else if (report.isAnyPermissionPermanentlyDenied()) {
                            //  showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    public void requestPermission() {

        Dexter.withContext(activity)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_CONNECT
                ).withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            createFolder();
                        } else if (report.isAnyPermissionPermanentlyDenied()) {
                            // Do not re-request in a loop — that hangs the Home screen.
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();

    }


    public void showSettingsDialog() {

        BottomSheetUi.showConfirm(
                activity,
                getString(R.string.toast_need_permissions),
                getString(R.string.toast_this_app_needs_permission_to_use_this_fe),
                "GOTO SETTINGS",
                "Cancel",
                true,
                this::openSettings);
    }

    // navigating user to app settings
    public void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }

    private boolean connectivityReceiversRegistered = false;

    private void registerConnectivityReceivers() {
        if (activity == null || connectivityReceiversRegistered) {
            return;
        }
        try {
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            if (licenceKeyReceiver != null) {
                activity.registerReceiver(licenceKeyReceiver, filter);
            }
            if (offlineToNetworkReceiver != null) {
                activity.registerReceiver(offlineToNetworkReceiver, filter);
            }
            activity.registerReceiver(networkStatusReceiver, filter);
            connectivityReceiversRegistered = true;
        } catch (Exception e) {
            Log.e("Home", "registerConnectivityReceivers", e);
        }
    }

    private void unregisterConnectivityReceivers() {
        if (activity == null || !connectivityReceiversRegistered) {
            return;
        }
        try {
            if (licenceKeyReceiver != null) {
                activity.unregisterReceiver(licenceKeyReceiver);
            }
            if (offlineToNetworkReceiver != null) {
                activity.unregisterReceiver(offlineToNetworkReceiver);
            }
            activity.unregisterReceiver(networkStatusReceiver);
        } catch (Exception e) {
            Log.e("Home", "unregisterConnectivityReceivers", e);
        } finally {
            connectivityReceiversRegistered = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopHomeClock();
        AppExecutors.get().removeMainCallbacks(deferredPrinterConnectRunnable);
        unregisterConnectivityReceivers();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerConnectivityReceivers();
        updateHomeHeader();
        startHomeClock();
        setValidationUI();
    }

    private void updateHomeHeader() {
        if (binding == null) {
            return;
        }
        lastGreetingHour = -1;
        binding.shopName.setText(getGreeting());
        updateHomeDateTime();
        updateOnlineStatusUi();
        loadHomeStoreImage();
    }

    private void updateOnlineStatusUi() {
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
        Date now = new Date();
        binding.homeDateTime.setText(homeDateTimeFormat.format(now));
        if (binding.homeBusinessHours != null) {
            String hoursLine = BusinessHours.homeStatusLine(activity);
            if (hoursLine.isEmpty()) {
                binding.homeBusinessHours.setVisibility(View.GONE);
            } else {
                binding.homeBusinessHours.setVisibility(View.VISIBLE);
                binding.homeBusinessHours.setText(hoursLine);
                binding.homeBusinessHours.setTextColor(ContextCompat.getColor(activity, R.color.white));
            }
        }

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour != lastGreetingHour && binding.shopName != null) {
            lastGreetingHour = hour;
            binding.shopName.setText(getGreeting());
        }
    }

    private String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour >= 5 && hour < 12) {
            greeting = getString(R.string.good_morning);
        } else if (hour >= 12 && hour < 17) {
            greeting = getString(R.string.good_afternoon);
        } else if (hour >= 17 && hour < 21) {
            greeting = getString(R.string.good_evening);
        } else {
            greeting = getString(R.string.good_night);
        }
        return greeting + " \uD83D\uDC4B";
    }

    private synchronized List<CompanyResponse> getCachedCompanyDetails() {
        if (cachedCompanyDetails != null) {
            return cachedCompanyDetails;
        }
        // Prefer DB thread; if called from main, still safe but may hitch once
        cachedCompanyDetails = posBillingWalaDatabase.getCompanyDetails();
        return cachedCompanyDetails;
    }

    private void loadHomeStoreImage() {
        if (binding == null) {
            return;
        }
        if (cachedStoreLogo != null && !cachedStoreLogo.isRecycled()) {
            binding.userPhoto.setImageBitmap(cachedStoreLogo);
            return;
        }

        binding.userPhoto.setImageResource(R.drawable.app_logo);

        AppExecutors.get().db().execute(() -> {
            Bitmap decoded = null;
            String logoRaw = null;
            try {
                List<CompanyResponse> companyList = getCachedCompanyDetails();
                if (companyList != null && !companyList.isEmpty()) {
                    logoRaw = companyList.get(0).getCompanyLogo();
                    if (!TextUtils.isEmpty(logoRaw)) {
                        if (logoRaw.equals(cachedStoreLogoRaw) && cachedStoreLogo != null && !cachedStoreLogo.isRecycled()) {
                            decoded = cachedStoreLogo;
                        } else {
                            byte[] bytes = Base64.decode(logoRaw, Base64.DEFAULT);
                            decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            final Bitmap logoBitmap = decoded;
            final String raw = logoRaw;
            AppExecutors.get().main(() -> {
                if (!isAdded() || binding == null) {
                    return;
                }
                if (logoBitmap != null) {
                    cachedStoreLogo = logoBitmap;
                    cachedStoreLogoRaw = raw;
                    binding.userPhoto.setImageBitmap(logoBitmap);
                    return;
                }
                if (!TextUtils.isEmpty(MainActivity.shopImage)
                        && DetectConnection.checkInternetConnection(activity)) {
                    try {
                        Picasso.get()
                                .load(BuildConfig.MEDIA_BASE_URL + MainActivity.shopImage)
                                .placeholder(R.drawable.app_logo)
                                .error(R.drawable.app_logo)
                                .into(binding.userPhoto);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        stopHomeClock();
        AppExecutors.get().removeMainCallbacks(deferredPrinterConnectRunnable);
        cachedCompanyDetails = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        stopHomeClock();
        AppExecutors.get().removeMainCallbacks(deferredPrinterConnectRunnable);
        super.onDestroy();
    }

    public void getLowInventoryList() {

        binding.inventoryCardView.setVisibility(View.GONE);

        /*inventoryResponseList.clear();
        inventoryResponseList = posBillingWalaDatabase.getLowInventoryList();
        Log.e("inventoryResponseList", "" + inventoryResponseList.size());
        if (inventoryResponseList.size() > 0) {

            adapter = new InventoryAdapter(activity, inventoryResponseList);
            binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
            binding.recyclerView.addItemDecoration(new SimpleDividerItemDecoration(activity));
            binding.recyclerView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
            adapter.notifyItemInserted(inventoryResponseList.size() - 1);

            binding.inventoryCardView.setVisibility(View.GONE);

        } else {
            binding.inventoryCardView.setVisibility(View.GONE);
        }*/

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001) {
            if (resultCode == RESULT_OK) {
                // Bluetooth has been enabled
                Log.e("onActivityResult: ", "Bluetooth is now ON");
            } else {
                // Bluetooth enabling was denied
                Log.e("onActivityResult: ", "Bluetooth was not enabled");
            }
        }
    }
}
