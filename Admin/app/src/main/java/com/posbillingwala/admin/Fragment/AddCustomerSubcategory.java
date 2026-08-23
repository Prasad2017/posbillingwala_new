package com.posbillingwala.admin.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
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

import com.google.android.material.textfield.TextInputEditText;
import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Adapter.SubcategoryAdapter;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.ProductSubcategoryResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, StaticFieldLeak")
public class AddCustomerSubcategory extends Fragment {

    public static Activity activity;
    public static String customerId;
    public static String categoryId;
    public static String categoryName;
    public static RecyclerView subcategoryRecyclerview;
    public static CardView subcategoryListCardView;
    public static TextView noDataFound;

    View view;
    @BindView(R.id.subcategoryName)
    TextInputEditText subcategoryNameEdit;
    @BindView(R.id.categoryInfo)
    TextView categoryInfo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_add_customer_subcategory, container, false);
        ButterKnife.bind(this, view);

        activity = getActivity();
        MainActivity.title.setText("Subcategories");

        Bundle bundle = getArguments();
        if (bundle != null) {
            customerId = bundle.getString("customerId");
            categoryId = bundle.getString("categoryId");
            categoryName = bundle.getString("categoryName");
        }

        categoryInfo.setText("Category: " + categoryName);
        subcategoryNameEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        subcategoryRecyclerview = view.findViewById(R.id.subcategoryRecyclerview);
        subcategoryListCardView = view.findViewById(R.id.subcategoryListCardView);
        noDataFound = view.findViewById(R.id.noDataFound);

        MainActivity.back.setOnClickListener(v -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            AddCustomerProductCategory addCustomerProductCategory = new AddCustomerProductCategory();
            Bundle backBundle = new Bundle();
            backBundle.putString("customerId", customerId);
            addCustomerProductCategory.setArguments(backBundle);
            ((MainActivity) activity).loadFragment(addCustomerProductCategory, true);
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

        return view;
    }

    @OnClick(R.id.addSubcategory)
    public void onAddSubcategory() {
        if (subcategoryNameEdit.getText().toString().trim().length() == 0) {
            Toast.makeText(activity, "Please enter subcategory name", Toast.LENGTH_SHORT).show();
            return;
        }
        addSubcategory();
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
                subcategoryNameEdit.getText().toString().trim(),
                getRandomString(10));
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && "1".equalsIgnoreCase(response.body().getStatus())) {
                    Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    subcategoryNameEdit.setText("");
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
            getSubcategoryList();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }
}
