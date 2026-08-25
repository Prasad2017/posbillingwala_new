package com.pos_billingwala.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.pos_billingwala.Activity.BluetoothPrint;
import com.pos_billingwala.Activity.DuplicateBluetoothPrint;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.HomeCategoryAdapter;
import com.pos_billingwala.Adapter.HomeComboAdapter;
import com.pos_billingwala.Adapter.HomeProductAdapter;
import com.pos_billingwala.Model.ComboResponse;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.AppExecutors;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Interface.ClickListerInterface;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Model.ProductCartResponse;
import com.pos_billingwala.Model.ProductCategoryResponse;
import com.pos_billingwala.Model.ProductPortionResponse;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.Model.ProductSubcategoryResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentCreatePosBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@SuppressLint({"Range", "SetTextI18n, NonConstantResourceId"})
public class CreatePos extends Fragment implements ClickListerInterface, View.OnClickListener {

    public static final String CATEGORY_ALL_ID = "ALL";

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
    public static HomeComboAdapter homeComboAdapter;
    public static FragmentCreatePosBinding binding;
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    PopupWindow mypopupWindow;
    private final Handler productSearchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingProductSearch;
    private boolean showingCombos = false;
    private List<ComboResponse> comboResponseList = new ArrayList<>();
    private int searchRequestId = 0;
    private int catalogRequestId = 0;

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
                    navigateFromPos();
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
                    selectAllCategory();
                    return;
                }
                // Debounce to avoid a DB hit on every keystroke
                pendingProductSearch = () -> searchHomeProduct(query);
                productSearchHandler.postDelayed(pendingProductSearch, 250);
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
        binding.clearCart.setOnClickListener(this);
        binding.menuIcon.setOnClickListener(this);
        binding.productsTab.setOnClickListener(this);
        binding.combosTab.setOnClickListener(this);

        binding.productRecyclerView.setHasFixedSize(true);
        binding.productRecyclerView.setItemViewCacheSize(24);
        binding.categoryRecyclerView.setHasFixedSize(true);
        binding.categoryRecyclerView.setItemViewCacheSize(12);

        return view;

    }

    private void setupSubcategoryFilter(String categoryId) {
        binding.subcategoryChipGroup.removeAllViews();
        binding.subcategoryChipGroup.setOnCheckedStateChangeListener(null);
        binding.subcategoryScrollView.setVisibility(View.GONE);

        if (categoryId == null || CATEGORY_ALL_ID.equals(categoryId)) {
            selectedSubcategoryId = null;
            return;
        }

        final String catId = categoryId;
        AppExecutors.get().db().execute(() -> {
            List<ProductSubcategoryResponse> subcategories =
                    posBillingWalaDatabase.getProductSubcategoryList(catId);
            AppExecutors.get().main(() -> {
                if (!isAdded() || binding == null) {
                    return;
                }
                binding.subcategoryChipGroup.removeAllViews();
                binding.subcategoryChipGroup.setOnCheckedStateChangeListener(null);
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
            });
        });
    }

    private void clearSubcategoryFilter() {
        selectedSubcategoryId = null;
        if (binding != null) {
            binding.subcategoryChipGroup.removeAllViews();
            binding.subcategoryChipGroup.setOnCheckedStateChangeListener(null);
            binding.subcategoryScrollView.setVisibility(View.GONE);
        }
    }

    private void selectAllCategory() {
        selectedCategoryId = CATEGORY_ALL_ID;
        categoryName = null;
        clearSubcategoryFilter();
        if (homeCategoryAdapter != null) {
            homeCategoryAdapter.setSelectedCategoryId(CATEGORY_ALL_ID);
        }
        if (binding != null) {
            binding.categoryRecyclerView.setVisibility(View.VISIBLE);
        }
        getHomeProductList();
    }

    private boolean isAllCategory(String categoryId) {
        return categoryId == null || CATEGORY_ALL_ID.equals(categoryId);
    }

    private String resolveCategoryName(String categoryId) {
        if (isAllCategory(categoryId)) {
            return null;
        }
        if (productCategoryResponseList == null) {
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
            if (showingCombos) {
                showComboCatalog();
            } else {
                selectAllCategory();
            }
            return;
        }

        final String query = productName.trim();
        final int requestId = ++searchRequestId;
        final boolean combos = showingCombos;
        final String table = tableNumber;
        final String orderStatus = cartOrderStatus;

        AppExecutors.get().db().execute(() -> {
            if (combos) {
                List<ComboResponse> results = posBillingWalaDatabase.searchCombos(query, table, orderStatus);
                AppExecutors.get().main(() -> {
                    if (!isAdded() || binding == null || requestId != searchRequestId || !showingCombos) {
                        return;
                    }
                    comboResponseList = results;
                    bindComboAdapter(comboResponseList);
                    binding.categoryRecyclerView.setVisibility(View.GONE);
                    binding.productLinearLayout.setVisibility(View.VISIBLE);
                });
                return;
            }

            List<ProductResponse> results = posBillingWalaDatabase.searchProducts(query, table, orderStatus);
            AppExecutors.get().main(() -> {
                if (!isAdded() || binding == null || requestId != searchRequestId || showingCombos) {
                    return;
                }
                searchHomeProductResponseList.clear();
                searchHomeProductResponseList.addAll(results);
                homeProductResponseList.clear();
                homeProductResponseList.addAll(results);
                bindProductAdapter(searchHomeProductResponseList);
                binding.categoryRecyclerView.setVisibility(View.GONE);
                binding.productLinearLayout.setVisibility(View.VISIBLE);
            });
        });
    }

    private void bindProductAdapter(List<ProductResponse> products) {
        if (homeProductAdapter == null || binding.productRecyclerView.getAdapter() != homeProductAdapter) {
            homeProductAdapter = new HomeProductAdapter(activity, products, CreatePos.this);
            binding.productRecyclerView.setAdapter(homeProductAdapter);
        } else {
            homeProductAdapter.submitList(products);
        }
    }

    private void bindComboAdapter(List<ComboResponse> combos) {
        homeComboAdapter = new HomeComboAdapter(activity, combos, this::comboClicked);
        binding.productRecyclerView.setAdapter(homeComboAdapter);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.homeCardView) {
            navigateFromPos();
        } else if (id == R.id.backToCategory) {
            binding.productSearch.setText("");
            binding.productSearch.clearFocus();
            selectAllCategory();
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
        } else if (id == R.id.clearCart) {
            confirmClearCart();
        } else if (id == R.id.menuIcon) {
            setPopUpWindow();
        } else if (id == R.id.productsTab) {
            showProductCatalog();
        } else if (id == R.id.combosTab) {
            showComboCatalog();
        }
    }

    private void confirmClearCart() {
        if (productCartResponseList == null || productCartResponseList.isEmpty()) {
            Toast.makeText(activity, getString(R.string.toast_cart_is_empty), Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(activity, R.style.ThemeDialog)
                .setTitle(getString(R.string.ui_clear_cart_confirm_title))
                .setMessage(getString(R.string.ui_clear_cart_confirm_message))
                .setCancelable(true)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        clearCart();
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .show();
    }

    private void clearCart() {
        final String table = tableNumber;
        final String orderStatus = cartOrderStatus;
        AppExecutors.get().runDbThenMain(this, () -> {
            posBillingWalaDatabase.clearCart(table, orderStatus);
            productCartResponseList = new ArrayList<>();
        }, () -> {
            bindCartCountUi();
            refreshCatalogAfterCart();
            Toast.makeText(activity, getString(R.string.toast_cart_cleared), Toast.LENGTH_SHORT).show();
        });
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
        AppExecutors.get().runDbThenMain(this, () -> {
            printerSettingResponseList = posBillingWalaDatabase.getPrinterSettingDetails();
        }, () -> {
            // cached for print flow; no UI bind required here
        });
    }

    public void getCompanyDetails() {
        AppExecutors.get().runDbThenMain(this, () -> {
            companyResponseList = posBillingWalaDatabase.getCompanyDetails();
        }, () -> {
            if (companyResponseList != null && !companyResponseList.isEmpty()) {
                getHomeProductCategoryList();
                getCartCount();
            } else {
                Toast.makeText(activity, getString(R.string.toast_please_fill_shop_details), Toast.LENGTH_SHORT).show();
                ((MainActivity) activity).loadFragment(new CompanyDetailSetting(), true);
            }
        });
    }


    public void getCartCount() {
        final String table = tableNumber;
        final String orderStatus = cartOrderStatus;
        AppExecutors.get().runDbThenMain(this, () -> {
            productCartResponseList = posBillingWalaDatabase.getCartProductList(table, orderStatus);
        }, this::bindCartCountUi);
    }

    private void bindCartCountUi() {
        if (binding == null) {
            return;
        }
        String discountType = "";
        float totalPerProductAmount = 0f, discountAmount = 0f, totalCGST = 0f, totalSGST = 0f, totalPerProductGST = 0f, totalGST = 0f;
        if (productCartResponseList == null || productCartResponseList.isEmpty()) {
            binding.totalItems.setText(getString(R.string.ui_total_items_0));
            binding.totalAmount.setText("");
            binding.clearCart.setVisibility(View.GONE);
            return;
        }
        binding.clearCart.setVisibility(View.VISIBLE);

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
            } else if (discountAmount != 0f) {
                discountAmount = subTotalAmt / (100 / discountAmount);
            }
        } else if (discountAmount != 0f) {
            discountAmount = subTotalAmt / (100 / discountAmount);
        }

        float shopCGST = 0f, shopSGST = 0f;
        if (!companyResponseList.isEmpty() && companyResponseList.get(0).getShopCGST() != null) {
            shopCGST = subTotalAmt * (Float.parseFloat(companyResponseList.get(0).getShopCGST().trim()) / 100);
        }

        if (!companyResponseList.isEmpty() && companyResponseList.get(0).getShopSGST() != null) {
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

    public void getHomeProductCategoryList() {
        final int requestId = ++catalogRequestId;
        showCatalogLoader();
        AppExecutors.get().runDbThenMain(this, () -> {
            productCategoryResponseList = posBillingWalaDatabase.getProductCategoryList();
        }, () -> {
            hideCatalogLoader();
            if (requestId != catalogRequestId || binding == null) {
                return;
            }
            List<ProductCategoryResponse> displayCategories = new ArrayList<>();
            ProductCategoryResponse allCategory = new ProductCategoryResponse();
            allCategory.setCategoryId(CATEGORY_ALL_ID);
            allCategory.setCategoryName(getString(R.string.ui_all));
            displayCategories.add(allCategory);
            if (productCategoryResponseList != null) {
                displayCategories.addAll(productCategoryResponseList);
            }
            if (selectedCategoryId == null) {
                selectedCategoryId = CATEGORY_ALL_ID;
                categoryName = null;
            }
            homeCategoryAdapter = new HomeCategoryAdapter(activity, displayCategories, CreatePos.this);
            homeCategoryAdapter.setSelectedCategoryId(selectedCategoryId);
            binding.categoryRecyclerView.setLayoutManager(
                    new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
            binding.categoryRecyclerView.setAdapter(CreatePos.homeCategoryAdapter);
            binding.categoryRecyclerView.setVisibility(View.VISIBLE);
            if (isAllCategory(selectedCategoryId)) {
                clearSubcategoryFilter();
                getHomeProductList();
            } else if (categoryName != null && !categoryName.isEmpty()) {
                setupSubcategoryFilter(selectedCategoryId);
                getHomeProductList();
            }
        });
    }

    @Override
    public void categoryClicked(String categoryId) {
        if (isAllCategory(categoryId)) {
            selectAllCategory();
            return;
        }
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
        final String productId = productResponse.getProductId();
        AppExecutors.get().db().execute(() -> {
            boolean hasPortions = posBillingWalaDatabase.hasProductPortions(productId);
            List<ProductPortionResponse> portions = hasPortions
                    ? posBillingWalaDatabase.getProductPortionList(productId)
                    : null;
            AppExecutors.get().main(() -> {
                if (!isAdded()) {
                    return;
                }
                if (hasPortions && portions != null && !portions.isEmpty()) {
                    showPortionDialog(productResponse, portions);
                } else {
                    handleProductSelection(productResponse, null);
                }
            });
        });
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
        final String portionId = portion != null ? portion.getPortionId() : null;
        final String linePrice = resolveLinePrice(productResponse, portion);
        final String table = tableNumber;
        final String orderStatus = cartOrderStatus;
        AppExecutors.get().runDbThenMain(this, () -> {
            List<ProductCartResponse> existing = posBillingWalaDatabase.getCartProductDetails(
                    productResponse.getProductId(), portionId, table, orderStatus);
            productCartResponseList = existing;
            if (existing != null && !existing.isEmpty()) {
                int currentQty = parseCartQuantity(existing.get(0).getProductQuantity());
                posBillingWalaDatabase.updateCart(existing.get(0).getCartId(),
                        String.valueOf(currentQty + quantity), linePrice);
            } else {
                String portionName = portion != null ? portion.getPortionName() : null;
                posBillingWalaDatabase.addToCart(MainActivity.userId, productResponse, linePrice,
                        String.valueOf(quantity), table, "0", orderStatus, portionId, portionName);
            }
        }, () -> {
            getCartCount();
            refreshCatalogAfterCart();
        });
    }

    private int parseCartQuantity(String value) {
        try {
            return Math.max(0, (int) Float.parseFloat(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private void handleProductSelection(ProductResponse productResponse, ProductPortionResponse portion) {
        final String portionId = portion != null ? portion.getPortionId() : null;
        final String table = tableNumber;
        final String orderStatus = cartOrderStatus;
        AppExecutors.get().runDbThenMain(this, () -> {
            productCartResponseList = posBillingWalaDatabase.getCartProductDetails(
                    productResponse.getProductId(), portionId, table, orderStatus);
        }, () -> {
            boolean openedQuantityDialog = false;
            if (!printerSettingResponseList.isEmpty()) {
                if (printerSettingResponseList.get(0).getProductQuantityUpdate() != null &&
                        printerSettingResponseList.get(0).getProductQuantityUpdate().equalsIgnoreCase("on")) {
                    setUpdateQuantity(productResponse, portion);
                    openedQuantityDialog = true;
                }
            }
            if (!openedQuantityDialog) {
                // updateCart / addToCart already refresh cart + catalog (same as before)
                updateCartDetails(productResponse, portion);
            } else if (binding != null) {
                // Same as original: refresh list while quantity dialog is open
                if (!binding.productSearch.getText().toString().isEmpty()) {
                    searchHomeProduct(binding.productSearch.getText().toString());
                } else {
                    refreshCatalogAfterCart();
                }
            }
        });
    }

    public void updateCartDetails(ProductResponse productResponse, ProductPortionResponse portion) {
        String linePrice = resolveLinePrice(productResponse, portion);
        if (productCartResponseList != null && !productCartResponseList.isEmpty()) {
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
        AppExecutors.get().runDbThenMain(this, () -> {
            posBillingWalaDatabase.updateCart(cartId, productQuantity, productPrice);
        }, () -> {
            getCartCount();
            refreshCatalogAfterCart();
        });
    }

    public void addToCart(ProductResponse productResponse, String productChangePrice, String productQuantity) {
        addToCart(productResponse, productChangePrice, productQuantity, null);
    }

    public void addToCart(ProductResponse productResponse, String productChangePrice, String
            productQuantity, ProductPortionResponse portion) {

        final String portionId = portion != null ? portion.getPortionId() : null;
        final String portionName = portion != null ? portion.getPortionName() : null;
        final String table = tableNumber;
        final String orderStatus = cartOrderStatus;
        AppExecutors.get().runDbThenMain(this, () -> {
            posBillingWalaDatabase.addToCart(MainActivity.userId, productResponse, productChangePrice,
                    productQuantity, table, "0", orderStatus, portionId, portionName);
        }, () -> {
            getCartCount();
            refreshCatalogAfterCart();
        });
    }

    private void refreshCatalogAfterCart() {
        if (!binding.productSearch.getText().toString().isEmpty()) {
            searchHomeProduct(binding.productSearch.getText().toString());
        } else if (showingCombos) {
            showComboCatalog();
        } else {
            getHomeProductList();
        }
    }

    private void showProductCatalog() {
        showingCombos = false;
        binding.productsTab.setBackgroundResource(R.drawable.fill_button_rounded_border);
        binding.productsTab.setTextColor(activity.getResources().getColor(R.color.white));
        binding.combosTab.setBackgroundResource(R.drawable.button_rounded_border);
        binding.combosTab.setTextColor(activity.getResources().getColor(R.color.black));
        binding.categoryRecyclerView.setVisibility(View.VISIBLE);
        if (!binding.productSearch.getText().toString().isEmpty()) {
            searchHomeProduct(binding.productSearch.getText().toString());
        } else {
            getHomeProductList();
        }
    }

    private void showComboCatalog() {
        showingCombos = true;
        binding.combosTab.setBackgroundResource(R.drawable.fill_button_rounded_border);
        binding.combosTab.setTextColor(activity.getResources().getColor(R.color.white));
        binding.productsTab.setBackgroundResource(R.drawable.button_rounded_border);
        binding.productsTab.setTextColor(activity.getResources().getColor(R.color.black));
        binding.categoryRecyclerView.setVisibility(View.GONE);
        binding.subcategoryScrollView.setVisibility(View.GONE);
        final String table = tableNumber;
        final String orderStatus = cartOrderStatus;
        final int requestId = ++catalogRequestId;
        showCatalogLoader();
        AppExecutors.get().runDbThenMain(this, () -> {
            comboResponseList = posBillingWalaDatabase.getPosComboList(table, orderStatus);
        }, () -> {
            hideCatalogLoader();
            if (requestId != catalogRequestId || binding == null || !showingCombos) {
                return;
            }
            bindComboAdapter(comboResponseList);
            binding.productLinearLayout.setVisibility(View.VISIBLE);
        });
    }

    public void comboClicked(ComboResponse combo) {
        if (combo == null) {
            return;
        }
        final String table = tableNumber;
        final String orderStatus = cartOrderStatus;
        AppExecutors.get().db().execute(() -> {
            List<ProductCartResponse> existing = posBillingWalaDatabase.getCartComboDetails(
                    combo.getComboId(), table, orderStatus);
            final boolean hasExisting = existing != null && !existing.isEmpty();
            final String existingCartId = hasExisting ? existing.get(0).getCartId() : null;
            final int currentQty = hasExisting ? parseCartQuantity(existing.get(0).getProductQuantity()) : 0;
            if (hasExisting) {
                posBillingWalaDatabase.updateCart(existingCartId,
                        String.valueOf(currentQty + 1), combo.getComboPrice());
            } else {
                posBillingWalaDatabase.addComboToCart(MainActivity.userId, combo, "1",
                        table, "0", orderStatus);
            }
            AppExecutors.get().main(() -> {
                if (!isAdded()) {
                    return;
                }
                getCartCount();
                refreshCatalogAfterCart();
            });
        });
    }

    public void getHomeProductList() {
        if (showingCombos) {
            showComboCatalog();
            return;
        }
        final String catName = categoryName;
        final String table = tableNumber;
        final String orderStatus = cartOrderStatus;
        final String subId = selectedSubcategoryId;
        final int requestId = ++catalogRequestId;
        showCatalogLoader();
        AppExecutors.get().runDbThenMain(this, () -> {
            productResponseList = posBillingWalaDatabase.getHomeProductList(catName, table, orderStatus, subId);
        }, () -> {
            hideCatalogLoader();
            if (requestId != catalogRequestId || binding == null || showingCombos) {
                return;
            }
            bindProductAdapter(productResponseList);
            binding.categoryRecyclerView.setVisibility(View.VISIBLE);
            binding.productLinearLayout.setVisibility(View.VISIBLE);
        });
    }

    private void showCatalogLoader() {
        if (binding != null) {
            ListLoader.setVisible(binding.catalogProgressBar, true);
        }
    }

    private void hideCatalogLoader() {
        if (binding != null) {
            ListLoader.setVisible(binding.catalogProgressBar, false);
        }
    }

    public void getHomeAllProductList() {
        if (binding != null && !binding.productSearch.getText().toString().isEmpty()) {
            searchHomeProduct(binding.productSearch.getText().toString());
        }
    }

    @Override
    public void onDestroyView() {
        if (pendingProductSearch != null) {
            productSearchHandler.removeCallbacks(pendingProductSearch);
        }
        searchRequestId++;
        catalogRequestId++;
        hideCatalogLoader();
        super.onDestroyView();
    }

    private void navigateFromPos() {
        // Same destinations as before (table / take-away / home)
        if (cartOrderStatus != null && cartOrderStatus.equalsIgnoreCase("table_wise")) {
            ((MainActivity) activity).goBackTo(new InvoiceCompanyTable(), true);
        } else if (cartOrderStatus != null && cartOrderStatus.equalsIgnoreCase("take_away")) {
            ((MainActivity) activity).goBackTo(new InvoiceTakeAway(), true);
        } else {
            ((MainActivity) activity).navigateToHome();
        }
    }

}