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
import com.posbillingwala.owner.Model.ProductSubcategoryResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.FragmentAddCustomerProductBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, ClickableViewAccessibility")
public class AddCustomerProduct extends Fragment {

    public static Activity activity;
    public FragmentAddCustomerProductBinding binding;
    public String[] categoryIdList, categoryNameList, unitNameList;
    public String[] subcategoryIdList, subcategoryNameList;
    public String categoryId, categoryName, unitName, subcategoryId;
    public List<ProductCategoryResponse> productCategoryResponseList = new ArrayList<>();
    private ProductPortionSectionHelper portionSectionHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddCustomerProductBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        activity = getActivity();

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

        binding.productName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        portionSectionHelper = new ProductPortionSectionHelper(activity, view);
        portionSectionHelper.setOnPortionMasterLinkClick(() -> {
            AddCustomerPortionMaster fragment = new AddCustomerPortionMaster();
            Bundle args = new Bundle();
            args.putString("returnTo", "product");
            fragment.setArguments(args);
            ((MainActivity) activity).loadFragment(fragment, true);
        });

        binding.categorySpinner.setOnItemSelectedListener((position, item) -> {
            categoryId = categoryIdList[position];
            categoryName = categoryNameList[position];
            loadSubcategoriesForCategory(categoryId);
        });

        binding.subcategorySpinner.setOnItemSelectedListener((position, item) -> {
            if (subcategoryIdList != null && position >= 0 && position < subcategoryIdList.length) {
                subcategoryId = subcategoryIdList[position];
            }
        });

        unitNameList = activity.getResources().getStringArray(R.array.product_unit);
        binding.unitSpinner.setItems(unitNameList);
        binding.unitSpinner.setSelectedIndex(0);
        unitName = unitNameList[0];
        binding.unitSpinner.setOnItemSelectedListener((position, item) -> unitName = unitNameList[position]);

        binding.backToHome.setOnClickListener(view1 -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new AllCustomerProductList(), true);
        });

        binding.addProduct.setOnClickListener(v -> {
            if (categoryId != null) {
                if (!binding.productProduct.getText().toString().isEmpty()) {
                    if (!binding.productName.getText().toString().isEmpty()) {
                        if (!binding.productPrice.getText().toString().isEmpty()) {
                            if (unitName != null) {
                                addProduct();
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

        return view;
    }

    public void addProduct() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().saveProduct(
                MainActivity.userId, categoryId, categoryName,
                binding.productProduct.getText().toString(),
                binding.productName.getText().toString(),
                binding.productPrice.getText().toString(),
                unitName,
                binding.productCGST.getText().toString(),
                binding.productSGST.getText().toString(),
                getRandomString(10),
                subcategoryId != null ? subcategoryId : "");
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                        String savedProductId = response.body().getProductId();
                        Runnable finish = () -> {
                            pDialog.dismiss();
                            Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                            ((MainActivity) activity).loadFragment(new AllCustomerProductList(), true);
                        };
                        if (savedProductId != null && !savedProductId.isEmpty() && portionSectionHelper.hasPortions()) {
                            portionSectionHelper.savePortionsForProduct(savedProductId, finish);
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
                Log.e("serverError", t.getMessage());
                pDialog.dismiss();
            }
        });
    }

    public String getRandomString(final int sizeOfRandomString) {
        String ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnm";
        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DetectConnection.checkInternetConnection(activity)) {
            getProductCategoryList();
            if (portionSectionHelper != null) {
                portionSectionHelper.refresh();
            }
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    public void getProductCategoryList() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

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
                        binding.categorySpinner.setItems(categoryNameList);
                        binding.categorySpinner.setSelectedIndex(0);
                        categoryId = categoryIdList[0];
                        categoryName = categoryNameList[0];
                        loadSubcategoriesForCategory(categoryId);
                    }
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.e("categoryError", t.getMessage());
                pDialog.dismiss();
            }
        });
    }

    private void loadSubcategoriesForCategory(String selectedCategoryId) {
        subcategoryId = null;
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
                    binding.subcategorySpinner.setSelectedIndex(0);
                    subcategoryId = subcategoryIdList[0];
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.e("subcategoryError", "" + t.getMessage());
            }
        });
    }
}
