package com.posbillingwala.owner.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.ProductCategoryResponse;
import com.posbillingwala.owner.Model.ProductResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.FragmentUpdateProductBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, UseCompatLoadingForDrawables, StaticFieldLeak")
public class UpdateProduct extends Fragment {

    public static Activity activity;
    public FragmentUpdateProductBinding binding;
    public String[] categoryIdList, categoryNameList, unitNameList;
    public String productId, categoryId, categoryName, unitName;
    public List<ProductCategoryResponse> productCategoryResponseList = new ArrayList<>();
    public List<ProductResponse> productResponseList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUpdateProductBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        activity = getActivity();

        Bundle bundle = getArguments();
        if (bundle != null) {
            productId = bundle.getString("productId");
        }

        binding.productName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                Log.i("tag", "onKey Back listener is working!!!");
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new AllCustomerProductList(), true);
                return true;
            }
            return false;
        });

        setupSpinners();
        setupOnClickListeners();

        return view;
    }

    public void setupSpinners() {
        binding.categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                categoryId = categoryIdList[position];
                categoryName = categoryNameList[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        binding.unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                unitName = unitNameList[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        binding.categorySpinner.setOnTouchListener((v, event) -> {
            hideKeyboard();
            return false;
        });

        binding.unitSpinner.setOnTouchListener((v, event) -> {
            hideKeyboard();
            return false;
        });
    }

    public void setupOnClickListeners() {
        binding.backToHome.setOnClickListener(v -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new AllCustomerProductList(), true);
        });

        binding.updateProduct.setOnClickListener(v -> {
            if (categoryId != null) {
                if (!binding.productCode.getText().toString().isEmpty()) {
                    if (!binding.productName.getText().toString().isEmpty()) {
                        if (!binding.productPrice.getText().toString().isEmpty()) {
                            if (unitName != null) {
                                updateProduct();
                            } else {
                                Toast.makeText(activity, "Please select product unit", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(activity, "Please enter product price", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(activity, "Please enter product name", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(activity, "Please enter product code", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(activity, "Please select product category", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void updateProduct() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().updateProduct(productId, categoryId, categoryName,
                binding.productName.getText().toString(),
                binding.productPrice.getText().toString(),
                unitName,
                binding.productCGST.getText().toString(),
                binding.productSGST.getText().toString(),
                binding.productCode.getText().toString());
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                pDialog.dismiss();
                if (response.isSuccessful()) {
                    if ("1".equalsIgnoreCase(response.body().getStatus())) {
                        Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                        ((MainActivity) activity).loadFragment(new AllCustomerProductList(), true);
                    } else {
                        Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Oops...")
                        .setContentText("Something went wrong!")
                        .setCancelClickListener(SweetAlertDialog::dismiss)
                        .show();
            }
        });
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DetectConnection.checkInternetConnection(activity)) {
            getProductDetails();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    public void getProductDetails() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().getProductDetails(productId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                pDialog.dismiss();
                if (response.isSuccessful()) {
                    productResponseList = response.body().getProductResponseList();
                    if (!productResponseList.isEmpty()) {
                        ProductResponse productResponse = productResponseList.get(0);

                        categoryName = productResponse.getCategoryName();
                        unitName = productResponse.getProductUnit();

                        binding.productName.setText(productResponse.getProductName());
                        binding.productPrice.setText(productResponse.getProductPrice().replace(".00", ""));
                        binding.productCGST.setText(productResponse.getProductCGST());
                        binding.productSGST.setText(productResponse.getProductSGST());
                        binding.productCode.setText(productResponse.getProductCode());

                        unitNameList = activity.getResources().getStringArray(R.array.product_unit);
                        try {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, unitNameList);
                            adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
                            binding.unitSpinner.setAdapter(adapter);
                            if (unitName != null) {
                                binding.unitSpinner.setSelection(adapter.getPosition(unitName));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        getProductCategoryList();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                Log.e("tag", t.getMessage());
            }
        });
    }

    public void getProductCategoryList() {
        Call<AllApiResponse> call = Api.getClient().getCategoryList(MainActivity.userId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    productCategoryResponseList = response.body().getProductCategoryResponseList();
                    if (!productCategoryResponseList.isEmpty()) {
                        categoryIdList = new String[productCategoryResponseList.size()];
                        categoryNameList = new String[productCategoryResponseList.size()];
                        for (int i = 0; i < productCategoryResponseList.size(); i++) {
                            categoryIdList[i] = productCategoryResponseList.get(i).getCategoryId();
                            categoryNameList[i] = productCategoryResponseList.get(i).getCategoryName();

                            try {
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, categoryNameList);
                                adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
                                binding.categorySpinner.setAdapter(adapter);
                                if (categoryName != null) {
                                    binding.categorySpinner.setSelection(adapter.getPosition(categoryName));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.e("tag", t.getMessage());
            }
        });
    }
}
