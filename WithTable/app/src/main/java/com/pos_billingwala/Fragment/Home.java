package com.pos_billingwala.Fragment;

import static android.app.Activity.RESULT_OK;
import static com.pos_billingwala.Utils.RequestCodes.directory_path;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
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
import com.pos_billingwala.Extra.Common;
import com.pos_billingwala.Extra.LicenseValidator;
import com.pos_billingwala.Extra.DetectConnection;
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
    public static CardView totalSalesCardView, todaySalesCardView;
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
    List<PrinterSettingResponse> printerSettingResponseList = new ArrayList<>();
    FragmentHomeBinding binding;

    BluetoothAdapter bluetoothAdapter;

    public static long getUnitBetweenDates(Date startDate, Date endDate, TimeUnit unit) {
        long timeDiff = endDate.getTime() - startDate.getTime();
        return unit.convert(timeDiff, TimeUnit.MILLISECONDS);
    }

    public static void totalLicenceDays() {

        if (activity != null && LicenseValidator.hasStoredPayload(activity)) {
            POSBillingWalaDatabase db = new POSBillingWalaDatabase(activity);
            LicenseValidator.ValidationResult result = LicenseValidator.validate(activity, db);
            if (!result.valid) {
                forceLogoutToLogin();
            }
            return;
        }

        Date c = Calendar.getInstance().getTime();
        System.out.println("Current time => " + c);
        SimpleDateFormat todayDF = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = todayDF.format(c);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        Date startDate, endDate;
        long numberOfDays;
        try {
            if (!MainActivity.LicenceKeyExpireDate.equalsIgnoreCase("")) {
                startDate = dateFormat.parse(todayDate);
                endDate = dateFormat.parse(MainActivity.LicenceKeyExpireDate);
                numberOfDays = getUnitBetweenDates(startDate, endDate, TimeUnit.DAYS);

                // Valid through end of expiry day (aligned with server P4-1)
                if (numberOfDays < 0) {
                    forceLogoutToLogin();
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
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
        activity.startActivity(intent);
        activity.finish();
    }

    public static void setValidationUI() {
        if (MainActivity.fastBilling.equalsIgnoreCase("1")) {
            fastBilling.setVisibility(View.VISIBLE);
        } else {
            fastBilling.setVisibility(View.GONE);
        }

        if (MainActivity.dineIn.equalsIgnoreCase("1")) {
            tableBilling.setVisibility(View.VISIBLE);
        } else {
            tableBilling.setVisibility(View.GONE);
        }

        if (MainActivity.takeAway.equalsIgnoreCase("1")) {
            takeAwayBilling.setVisibility(View.VISIBLE);
        } else {
            takeAwayBilling.setVisibility(View.GONE);
        }

        if (MainActivity.mess.equalsIgnoreCase("1")) {
            messBilling.setVisibility(View.VISIBLE);
        } else {
            messBilling.setVisibility(View.GONE);
        }

        if (MainActivity.totalSaleData.equalsIgnoreCase("1")) {
            totalSalesCardView.setVisibility(View.VISIBLE);
        } else {
            totalSalesCardView.setVisibility(View.GONE);
        }

        if (MainActivity.todaySaleData.equalsIgnoreCase("1")) {
            todaySalesCardView.setVisibility(View.VISIBLE);
        } else {
            todaySalesCardView.setVisibility(View.GONE);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here

        activity = getActivity();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(activity, "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show();
        }

        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);
        // Receivers registered in onResume / unregistered in onPause (single registration only)
        licenceKeyReceiver = new LicenceKeyReceiver();
        offlineToNetworkReceiver = new OfflineToNetworkReceiver();

        binding.shopName.setText("Hi " + MainActivity.shopName);
        try {
            Picasso.get()
                    .load(BuildConfig.MEDIA_BASE_URL + MainActivity.shopImage)
                    .placeholder(R.drawable.app_logo)
                    .into(binding.userPhoto);
        } catch (Exception e) {
            e.printStackTrace();
        }

        binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (DetectConnection.checkInternetConnection(activity)) {
                    LicenceKeyReceiver.getLicenceKeyData(activity);
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
                Toast.makeText(activity, "Bluetooth permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    public void initViews() {
        fastBilling = view.findViewById(R.id.fastBilling);
        tableBilling = view.findViewById(R.id.tableBilling);
        takeAwayBilling = view.findViewById(R.id.takeAwayBilling);
        messBilling = view.findViewById(R.id.messBilling);
        totalSalesCardView = view.findViewById(R.id.totalSalesCardView);
        todaySalesCardView = view.findViewById(R.id.todaySalesCardView);

        binding.userSettingIcon.setOnClickListener(this);
        binding.fastBilling.setOnClickListener(this);
        binding.tableBilling.setOnClickListener(this);
        binding.takeAwayBilling.setOnClickListener(this);
        binding.messBilling.setOnClickListener(this);
        binding.productCardView.setOnClickListener(this);
        binding.categoryCardView.setOnClickListener(this);
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
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new UserSetting(), true);
        } else if (id == R.id.fastBilling) {
            if (MainActivity.fastBilling.equalsIgnoreCase("1")) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                CreatePos createPos = new CreatePos();
                Bundle bundle = new Bundle();
                bundle.putString("tableNumber", "FS" + getRandomString(3));
                bundle.putString("cartOrderStatus", "fast_billing");
                createPos.setArguments(bundle);
                ((MainActivity) activity).loadFragment(createPos, true);
            } else {
                Toast.makeText(activity, "you have not selected fast billing. Please contact your owner", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.tableBilling) {
            if (MainActivity.dineIn.equalsIgnoreCase("1")) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new InvoiceCompanyTable(), true);
            } else {
                Toast.makeText(activity, "you have not selected dine-in. Please contact your owner", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.takeAwayBilling) {
            if (MainActivity.takeAway.equalsIgnoreCase("1")) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new InvoiceTakeAway(), true);
            } else {
                Toast.makeText(activity, "you have not selected take away. Please contact your owner", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.messBilling) {
            if (MainActivity.mess.equalsIgnoreCase("1")) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new InvoiceMess(), true);
            } else {
                Toast.makeText(activity, "you have not selected take away. Please contact your owner", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.categoryCardView) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new AddCategory(), true);
        } else if (id == R.id.productCardView) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new ProductMaster(), true);
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
                .setTitle("Do you want to confirm to fetch from cloud?")
                .setMessage("Local data will be replaced with cloud data. Unsynced bills cannot be overwritten — sync them first.")
                .setCancelable(false)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        if (DetectConnection.checkInternetConnection(activity)) {
                            int unsynced = posBillingWalaDatabase.countUnsyncedInvoices();
                            if (unsynced > 0) {
                                Toast.makeText(activity,
                                        unsynced + " unsynced bill(s) found. Send to cloud first, then fetch.",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }
                            SQLiteDatabase database = posBillingWalaDatabase.getWritableDatabase();
                            posBillingWalaDatabase.resetTables(database);
                            NetworkDataFetcher.fetchAllData(activity);
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
                .setTitle("Do you want to confirm to send on cloud?")
                .setMessage("Your offline data will be send to the cloud.")
                .setCancelable(false)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        if (DetectConnection.checkInternetConnection(activity)) {
                            Toast.makeText(activity, "Offline Data uploading to server", Toast.LENGTH_SHORT).show();
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
        if (!MainActivity.userId.equalsIgnoreCase("")) {
            totalLicenceDays();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestNewPermission();
        } else {
            requestPermission();
        }
        getTotalCount();
        getPrinterSettingDetails();
        getLowInventoryList();
    }

    public void getPrinterSettingDetails() {
        printerSettingResponseList = posBillingWalaDatabase.getPrinterSettingDetails();
        if (!printerSettingResponseList.isEmpty()) {
            String bluetoothAddress = printerSettingResponseList.get(0).getBluetoothAddress() != null ? printerSettingResponseList.get(0).getBluetoothAddress() : "";
            if (!bluetoothAddress.equalsIgnoreCase("")) {
                try {
                    if (enableBluetooth()) {
                        new WoosimPrnMng(activity, bluetoothAddress, activity);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            String bluetoothKOTAddress = printerSettingResponseList.get(0).getBluetoothKOTAddress() != null ? printerSettingResponseList.get(0).getBluetoothKOTAddress() : "";
            if (!bluetoothKOTAddress.equalsIgnoreCase("")) {
                try {
                    if (enableBluetooth()) {
                        new KOTWoosimPrnMng(activity, bluetoothAddress, activity);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void createFolder() {

        File myDirectory = new File(directory_path);
        if (!myDirectory.exists()) {
            myDirectory.mkdirs();
        }

    }

    @SuppressLint("Recycle")
    public void getTotalCount() {

        Date c = Calendar.getInstance().getTime();
        System.out.println("Current time => " + c);
        SimpleDateFormat todayDF = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = todayDF.format(c);

        SQLiteDatabase database = posBillingWalaDatabase.getWritableDatabase();
        Cursor cursor;
        //Invoice Currency
        cursor = database.rawQuery("SELECT currencyName FROM " + POSBillingWalaDatabase.COMPANY_TABLE, null);
        while (cursor.moveToNext()) {
            String currencyName = cursor.getString(cursor.getColumnIndex("currencyName"));
            if (currencyName != null) {
                String[] separated = currencyName.trim().split(":");
                try {
                    currencyName = separated[1];
                    MainActivity.currencyName = currencyName;
                    Common.saveUserData(activity, "currencyName", currencyName);
                } catch (Exception e) {
                    e.printStackTrace();
                    MainActivity.currencyName = "\u20B9";
                }
            } else {
                MainActivity.currencyName = "\u20B9";
            }
        }
        //Total Category
        cursor = database.rawQuery("SELECT COUNT(categoryId) as totalCategory FROM " + POSBillingWalaDatabase.PRODUCT_CATEGORY_TABLE + " WHERE categoryDeletedStatus = '0'", null);
        while (cursor.moveToNext()) {
            String totalCategory = cursor.getString(cursor.getColumnIndex("totalCategory"));
            binding.totalCategory.setText(totalCategory);
        }
        //Total Product
        cursor = database.rawQuery("SELECT COUNT(productId) as totalProduct FROM " + POSBillingWalaDatabase.PRODUCT_TABLE + " WHERE categoryName !='' AND productDeletedStatus='0'", null);
        while (cursor.moveToNext()) {
            String totalProduct = cursor.getString(cursor.getColumnIndex("totalProduct"));
            binding.totalProduct.setText(totalProduct);
        }
        //Total Sale
        cursor = database.rawQuery("SELECT SUM(totalAmount) as totalAmount FROM " + POSBillingWalaDatabase.INVOICE_TABLE, null);
        while (cursor.moveToNext()) {
            float totalAmt;
            if ((cursor.getString(cursor.getColumnIndex("totalAmount")) != null)) {
                totalAmt = Float.parseFloat(cursor.getString(cursor.getColumnIndex("totalAmount")));
            } else {
                totalAmt = 0f;
            }

            String totalAmount = "";
            if (totalAmt >= 1000) {
                totalAmount = String.format(Locale.US, "%.2f", (totalAmt / 1000)) + "K";
            } else if (totalAmt >= 100000) {
                totalAmount = String.format(Locale.US, "%.2f", (totalAmt / 100000)) + "Lac";
            } else if (totalAmt >= 10000000) {
                totalAmount = String.format(Locale.US, "%.2f", (totalAmt / 10000000)) + "Cr";
            } else {
                totalAmount = String.format(Locale.US, "%.2f", totalAmt);
            }

            binding.totalSale.setText(MainActivity.currencyName + " " + totalAmount);
        }
        //Today Sale
        cursor = database.rawQuery("SELECT SUM(totalAmount) as totalAmount FROM " + POSBillingWalaDatabase.INVOICE_TABLE + " WHERE invoiceDate LIKE '%" + todayDate + "%'", null);
        while (cursor.moveToNext()) {
            float totalAmt;
            if ((cursor.getString(cursor.getColumnIndex("totalAmount")) != null)) {
                totalAmt = Float.parseFloat(cursor.getString(cursor.getColumnIndex("totalAmount")));
            } else {
                totalAmt = 0f;
            }

            String totalAmount = "";
            if (totalAmt >= 1000) {
                totalAmount = String.format(Locale.US, "%.2f", (totalAmt / 1000)) + "K";
            } else if (totalAmt >= 100000) {
                totalAmount = String.format(Locale.US, "%.2f", (totalAmt / 100000)) + "Lac";
            } else if (totalAmt >= 10000000) {
                totalAmount = String.format(Locale.US, "%.2f", (totalAmt / 10000000)) + "Cr";
            } else {
                totalAmount = String.format(Locale.US, "%.2f", totalAmt);
            }

            binding.todaySaleAmount.setText(MainActivity.currencyName + " " + totalAmount);
        }

        database.close();
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
        builder.setTitle("Need Permissions");
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.");
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
        } catch (Exception e) {
            Log.e("Home", "unregisterConnectivityReceivers", e);
        } finally {
            connectivityReceiversRegistered = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
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
    }

    @Override
    public void onDestroy() {
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
