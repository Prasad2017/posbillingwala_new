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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Adapter.CategoryAdapter;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Extra.SimpleDividerItemDecoration;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.FoodTypeResponse;
import com.posbillingwala.admin.Model.ProductCategoryResponse;
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

@SuppressLint("SetTextI18n, NonConstantResourceId, UseCompatLoadingForDrawables, StaticFieldLeak")
public class AddCustomerProductCategory extends Fragment {

    public static Activity activity;
    public static List<ProductCategoryResponse> productCategoryResponseList = new ArrayList<>();
    public static CategoryAdapter categoryAdapter;
    public static RecyclerView categoryRecyclerview;
    public static CardView categoryListCardView;
    public static TextView noDataFound;
    public static String customerId;
    View view;
    @BindView(R.id.categoryName)
    TextInputEditText textInputEditText;
    @BindView(R.id.foodTypeSpinner)
    AutoCompleteTextView foodTypeSpinner;
    String[] foodTypeIdList, foodTypeNameList;
    String foodTypeId;

    public static void getProductCategoryList() {

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
                    if (productCategoryResponseList.size() > 0) {

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
        view = inflater.inflate(R.layout.fragment_add_customer_product_category, container, false);
        ButterKnife.bind(this, view);

        activity = getActivity();
        MainActivity.title.setText("Product Category");

        initViews();

        Bundle bundle = getArguments();
        if (bundle != null) {
            customerId = bundle.getString("customerId");
        }

        textInputEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        foodTypeSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (foodTypeIdList != null && position >= 0 && position < foodTypeIdList.length) {
                    foodTypeId = foodTypeIdList[position];
                }
            }
        });

        MainActivity.back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new AllCustomerList(), true);
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
                    ((MainActivity) activity).loadFragment(new AllCustomerList(), true);
                    return true;
                }
                return false;
            }
        });


        return view;

    }

    private void initViews() {

        categoryRecyclerview = view.findViewById(R.id.categoryRecyclerview);
        categoryListCardView = view.findViewById(R.id.categoryListCardView);
        noDataFound = view.findViewById(R.id.noDataFound);

    }

    @OnClick({R.id.addCategory})
    public void onClick(View view) {
        if (view.getId() == R.id.addCategory) {
            if (textInputEditText.getText().toString().length() > 0) {
                if (foodTypeId != null) {
                    addProductCategory();
                } else {
                    Toast.makeText(activity, "Please select food type", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(activity, "Please add category", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addProductCategory() {

        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        String categoryNetworkStatus = getRandomString(10);
        String categoryName = textInputEditText.getText().toString();

        Call<AllApiResponse> call = Api.getClient().saveCategory(customerId, categoryName, categoryNetworkStatus, foodTypeId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                        Toast.makeText(activity, "" + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        textInputEditText.setText("");
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
            getFoodTypeList();
            getProductCategoryList();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void getFoodTypeList() {
        Call<AllApiResponse> call = Api.getClient().getFoodTypeList();
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<FoodTypeResponse> foodTypes = response.body().getFoodTypeResponseList();
                    if (foodTypes != null && foodTypes.size() > 0) {
                        foodTypeIdList = new String[foodTypes.size()];
                        foodTypeNameList = new String[foodTypes.size()];
                        for (int i = 0; i < foodTypes.size(); i++) {
                            foodTypeIdList[i] = foodTypes.get(i).getFoodTypeId();
                            foodTypeNameList[i] = foodTypes.get(i).getFoodTypeName();
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, R.layout.spinner_item_layout, foodTypeNameList);
                        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
                        foodTypeSpinner.setAdapter(adapter);
                        foodTypeSpinner.setText(foodTypeNameList[0], false);
                        foodTypeId = foodTypeIdList[0];
                    }
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                Log.e("foodTypeError", "" + t.getMessage());
            }
        });
    }


}