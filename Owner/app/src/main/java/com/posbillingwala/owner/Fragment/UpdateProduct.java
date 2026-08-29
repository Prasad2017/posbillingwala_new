package com.posbillingwala.owner.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Extra.ProductPortionSectionHelper;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.ProductCategoryResponse;
import com.posbillingwala.owner.Model.ProductResponse;
import com.posbillingwala.owner.Model.ProductSubcategoryResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.FragmentUpdateProductBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, UseCompatLoadingForDrawables, StaticFieldLeak")
public class UpdateProduct extends Fragment {

    public static Activity activity;
    public FragmentUpdateProductBinding binding;
    public String[] categoryIdList, categoryNameList, unitNameList;
    public String[] subcategoryIdList, subcategoryNameList;
    public String productId, categoryId, categoryName, unitName, subcategoryId;
    public String preselectSubcategoryId;
    public List<ProductCategoryResponse> productCategoryResponseList = new ArrayList<>();
    public List<ProductResponse> productResponseList = new ArrayList<>();
    private ProductPortionSectionHelper portionSectionHelper;
    private boolean categorySpinnerReady = false;

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

        portionSectionHelper = new ProductPortionSectionHelper(activity, view);
        portionSectionHelper.setOnPortionMasterLinkClick(() -> {
            AddCustomerPortionMaster fragment = new AddCustomerPortionMaster();
            Bundle args = new Bundle();
            args.putString("returnTo", "updateProduct");
            fragment.setArguments(args);
            ((MainActivity) activity).loadFragment(fragment, true);
        });

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
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
        binding.categorySpinner.setOnItemSelectedListener((position, item) -> {
            categoryId = categoryIdList[position];
            categoryName = categoryNameList[position];
            if (categorySpinnerReady) {
                loadSubcategoriesForCategory(categoryId, null);
            }
        });

        binding.subcategorySpinner.setOnItemSelectedListener((position, item) -> {
            if (subcategoryIdList != null && position >= 0 && position < subcategoryIdList.length) {
                subcategoryId = subcategoryIdList[position];
            }
        });

        binding.unitSpinner.setOnItemSelectedListener((position, item) -> unitName = unitNameList[position]);
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
                binding.productCode.getText().toString(),
                subcategoryId != null ? subcategoryId : "");
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if ("1".equalsIgnoreCase(response.body().getStatus())) {
                        Runnable finish = () -> {
                            pDialog.dismiss();
                            Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                            ((MainActivity) activity).loadFragment(new AllCustomerProductList(), true);
                        };
                        if (portionSectionHelper.hasPortions()) {
                            portionSectionHelper.savePortionsForProduct(productId, finish);
                        } else {
                            finish.run();
                        }
                        return;
                    } else {
                        Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                pDialog.dismiss();
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
                if (response.isSuccessful() && response.body() != null) {
                    productResponseList = response.body().getProductResponseList();
                    if (productResponseList != null && !productResponseList.isEmpty()) {
                        ProductResponse productResponse = productResponseList.get(0);

                        categoryName = productResponse.getCategoryName();
                        categoryId = productResponse.getCategoryId();
                        unitName = productResponse.getProductUnit();
                        preselectSubcategoryId = productResponse.getSubcategoryId();

                        binding.productName.setText(productResponse.getProductName());
                        binding.productPrice.setText(productResponse.getProductPrice().replace(".00", ""));
                        binding.productCGST.setText(productResponse.getProductCGST());
                        binding.productSGST.setText(productResponse.getProductSGST());
                        binding.productCode.setText(productResponse.getProductCode());

                        unitNameList = activity.getResources().getStringArray(R.array.product_unit);
                        try {
                            binding.unitSpinner.setItems(unitNameList);
                            if (unitName != null) {
                                int unitIndex = binding.unitSpinner.indexOf(unitName);
                                if (unitIndex >= 0) {
                                    binding.unitSpinner.setSelectedIndex(unitIndex);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        getProductCategoryList();
                        if (portionSectionHelper != null) {
                            portionSectionHelper.loadExistingForProduct(productId);
                        }
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
                if (response.isSuccessful() && response.body() != null) {
                    productCategoryResponseList = response.body().getProductCategoryResponseList();
                    if (productCategoryResponseList != null && !productCategoryResponseList.isEmpty()) {
                        categoryIdList = new String[productCategoryResponseList.size()];
                        categoryNameList = new String[productCategoryResponseList.size()];
                        for (int i = 0; i < productCategoryResponseList.size(); i++) {
                            categoryIdList[i] = productCategoryResponseList.get(i).getCategoryId();
                            categoryNameList[i] = productCategoryResponseList.get(i).getCategoryName();
                        }
                        try {
                            binding.categorySpinner.setItems(categoryNameList);
                            if (categoryName != null) {
                                int categoryIndex = binding.categorySpinner.indexOf(categoryName);
                                if (categoryIndex >= 0) {
                                    binding.categorySpinner.setSelectedIndex(categoryIndex);
                                }
                            }
                            loadSubcategoriesForCategory(categoryId, preselectSubcategoryId);
                            categorySpinnerReady = true;
                        } catch (Exception e) {
                            e.printStackTrace();
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

    private void loadSubcategoriesForCategory(String selectedCategoryId, String preselectId) {
        Call<AllApiResponse> call = Api.getClient().getSubcategoryList(MainActivity.userId, selectedCategoryId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ProductSubcategoryResponse> subcategories = response.body().getSubcategoryResponseList();
                    if (subcategories != null && !subcategories.isEmpty()) {
                        subcategoryIdList = new String[subcategories.size() + 1];
                        subcategoryNameList = new String[subcategories.size() + 1];
                        subcategoryIdList[0] = "";
                        subcategoryNameList[0] = "None";
                        for (int i = 0; i < subcategories.size(); i++) {
                            subcategoryIdList[i + 1] = subcategories.get(i).getSubcategoryId();
                            subcategoryNameList[i + 1] = subcategories.get(i).getSubcategoryName();
                        }
                    } else {
                        subcategoryIdList = new String[]{""};
                        subcategoryNameList = new String[]{"None"};
                    }
                    binding.subcategorySpinner.setItems(subcategoryNameList);
                    int selection = 0;
                    if (preselectId != null && preselectId.length() > 0) {
                        for (int i = 1; i < subcategoryIdList.length; i++) {
                            if (preselectId.equals(subcategoryIdList[i])) {
                                selection = i;
                                break;
                            }
                        }
                    }
                    binding.subcategorySpinner.setSelectedIndex(selection);
                    subcategoryId = subcategoryIdList[selection];
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.e("subcategoryError", "" + t.getMessage());
            }
        });
    }
}
