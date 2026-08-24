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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Activity.ProductListBluetoothPrint;
import com.pos_billingwala.Adapter.ProductAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.AppExecutors;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentProductMasterBinding;

import java.util.ArrayList;
import java.util.List;


public class ProductMaster extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public static RecyclerView productRecyclerView;
    public static List<ProductResponse> productResponseList = new ArrayList<>();
    public static List<ProductResponse> searchProductResponseList = new ArrayList<>();
    public static POSBillingWalaDatabase posBillingWalaDatabase;
    public static ProductAdapter productAdapter;
    public static TextView noDataFound;
    public static CardView printProductCardView;
    public static LinearLayout linearLayout;
    public static TextInputEditText searchProduct;
    public static ImageView voiceSearchProduct;
    View view;
    FragmentProductMasterBinding binding;

    public static void getProductList() {
        if (activity == null || posBillingWalaDatabase == null) {
            return;
        }
        final cn.pedant.SweetAlert.SweetAlertDialog loader = ListLoader.show(activity);
        AppExecutors.get().db().execute(() -> {
            List<ProductResponse> list = posBillingWalaDatabase.getAllProductList("", "");
            AppExecutors.get().main(() -> {
                try {
                    if (activity == null || productRecyclerView == null) {
                        return;
                    }
                    productResponseList.clear();
                    productResponseList = list != null ? list : new ArrayList<>();
                    if (!productResponseList.isEmpty()) {
                        productAdapter = new ProductAdapter(activity, productResponseList);
                        productRecyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
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
                    }
                } finally {
                    ListLoader.dismiss(loader);
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

        voiceSearchProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Call Activity for Voice Input */
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
                try {
                    startActivityForResult(intent, 1);
                } catch (ActivityNotFoundException a) {
                    Toast.makeText(activity, getString(R.string.toast_oops_your_device_doesnt_support_speech_t), Toast.LENGTH_SHORT).show();
                }
            }
        });


        return view;
    }

    public void initViews() {
        productRecyclerView = view.findViewById(R.id.productRecyclerView);
        noDataFound = view.findViewById(R.id.noDataFound);
        searchProduct = view.findViewById(R.id.searchProduct);
        voiceSearchProduct = view.findViewById(R.id.voiceSearchProduct);
        linearLayout = view.findViewById(R.id.linearLayout);
        printProductCardView = view.findViewById(R.id.printProductCardView);


        binding.backToHome.setOnClickListener(this);
        binding.addProduct.setOnClickListener(this);
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
            for (int i = 0; i < productResponseList.size(); i++)
                if ((productResponseList.get(i).getProductName() +
                        (productResponseList.get(i).getCategoryName() != null ? productResponseList.get(i).getCategoryName() : "") +
                        (!productResponseList.get(i).getProductCode().equals("") ? productResponseList.get(i).getProductCode() : "") +
                        (!productResponseList.get(i).getProductPrice().equals("") ? productResponseList.get(i).getProductPrice() : ""))
                        .toLowerCase().contains(s.toLowerCase().trim())) {
                    searchProductResponseList.add(productResponseList.get(i));
                }

            if (searchProductResponseList.isEmpty()) {
                productRecyclerView.setVisibility(View.GONE);
                noDataFound.setVisibility(View.VISIBLE);
            } else {
                noDataFound.setVisibility(View.GONE);
                productRecyclerView.setVisibility(View.VISIBLE);
            }

        } else {

            searchProductResponseList = new ArrayList<>();
            searchProductResponseList.addAll(productResponseList);

            productRecyclerView.setVisibility(View.VISIBLE);
            noDataFound.setVisibility(View.GONE);

        }

        productAdapter = new ProductAdapter(activity, searchProductResponseList);
        productRecyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
        productRecyclerView.setAdapter(productAdapter);

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToHome) {
            navigateToCaller();
        } else if (id == R.id.printProductCardView) {
            startActivity(new Intent(activity, ProductListBluetoothPrint.class));
        } else if (id == R.id.addProduct) {
            ((MainActivity) activity).loadFragment(new AddProduct(), true);
        }
    }

    private void navigateToCaller() {
        if (getArguments() != null && MasterData.OPENED_FROM_MASTER.equals(getArguments().getString("openedFrom"))) {
            ((MainActivity) activity).goBackTo(new MasterData(), true);
        } else {
            ((MainActivity) activity).navigateToHome();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        getProductList();
    }
}