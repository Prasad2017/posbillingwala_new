package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jaredrummler.materialspinner.MaterialSpinner;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.SubcategoryAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Extra.SimpleDividerItemDecoration;
import com.pos_billingwala.Model.ProductCategoryResponse;
import com.pos_billingwala.Model.ProductSubcategoryResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentAddSubcategoryBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class AddSubcategory extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public static POSBillingWalaDatabase posBillingWalaDatabase;
    public static List<ProductSubcategoryResponse> subcategoryResponseList = new ArrayList<>();
    public static SubcategoryAdapter subcategoryAdapter;
    public static RecyclerView subcategoryRecyclerview;
    public static CardView subcategoryListCardView;
    public static TextView noDataFound;
    View view;

    FragmentAddSubcategoryBinding binding;
    List<ProductCategoryResponse> productCategoryResponseList = new ArrayList<>();
    String[] categoryIdList, categoryNameList;
    String categoryId, categoryName;
    public static String selectedCategoryId;

    public static void getSubcategoryList() {
        SweetAlertDialog loader = ListLoader.show(activity);
        try {
            subcategoryResponseList.clear();
            if (selectedCategoryId != null) {
                subcategoryResponseList = posBillingWalaDatabase.getProductSubcategoryList(selectedCategoryId);
            }
            if (!subcategoryResponseList.isEmpty()) {
                subcategoryAdapter = new SubcategoryAdapter(activity, subcategoryResponseList);
                subcategoryRecyclerview.setLayoutManager(new GridLayoutManager(activity, 1));
                subcategoryRecyclerview.setAdapter(subcategoryAdapter);
                subcategoryRecyclerview.addItemDecoration(new SimpleDividerItemDecoration(activity));

                subcategoryListCardView.setVisibility(View.VISIBLE);
                noDataFound.setVisibility(View.GONE);
            } else {
                subcategoryListCardView.setVisibility(View.GONE);
                noDataFound.setVisibility(View.VISIBLE);
            }
        } finally {
            ListLoader.dismiss(loader);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddSubcategoryBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();
        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        initViews();

        binding.subcategoryName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

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
        subcategoryRecyclerview = view.findViewById(R.id.subcategoryRecyclerview);
        subcategoryListCardView = view.findViewById(R.id.subcategoryListCardView);
        noDataFound = view.findViewById(R.id.noDataFound);

        binding.backToHome.setOnClickListener(this);
        binding.addSubcategory.setOnClickListener(this);

        setupCategorySpinner();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToHome) {
            navigateToCaller();
        } else if (id == R.id.addSubcategory) {
            if (categoryId == null || categoryId.isEmpty()) {
                Toast.makeText(activity, getString(R.string.toast_please_select_a_category), Toast.LENGTH_SHORT).show();
            } else if (binding.subcategoryName.getText().toString().trim().isEmpty()) {
                Toast.makeText(activity, getString(R.string.toast_please_add_subcategory_name), Toast.LENGTH_SHORT).show();
            } else {
                addProductSubcategory();
            }
        }
    }

    private void navigateToCaller() {
        if (getArguments() != null && MasterData.OPENED_FROM_MASTER.equals(getArguments().getString("openedFrom"))) {
            ((MainActivity) activity).goBackTo(new MasterData(), true);
        } else {
            ((MainActivity) activity).navigateToHome();
        }
    }

    private void addProductSubcategory() {
        String name = binding.subcategoryName.getText().toString().trim();
        List<ProductSubcategoryResponse> existing = posBillingWalaDatabase.getProductSubcategoryNameList(categoryId, name);
        if (!existing.isEmpty()) {
            Toast.makeText(activity, getString(R.string.toast_subcategory_already_exists_in_this_categ), Toast.LENGTH_SHORT).show();
            return;
        }

        posBillingWalaDatabase.insertProductSubcategory(
                categoryId,
                name,
                "0",
                getRandomString(10),
                0);

        Toast.makeText(activity, getString(R.string.toast_subcategory_added_successfully), Toast.LENGTH_SHORT).show();
        binding.subcategoryName.setText("");
        getSubcategoryList();
    }

    private void setupCategorySpinner() {
        productCategoryResponseList = posBillingWalaDatabase.getProductCategoryList();
        if (productCategoryResponseList.isEmpty()) {
            Toast.makeText(activity, getString(R.string.toast_please_add_a_category_first), Toast.LENGTH_SHORT).show();
            binding.addSubcategory.setEnabled(false);
            return;
        }

        binding.addSubcategory.setEnabled(true);
        categoryIdList = new String[productCategoryResponseList.size()];
        categoryNameList = new String[productCategoryResponseList.size()];
        for (int i = 0; i < productCategoryResponseList.size(); i++) {
            categoryIdList[i] = productCategoryResponseList.get(i).getCategoryId();
            categoryNameList[i] = productCategoryResponseList.get(i).getCategoryName();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, categoryNameList);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
        binding.categorySpinner.setAdapter(adapter);

        categoryId = categoryIdList[0];
        categoryName = categoryNameList[0];
        selectedCategoryId = categoryId;

        binding.categorySpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                categoryId = categoryIdList[position];
                categoryName = categoryNameList[position];
                selectedCategoryId = categoryId;
                getSubcategoryList();
            }
        });

        getSubcategoryList();
    }

    public String getRandomString(final int sizeOfRandomString) {
        String ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnm";
        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; ++i) {
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        }
        return sb.toString();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        getSubcategoryList();
    }
}
