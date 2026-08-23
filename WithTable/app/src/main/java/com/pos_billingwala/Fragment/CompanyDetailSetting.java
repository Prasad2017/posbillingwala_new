package com.pos_billingwala.Fragment;

import static android.app.Activity.RESULT_OK;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentCompanyDetailSettingBinding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@SuppressLint("SetTextI18n, StaticFieldLeak")
public class CompanyDetailSetting extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public static String gstStatus = "off", tableStatus = "off", companyId, currencyName, imageName = "", imageType = "";
    public static String companyLogo, paymentLogo;
    public static Uri imageUri;
    public static boolean isClicked;
    public static String[] currencyList;
    public static POSBillingWalaDatabase posBillingWalaDatabase;
    public static List<CompanyResponse> companyResponseList = new ArrayList<>();
    public static FragmentCompanyDetailSettingBinding binding;
    View view;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCompanyDetailSettingBinding.inflate(inflater, container, false);
        view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here

        activity = getActivity();

        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                    ((MainActivity) activity).loadFragment(new UserSetting(), true);
                    return true;
                }
                return false;
            }
        });

        binding.shopName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        binding.cashierName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        binding.shopAddress.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        binding.countryName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        binding.stateName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        binding.shopName.setText(MainActivity.shopName);
        binding.cashierName.setText(MainActivity.userName);


        binding.gstSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    gstStatus = "on";
                    binding.shopGSTLayout.setVisibility(View.VISIBLE);
                } else {
                    gstStatus = "off";
                    binding.shopGSTLayout.setVisibility(View.GONE);
                }

            }
        });


        binding.tableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    tableStatus = "on";
                    binding.noOfTableLayout.setVisibility(View.VISIBLE);
                } else {
                    tableStatus = "off";
                    binding.noOfTableLayout.setVisibility(View.GONE);
                }
            }
        });

        binding.currencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currencyName = currencyList[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        binding.shopCGST.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().isEmpty()) {
                    binding.shopSGST.setText(binding.shopCGST.getText().toString());
                } else {
                    binding.shopCGST.setText("0");
                    binding.shopSGST.setText("0");
                }
            }
        });

        binding.addProfile.setOnClickListener(this);
        binding.addPaymentQR.setOnClickListener(this);
        binding.backToSetting.setOnClickListener(this);
        binding.saveDetails.setOnClickListener(this);

        return view;
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.addProfile) {
            imageName = "profile";
            selectImage();
        } else if (id == R.id.addPaymentQR) {
            imageName = "paymentQR";
            selectImage();
        } else if (id == R.id.backToSetting) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new UserSetting(), true);
        } else if (id == R.id.saveDetails) {
            if (!binding.shopName.getText().toString().isEmpty()) {
                if (!binding.cashierName.getText().toString().isEmpty()) {
                    if (!binding.shopMobile.getText().toString().isEmpty()) {
                        if (!binding.shopAddress.getText().toString().isEmpty()) {
                            if (!binding.countryName.getText().toString().isEmpty()) {
                                if (!binding.stateName.getText().toString().isEmpty()) {
                                    addCompanyDetails();
                                } else {
                                    Toast.makeText(activity, getString(R.string.toast_please_fill_state_name), Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(activity, getString(R.string.toast_please_fill_country_name), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(activity, getString(R.string.toast_please_fill_company_address), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(activity, getString(R.string.toast_please_fill_company_mobile_number), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(activity, getString(R.string.toast_please_fill_cashier_name), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(activity, getString(R.string.toast_please_fill_company_name), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void selectImage() {

        View dialogView = getLayoutInflater().inflate(R.layout.picture_selection_dialog, null);
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        dialog.setContentView(dialogView);
        dialog.setCancelable(false);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        ((dialogView.findViewById(R.id.cameraLayout))).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                cameraIntent();
            }
        });

        ((dialogView.findViewById(R.id.chooseGalleryLayout))).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                galleryIntent();
            }
        });

        ((dialogView.findViewById(R.id.closeDialog))).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);

    }

    private void cameraIntent() {
        isClicked = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "image");
            values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
            imageUri = activity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } else {
            imageUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "image.jpg"));
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION | FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, 100);
    }

    @SuppressLint("IntentReset")
    public void galleryIntent() {
        // choose from  external storage
        isClicked = true;
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION | FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, 200);
    }

    // This method will help to retrieve the image
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 100) {
                try {
                    Bitmap photo = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), imageUri);
                    File finalFile = new File(getRealPathFromURI(imageUri));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == 200) {
                Uri selectedImage = data.getData();
                // CALL THIS METHOD TO GET THE ACTUAL PATH
                File finalFile = new File(getRealPathFromURI(selectedImage));
            }
        } else {
            Toast.makeText(activity, getString(R.string.toast_canceled_by_user), Toast.LENGTH_SHORT).show();
        }

    }

    public String getRealPathFromURI(Uri uri) {

        if (activity.getContentResolver() != null) {
            Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                int idx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                imageType = cursor.getString(idx);
                File imgFile = new File(imageType);

                try {
                    if (imgFile.exists()) {
                        Bitmap myLogo = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                        // initialize byte stream
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        // compress Bitmap
                        myLogo.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        // Initialize byte array
                        byte[] bytes = stream.toByteArray();
                        // get base64 encoded string
                        if (imageName.equalsIgnoreCase("profile")) {
                            companyLogo = Base64.encodeToString(bytes, Base64.DEFAULT);
                            binding.profilePhoto.setImageBitmap(myLogo);
                        } else {
                            paymentLogo = Base64.encodeToString(bytes, Base64.DEFAULT);
                            binding.paymentQRPhoto.setImageBitmap(myLogo);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            cursor.close();
        }
        return imageType;
    }

    public void addCompanyDetails() {

        String noOfTable;
        if (!binding.noOfTable.getText().toString().isEmpty()) {
            noOfTable = binding.noOfTable.getText().toString();
        } else {
            noOfTable = "0";
        }

        if (binding.saveDetails.getText().toString().equalsIgnoreCase("Save Details")) {
            posBillingWalaDatabase.addCompanyDetails(companyLogo, binding.shopName.getText().toString(), binding.cashierName.getText().toString(), binding.shopMobile.getText().toString(),
                    binding.shopAddress.getText().toString(), currencyName, tableStatus, noOfTable, binding.countryName.getText().toString(), binding.stateName.getText().toString(), gstStatus, binding.gstNumber.getText().toString(),
                    binding.shopCGST.getText().toString(), binding.shopSGST.getText().toString(), binding.panNumber.getText().toString(), binding.shopFssai.getText().toString(), 0, paymentLogo);
            Toast.makeText(activity, getString(R.string.toast_company_details_saved), Toast.LENGTH_SHORT).show();
        } else {
            posBillingWalaDatabase.updateCompanyDetails(companyLogo, companyId, binding.shopName.getText().toString(), binding.cashierName.getText().toString(), binding.shopMobile.getText().toString(),
                    binding.shopAddress.getText().toString(), currencyName, tableStatus, noOfTable, binding.countryName.getText().toString(), binding.stateName.getText().toString(), gstStatus, binding.gstNumber.getText().toString(),
                    binding.shopCGST.getText().toString(), binding.shopSGST.getText().toString(), binding.panNumber.getText().toString(), binding.shopFssai.getText().toString(), 0, paymentLogo);
            Toast.makeText(activity, getString(R.string.toast_company_details_updated), Toast.LENGTH_SHORT).show();
        }

        getCompanyDetails();

    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestNewPermission();
        } else {
            requestPermission();
        }
        if (!isClicked) {
            getCompanyDetails();
        }
    }

    public void requestNewPermission() {

        Dexter.withContext(activity)
                .withPermissions(android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_MEDIA_IMAGES
                ).withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {

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
                        Manifest.permission.ACCESS_FINE_LOCATION
                ).withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {

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


    public void getCompanyDetails() {
        companyResponseList = posBillingWalaDatabase.getCompanyDetails();

        if (!companyResponseList.isEmpty()) {
            CompanyResponse companyResponse = companyResponseList.get(0);

            companyId = companyResponse.getCompanyId();
            binding.shopName.setText(companyResponse.getCompanyName());
            binding.cashierName.setText(companyResponse.getCashierName());
            binding.shopMobile.setText(companyResponse.getCompanyMobile());
            binding.shopAddress.setText(companyResponse.getCompanyAddress());
            binding.countryName.setText(companyResponse.getCountryName());
            binding.stateName.setText(companyResponse.getStateName());
            gstStatus = companyResponse.getGstStatus();

            binding.gstNumber.setText(companyResponse.getGstNumber());
            binding.panNumber.setText(companyResponse.getPanNumber());
            binding.shopFssai.setText(companyResponse.getCompanyFssis());
            binding.shopCGST.setText(companyResponse.getShopCGST());
            binding.shopSGST.setText(companyResponse.getShopSGST());
            tableStatus = companyResponse.getTableStatus();

            binding.noOfTable.setText(companyResponse.getNoOfTable());

            currencyName = companyResponse.getCurrencyName();

            if (companyResponse.getCompanyLogo() != null) {
                companyLogo = companyResponse.getCompanyLogo();
                // decode base64 string
                try {
                    byte[] bytes = Base64.decode(companyLogo, Base64.DEFAULT);
                    // Initialize bitmap
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    binding.profilePhoto.setImageBitmap(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (companyResponse.getPaymentLogo() != null) {
                paymentLogo = companyResponse.getPaymentLogo();
                // decode base64 string
                try {
                    byte[] bytes = Base64.decode(paymentLogo, Base64.DEFAULT);
                    // Initialize bitmap
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    binding.paymentQRPhoto.setImageBitmap(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            binding.saveDetails.setText("Update Details");

        } else {
            binding.saveDetails.setText("Save Details");
        }

        currencyList = activity.getResources().getStringArray(R.array.currency_list);
        try {
            final ArrayAdapter adapter = new ArrayAdapter(activity, android.R.layout.simple_spinner_item, currencyList);
            adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
            binding.currencySpinner.setAdapter(adapter);
            if (currencyName != null) {
                binding.currencySpinner.setSelection(adapter.getPosition(currencyName));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (gstStatus.equalsIgnoreCase("off")) {
            binding.gstSwitch.setChecked(false);
            binding.shopGSTLayout.setVisibility(View.GONE);
        } else {
            binding.gstSwitch.setChecked(true);
            binding.shopGSTLayout.setVisibility(View.VISIBLE);
        }

        if (MainActivity.dineIn.equalsIgnoreCase("1")) {
            if (tableStatus.equalsIgnoreCase("off")) {
                binding.tableSwitch.setChecked(false);
                binding.noOfTableLayout.setVisibility(View.GONE);
            } else {
                binding.tableSwitch.setChecked(true);
                binding.noOfTableLayout.setVisibility(View.VISIBLE);
            }
        } else {
            binding.tableSwitch.setChecked(false);
            binding.noOfTableLayout.setVisibility(View.GONE);
        }

    }


}