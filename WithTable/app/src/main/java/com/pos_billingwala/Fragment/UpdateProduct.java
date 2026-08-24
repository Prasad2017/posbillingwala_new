package com.pos_billingwala.Fragment;

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
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.jaredrummler.materialspinner.MaterialSpinner;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ProductPortionSectionHelper;
import com.pos_billingwala.Model.ProductCategoryResponse;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.Model.ProductSubcategoryResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentUpdateProductBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class UpdateProduct extends Fragment implements View.OnClickListener {

    public static Activity activity;
    View view;
    String[] categoryIdList, categoryNameList, unitNameList;
    String[] subcategoryIdList, subcategoryNameList;
    String productId, categoryId, categoryName, unitName, subcategoryId;
    List<ProductCategoryResponse> productCategoryResponseList = new ArrayList<>();
    List<ProductResponse> productResponseList = new ArrayList<>();
    POSBillingWalaDatabase posBillingWalaDatabase;
    FragmentUpdateProductBinding binding;
    ProductPortionSectionHelper portionSectionHelper;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentUpdateProductBinding.inflate(inflater, container, false);
        view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here

        activity = getActivity();

        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        Bundle bundle = getArguments();
        productId = bundle.getString("productId");

        portionSectionHelper = new ProductPortionSectionHelper(
                activity, posBillingWalaDatabase, view);
        portionSectionHelper.setOnPortionMasterLinkClick(this::openPortionMaster);
        portionSectionHelper.setOnPortionsChanged(this::syncProductCostVisibility);
        portionSectionHelper.loadExistingForProduct(productId);

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    ((MainActivity) activity).goBackTo(new ProductMaster(), true);
                    return true;
                }
                return false;
            }
        });

        binding.productName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        binding.categorySpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                categoryId = categoryIdList[position];
                categoryName = categoryNameList[position];
                loadSubcategorySpinner(categoryId, null);
            }
        });

        binding.unitSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                unitName = unitNameList[position];
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
        binding.updateProduct.setOnClickListener(this);

        return view;

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToProduct) {
            ((MainActivity) activity).goBackTo(new ProductMaster(), true);
        } else if (id == R.id.updateProduct) {
            if (categoryId != null) {
                if (!binding.productName.getText().toString().isEmpty()) {
                    String price = binding.productPrice.getText().toString().trim();
                    boolean hasPortions = portionSectionHelper != null && portionSectionHelper.hasPortions();
                    if (!price.isEmpty() || hasPortions) {
                        if (unitName != null) {
                            updateProduct();
                        } else {
                            Toast.makeText(activity, getString(R.string.toast_please_select_product_unit), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(activity, getString(R.string.toast_please_enter_product_price_or_add_portio), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(activity, getString(R.string.toast_please_enter_product_name), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(activity, getString(R.string.toast_please_select_product_category), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openPortionMaster() {
        AddPortionMaster addPortionMaster = new AddPortionMaster();
        Bundle bundle = new Bundle();
        bundle.putString("returnTo", "updateProduct");
        bundle.putString("productId", productId);
        addPortionMaster.setArguments(bundle);
                ((MainActivity) activity).loadFragment(addPortionMaster, true);
    }

    public void updateProduct() {

        String productPrice = binding.productPrice.getText().toString().trim();
        if (productPrice.isEmpty() && portionSectionHelper.hasPortions()) {
            productPrice = "0";
        }

        posBillingWalaDatabase.updateProduct(MainActivity.userId, productId, categoryId, categoryName, binding.productCode.getText().toString(), binding.productName.getText().toString(), productPrice,
                unitName, binding.productCGST.getText().toString(), binding.productSGST.getText().toString(), 0, subcategoryId);

        portionSectionHelper.savePortionsForProduct(productId);

        Toast.makeText(activity, getString(R.string.toast_product_updated_successfully), Toast.LENGTH_SHORT).show();
        ((MainActivity) activity).goBackTo(new ProductMaster(), true);

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
        getProductDetails();
        getProductCategoryList();
        if (portionSectionHelper != null) {
            portionSectionHelper.refresh();
        }
        syncProductCostVisibility();
    }

    private void syncProductCostVisibility() {
        if (binding == null) {
            return;
        }
        boolean hideCost = portionSectionHelper != null && portionSectionHelper.shouldHideProductCost();
        int visibility = hideCost ? View.GONE : View.VISIBLE;
        binding.productPrice.clearFocus();
        binding.productPriceSection.setVisibility(visibility);
        binding.productPriceLayout.setVisibility(visibility);
        binding.productPrice.setVisibility(visibility);
        binding.productGstSection.setVisibility(visibility);
    }

    public void getProductDetails() {

        productResponseList = posBillingWalaDatabase.getProductDetail(productId);
        if (!productResponseList.isEmpty()) {

            ProductResponse productResponse = productResponseList.get(0);

            categoryName = productResponse.getCategoryName();
            unitName = productResponse.getProductUnit();
            subcategoryId = productResponse.getSubcategoryId();
            categoryId = productResponse.getCategoryId();

            binding.productName.setText(productResponse.getProductName());
            binding.productPrice.setText(productResponse.getProductPrice());
            binding.productCGST.setText(productResponse.getProductCGST());
            binding.productSGST.setText(productResponse.getProductSGST());
            binding.productCode.setText(productResponse.getProductCode());

            unitNameList = activity.getResources().getStringArray(R.array.product_unit);
            try {
                final ArrayAdapter adapter = new ArrayAdapter(activity, android.R.layout.simple_spinner_item, unitNameList);
                adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
                binding.unitSpinner.setAdapter(adapter);
                if (unitName != null) {
                    int unitIndex = adapter.getPosition(unitName);
                    if (unitIndex >= 0) {
                        binding.unitSpinner.setSelectedIndex(unitIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    public void getProductCategoryList() {

        productCategoryResponseList = posBillingWalaDatabase.getProductCategoryList();
        if (!productCategoryResponseList.isEmpty()) {
            categoryIdList = new String[productCategoryResponseList.size()];
            categoryNameList = new String[productCategoryResponseList.size()];
            for (int i = 0; i < productCategoryResponseList.size(); i++) {

                categoryIdList[i] = productCategoryResponseList.get(i).getCategoryId();
                categoryNameList[i] = productCategoryResponseList.get(i).getCategoryName();

                try {
                    final ArrayAdapter adapter = new ArrayAdapter(activity, android.R.layout.simple_spinner_item, categoryNameList);
                    adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
                    binding.categorySpinner.setAdapter(adapter);
                    if (categoryName != null) {
                        int categoryIndex = adapter.getPosition(categoryName);
                        if (categoryIndex >= 0) {
                            binding.categorySpinner.setSelectedIndex(categoryIndex);
                        }
                    }
                    loadSubcategorySpinner(categoryId, subcategoryId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void loadSubcategorySpinner(String selectedCategoryId, String preselectSubcategoryId) {
        List<ProductSubcategoryResponse> subcategories = posBillingWalaDatabase.getProductSubcategoryList(selectedCategoryId);
        if (subcategories.isEmpty()) {
            binding.subcategorySection.setVisibility(View.GONE);
            subcategoryId = null;
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
}