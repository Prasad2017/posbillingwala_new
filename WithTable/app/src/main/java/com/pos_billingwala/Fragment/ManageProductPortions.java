package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.os.Bundle;
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
import com.pos_billingwala.Adapter.PortionAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Extra.MasterListTabletUi;
import com.pos_billingwala.Model.PortionMasterResponse;
import com.pos_billingwala.Model.ProductPortionResponse;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentManageProductPortionsBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class ManageProductPortions extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public static POSBillingWalaDatabase posBillingWalaDatabase;
    public static List<ProductPortionResponse> portionResponseList = new ArrayList<>();
    public static PortionAdapter portionAdapter;
    public static RecyclerView portionRecyclerview;
    public static CardView portionListCardView;
    public static TextView noDataFound;
    public static String selectedProductId;

    View view;
    FragmentManageProductPortionsBinding binding;
    String productId;
    ProductResponse productResponse;
    List<PortionMasterResponse> portionMasterList = new ArrayList<>();
    String[] portionMasterIdList;
    String[] portionMasterNameList;
    String selectedPortionMasterId;

    public static void getPortionList() {
        SweetAlertDialog loader = ListLoader.show(activity);
        try {
            portionResponseList.clear();
            if (selectedProductId != null) {
                portionResponseList = posBillingWalaDatabase.getProductPortionList(selectedProductId);
            }
            if (!portionResponseList.isEmpty()) {
                portionAdapter = new PortionAdapter(activity, selectedProductId, portionResponseList);
                portionRecyclerview.setLayoutManager(new GridLayoutManager(activity,
                        MasterListTabletUi.listColumnCount(activity)));
                portionRecyclerview.setAdapter(portionAdapter);

                portionListCardView.setVisibility(View.VISIBLE);
                noDataFound.setVisibility(View.GONE);
            } else {
                portionListCardView.setVisibility(View.GONE);
                noDataFound.setVisibility(View.VISIBLE);
            }
        } finally {
            ListLoader.dismiss(loader);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentManageProductPortionsBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();
        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        if (getArguments() != null) {
            productId = getArguments().getString("productId");
        }
        selectedProductId = productId;

        initViews();
        loadProductInfo();
        setupPortionMasterSpinner();
        suggestNextSortOrder();

        MasterListTabletUi.applyFormListSplitBelowHeader(activity, binding.portionPageContainer,
                binding.productInfoCard, binding.portionFormCard, binding.portionListCardView);

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                Log.i("tag", "onKey Back listener is working!!!");
                navigateBack();
                return true;
            }
            return false;
        });

        return view;
    }

    private void initViews() {
        portionRecyclerview = view.findViewById(R.id.portionRecyclerview);
        portionListCardView = view.findViewById(R.id.portionListCardView);
        noDataFound = view.findViewById(R.id.noDataFound);

        binding.backToProductMaster.setOnClickListener(this);
        binding.addPortion.setOnClickListener(this);
        binding.managePortionMaster.setOnClickListener(this);
    }

    private void navigateBack() {
        ((MainActivity) activity).navigateBack();
    }

    private void loadProductInfo() {
        if (productId == null) {
            binding.productInfo.setText("Product not found");
            binding.addPortion.setEnabled(false);
            return;
        }
        List<ProductResponse> details = posBillingWalaDatabase.getProductDetail(productId);
        if (details.isEmpty()) {
            binding.productInfo.setText("Product not found");
            binding.addPortion.setEnabled(false);
            return;
        }
        productResponse = details.get(0);
        String category = productResponse.getCategoryName() != null ? productResponse.getCategoryName() : "-";
        String header = productResponse.getProductName() + " (" + category + ")\n";
        if (posBillingWalaDatabase.hasProductPortions(productId)) {
            binding.productInfo.setText(header
                    + "Selling prices are set per portion below (base product price is unused while portions exist)");
        } else {
            binding.productInfo.setText(header
                    + "No portions — product price is used for billing. Optionally add portions.\n"
                    + "Base price: " + MainActivity.currencyName + " " + productResponse.getProductPrice());
        }
    }

    private void setupPortionMasterSpinner() {
        portionMasterList = posBillingWalaDatabase.getPortionMasterList();
        if (portionMasterList.isEmpty()) {
            binding.portionMasterSection.setVisibility(View.GONE);
            binding.addPortion.setEnabled(false);
            Toast.makeText(activity, getString(R.string.toast_please_add_portion_masters_first), Toast.LENGTH_SHORT).show();
            return;
        }

        binding.portionMasterSection.setVisibility(View.VISIBLE);
        binding.addPortion.setEnabled(true);
        portionMasterIdList = new String[portionMasterList.size()];
        portionMasterNameList = new String[portionMasterList.size()];
        for (int i = 0; i < portionMasterList.size(); i++) {
            portionMasterIdList[i] = portionMasterList.get(i).getPortionMasterId();
            portionMasterNameList[i] = portionMasterList.get(i).getPortionName();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, portionMasterNameList);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
        binding.portionMasterSpinner.setAdapter(adapter);

        selectedPortionMasterId = portionMasterIdList[0];
        binding.portionMasterSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                selectedPortionMasterId = portionMasterIdList[position];
            }
        });
    }

    private void suggestNextSortOrder() {
        int next = posBillingWalaDatabase.countActiveProductPortions(productId) + 1;
        binding.portionSortOrder.setText(String.valueOf(next));
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToProductMaster) {
            navigateBack();
        } else if (id == R.id.addPortion) {
            addProductPortion();
        } else if (id == R.id.managePortionMaster) {
            openPortionMaster();
        }
    }

    private void openPortionMaster() {
        AddPortionMaster addPortionMaster = new AddPortionMaster();
        Bundle bundle = new Bundle();
        bundle.putString("returnTo", "productPortions");
        bundle.putString("productId", productId);
        addPortionMaster.setArguments(bundle);
        ((MainActivity) activity).loadFragment(addPortionMaster, true);
    }

    private void addProductPortion() {
        if (productId == null || productId.isEmpty()) {
            Toast.makeText(activity, getString(R.string.toast_invalid_product), Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedPortionMasterId == null || selectedPortionMasterId.isEmpty()) {
            Toast.makeText(activity, getString(R.string.select_portion_master), Toast.LENGTH_SHORT).show();
            return;
        }
        String price = binding.portionPrice.getText().toString().trim();
        if (price.isEmpty()) {
            Toast.makeText(activity, getString(R.string.toast_please_add_portion_price), Toast.LENGTH_SHORT).show();
            return;
        }

        PortionMasterResponse master = posBillingWalaDatabase.getPortionMasterById(selectedPortionMasterId);
        if (master == null) {
            Toast.makeText(activity, getString(R.string.toast_invalid_portion_master), Toast.LENGTH_SHORT).show();
            return;
        }

        int sortOrder = parseSortOrder(binding.portionSortOrder.getText().toString(),
                posBillingWalaDatabase.countActiveProductPortions(productId) + 1);

        ProductPortionResponse existing = posBillingWalaDatabase.getProductPortionByMasterId(
                productId, selectedPortionMasterId);

        posBillingWalaDatabase.insertProductPortion(
                productId,
                selectedPortionMasterId,
                master.getPortionName(),
                price,
                sortOrder,
                "0",
                existing != null && existing.getPortionNetworkStatus() != null
                        ? existing.getPortionNetworkStatus() : getRandomString(10),
                0);

        Toast.makeText(activity,
                existing != null ? "Portion price updated" : "Portion added successfully",
                Toast.LENGTH_SHORT).show();
        binding.portionPrice.setText("");
        suggestNextSortOrder();
        loadProductInfo();
        getPortionList();
    }

    private int parseSortOrder(String input, int fallback) {
        try {
            if (input != null && !input.trim().isEmpty()) {
                return Integer.parseInt(input.trim());
            }
        } catch (NumberFormatException ignored) {
        }
        return fallback;
    }

    public String getRandomString(final int sizeOfRandomString) {
        String allowed = "0123456789qwertyuiopasdfghjklzxcvbnm";
        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; ++i) {
            sb.append(allowed.charAt(random.nextInt(allowed.length())));
        }
        return sb.toString();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        setupPortionMasterSpinner();
        loadProductInfo();
        getPortionList();
    }
}
