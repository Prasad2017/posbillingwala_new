package com.pos_billingwala.Fragment;

import android.app.Activity;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.CategoryAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Extra.MasterListTabletUi;
import com.pos_billingwala.Model.ProductCategoryResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentAddCategoryBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cn.pedant.SweetAlert.SweetAlertDialog;


public class AddCategory extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public static POSBillingWalaDatabase posBillingWalaDatabase;
    public static List<ProductCategoryResponse> productCategoryResponseList = new ArrayList<>();
    public static List<ProductCategoryResponse> productCategoryNameResponseList = new ArrayList<>();
    public static CategoryAdapter categoryAdapter;
    public static RecyclerView categoryRecyclerview;
    public static View categoryListCardView;
    public static View noDataFound;
    View view;

    FragmentAddCategoryBinding binding;


    public static void getHomeProductCategoryList() {
        SweetAlertDialog loader = ListLoader.show(activity);
        try {
            productCategoryResponseList.clear();
            productCategoryResponseList = posBillingWalaDatabase.getProductCategoryList();
            if (!productCategoryResponseList.isEmpty()) {

                categoryAdapter = new CategoryAdapter(activity, productCategoryResponseList);
                categoryRecyclerview.setLayoutManager(new GridLayoutManager(activity,
                        MasterListTabletUi.listColumnCount(activity)));
                categoryRecyclerview.setAdapter(categoryAdapter);

                categoryListCardView.setVisibility(View.VISIBLE);
                noDataFound.setVisibility(View.GONE);

            } else {
                categoryListCardView.setVisibility(View.GONE);
                noDataFound.setVisibility(View.VISIBLE);
            }
        } finally {
            ListLoader.dismiss(loader);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddCategoryBinding.inflate(inflater, container, false);
        view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here


        activity = getActivity();

        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);


        initViews();

        MasterListTabletUi.applyFormListSplit(activity, binding.categoryMasterContainer,
                binding.categoryFormCard, binding.categoryListCardView);

        binding.categoryName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

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

        return view;
    }

    public void initViews() {

        categoryRecyclerview = view.findViewById(R.id.categoryRecyclerview);
        categoryListCardView = view.findViewById(R.id.categoryListCardView);
        noDataFound = view.findViewById(R.id.noDataFound);

        binding.backToHome.setOnClickListener(this);
        binding.addCategory.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToHome) {
            navigateToCaller();
        } else if (id == R.id.addCategory) {
            if (!binding.categoryName.getText().toString().isEmpty()) {
                addProductCategory();
            } else {
                Toast.makeText(activity, getString(R.string.toast_please_add_category), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void navigateToCaller() {
        ((MainActivity) activity).navigateBack();
    }

    public void addProductCategory() {

        productCategoryNameResponseList.clear();
        productCategoryNameResponseList = posBillingWalaDatabase.getProductCategoryNameList(binding.categoryName.getText().toString());
        if (!productCategoryNameResponseList.isEmpty()) {
            Toast.makeText(activity, getString(R.string.toast_product_category_already_added), Toast.LENGTH_SHORT).show();
        } else {

            posBillingWalaDatabase.insertProductCategory(
                    binding.categoryName.getText().toString(),
                    0,
                    "0",
                    getRandomString(10),
                    posBillingWalaDatabase.getDefaultFoodTypeId());

            Toast.makeText(activity, getString(R.string.toast_product_category_added_successfully), Toast.LENGTH_SHORT).show();
            binding.categoryName.setText("");
        }

        getHomeProductCategoryList();

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
        ((MainActivity) activity).lockUnlockDrawer(1);
        getHomeProductCategoryList();
    }
}