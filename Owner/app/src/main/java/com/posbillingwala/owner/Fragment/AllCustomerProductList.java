package com.posbillingwala.owner.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Adapter.ProductAdapter;
import com.posbillingwala.owner.Extra.CatalogListSearch;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.ProductResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.Utils.CatalogImportExportHelper;
import com.posbillingwala.owner.databinding.FragmentAllCustomerProductListBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, UseCompatLoadingForDrawables, StaticFieldLeak")
public class AllCustomerProductList extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public static RecyclerView productRecyclerView;
    public static List<ProductResponse> productResponseList = new ArrayList<>();
    public static ProductAdapter productAdapter;
    public static View noDataFound;
    public static View productListContent;
    public static View listNoResults;
    public static TextView productCountText;
    public static TextInputEditText searchProduct;
    View view;
    FragmentAllCustomerProductListBinding binding;
    CatalogImportExportHelper catalogImportExportHelper;

    public static void getProductList() {

        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().getProductList(MainActivity.userId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    productResponseList = response.body() != null
                            && response.body().getProductResponseList() != null
                            ? response.body().getProductResponseList()
                            : new ArrayList<>();
                    if (!productResponseList.isEmpty()) {
                        if (productListContent != null) {
                            productListContent.setVisibility(View.VISIBLE);
                        }
                        noDataFound.setVisibility(View.GONE);
                        applyProductSearch();
                    } else {
                        productRecyclerView.setVisibility(View.GONE);
                        if (productListContent != null) {
                            productListContent.setVisibility(View.GONE);
                        }
                        noDataFound.setVisibility(View.VISIBLE);
                    }

                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                Log.e("tag", t.getMessage());
                pDialog.dismiss();
            }
        });


    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentAllCustomerProductListBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();

        initViews();

        catalogImportExportHelper = new CatalogImportExportHelper(
                this, MainActivity.userId, "products", "Products", AllCustomerProductList::getProductList);
        catalogImportExportHelper.bindBar(binding.catalogImportExportBar.getRoot());

        binding.listSearch.listSearchInput.setHint(R.string.ui_search_product);
        binding.listSearch.listSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                applyProductSearch();
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
                    ((MainActivity) activity).loadFragment(new UserSetting(), true);
                    return true;
                }
                return false;
            }
        });

        binding.backToHome.setOnClickListener(this);
        binding.addProduct.setOnClickListener(this);
        binding.addProductEmpty.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.backToHome) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new UserSetting(), true);
        } else if (view.getId() == R.id.addProduct || view.getId() == R.id.addProductEmpty) {
            ((MainActivity) activity).loadFragment(new AddCustomerProduct(), true);
        }
    }

    public void initViews() {
        productRecyclerView = binding.productRecyclerView;
        productRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
        noDataFound = binding.noDataFound;
        productListContent = binding.linearLayout;
        productCountText = binding.productCountText;
        listNoResults = binding.listNoResults;
        searchProduct = binding.listSearch.listSearchInput;
    }

    public static void applyProductSearch() {
        if (activity == null || productRecyclerView == null || productResponseList == null) {
            return;
        }
        String query = CatalogListSearch.currentQuery(searchProduct);
        List<ProductResponse> filtered = new ArrayList<>();
        for (ProductResponse item : productResponseList) {
            if (item == null) {
                continue;
            }
            if (CatalogListSearch.matches(query,
                    item.getProductName(),
                    item.getCategoryName(),
                    item.getSubcategoryName(),
                    item.getProductCode(),
                    item.getProductPrice())) {
                filtered.add(item);
            }
        }
        productAdapter = new ProductAdapter(activity, filtered);
        productRecyclerView.setAdapter(productAdapter);
        CatalogListSearch.showFilterEmpty(productRecyclerView, listNoResults, !filtered.isEmpty());
        if (productCountText != null) {
            productCountText.setText(filtered.size() + " Products");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (catalogImportExportHelper != null) {
            catalogImportExportHelper.handleActivityResult(requestCode, resultCode, data);
        }
    }

    public void onStart() {
        super.onStart();
        if (DetectConnection.checkInternetConnection(activity)) {
            getProductList();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

}