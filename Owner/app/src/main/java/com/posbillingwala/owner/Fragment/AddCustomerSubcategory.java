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

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Adapter.SubcategoryAdapter;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.ProductSubcategoryResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.Utils.CatalogImportExportHelper;
import com.posbillingwala.owner.databinding.FragmentAddCustomerSubcategoryBinding;

import java.util.List;
import java.util.Random;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, StaticFieldLeak")
public class AddCustomerSubcategory extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public static String categoryId;
    public static String categoryName;
    public static RecyclerView subcategoryRecyclerview;
    public static CardView subcategoryListCardView;
    public static TextView noDataFound;

    FragmentAddCustomerSubcategoryBinding binding;
    CatalogImportExportHelper catalogImportExportHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddCustomerSubcategoryBinding.inflate(inflater, container, false);

        activity = getActivity();

        Bundle bundle = getArguments();
        if (bundle != null) {
            categoryId = bundle.getString("categoryId");
            categoryName = bundle.getString("categoryName");
        }

        binding.categoryInfo.setText("Category: " + categoryName);
        binding.subcategoryName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        subcategoryRecyclerview = binding.subcategoryRecyclerview;
        subcategoryListCardView = binding.subcategoryListCardView;
        noDataFound = binding.noDataFound;

        catalogImportExportHelper = new CatalogImportExportHelper(
                this, MainActivity.userId, "subcategories", "Sub Categories", AddCustomerSubcategory::getSubcategoryList);
        catalogImportExportHelper.bindBar(binding.catalogImportExportBar.getRoot());

        binding.backToHome.setOnClickListener(v -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new AddCustomerProductCategory(), true);
        });

        binding.getRoot().setFocusableInTouchMode(true);
        binding.getRoot().requestFocus();
        binding.getRoot().setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                binding.backToHome.performClick();
                return true;
            }
            return false;
        });

        binding.addSubcategory.setOnClickListener(this);

        return binding.getRoot();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.addSubcategory) {
            if (binding.subcategoryName.getText().toString().trim().length() == 0) {
                Toast.makeText(activity, "Please enter subcategory name", Toast.LENGTH_SHORT).show();
                return;
            }
            addSubcategory();
        }
    }

    private void addSubcategory() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().saveSubcategory(
                MainActivity.userId,
                categoryId,
                binding.subcategoryName.getText().toString().trim(),
                getRandomString(10));
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && "1".equalsIgnoreCase(response.body().getStatus())) {
                    Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    binding.subcategoryName.setText("");
                    getSubcategoryList();
                } else if (response.body() != null) {
                    Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                Log.e("subcategoryError", "" + t.getMessage());
            }
        });
    }

    public static void getSubcategoryList() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().getSubcategoryList(MainActivity.userId, categoryId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ProductSubcategoryResponse> list = response.body().getSubcategoryResponseList();
                    if (list != null && !list.isEmpty()) {
                        SubcategoryAdapter adapter = new SubcategoryAdapter(activity, list);
                        subcategoryRecyclerview.setLayoutManager(new GridLayoutManager(activity, 1));
                        subcategoryRecyclerview.setAdapter(adapter);
                        subcategoryListCardView.setVisibility(View.VISIBLE);
                        noDataFound.setVisibility(View.GONE);
                    } else {
                        subcategoryListCardView.setVisibility(View.GONE);
                        noDataFound.setVisibility(View.VISIBLE);
                    }
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
            }
        });
    }

    private String getRandomString(final int sizeOfRandomString) {
        String allowed = "0123456789qwertyuiopasdfghjklzxcvbnm";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; ++i) {
            sb.append(allowed.charAt(random.nextInt(allowed.length())));
        }
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
        MainActivity.drawerLayout.closeDrawers();
        if (DetectConnection.checkInternetConnection(activity)) {
            getSubcategoryList();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }
}
