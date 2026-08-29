package com.pos_billingwala.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ProductPortionSectionHelper;
import com.pos_billingwala.Model.ProductCategoryResponse;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.Model.ProductSubcategoryResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentAddProductBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


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
    ProductPortionSectionHelper portionSectionHelper;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddProductBinding.inflate(inflater, container, false);
        view = binding.getRoot();


        activity = getActivity();

        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        portionSectionHelper = new ProductPortionSectionHelper(
                activity, posBillingWalaDatabase, view);
        portionSectionHelper.setOnPortionMasterLinkClick(this::openPortionMaster);
        portionSectionHelper.setOnPortionsChanged(this::syncProductCostVisibility);

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    ((MainActivity) activity).navigateBack();
                    return true;
                }
                return false;
            }
        });

        binding.productFormBody.productName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);


        binding.productFormBody.categoryDropdown.setOnItemSelectedListener((position, item) -> {
            activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            try {
                categoryId = categoryIdList[position];
                categoryName = categoryNameList[position];
                loadSubcategoryDropdown(categoryId, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        unitNameList = activity.getResources().getStringArray(R.array.product_unit);
        binding.productFormBody.unitDropdown.setItems(unitNameList);
        binding.productFormBody.unitDropdown.setSelectedIndex(0);
        unitName = unitNameList[0];
        binding.productFormBody.unitDropdown.setOnItemSelectedListener((position, item) -> {
            try {
                unitName = unitNameList[position];
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        binding.productFormBody.subcategoryDropdown.setOnItemSelectedListener((position, item) -> {
            if (subcategoryIdList != null && position >= 0 && position < subcategoryIdList.length) {
                subcategoryId = subcategoryIdList[position];
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
            ((MainActivity) activity).navigateBack();
        } else if (id == R.id.addProduct) {
            if (categoryId != null) {
                if (!binding.productFormBody.productName.getText().toString().isEmpty()) {
                    String price = binding.productFormBody.productPrice.getText().toString().trim();
                    boolean hasPortions = portionSectionHelper != null && portionSectionHelper.hasPortions();
                    boolean openPrice = binding.productFormBody.openPriceSwitch.isChecked();
                    if (!price.isEmpty() || hasPortions || openPrice) {
                        if (unitName != null) {
                            addProduct();
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

    public void addProduct() {

        String networkStatus = getRandomString(10);
        String productPrice = binding.productFormBody.productPrice.getText().toString().trim();
        boolean openPriceOn = binding.productFormBody.openPriceSwitch.isChecked();
        if (productPrice.isEmpty() && (portionSectionHelper.hasPortions() || openPriceOn)) {
            productPrice = "0";
        }
        String openPrice = openPriceOn ? "on" : "off";
        long rowId = posBillingWalaDatabase.addProductAndReturnId(
                MainActivity.ownerId, categoryId, categoryName,
                binding.productFormBody.productCode.getText().toString(),
                binding.productFormBody.productName.getText().toString(),
                productPrice,
                unitName, binding.productFormBody.productCGST.getText().toString(),
                binding.productFormBody.productSGST.getText().toString(), 0, networkStatus, "0", subcategoryId,
                openPrice);

        String newProductId = rowId > 0 ? String.valueOf(rowId) : null;
        if (newProductId == null) {
            newProductId = posBillingWalaDatabase.getProductIdByNetworkStatus(networkStatus);
        }
        if (newProductId != null && portionSectionHelper.hasPortions()) {
            portionSectionHelper.savePortionsForProduct(newProductId);
        }

        Toast.makeText(activity, getString(R.string.toast_product_added_successfully), Toast.LENGTH_SHORT).show();
        ((MainActivity) activity).navigateBack();
    }

    private void openPortionMaster() {
        AddPortionMaster addPortionMaster = new AddPortionMaster();
        Bundle bundle = new Bundle();
        bundle.putString("returnTo", "addProduct");
        addPortionMaster.setArguments(bundle);
                ((MainActivity) activity).loadFragment(addPortionMaster, true);
    }

    private void loadSubcategoryDropdown(String selectedCategoryId, String preselectSubcategoryId) {
        subcategoryId = null;
        List<ProductSubcategoryResponse> subcategories = posBillingWalaDatabase.getProductSubcategoryList(selectedCategoryId);
        if (subcategories.isEmpty()) {
            binding.productFormBody.subcategorySection.setVisibility(View.GONE);
            subcategoryIdList = null;
            subcategoryNameList = null;
            return;
        }

        binding.productFormBody.subcategorySection.setVisibility(View.VISIBLE);
        subcategoryIdList = new String[subcategories.size() + 1];
        subcategoryNameList = new String[subcategories.size() + 1];
        subcategoryIdList[0] = null;
        subcategoryNameList[0] = "None";
        for (int i = 0; i < subcategories.size(); i++) {
            subcategoryIdList[i + 1] = subcategories.get(i).getSubcategoryId();
            subcategoryNameList[i + 1] = subcategories.get(i).getSubcategoryName();
        }

        binding.productFormBody.subcategoryDropdown.setItems(subcategoryNameList);

        int selection = 0;
        if (preselectSubcategoryId != null) {
            for (int i = 1; i < subcategoryIdList.length; i++) {
                if (preselectSubcategoryId.equals(subcategoryIdList[i])) {
                    selection = i;
                    break;
                }
            }
        }
        binding.productFormBody.subcategoryDropdown.setSelectedIndex(selection);
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
        binding.productFormBody.productPrice.clearFocus();
        binding.productFormBody.productPriceSection.setVisibility(visibility);
        binding.productFormBody.productPriceLayout.setVisibility(visibility);
        binding.productFormBody.productPrice.setVisibility(visibility);
        binding.productFormBody.productGstSection.setVisibility(visibility);
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
            categoryId = categoryIdList[0];
            categoryName = categoryNameList[0];
            binding.productFormBody.categoryDropdown.setItems(categoryNameList);
            binding.productFormBody.categoryDropdown.setSelectedIndex(0);
            loadSubcategoryDropdown(categoryId, null);
        }

    }

    @SuppressLint("SetTextI18n")
    public void getProductList() {

        productResponseList = posBillingWalaDatabase.getAllDESCProductList();
        if (!productResponseList.isEmpty()) {
            try {
                int productCode = Integer.parseInt(productResponseList.get(0).getProductCode()) + 1;
                binding.productFormBody.productCode.setText(String.valueOf(productCode));
            } catch (Exception e) {
                binding.productFormBody.productCode.setText("");
            }
        } else {
            int productCode = 1;
            binding.productFormBody.productCode.setText(String.valueOf(productCode));
        }

    }

}