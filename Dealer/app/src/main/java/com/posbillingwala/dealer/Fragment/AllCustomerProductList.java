package com.posbillingwala.dealer.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.dealer.Activity.MainActivity;
import com.posbillingwala.dealer.Adapter.ProductAdapter;
import com.posbillingwala.dealer.Extra.DetectConnection;
import com.posbillingwala.dealer.Model.AllApiResponse;
import com.posbillingwala.dealer.Model.ProductResponse;
import com.posbillingwala.dealer.R;
import com.posbillingwala.dealer.Retrofit.Api;
import com.posbillingwala.dealer.Utils.CatalogImportExportHelper;
import com.posbillingwala.dealer.databinding.FragmentAllCustomerProductListBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, UseCompatLoadingForDrawables, StaticFieldLeak")
public class AllCustomerProductList extends Fragment {

    public static Activity activity;
    public static RecyclerView productRecyclerView;
    public static List<ProductResponse> productResponseList = new ArrayList<>();
    public static ProductAdapter productAdapter;
    public static TextView noDataFound;
    public static String customerId;
    View view;
    FragmentAllCustomerProductListBinding binding;
    CatalogImportExportHelper catalogImportExportHelper;

    public static void getProductList() {

        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().getProductList(customerId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    productResponseList = response.body().getProductResponseList();
                    if (productResponseList.size() > 0) {

                        productAdapter = new ProductAdapter(activity, productResponseList);
                        productRecyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
                        productRecyclerView.setAdapter(productAdapter);
                        productAdapter.notifyDataSetChanged();
                        productAdapter.notifyItemInserted(productResponseList.size() - 1);
                        productRecyclerView.setHasFixedSize(true);

                        productRecyclerView.setVisibility(View.VISIBLE);
                        noDataFound.setVisibility(View.GONE);

                    } else {
                        productRecyclerView.setVisibility(View.GONE);
                        noDataFound.setVisibility(View.VISIBLE);
                    }

                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                Log.e("tag", "" + t.getMessage());
                pDialog.dismiss();
            }
        });


    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAllCustomerProductListBinding.inflate(inflater, container, false);
        view = binding.getRoot();
        activity = getActivity();

        // Fragment locked in landscape screen orientation
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

        initViews();

        Bundle bundle = getArguments();
        if (bundle != null) {
            customerId = bundle.getString("customerId");
        }

        if (customerId != null) {
            catalogImportExportHelper = new CatalogImportExportHelper(
                    this, customerId, "products", "Products", AllCustomerProductList::getProductList);
            catalogImportExportHelper.bindBar(binding.catalogImportExportBar.getRoot());
        }

        initViews();

        MainActivity.title.setText("Product List");

        MainActivity.back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new AllCustomerList(), false);
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
                    ((MainActivity) activity).loadFragment(new AllCustomerList(), false);
                    return true;
                }
                return false;
            }
        });


        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (catalogImportExportHelper != null) {
            catalogImportExportHelper.handleActivityResult(requestCode, resultCode, data);
        }
    }

    private void initViews() {
        productRecyclerView = binding.productRecyclerView;
        noDataFound = binding.noDataFound;
    }

    public void onStart() {
        super.onStart();
        Log.e("onStart", "called");
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(1);
        MainActivity.drawerLayout.closeDrawers();
        if (DetectConnection.checkInternetConnection(activity)) {
            getProductList();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

}
