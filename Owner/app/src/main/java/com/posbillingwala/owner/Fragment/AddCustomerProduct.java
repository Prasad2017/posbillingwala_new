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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.ProductCategoryResponse;
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
    public String categoryId, categoryName, unitName;
    public List<ProductCategoryResponse> productCategoryResponseList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentAddCustomerProductBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        activity = getActivity();

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                    ((MainActivity) activity).loadFragment(new AllCustomerProductList(), true);
                    return true;
                }
                return false;
            }
        });

        binding.productName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(activity, R.layout.spinner_item_layout, new String[]{});
        binding.categorySpinner.setAdapter(categoryAdapter);
        binding.categorySpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                categoryId = categoryIdList[position];
                categoryName = categoryNameList[position];
            }
        });

        unitNameList = activity.getResources().getStringArray(R.array.product_unit);
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(activity, R.layout.spinner_item_layout, unitNameList);
        binding.unitSpinner.setAdapter(unitAdapter);
        binding.unitSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                unitName = unitNameList[position];
            }
        });

        binding.categorySpinner.setOnTouchListener((v, event) -> {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
            return false;
        });

        binding.unitSpinner.setOnTouchListener((var v, var event) -> {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
            return false;
        });

        binding.backToHome.setOnClickListener(view1 -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new AllCustomerProductList(), true);
        });

        binding.addProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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

        Call<AllApiResponse> call = Api.getClient().saveProduct(MainActivity.userId, categoryId, categoryName, binding.productProduct.getText().toString(), binding.productName.getText().toString(), binding.productPrice.getText().toString(),
                unitName, binding.productCGST.getText().toString(), binding.productSGST.getText().toString(), getRandomString(10));
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                        Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                        ((MainActivity) activity).loadFragment(new AllCustomerProductList(), true);
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
        Log.e("onStart", "called");
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

        Call<AllApiResponse> call = Api.getClient().getCategoryList(MainActivity.userId);
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

                        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(activity, R.layout.spinner_item_layout, categoryNameList);
                        binding.categorySpinner.setAdapter(categoryAdapter);
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
}
