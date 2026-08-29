package com.posbillingwala.admin.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jaredrummler.materialspinner.MaterialSpinner;
import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Adapter.SubcategoryAdapter;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.ProductCategoryResponse;
import com.posbillingwala.admin.Model.ProductSubcategoryResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.Utils.CatalogImportExportHelper;
import com.posbillingwala.admin.databinding.FragmentAddCustomerSubcategoryBinding;

import java.util.List;
import java.util.Random;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, StaticFieldLeak")
public class AddCustomerSubcategory extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public static String customerId;
    public static String categoryId;
    public static RecyclerView subcategoryRecyclerview;
    public static CardView subcategoryListCardView;
    public static TextView noDataFound;

    View view;
    FragmentAddCustomerSubcategoryBinding binding;

    String[] categoryIdList, categoryNameList;
    CatalogImportExportHelper catalogImportExportHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddCustomerSubcategoryBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();
        MainActivity.title.setText("Subcategories");

        Bundle bundle = getArguments();
        if (bundle != null) {
            customerId = bundle.getString("customerId");
            if (bundle.containsKey("categoryId")) {
                categoryId = bundle.getString("categoryId");
            }
        }

        if (customerId != null) {
            catalogImportExportHelper = new CatalogImportExportHelper(
                    this, customerId, "subcategories", "Sub Categories", AddCustomerSubcategory::getSubcategoryList);
            catalogImportExportHelper.bindBar(binding.catalogImportExportBar.getRoot());
        }

        binding.subcategoryName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        subcategoryRecyclerview = binding.subcategoryRecyclerview;
        subcategoryListCardView = binding.subcategoryListCardView;
        noDataFound = binding.noDataFound;

        MainActivity.back.setOnClickListener(v -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new AllCustomerList(), true);
        });

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                MainActivity.back.performClick();
                return true;
            }
            return false;
        });

        binding.categorySpinner.setOnTouchListener((v, event) -> {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
            return false;
        });

        binding.categorySpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                if (categoryIdList != null && position >= 0 && position < categoryIdList.length) {
                    categoryId = categoryIdList[position];
                    getSubcategoryList();
                }
            }
        });

        binding.addSubcategory.setOnClickListener(this);

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (catalogImportExportHelper != null) {
            catalogImportExportHelper.handleActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.addSubcategory) {
            if (categoryId == null || categoryId.isEmpty()) {
                Toast.makeText(activity, "Please select a category", Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.subcategoryName.getText().toString().trim().length() == 0) {
                Toast.makeText(activity, "Please enter subcategory name", Toast.LENGTH_SHORT).show();
                return;
            }
            addSubcategory();
        }
    }

    private void loadCategories() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().getCategoryList(customerId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                if (response.isSuccessful() && response.body() != null
                        && response.body().getProductCategoryResponseList() != null
                        && !response.body().getProductCategoryResponseList().isEmpty()) {
                    List<ProductCategoryResponse> categories = response.body().getProductCategoryResponseList();
                    categoryIdList = new String[categories.size()];
                    categoryNameList = new String[categories.size()];
                    int preselectIndex = 0;
                    for (int i = 0; i < categories.size(); i++) {
                        categoryIdList[i] = categories.get(i).getCategoryId();
                        categoryNameList[i] = categories.get(i).getCategoryName();
                        if (categoryId != null && categoryId.equals(categoryIdList[i])) {
                            preselectIndex = i;
                        }
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, categoryNameList);
                    adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
                    binding.categorySpinner.setAdapter(adapter);
                    categoryId = categoryIdList[preselectIndex];
                    if (preselectIndex >= 0) {
                        binding.categorySpinner.setSelectedIndex(preselectIndex);
                    } else {
                        getSubcategoryList();
                    }
                } else {
                    Toast.makeText(activity, "Please add a category first", Toast.LENGTH_SHORT).show();
                    subcategoryListCardView.setVisibility(View.GONE);
                    noDataFound.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Log.e("categoryLoadError", "" + t.getMessage());
            }
        });
    }

    private void addSubcategory() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().saveSubcategory(
                customerId,
                categoryId,
                binding.subcategoryName.getText().toString().trim(),
                getRandomString(10));
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
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
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Log.e("subcategoryError", "" + t.getMessage());
            }
        });
    }

    public static void getSubcategoryList() {
        if (activity == null || categoryId == null || categoryId.isEmpty()) {
            return;
        }
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().getSubcategoryList(customerId, categoryId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
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
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
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
    public void onStart() {
        super.onStart();
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(1);
        MainActivity.drawerLayout.closeDrawers();
        if (DetectConnection.checkInternetConnection(activity)) {
            loadCategories();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }
}
