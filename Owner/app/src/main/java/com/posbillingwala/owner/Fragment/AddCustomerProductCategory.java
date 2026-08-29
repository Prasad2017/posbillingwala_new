package com.posbillingwala.owner.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Adapter.CategoryAdapter;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Extra.SimpleDividerItemDecoration;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.ProductCategoryResponse;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.Utils.CatalogImportExportHelper;
import com.posbillingwala.owner.databinding.FragmentAddCustomerProductCategoryBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, UseCompatLoadingForDrawables, StaticFieldLeak")
public class AddCustomerProductCategory extends Fragment {

    public static Activity activity;
    public static List<ProductCategoryResponse> productCategoryResponseList = new ArrayList<>();
    public static CategoryAdapter categoryAdapter;
    public static RecyclerView categoryRecyclerview;
    public static CardView categoryListCardView;
    public static TextView noDataFound;
    public FragmentAddCustomerProductCategoryBinding binding;
    CatalogImportExportHelper catalogImportExportHelper;

    public static void getProductCategoryList() {
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
                        categoryAdapter = new CategoryAdapter(activity, productCategoryResponseList);
                        categoryRecyclerview.setLayoutManager(new GridLayoutManager(activity, 1));
                        categoryRecyclerview.setAdapter(categoryAdapter);
                        categoryRecyclerview.addItemDecoration(new SimpleDividerItemDecoration(activity));
                        categoryAdapter.notifyDataSetChanged();
                        categoryAdapter.notifyItemInserted(productCategoryResponseList.size() - 1);
                        categoryRecyclerview.setHasFixedSize(true);

                        categoryListCardView.setVisibility(View.VISIBLE);
                        noDataFound.setVisibility(View.GONE);

                    } else {
                        categoryListCardView.setVisibility(View.GONE);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentAddCustomerProductCategoryBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        activity = getActivity();

        initViews();

        catalogImportExportHelper = new CatalogImportExportHelper(
                this, MainActivity.userId, "categories", "Categories", AddCustomerProductCategory::getProductCategoryList);
        catalogImportExportHelper.bindBar(binding.catalogImportExportBar.getRoot());

        binding.categoryName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

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

        binding.backToHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new UserSetting(), true);
            }
        });

        binding.addCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!binding.categoryName.getText().toString().isEmpty()) {
                    addProductCategory();
                } else {
                    Toast.makeText(activity, "Please add category", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.managePortionMaster.setOnClickListener(v -> {
            ((MainActivity) activity).loadFragment(new AddCustomerPortionMaster(), true);
        });

        return view;
    }

    public void initViews() {
        categoryRecyclerview = binding.categoryRecyclerview;
        categoryListCardView = binding.categoryListCardView;
        noDataFound = binding.noDataFound;
    }

    public void addProductCategory() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        String categoryNetworkStatus = getRandomString(10);
        String categoryName = binding.categoryName.getText().toString();

        Call<AllApiResponse> call = Api.getClient().saveCategory(MainActivity.userId, categoryName, categoryNetworkStatus);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                        Toast.makeText(activity, "" + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        getProductCategoryList();
                    } else {
                        Toast.makeText(activity, "" + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Log.e("serverError", "" + t.getMessage());
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (catalogImportExportHelper != null) {
            catalogImportExportHelper.handleActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.e("onStart", "called");
        MainActivity.drawerLayout.closeDrawers();
        if (DetectConnection.checkInternetConnection(activity)) {
            getProductCategoryList();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }
}
