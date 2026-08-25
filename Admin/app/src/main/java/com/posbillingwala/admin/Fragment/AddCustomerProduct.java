package com.posbillingwala.admin.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.jaredrummler.materialspinner.MaterialSpinner;
import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Extra.ProductPortionSectionHelper;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.ProductCategoryResponse;
import com.posbillingwala.admin.Model.ProductResponse;
import com.posbillingwala.admin.Model.ProductSubcategoryResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentAddCustomerProductBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, UseCompatLoadingForDrawables, StaticFieldLeak")
public class AddCustomerProduct extends Fragment implements View.OnClickListener {

    public static Activity activity;
    View view;
    FragmentAddCustomerProductBinding binding;
    String[] categoryIdList, categoryNameList, unitNameList;
    String[] subcategoryIdList, subcategoryNameList;
    String customerId, categoryId, categoryName, unitName, subcategoryId;
    List<ProductCategoryResponse> productCategoryResponseList = new ArrayList<>();
    ProductPortionSectionHelper portionSectionHelper;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddCustomerProductBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();
        MainActivity.title.setText("Add Product");

        Bundle bundle = getArguments();
        if (bundle != null) {
            customerId = bundle.getString("customerId");
        }

        portionSectionHelper = new ProductPortionSectionHelper(activity, binding.productPortionSectionInclude.getRoot());
        portionSectionHelper.setCustomerId(customerId);
        portionSectionHelper.setOnPortionMasterLinkClick(this::openPortionMaster);

        MainActivity.back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                AllCustomerProductList allCustomerProductList = new AllCustomerProductList();
                Bundle backBundle = new Bundle();
                backBundle.putString("customerId", customerId);
                allCustomerProductList.setArguments(backBundle);
                ((MainActivity) activity).loadFragment(allCustomerProductList, true);
            }
        });

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    MainActivity.back.performClick();
                    return true;
                }
                return false;
            }
        });

        binding.productName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        binding.categorySpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                categoryId = categoryIdList[position];
                categoryName = categoryNameList[position];
                loadSubcategoriesForCategory(categoryId);
            }
        });

        binding.subcategorySpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                if (subcategoryIdList != null && position >= 0 && position < subcategoryIdList.length) {
                    subcategoryId = subcategoryIdList[position];
                }
            }
        });

        unitNameList = activity.getResources().getStringArray(R.array.product_unit);
        try {
            final ArrayAdapter adapter = new ArrayAdapter(activity, android.R.layout.simple_spinner_item, unitNameList);
            adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
            binding.unitSpinner.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        binding.unitSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                unitName = unitNameList[position];
            }
        });

        binding.categorySpinner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
                return false;
            }
        });

        binding.unitSpinner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
                return false;
            }
        });

        binding.addProduct.setOnClickListener(this);

        return view;

    }

    private void openPortionMaster() {
        AddCustomerPortionMaster fragment = new AddCustomerPortionMaster();
        Bundle bundle = new Bundle();
        bundle.putString("customerId", customerId);
        bundle.putString("returnTo", "addProduct");
        fragment.setArguments(bundle);
        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
        ((MainActivity) activity).loadFragment(fragment, true);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.addProduct) {
            if (categoryId != null) {
                if (binding.productName.getText().toString().length() > 0) {
                    String price = binding.productPrice.getText().toString().trim();
                    boolean hasPortions = portionSectionHelper != null && portionSectionHelper.hasPortions();
                    if (price.length() > 0 || hasPortions) {
                        if (unitName != null) {
                            addProduct();
                        } else {
                            Toast.makeText(activity, "Please select product unit", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(activity, "Please enter product price or add portions", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(activity, "Please enter product name", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(activity, "Please select product category", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addProduct() {

        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        String productNetworkStatus = getRandomString(10);
        String price = binding.productPrice.getText().toString().trim();
        if (price.isEmpty() && portionSectionHelper.hasPortions()) {
            price = "0";
        }

        Call<AllApiResponse> call = Api.getClient().saveProduct(customerId, categoryId, categoryName,
                binding.productName.getText().toString(), price,
                unitName, binding.productCGST.getText().toString(), binding.productSGST.getText().toString(),
                productNetworkStatus,
                subcategoryId != null ? subcategoryId : "");
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                        if (portionSectionHelper.hasPortions()) {
                            String savedProductId = response.body().getProductId();
                            if (savedProductId != null && savedProductId.length() > 0) {
                                portionSectionHelper.savePortionsForProduct(savedProductId, allOk -> {
                                    pDialog.dismiss();
                                    Toast.makeText(activity, allOk ? "Product & portions saved" : "Product saved; some portions failed", Toast.LENGTH_SHORT).show();
                                    navigateToProductList();
                                });
                            } else {
                                resolveProductIdAndSavePortions(productNetworkStatus, pDialog);
                            }
                        } else {
                            pDialog.dismiss();
                            Toast.makeText(activity, "" + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                            navigateToProductList();
                        }
                    } else {
                        pDialog.dismiss();
                        Toast.makeText(activity, "" + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    pDialog.dismiss();
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                Log.e("serverError", "" + t.getMessage());
                pDialog.dismiss();
            }
        });

    }

    private void resolveProductIdAndSavePortions(String productNetworkStatus, SweetAlertDialog pDialog) {
        Call<AllApiResponse> call = Api.getClient().getProductList(customerId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                String productId = null;
                if (response.isSuccessful() && response.body() != null) {
                    List<ProductResponse> products = response.body().getProductResponseList();
                    if (products != null) {
                        for (ProductResponse product : products) {
                            if (productNetworkStatus.equals(product.getProductNetworkStatus())) {
                                productId = product.getProductId();
                                break;
                            }
                        }
                        if (productId == null) {
                            String name = binding.productName.getText().toString().trim();
                            for (int i = products.size() - 1; i >= 0; i--) {
                                if (name.equalsIgnoreCase(products.get(i).getProductName())) {
                                    productId = products.get(i).getProductId();
                                    break;
                                }
                            }
                        }
                    }
                }
                if (productId == null) {
                    pDialog.dismiss();
                    Toast.makeText(activity, "Product saved but portions could not be linked", Toast.LENGTH_SHORT).show();
                    navigateToProductList();
                    return;
                }
                portionSectionHelper.savePortionsForProduct(productId, allOk -> {
                    pDialog.dismiss();
                    Toast.makeText(activity, allOk ? "Product & portions saved" : "Product saved; some portions failed", Toast.LENGTH_SHORT).show();
                    navigateToProductList();
                });
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Toast.makeText(activity, "Product saved but portions could not be linked", Toast.LENGTH_SHORT).show();
                navigateToProductList();
            }
        });
    }

    private void navigateToProductList() {
        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
        AllCustomerProductList allCustomerProductList = new AllCustomerProductList();
        Bundle bundle = new Bundle();
        bundle.putString("customerId", customerId);
        allCustomerProductList.setArguments(bundle);
        ((MainActivity) activity).loadFragment(allCustomerProductList, true);
    }

    private String getRandomString(final int sizeOfRandomString) {

        String ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnm";

        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }


    public void onStart() {
        super.onStart();
        Log.e("onStart", "called");
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(1);
        MainActivity.drawerLayout.closeDrawers();
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

        Call<AllApiResponse> call = Api.getClient().getCategoryList(customerId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    productCategoryResponseList = response.body().getProductCategoryResponseList();
                    if (productCategoryResponseList.size() > 0) {

                        categoryIdList = new String[productCategoryResponseList.size()];
                        categoryNameList = new String[productCategoryResponseList.size()];
                        for (int i = 0; i < productCategoryResponseList.size(); i++) {

                            categoryIdList[i] = productCategoryResponseList.get(i).getCategoryId();
                            categoryNameList[i] = productCategoryResponseList.get(i).getCategoryName();

                        }

                        try {
                            final ArrayAdapter adapter = new ArrayAdapter(activity, android.R.layout.simple_spinner_item, categoryNameList);
                            adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
                            binding.categorySpinner.setAdapter(adapter);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                Log.e("categoryError", "" + t.getMessage());
                pDialog.dismiss();
            }
        });

    }

    private void loadSubcategoriesForCategory(String selectedCategoryId) {
        subcategoryId = null;
        Call<AllApiResponse> call = Api.getClient().getSubcategoryList(customerId, selectedCategoryId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ProductSubcategoryResponse> subcategories = response.body().getSubcategoryResponseList();
                    if (subcategories != null && subcategories.size() > 0) {
                        subcategoryIdList = new String[subcategories.size() + 1];
                        subcategoryNameList = new String[subcategories.size() + 1];
                        subcategoryIdList[0] = "";
                        subcategoryNameList[0] = "None";
                        for (int i = 0; i < subcategories.size(); i++) {
                            subcategoryIdList[i + 1] = subcategories.get(i).getSubcategoryId();
                            subcategoryNameList[i + 1] = subcategories.get(i).getSubcategoryName();
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, subcategoryNameList);
                        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
                        binding.subcategorySpinner.setAdapter(adapter);
                        binding.subcategorySpinner.setSelectedIndex(0);
                        subcategoryId = subcategoryIdList[0];
                    } else {
                        subcategoryIdList = new String[]{""};
                        subcategoryNameList = new String[]{"None"};
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, subcategoryNameList);
                        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
                        binding.subcategorySpinner.setAdapter(adapter);
                        binding.subcategorySpinner.setSelectedIndex(0);
                        subcategoryId = "";
                    }
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                Log.e("subcategoryError", "" + t.getMessage());
            }
        });
    }

}
