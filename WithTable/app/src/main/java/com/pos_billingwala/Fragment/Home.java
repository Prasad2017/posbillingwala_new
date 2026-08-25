package com.pos_billingwala.Fragment;

import static android.app.Activity.RESULT_OK;
import static com.pos_billingwala.Utils.RequestCodes.directory_path;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import com.pos_billingwala.Extra.ShopHeaderBuilder;
import com.pos_billingwala.Extra.Common;
import com.pos_billingwala.Extra.LicenceExpiredUi;
import com.pos_billingwala.Extra.LicenseModules;
import com.pos_billingwala.Extra.LicenseValidator;
import com.pos_billingwala.Extra.DetectConnection;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.NetworkToOffline.NetworkDataFetcher;
import com.pos_billingwala.NetworkToOffline.Receiver.LicenceKeyReceiver;
import com.pos_billingwala.NetworkToOffline.Receiver.OfflineToNetworkReceiver;
import com.pos_billingwala.NetworkToOffline.UserSynchronizeData;
import com.pos_billingwala.Print.KOTWoosimPrnMng;
import com.pos_billingwala.Print.WoosimPrnMng;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentHomeBinding;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;


@SuppressLint("SetTextI18n, Range, StaticFieldLeak, NonConstantResourceId")
public class Home extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public static TextView fastBilling, tableBilling, takeAwayBilling, messBilling;
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
    //AdView
    public AdView adView;
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    LicenceKeyReceiver licenceKeyReceiver;
    OfflineToNetworkReceiver offlineToNetworkReceiver;
    private final BroadcastReceiver networkStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateOnlineStatusUi();
        }
    };
    List<PrinterSettingResponse> printerSettingResponseList = new ArrayList<>();
    FragmentHomeBinding binding;

    BluetoothAdapter bluetoothAdapter;
    private final Handler homeClockHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat homeDateTimeFormat =
            new SimpleDateFormat("EEE, dd MMM yyyy  hh:mm:ss a", Locale.getDefault());
    private int lastGreetingHour = -1;
    private String cachedDisplayShopName;
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
        boolean showFast = LicenseModules.isEnabled(MainActivity.fastBilling);
        boolean showDineIn = LicenseModules.isEnabled(MainActivity.dineIn);
        boolean showTakeAway = LicenseModules.isEnabled(MainActivity.takeAway);
        boolean showMess = LicenseModules.isEnabled(MainActivity.mess);

        LicenseModules.setVisible(fastBilling, showFast);
        LicenseModules.setVisible(tableBilling, showDineIn);
        LicenseModules.setVisible(takeAwayBilling, showTakeAway);
        LicenseModules.setVisible(messBilling, showMess);
        LicenseModules.setVisible(posBillingRow1, showFast || showDineIn);
        LicenseModules.setVisible(posBillingRow2, showTakeAway || showMess);

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
                }
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        });

        initViews();
        setValidationUI();
        initAds();
        enableBluetooth();

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

    }

    public void initAds() {

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
                Log.e("loadAdError", String.valueOf(loadAdError));
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
            confirmSynchronizeData();
        }

    }

    public void confirmFetchData() {

        new MaterialAlertDialogBuilder(activity, R.style.ThemeDialog)
                .setTitle(getString(R.string.toast_do_you_want_to_confirm_to_fetch_from_clo))
                .setMessage(getString(R.string.toast_local_data_will_be_replaced_with_cloud_d))
                .setCancelable(false)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        if (DetectConnection.checkInternetConnection(activity)) {
                            NetworkDataFetcher.resetAndFetchAllData(activity, posBillingWalaDatabase);
                        } else {
                            DetectConnection.noInternetConnection(activity);
                        }
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .show();

    }

    public void confirmSynchronizeData() {

        new MaterialAlertDialogBuilder(activity, R.style.ThemeDialog)
                .setTitle(getString(R.string.toast_do_you_want_to_confirm_to_send_on_cloud))
                .setMessage(getString(R.string.toast_your_offline_data_will_be_send_to_the_cl))
                .setCancelable(false)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        if (DetectConnection.checkInternetConnection(activity)) {
                            Toast.makeText(activity, getString(R.string.toast_offline_data_uploading_to_server), Toast.LENGTH_SHORT).show();
                            UserSynchronizeData userSynchronizeData = new UserSynchronizeData(activity);
                        } else {
                            DetectConnection.noInternetConnection(activity);
                        }

                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .show();

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestNewPermission();
        } else {
            requestPermission();
        }
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
                    if (!bluetoothAddress.equalsIgnoreCase("")) {
                        new WoosimPrnMng(activity, bluetoothAddress, activity);
                    }
                    if (!bluetoothKOTAddress.equalsIgnoreCase("")) {
                        new KOTWoosimPrnMng(activity, bluetoothKOTAddress, activity);
                    }
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
        AppExecutors.get().db().execute(() -> {
            Date c = Calendar.getInstance().getTime();
            SimpleDateFormat todayDF = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String todayDate = todayDF.format(c);

            String currencyName = MainActivity.currencyName;
            String totalSubcategory = "0";
            String totalProduct = "0";
            String totalCombo = "0";
            String totalSaleText = MainActivity.currencyName + " 0.00";
            String todaySaleText = MainActivity.currencyName + " 0.00";

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

                cursor = database.rawQuery("SELECT SUM(totalAmount) as totalAmount FROM " + POSBillingWalaDatabase.INVOICE_TABLE, null);
                if (cursor.moveToNext()) {
                    totalSaleText = currencyName + " " + formatCompactAmount(cursor.getString(cursor.getColumnIndex("totalAmount")));
                }
                cursor.close();

                cursor = database.rawQuery("SELECT SUM(totalAmount) as totalAmount FROM " + POSBillingWalaDatabase.INVOICE_TABLE + " WHERE invoiceDate LIKE ?", new String[]{"%" + todayDate + "%"});
                if (cursor.moveToNext()) {
                    todaySaleText = currencyName + " " + formatCompactAmount(cursor.getString(cursor.getColumnIndex("totalAmount")));
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
            });
        });
    }

    private String formatCompactAmount(String amountRaw) {
        float totalAmt = 0f;
        try {
            if (amountRaw != null) {
                totalAmt = Float.parseFloat(amountRaw);
            }
        } catch (Exception ignored) {
        }
        if (totalAmt >= 10000000) {
            return String.format(Locale.US, "%.2f", (totalAmt / 10000000)) + "Cr";
        } else if (totalAmt >= 100000) {
            return String.format(Locale.US, "%.2f", (totalAmt / 100000)) + "Lac";
        } else if (totalAmt >= 1000) {
            return String.format(Locale.US, "%.2f", (totalAmt / 1000)) + "K";
        }
        return String.format(Locale.US, "%.2f", totalAmt);
    }

    public void requestNewPermission() {

        Dexter.withContext(activity)
                .withPermissions(
                        Manifest.permission.CAMERA,
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
                            //  showSettingsDialog();
                            requestPermission();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();

    }


    public void showSettingsDialog() {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        builder.setTitle(getString(R.string.toast_need_permissions));
        builder.setMessage(getString(R.string.toast_this_app_needs_permission_to_use_this_fe));
        builder.setPositiveButton("GOTO SETTINGS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                openSettings();
            }

        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();

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
        if (adView != null) {
            adView.pause();
        }
        unregisterConnectivityReceivers();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
        registerConnectivityReceivers();
        updateHomeHeader();
        startHomeClock();
    }

    private void updateHomeHeader() {
        if (binding == null) {
            return;
        }
        lastGreetingHour = -1;
        binding.shopName.setText(getGreeting() + ", " + getDisplayShopName());
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

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour != lastGreetingHour && binding.shopName != null) {
            lastGreetingHour = hour;
            binding.shopName.setText(getGreeting() + ", " + getDisplayShopName());
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

    private String getDisplayShopName() {
        if (cachedDisplayShopName != null) {
            return cachedDisplayShopName;
        }
        if (!TextUtils.isEmpty(MainActivity.shopName)) {
            cachedDisplayShopName = MainActivity.shopName;
            return cachedDisplayShopName;
        }
        // Resolve shop name off the main thread; show placeholder until ready
        AppExecutors.get().db().execute(() -> {
            String resolved = "POS Billingwala";
            try {
                List<CompanyResponse> companyList = getCachedCompanyDetails();
                if (companyList != null && !companyList.isEmpty()) {
                    String name = ShopHeaderBuilder.resolveShopName1(companyList.get(0));
                    if (!TextUtils.isEmpty(name)) {
                        resolved = name;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            final String shop = resolved;
            AppExecutors.get().main(() -> {
                if (!isAdded() || binding == null) {
                    return;
                }
                cachedDisplayShopName = shop;
                binding.shopName.setText(getGreeting() + ", " + shop);
            });
        });
        return "POS Billingwala";
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
                if (!TextUtils.isEmpty(MainActivity.shopImage)) {
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
        if (adView != null) {
            adView.destroy();
        }
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
