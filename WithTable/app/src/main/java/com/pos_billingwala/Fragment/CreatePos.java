package com.pos_billingwala.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.pos_billingwala.Activity.BluetoothPrint;
import com.pos_billingwala.Activity.DuplicateBluetoothPrint;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.HomeCategoryAdapter;
import com.pos_billingwala.Adapter.HomeProductAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Interface.ClickListerInterface;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Model.ProductCartResponse;
import com.pos_billingwala.Model.ProductCategoryResponse;
import com.pos_billingwala.Model.ProductPortionResponse;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.Model.ProductSubcategoryResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.WorkerClass.HomeAllProductsWorker;
import com.pos_billingwala.WorkerClass.HomeProductsWorker;
import com.pos_billingwala.WorkerClass.ProductCategoryWorker;
import com.pos_billingwala.databinding.FragmentCreatePosBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@SuppressLint({"Range", "SetTextI18n, NonConstantResourceId"})
public class CreatePos extends Fragment implements ClickListerInterface, View.OnClickListener {

    public static String tableNumber, cartOrderStatus;
    public static Activity activity;
    public static String categoryName;
    public static String selectedCategoryId;
    public static String selectedSubcategoryId;
    public static List<CompanyResponse> companyResponseList = new ArrayList<>();
    public static List<ProductCategoryResponse> productCategoryResponseList = new ArrayList<>();
    public static List<ProductResponse> productResponseList = new ArrayList<>();
    public static List<ProductResponse> searchHomeProductResponseList = new ArrayList<>();
    public static List<ProductResponse> homeProductResponseList = new ArrayList<>();
    public static List<ProductCartResponse> productCartResponseList = new ArrayList<>();
    public static List<PrinterSettingResponse> printerSettingResponseList = new ArrayList<>();
    public static HomeCategoryAdapter homeCategoryAdapter;
    public static HomeProductAdapter homeProductAdapter;
    public static FragmentCreatePosBinding binding;
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    PopupWindow mypopupWindow;
    private final Handler productSearchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingProductSearch;

    /* When Mic activity close */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK && null != data) {
                String yourResult = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
                binding.productSearch.setText(yourResult.replace(" ", ""));
                searchHomeProduct(yourResult.replace(" ", ""));
            }
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCreatePosBinding.inflate(inflater, container, false);
        view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here


        activity = getActivity();


        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);
        Bundle bundle = getArguments();
        if (bundle != null) {
            tableNumber = bundle.getString("tableNumber");
            cartOrderStatus = bundle.getString("cartOrderStatus");
            if (cartOrderStatus.equalsIgnoreCase("table_wise")) {
                binding.posHeading.setText("Bill: Table No." + tableNumber);
                binding.menuIcon.setVisibility(View.GONE);
            } else if (cartOrderStatus.equalsIgnoreCase("take_away")) {
                binding.posHeading.setText("Take Away No. " + tableNumber);
                binding.menuIcon.setVisibility(View.GONE);
            } else {
                binding.posHeading.setText("Fast Billing");
                binding.menuIcon.setVisibility(View.VISIBLE);
            }
        }

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    if (cartOrderStatus.equalsIgnoreCase("table_wise")) {
                        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                        ((MainActivity) activity).loadFragment(new InvoiceCompanyTable(), true);
                    } else if (cartOrderStatus.equalsIgnoreCase("take_away")) {
                        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                        ((MainActivity) activity).loadFragment(new InvoiceTakeAway(), true);
                    } else {
                        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                        ((MainActivity) activity).loadFragment(new Home(), false);
                    }
                    return true;
                }
                return false;
            }
        });


        binding.productSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (pendingProductSearch != null) {
                    productSearchHandler.removeCallbacks(pendingProductSearch);
                }
                final String query = s != null ? s.toString() : "";
                if (query.isEmpty()) {
                    binding.categoryRecyclerView.setVisibility(View.VISIBLE);
                    binding.productLinearLayout.setVisibility(View.GONE);
                    clearSubcategoryFilter();
                    return;
                }
                // Debounce to avoid a DB hit on every keystroke
                pendingProductSearch = () -> searchHomeProduct(query);
                productSearchHandler.postDelayed(pendingProductSearch, 180);
            }
        });


        binding.voiceSearchProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Call Activity for Voice Input */
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
                try {
                    startActivityForResult(intent, 1);
                } catch (ActivityNotFoundException a) {
                    Toast.makeText(activity, getString(R.string.toast_oops_your_device_doesnt_support_speech_t), Toast.LENGTH_SHORT).show();
                }
            }
        });


        binding.homeCardView.setOnClickListener(this);
        binding.backToCategory.setOnClickListener(this);
        binding.cartLayout.setOnClickListener(this);
        binding.menuIcon.setOnClickListener(this);

        return view;

    }

    private void setupSubcategoryFilter(String categoryId) {
        binding.subcategoryChipGroup.removeAllViews();
        binding.subcategoryChipGroup.setOnCheckedStateChangeListener(null);

        List<ProductSubcategoryResponse> subcategories = posBillingWalaDatabase.getProductSubcategoryList(categoryId);
        if (subcategories == null || subcategories.isEmpty()) {
            binding.subcategoryScrollView.setVisibility(View.GONE);
            selectedSubcategoryId = null;
            return;
        }

        binding.subcategoryScrollView.setVisibility(View.VISIBLE);

        Chip allChip = new Chip(activity);
        allChip.setText("All");
        allChip.setCheckable(true);
        allChip.setChecked(true);
        allChip.setTag("");
        binding.subcategoryChipGroup.addView(allChip);

        for (ProductSubcategoryResponse subcategory : subcategories) {
            Chip chip = new Chip(activity);
            chip.setText(subcategory.getSubcategoryName());
            chip.setCheckable(true);
            chip.setTag(subcategory.getSubcategoryId());
            binding.subcategoryChipGroup.addView(chip);
        }

        binding.subcategoryChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            Chip selectedChip = group.findViewById(checkedIds.get(0));
            if (selectedChip == null) {
                return;
            }
            Object tag = selectedChip.getTag();
            if (tag == null || String.valueOf(tag).isEmpty()) {
                selectedSubcategoryId = null;
            } else {
                selectedSubcategoryId = String.valueOf(tag);
            }
            getHomeProductList();
        });
    }

    private void clearSubcategoryFilter() {
        selectedCategoryId = null;
        selectedSubcategoryId = null;
        binding.subcategoryChipGroup.removeAllViews();
        binding.subcategoryChipGroup.setOnCheckedStateChangeListener(null);
        binding.subcategoryScrollView.setVisibility(View.GONE);
    }

    private String resolveCategoryName(String categoryId) {
        if (categoryId == null || productCategoryResponseList == null) {
            return categoryName;
        }
        for (ProductCategoryResponse category : productCategoryResponseList) {
            if (categoryId.equals(category.getCategoryId())) {
                return category.getCategoryName();
            }
        }
        return categoryName;
    }

    public void searchHomeProduct(String productName) {

        if (productName == null || productName.trim().isEmpty()) {
            binding.categoryRecyclerView.setVisibility(View.VISIBLE);
            binding.productLinearLayout.setVisibility(View.GONE);
            return;
        }

        searchHomeProductResponseList.clear();
        List<ProductResponse> results = posBillingWalaDatabase.searchProducts(
                productName.trim(), tableNumber, cartOrderStatus);
        searchHomeProductResponseList.addAll(results);
        // Keep legacy list in sync for any callers that still read it
        homeProductResponseList.clear();
        homeProductResponseList.addAll(results);

        homeProductAdapter = new HomeProductAdapter(activity, searchHomeProductResponseList, CreatePos.this);
        binding.productRecyclerView.setAdapter(homeProductAdapter);

        binding.categoryRecyclerView.setVisibility(View.GONE);
        binding.productLinearLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.homeCardView) {
            if (cartOrderStatus.equalsIgnoreCase("table_wise")) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new InvoiceCompanyTable(), true);
            } else if (cartOrderStatus.equalsIgnoreCase("take_away")) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new InvoiceTakeAway(), true);
            } else {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new Home(), false);
            }
        } else if (id == R.id.backToCategory) {
            binding.productSearch.setText("");
            binding.productSearch.clearFocus();
            clearSubcategoryFilter();
            binding.categoryRecyclerView.setVisibility(View.VISIBLE);
            binding.productLinearLayout.setVisibility(View.GONE);
        } else if (id == R.id.cartLayout) {
            if (!productCartResponseList.isEmpty()) {
                Intent intent = new Intent(activity, BluetoothPrint.class);
                intent.putExtra("invoiceRunningStatus", "printBill");
                intent.putExtra("tableNumber", tableNumber);
                intent.putExtra("cartOrderStatus", cartOrderStatus);
                startActivity(intent);
            } else {
                Toast.makeText(activity, getString(R.string.toast_add_product_into_cart), Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.menuIcon) {
            setPopUpWindow();
        }
    }

    public void setPopUpWindow() {

        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.share_dialog, null);
        mypopupWindow = new PopupWindow(view, RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT, true);

        LinearLayout saveInvoiceLayout = view.findViewById(R.id.saveInvoiceLayout);
        LinearLayout duplicateInvoicePrintLayout = view.findViewById(R.id.duplicateInvoicePrintLayout);

        saveInvoiceLayout.setVisibility(View.GONE);

        duplicateInvoicePrintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mypopupWindow.dismiss();

                Intent intent = new Intent(activity, DuplicateBluetoothPrint.class);
                intent.putExtra("invoiceRunningStatus", "printBill");
                intent.putExtra("cartOrderStatus", "fast_billing");
                activity.startActivity(intent);

            }
        });

        mypopupWindow.showAsDropDown(binding.menuIcon, 0, -75);

    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        getCompanyDetails();
        getPrinterDetails();
    }

    public void getPrinterDetails() {
        printerSettingResponseList = posBillingWalaDatabase.getPrinterSettingDetails();
    }

    public void getCompanyDetails() {

        companyResponseList = posBillingWalaDatabase.getCompanyDetails();
        if (!companyResponseList.isEmpty()) {

            getHomeProductCategoryList();
            getHomeAllProductList();
            getCartCount();

        } else {
            Toast.makeText(activity, getString(R.string.toast_please_fill_shop_details), Toast.LENGTH_SHORT).show();
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new CompanyDetailSetting(), true);
        }

    }


    public void getCartCount() {

        productCartResponseList.clear();
        productCartResponseList = posBillingWalaDatabase.getCartProductList(tableNumber, cartOrderStatus);
        String discountType = "";
        float totalPerProductAmount = 0f, discountAmount = 0f, totalCGST = 0f, totalSGST = 0f, totalPerProductGST = 0f, totalGST = 0f;
        if (!productCartResponseList.isEmpty()) {

            for (int i = 0; i < productCartResponseList.size(); i++) {

                float productPrice = Float.parseFloat(productCartResponseList.get(i).getProductOldPrice());
                float productQuantity = Float.parseFloat(productCartResponseList.get(i).getProductQuantity());
                if (!CreatePos.companyResponseList.isEmpty()) {
                    if (CreatePos.companyResponseList.get(0).getGstStatus() != null) {
                        if (CreatePos.companyResponseList.get(0).getGstStatus().equalsIgnoreCase("On")) {
                            if (!productCartResponseList.get(i).getProductCGST().equalsIgnoreCase("")) {
                                totalCGST += Float.parseFloat(productCartResponseList.get(i).getProductCGST());
                            }
                            if (!productCartResponseList.get(i).getProductSGST().equalsIgnoreCase("")) {
                                totalSGST += Float.parseFloat(productCartResponseList.get(i).getProductSGST());
                            }
                            discountAmount = Float.parseFloat(productCartResponseList.get(i).getCartDiscount());
                            discountType = productCartResponseList.get(0).getCartDiscountType();
                            totalPerProductGST = (productPrice * ((totalCGST + totalSGST) / 100));
                            totalGST += (productPrice * ((totalCGST + totalSGST) / 100)) * productQuantity;

                            totalPerProductAmount = totalPerProductAmount + ((productPrice + totalPerProductGST) * productQuantity);
                        } else {
                            totalPerProductAmount = totalPerProductAmount + (productPrice * productQuantity);
                        }
                    } else {
                        totalPerProductAmount = totalPerProductAmount + (productPrice * productQuantity);
                    }
                } else {
                    totalPerProductAmount = totalPerProductAmount + (productPrice * productQuantity);
                }
            }

            float subTotalAmt = totalPerProductAmount - totalGST;
            binding.totalItems.setText("Total Items: " + productCartResponseList.size());
            if (discountType != null) {
                if (discountType.equalsIgnoreCase("Amount")) {
                    discountAmount = discountAmount;
                } else {
                    discountAmount = subTotalAmt / (100 / discountAmount);
                }
            } else {
                discountAmount = subTotalAmt / (100 / discountAmount);
            }

            float shopCGST = 0f, shopSGST = 0f;
            if (companyResponseList.get(0).getShopCGST() != null) {
                shopCGST = subTotalAmt * (Float.parseFloat(companyResponseList.get(0).getShopCGST().trim()) / 100);
            }

            if (companyResponseList.get(0).getShopSGST() != null) {
                if (!companyResponseList.get(0).getShopSGST().trim().equalsIgnoreCase("")) {
                    shopSGST = subTotalAmt * (Float.parseFloat(companyResponseList.get(0).getShopSGST().trim()) / 100);
                }
            }
            float totalShopGST = shopCGST + shopSGST;

            float totalAmount = totalPerProductAmount - discountAmount + totalShopGST;
            totalAmount = (float) Math.ceil(totalAmount);
            String totalPayableAmount = "Payable Amount<br/><b>" + MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", totalAmount) + "</b>";
            binding.totalAmount.setText(Html.fromHtml(totalPayableAmount));

        }

    }

    public void getHomeProductCategoryList() {

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ProductCategoryWorker.class).build();
        WorkManager.getInstance(activity).getWorkInfoByIdLiveData(workRequest.getId())
                .observe((LifecycleOwner) activity, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        if (!productCategoryResponseList.isEmpty()) {

                            homeCategoryAdapter = new HomeCategoryAdapter(activity, CreatePos.productCategoryResponseList, CreatePos.this);
                            homeCategoryAdapter.setSelectedCategoryId(selectedCategoryId);
                            binding.categoryRecyclerView.setLayoutManager(
                                    new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
                            binding.categoryRecyclerView.setAdapter(CreatePos.homeCategoryAdapter);

                            binding.categoryRecyclerView.setVisibility(View.VISIBLE);
                            if (selectedCategoryId == null) {
                                binding.productLinearLayout.setVisibility(View.GONE);
                            }

                        }
                    }
                });

        // Enqueue the work request
        WorkManager.getInstance(activity).enqueue(workRequest);


    }

    @Override
    public void categoryClicked(String categoryId) {
        selectedCategoryId = categoryId;
        selectedSubcategoryId = null;
        categoryName = resolveCategoryName(categoryId);
        if (homeCategoryAdapter != null) {
            homeCategoryAdapter.setSelectedCategoryId(categoryId);
        }
        setupSubcategoryFilter(categoryId);
        getHomeProductList();
    }

    public void setUpdateQuantity(ProductResponse productResponse, ProductPortionResponse portion) {

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.update_amount_quantity_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCancelable(false);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        TextView continueToQuantity = dialog.findViewById(R.id.continueToQuantity);
        TextView dismissQuantity = dialog.findViewById(R.id.dismissQuantity);
        TextInputEditText amountTxt = dialog.findViewById(R.id.amount);
        TextInputEditText quantityTxt = dialog.findViewById(R.id.quantity);

        String defaultPrice = resolveLinePrice(productResponse, portion);
        if (!productCartResponseList.isEmpty()) {
            amountTxt.setText(productCartResponseList.get(0).getResolvedLinePrice());
            quantityTxt.setText(productCartResponseList.get(0).getProductQuantity());
        } else {
            amountTxt.setText(defaultPrice);
            quantityTxt.setText("1");
        }

        quantityTxt.setSelection(quantityTxt.getText().toString().length());
        amountTxt.setSelection(amountTxt.getText().toString().length());

        dismissQuantity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        continueToQuantity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!quantityTxt.getText().toString().isEmpty()) {
                    if (Float.parseFloat(quantityTxt.getText().toString()) > 0) {
                        float totalQuantity = Float.parseFloat(quantityTxt.getText().toString());
                        if (!productCartResponseList.isEmpty()) {
                            updateCart(productCartResponseList.get(0).getCartId(), String.valueOf(totalQuantity), amountTxt.getText().toString());
                        } else {
                            addToCart(productResponse, amountTxt.getText().toString(), String.valueOf(totalQuantity), portion);
                        }
                        dialog.dismiss();
                    } else {
                        quantityTxt.setError("Enter Quantity");
                        quantityTxt.requestFocus();
                    }
                } else {
                    quantityTxt.setError("Enter Quantity");
                    quantityTxt.requestFocus();
                }
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);

    }

    @Override
    public void productClicked(ProductResponse productResponse) {
        if (posBillingWalaDatabase.hasProductPortions(productResponse.getProductId())) {
            List<ProductPortionResponse> portions = posBillingWalaDatabase.getProductPortionList(productResponse.getProductId());
            if (portions.isEmpty()) {
                handleProductSelection(productResponse, null);
                return;
            }
            showPortionDialog(productResponse, portions);
        } else {
            handleProductSelection(productResponse, null);
        }
    }

    private void showPortionDialog(ProductResponse productResponse, List<ProductPortionResponse> portions) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_select_portion);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCancelable(false);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        TextView productName = dialog.findViewById(R.id.productName);
        RadioGroup portionRadioGroup = dialog.findViewById(R.id.portionRadioGroup);
        TextView quantityMinus = dialog.findViewById(R.id.quantityMinus);
        TextView productQuantity = dialog.findViewById(R.id.productQuantity);
        TextView quantityPlus = dialog.findViewById(R.id.quantityPlus);
        TextView dismissPortion = dialog.findViewById(R.id.dismissPortion);
        TextView addPortionToCart = dialog.findViewById(R.id.addPortionToCart);

        productName.setText(productResponse.getProductName());
        final int[] quantity = {1};
        productQuantity.setText(String.valueOf(quantity[0]));

        for (int i = 0; i < portions.size(); i++) {
            ProductPortionResponse portion = portions.get(i);
            RadioButton radioButton = new RadioButton(activity);
            radioButton.setId(View.generateViewId());
            radioButton.setTag(i);
            radioButton.setText(portion.getPortionName() + "  —  "
                    + MainActivity.currencyName + " " + portion.getPortionPrice());
            radioButton.setTextColor(activity.getResources().getColor(R.color.black));
            radioButton.setTextSize(15);
            radioButton.setPadding(24, 20, 24, 20);
            radioButton.setTypeface(activity.getResources().getFont(R.font.poppinsregular));
            portionRadioGroup.addView(radioButton, new RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.MATCH_PARENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT));
            if (i == 0) {
                radioButton.setChecked(true);
            }
        }

        quantityMinus.setOnClickListener(v -> {
            if (quantity[0] > 1) {
                quantity[0]--;
                productQuantity.setText(String.valueOf(quantity[0]));
            }
        });
        quantityPlus.setOnClickListener(v -> {
            quantity[0]++;
            productQuantity.setText(String.valueOf(quantity[0]));
        });

        dismissPortion.setOnClickListener(v -> dialog.dismiss());
        addPortionToCart.setOnClickListener(v -> {
            int checkedId = portionRadioGroup.getCheckedRadioButtonId();
            if (checkedId == -1) {
                Toast.makeText(activity, getString(R.string.toast_please_select_portion), Toast.LENGTH_SHORT).show();
                return;
            }
            RadioButton selected = dialog.findViewById(checkedId);
            if (selected == null || !(selected.getTag() instanceof Integer)) {
                Toast.makeText(activity, getString(R.string.toast_please_select_portion), Toast.LENGTH_SHORT).show();
                return;
            }
            int index = (Integer) selected.getTag();
            if (index < 0 || index >= portions.size()) {
                Toast.makeText(activity, getString(R.string.toast_please_select_portion), Toast.LENGTH_SHORT).show();
                return;
            }
            addSelectedPortionToCart(productResponse, portions.get(index), quantity[0]);
            dialog.dismiss();
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }

    private void addSelectedPortionToCart(ProductResponse productResponse, ProductPortionResponse portion, int quantity) {
        String portionId = portion != null ? portion.getPortionId() : null;
        productCartResponseList = posBillingWalaDatabase.getCartProductDetails(
                productResponse.getProductId(), portionId, tableNumber, cartOrderStatus);
        String linePrice = resolveLinePrice(productResponse, portion);
        if (!productCartResponseList.isEmpty()) {
            int currentQty = parseCartQuantity(productCartResponseList.get(0).getProductQuantity());
            updateCart(productCartResponseList.get(0).getCartId(),
                    String.valueOf(currentQty + quantity), linePrice);
        } else {
            addToCart(productResponse, linePrice, String.valueOf(quantity), portion);
        }
    }

    private int parseCartQuantity(String value) {
        try {
            return Math.max(0, (int) Float.parseFloat(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private void handleProductSelection(ProductResponse productResponse, ProductPortionResponse portion) {
        String portionId = portion != null ? portion.getPortionId() : null;
        productCartResponseList = posBillingWalaDatabase.getCartProductDetails(
                productResponse.getProductId(), portionId, tableNumber, cartOrderStatus);
        if (!printerSettingResponseList.isEmpty()) {
            if (printerSettingResponseList.get(0).getProductQuantityUpdate() != null &&
                    printerSettingResponseList.get(0).getProductQuantityUpdate().equalsIgnoreCase("on")) {
                setUpdateQuantity(productResponse, portion);
            } else {
                updateCartDetails(productResponse, portion);
            }
        } else {
            updateCartDetails(productResponse, portion);
        }
        if (!binding.productSearch.getText().toString().isEmpty()) {
            getHomeAllProductList();
        } else {
            getHomeProductList();
        }
    }

    public void updateCartDetails(ProductResponse productResponse, ProductPortionResponse portion) {
        String linePrice = resolveLinePrice(productResponse, portion);
        if (!productCartResponseList.isEmpty()) {
            int quantity = Integer.parseInt(productCartResponseList.get(0).getProductQuantity());
            int totalQuantity = quantity + 1;
            updateCart(productCartResponseList.get(0).getCartId(), String.valueOf(totalQuantity), linePrice);
        } else {
            addToCart(productResponse, linePrice, "1", portion);
        }
    }

    private String resolveLinePrice(ProductResponse productResponse, ProductPortionResponse portion) {
        if (portion != null) {
            return portion.getPortionPrice();
        }
        return productResponse.getProductPrice();
    }

    public void updateCart(String cartId, String productQuantity, String productPrice) {
        posBillingWalaDatabase.updateCart(cartId, productQuantity, productPrice);
        Toast.makeText(activity, getString(R.string.toast_product_updated_into_cart), Toast.LENGTH_SHORT).show();
        getCartCount();
        if (!binding.productSearch.getText().toString().isEmpty()) {
            searchHomeProduct(binding.productSearch.getText().toString());
        } else {
            getHomeProductList();
        }
    }

    public void addToCart(ProductResponse productResponse, String productChangePrice, String productQuantity) {
        addToCart(productResponse, productChangePrice, productQuantity, null);
    }

    public void addToCart(ProductResponse productResponse, String productChangePrice, String
            productQuantity, ProductPortionResponse portion) {

        String portionId = portion != null ? portion.getPortionId() : null;
        String portionName = portion != null ? portion.getPortionName() : null;
        posBillingWalaDatabase.addToCart(MainActivity.userId, productResponse, productChangePrice, productQuantity, tableNumber, "0", cartOrderStatus, portionId, portionName);
        Toast.makeText(activity, getString(R.string.toast_product_added_into_cart), Toast.LENGTH_SHORT).show();
        getCartCount();
        if (!binding.productSearch.getText().toString().isEmpty()) {
            searchHomeProduct(binding.productSearch.getText().toString());
        } else {
            getHomeProductList();
        }
    }

    public void getHomeProductList() {
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(HomeProductsWorker.class).build();
        WorkManager.getInstance(activity).getWorkInfoByIdLiveData(workRequest.getId())
                .observe((LifecycleOwner) activity, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        homeProductAdapter = new HomeProductAdapter(activity, productResponseList, CreatePos.this);
                        binding.productRecyclerView.setAdapter(homeProductAdapter);
                        binding.categoryRecyclerView.setVisibility(View.VISIBLE);
                        binding.productLinearLayout.setVisibility(View.VISIBLE);
                    }
                });

        // Enqueue the work request
        WorkManager.getInstance(activity).enqueue(workRequest);

    }

    public void getHomeAllProductList() {

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(HomeAllProductsWorker.class).build();
        WorkManager.getInstance(activity).getWorkInfoByIdLiveData(workRequest.getId())
                .observe((LifecycleOwner) activity, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        if (!binding.productSearch.getText().toString().isEmpty()) {
                            searchHomeProduct(binding.productSearch.getText().toString());
                        }
                    }
                });

        // Enqueue the work request
        WorkManager.getInstance(activity).enqueue(workRequest);

    }

    @Override
    public void onDestroyView() {
        if (pendingProductSearch != null) {
            productSearchHandler.removeCallbacks(pendingProductSearch);
        }
        super.onDestroyView();
    }

}