package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Activity.ProductListBluetoothPrint;
import com.pos_billingwala.Adapter.ProductAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.AppExecutors;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Extra.ResponsiveUi;
import com.pos_billingwala.Extra.SimpleDividerItemDecoration;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentProductMasterBinding;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


public class ProductMaster extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public static RecyclerView productRecyclerView;
    public static List<ProductResponse> productResponseList = new ArrayList<>();
    public static List<ProductResponse> searchProductResponseList = new ArrayList<>();
    public static POSBillingWalaDatabase posBillingWalaDatabase;
    public static ProductAdapter productAdapter;
    public static View noDataFound;
    public static View printProductCardView;
    public static LinearLayout linearLayout;
    public static TextInputEditText searchProduct;
    public static TextView productCountText;
    View view;
    FragmentProductMasterBinding binding;
    private static WeakReference<ProductMaster> activeInstance;
    private static cn.pedant.SweetAlert.SweetAlertDialog activeLoader;

    public static void getProductList() {
        ProductMaster fragment = activeInstance != null ? activeInstance.get() : null;
        if (fragment == null || !fragment.isAdded()) {
            return;
        }
        getProductList(fragment);
    }

    private static void getProductList(ProductMaster fragment) {
        if (activity == null || posBillingWalaDatabase == null) {
            return;
        }
        ListLoader.dismiss(activeLoader);
        activeLoader = ListLoader.showForFragment(fragment);
        final cn.pedant.SweetAlert.SweetAlertDialog loader = activeLoader;
        AppExecutors.get().db().execute(() -> {
            List<ProductResponse> list = posBillingWalaDatabase.getAllProductList("", "");
            AppExecutors.get().main(() -> {
                try {
                    if (fragment == null || !fragment.isAdded() || fragment.getView() == null) {
                        return;
                    }
                    if (activity == null || productRecyclerView == null) {
                        return;
                    }
                    productResponseList.clear();
                    productResponseList = list != null ? list : new ArrayList<>();
                    if (!productResponseList.isEmpty()) {
                        productAdapter = new ProductAdapter(activity, productResponseList);
                        productRecyclerView.setAdapter(productAdapter);
                        if (linearLayout != null) {
                            linearLayout.setVisibility(View.VISIBLE);
                        }
                        if (printProductCardView != null) {
                            printProductCardView.setVisibility(View.VISIBLE);
                        }
                        if (noDataFound != null) {
                            noDataFound.setVisibility(View.GONE);
                        }
                        updateProductCount(productResponseList.size());
                    } else {
                        if (linearLayout != null) {
                            linearLayout.setVisibility(View.GONE);
                        }
                        if (printProductCardView != null) {
                            printProductCardView.setVisibility(View.GONE);
                        }
                        if (noDataFound != null) {
                            noDataFound.setVisibility(View.VISIBLE);
                        }
                        updateProductCount(0);
                    }
                } finally {
                    ListLoader.dismiss(loader);
                    if (activeLoader == loader) {
                        activeLoader = null;
                    }
                }
            });
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProductMasterBinding.inflate(inflater, container, false);
        view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here
        activity = getActivity();

        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);


        initViews();

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    navigateToCaller();
                    return true;
                }
                return false;
            }
        });

        searchProduct.setSelection(searchProduct.getText().toString().length());
        searchProduct.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                searchProductDetails(s.toString());
            }
        });

        return view;
    }

    public void initViews() {
        productRecyclerView = view.findViewById(R.id.productRecyclerView);
        // Dense product cards (actions + portions + tax) need wide columns.
        // POS catalog min-width (150dp) created 6-col crushed grids on tablets.
        int productColumns = 1;
        if (ResponsiveUi.isWideLayout(activity)) {
            int widthDp = ResponsiveUi.windowWidthDp(activity);
            int heightDp = ResponsiveUi.windowHeightDp(activity);
            int minCardDp = heightDp > 0 && heightDp < 720 ? 480 : 400;
            productColumns = Math.max(1, Math.min(2, widthDp / minCardDp));
        }
        productRecyclerView.setLayoutManager(new GridLayoutManager(activity, productColumns));
        if (productRecyclerView.getItemDecorationCount() == 0) {
            productRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(activity));
        }
        noDataFound = view.findViewById(R.id.noDataFound);
        searchProduct = view.findViewById(R.id.searchProduct);
        productCountText = view.findViewById(R.id.productCountText);
        linearLayout = view.findViewById(R.id.linearLayout);
        printProductCardView = view.findViewById(R.id.printProductCardView);

        binding.searchLayout.setEndIconOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
            try {
                startActivityForResult(intent, 1);
            } catch (ActivityNotFoundException a) {
                Toast.makeText(activity, getString(R.string.toast_oops_your_device_doesnt_support_speech_t), Toast.LENGTH_SHORT).show();
            }
        });


        binding.backToHome.setOnClickListener(this);
        binding.addProduct.setOnClickListener(this);
        binding.addProductEmpty.setOnClickListener(this);
        binding.printProductCardView.setOnClickListener(this);

    }

    /* When Mic activity close */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK && null != data) {
                String yourResult = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
                searchProduct.setText(yourResult.replace(" ", ""));
                searchProductDetails(yourResult.replace(" ", ""));
            }
        }
    }

    public void searchProductDetails(String s) {

        searchProductResponseList = new ArrayList<>();

        if (!s.isEmpty()) {
            for (int i = 0; i < productResponseList.size(); i++) {
                ProductResponse item = productResponseList.get(i);
                String subcategoryName = item.getSubcategoryName() != null ? item.getSubcategoryName() : "";
                if ((item.getProductName()
                        + (item.getCategoryName() != null ? item.getCategoryName() : "")
                        + subcategoryName
                        + (item.getProductCode() != null && !item.getProductCode().equals("") ? item.getProductCode() : "")
                        + (item.getProductPrice() != null && !item.getProductPrice().equals("") ? item.getProductPrice() : ""))
                        .toLowerCase().contains(s.toLowerCase().trim())) {
                    searchProductResponseList.add(item);
                }
            }

            if (searchProductResponseList.isEmpty()) {
                productRecyclerView.setVisibility(View.GONE);
                noDataFound.setVisibility(View.VISIBLE);
                updateProductCount(0);
            } else {
                noDataFound.setVisibility(View.GONE);
                productRecyclerView.setVisibility(View.VISIBLE);
                updateProductCount(searchProductResponseList.size());
            }

        } else {

            searchProductResponseList = new ArrayList<>();
            searchProductResponseList.addAll(productResponseList);

            productRecyclerView.setVisibility(View.VISIBLE);
            noDataFound.setVisibility(View.GONE);
            updateProductCount(searchProductResponseList.size());

        }

        productAdapter = new ProductAdapter(activity, searchProductResponseList);
        productRecyclerView.setAdapter(productAdapter);

    }

    private static void updateProductCount(int count) {
        if (productCountText != null && activity != null) {
            productCountText.setText(activity.getString(R.string.ui_products_count, count));
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToHome) {
            navigateToCaller();
        } else if (id == R.id.printProductCardView) {
            startActivity(new Intent(activity, ProductListBluetoothPrint.class));
        } else if (id == R.id.addProduct || id == R.id.addProductEmpty) {
            ((MainActivity) activity).loadFragment(new AddProduct(), true);
        }
    }

    private void navigateToCaller() {
        ((MainActivity) activity).navigateBack();
    }

    @Override
    public void onStart() {
        super.onStart();
        activeInstance = new WeakReference<>(this);
        ((MainActivity) activity).lockUnlockDrawer(1);
        getProductList();
    }

    @Override
    public void onDestroyView() {
        ListLoader.dismiss(activeLoader);
        activeLoader = null;
        if (activeInstance != null && activeInstance.get() == this) {
            activeInstance = null;
        }
        productRecyclerView = null;
        noDataFound = null;
        searchProduct = null;
        productCountText = null;
        linearLayout = null;
        printProductCardView = null;
        binding = null;
        super.onDestroyView();
    }
}