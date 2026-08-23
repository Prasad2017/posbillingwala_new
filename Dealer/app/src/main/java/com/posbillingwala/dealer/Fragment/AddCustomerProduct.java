package com.posbillingwala.dealer.Fragment;

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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.posbillingwala.dealer.Activity.MainActivity;
import com.posbillingwala.dealer.Extra.DetectConnection;
import com.posbillingwala.dealer.Model.AllApiResponse;
import com.posbillingwala.dealer.Model.ProductCategoryResponse;
import com.posbillingwala.dealer.Model.ProductSubcategoryResponse;
import com.posbillingwala.dealer.R;
import com.posbillingwala.dealer.Retrofit.Api;
import com.posbillingwala.dealer.databinding.FragmentAddCustomerProductBinding;

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
    String[] categoryIdList, categoryNameList, unitNameList;
    String[] subcategoryIdList, subcategoryNameList;
    String customerId, categoryId, categoryName, unitName, subcategoryId;
    List<ProductCategoryResponse> productCategoryResponseList = new ArrayList<>();
    FragmentAddCustomerProductBinding binding;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddCustomerProductBinding.inflate(inflater, container, false);
        view = binding.getRoot();


        activity = getActivity();
        MainActivity.title.setText("Add Product");

        Bundle bundle = getArguments();
        if (bundle != null) {
            customerId = bundle.getString("customerId");
        }

        MainActivity.back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                AllCustomerProductList allCustomerProductList = new AllCustomerProductList();
                Bundle bundle = new Bundle();
                bundle.putString("customerId", customerId);
                allCustomerProductList.setArguments(bundle);
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
                    ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                    AllCustomerProductList allCustomerProductList = new AllCustomerProductList();
                    Bundle bundle = new Bundle();
                    bundle.putString("customerId", customerId);
                    allCustomerProductList.setArguments(bundle);
                    ((MainActivity) activity).loadFragment(allCustomerProductList, true);
                    return true;
                }
                return false;
            }
        });

        binding.productName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        binding.categorySpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                categoryId = categoryIdList[position];
                categoryName = categoryNameList[position];
                loadSubcategoriesForCategory(categoryId);
            }
        });

        binding.subcategorySpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (subcategoryIdList != null && position >= 0 && position < subcategoryIdList.length) {
                    subcategoryId = subcategoryIdList[position];
                }
            }
        });

        unitNameList = activity.getResources().getStringArray(R.array.product_unit);
        try {
            final ArrayAdapter adapter = new ArrayAdapter(activity, R.layout.spinner_item_layout, unitNameList);
            adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
            binding.unitSpinner.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        binding.unitSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.addProduct) {
            if (categoryId != null) {
                if (!binding.productCode.getText().toString().isEmpty()) {
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
        }
    }

    private void addProduct() {

        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().saveProduct(customerId, categoryId, categoryName, binding.productCode.getText().toString(), binding.productName.getText().toString(), binding.productPrice.getText().toString(),
                unitName, binding.productCGST.getText().toString(), binding.productSGST.getText().toString(), getRandomString(10),
                subcategoryId != null ? subcategoryId : "");
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                        Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                        AllCustomerProductList allCustomerProductList = new AllCustomerProductList();
                        Bundle bundle = new Bundle();
                        bundle.putString("customerId", customerId);
                        allCustomerProductList.setArguments(bundle);
                        ((MainActivity) activity).loadFragment(allCustomerProductList, true);
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
                    if (!productCategoryResponseList.isEmpty()) {

                        categoryIdList = new String[productCategoryResponseList.size()];
                        categoryNameList = new String[productCategoryResponseList.size()];
                        for (int i = 0; i < productCategoryResponseList.size(); i++) {

                            categoryIdList[i] = productCategoryResponseList.get(i).getCategoryId();
                            categoryNameList[i] = productCategoryResponseList.get(i).getCategoryName();

                        }

                        try {
                            final ArrayAdapter adapter = new ArrayAdapter(activity, R.layout.spinner_item_layout, categoryNameList);
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
                Log.e("categoryError", t.getMessage());
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
                    if (subcategories != null && !subcategories.isEmpty()) {
                        subcategoryIdList = new String[subcategories.size() + 1];
                        subcategoryNameList = new String[subcategories.size() + 1];
                        subcategoryIdList[0] = "";
                        subcategoryNameList[0] = "None";
                        for (int i = 0; i < subcategories.size(); i++) {
                            subcategoryIdList[i + 1] = subcategories.get(i).getSubcategoryId();
                            subcategoryNameList[i + 1] = subcategories.get(i).getSubcategoryName();
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, R.layout.spinner_item_layout, subcategoryNameList);
                        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
                        binding.subcategorySpinner.setAdapter(adapter);
                        binding.subcategorySpinner.setText(subcategoryNameList[0], false);
                        subcategoryId = subcategoryIdList[0];
                    } else {
                        subcategoryIdList = new String[]{""};
                        subcategoryNameList = new String[]{"None"};
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, R.layout.spinner_item_layout, subcategoryNameList);
                        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
                        binding.subcategorySpinner.setAdapter(adapter);
                        binding.subcategorySpinner.setText(subcategoryNameList[0], false);
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