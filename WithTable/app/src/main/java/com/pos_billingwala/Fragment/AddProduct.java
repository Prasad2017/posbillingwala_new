package com.pos_billingwala.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.jaredrummler.materialspinner.MaterialSpinner;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.ProductCategoryResponse;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.Model.ProductSubcategoryResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentAddProductBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SuppressLint("ClickableViewAccessibility")
public class AddProduct extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public static List<ProductResponse> productResponseList = new ArrayList<>();
    View view;
    String[] categoryIdList, categoryNameList, unitNameList;
    String[] subcategoryIdList, subcategoryNameList;
    String categoryId, categoryName, unitName, subcategoryId;
    List<ProductCategoryResponse> productCategoryResponseList = new ArrayList<>();
    POSBillingWalaDatabase posBillingWalaDatabase;
    FragmentAddProductBinding binding;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddProductBinding.inflate(inflater, container, false);
        view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here


        activity = getActivity();

        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);


        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                    ((MainActivity) activity).loadFragment(new ProductMaster(), true);
                    return true;
                }
                return false;
            }
        });

        binding.productName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);


        binding.categorySpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                try {
                    categoryId = categoryIdList[position];
                    categoryName = categoryNameList[position];
                    loadSubcategorySpinner(categoryId, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });


        unitNameList = activity.getResources().getStringArray(R.array.product_unit);
        try {
            final ArrayAdapter adapter = new ArrayAdapter(activity, android.R.layout.simple_spinner_item, unitNameList);
            adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
            binding.unitSpinner.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        binding.unitSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                try {
                    unitName = unitNameList[position];
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });

        binding.categorySpinner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
                return false;
            }
        });

        binding.unitSpinner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
                return false;
            }
        });

        binding.subcategorySpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                if (subcategoryIdList != null && position >= 0 && position < subcategoryIdList.length) {
                    subcategoryId = subcategoryIdList[position];
                }
            }
        });

        binding.backToProduct.setOnClickListener(this);
        binding.addProduct.setOnClickListener(this);

        return view;

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToProduct) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new ProductMaster(), true);
        } else if (id == R.id.addProduct) {
            if (categoryId != null) {
                if (!binding.productName.getText().toString().isEmpty()) {
                    if (!binding.productPrice.getText().toString().isEmpty()) {
                        if (unitName != null) {
                            addProduct();
                        } else {
                            Toast.makeText(activity, "Please select product unit", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(activity, "Please enter product price", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(activity, "Please enter product name", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(activity, "Please select product category", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void addProduct() {

        posBillingWalaDatabase.addProduct(MainActivity.ownerId, categoryId, categoryName, binding.productCode.getText().toString(), binding.productName.getText().toString(), binding.productPrice.getText().toString(),
                unitName, binding.productCGST.getText().toString(), binding.productSGST.getText().toString(), 0, getRandomString(10), "0", subcategoryId);

        Toast.makeText(activity, "Product added successfully", Toast.LENGTH_SHORT).show();

        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
        ((MainActivity) activity).loadFragment(new ProductMaster(), true);

    }

    private void loadSubcategorySpinner(String selectedCategoryId, String preselectSubcategoryId) {
        subcategoryId = null;
        List<ProductSubcategoryResponse> subcategories = posBillingWalaDatabase.getProductSubcategoryList(selectedCategoryId);
        if (subcategories.isEmpty()) {
            binding.subcategorySection.setVisibility(View.GONE);
            subcategoryIdList = null;
            subcategoryNameList = null;
            return;
        }

        binding.subcategorySection.setVisibility(View.VISIBLE);
        subcategoryIdList = new String[subcategories.size() + 1];
        subcategoryNameList = new String[subcategories.size() + 1];
        subcategoryIdList[0] = null;
        subcategoryNameList[0] = "None";
        for (int i = 0; i < subcategories.size(); i++) {
            subcategoryIdList[i + 1] = subcategories.get(i).getSubcategoryId();
            subcategoryNameList[i + 1] = subcategories.get(i).getSubcategoryName();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, subcategoryNameList);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
        binding.subcategorySpinner.setAdapter(adapter);

        int selection = 0;
        if (preselectSubcategoryId != null) {
            for (int i = 1; i < subcategoryIdList.length; i++) {
                if (preselectSubcategoryId.equals(subcategoryIdList[i])) {
                    selection = i;
                    break;
                }
            }
        }
        binding.subcategorySpinner.setSelectedIndex(selection);
        subcategoryId = subcategoryIdList[selection];
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
        getProductCategoryList();
        getProductList();
    }

    public void getProductCategoryList() {

        productCategoryResponseList = posBillingWalaDatabase.getProductCategoryList();
        if (!productCategoryResponseList.isEmpty()) {
            categoryIdList = new String[productCategoryResponseList.size()];
            categoryNameList = new String[productCategoryResponseList.size()];
            for (int i = 0; i < productCategoryResponseList.size(); i++) {
                categoryIdList[i] = productCategoryResponseList.get(i).getCategoryId();
                categoryNameList[i] = productCategoryResponseList.get(i).getCategoryName();
            }
            try {
                final ArrayAdapter adapter = new ArrayAdapter(activity, android.R.layout.simple_spinner_item, categoryNameList);
                adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
                binding.categorySpinner.setAdapter(adapter);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    @SuppressLint("SetTextI18n")
    public void getProductList() {

        productResponseList = posBillingWalaDatabase.getAllDESCProductList();
        if (!productResponseList.isEmpty()) {
            try {
                int productCode = Integer.parseInt(productResponseList.get(0).getProductCode()) + 1;
                binding.productCode.setText(String.valueOf(productCode));
            } catch (Exception e) {
                binding.productCode.setText("");
            }
        } else {
            int productCode = 1;
            binding.productCode.setText(String.valueOf(productCode));
        }

    }

}