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
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.PortionAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.SimpleDividerItemDecoration;
import com.pos_billingwala.Model.ProductPortionResponse;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentManageProductPortionsBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    public static void getPortionList() {
        portionResponseList.clear();
        if (selectedProductId != null) {
            portionResponseList = posBillingWalaDatabase.getProductPortionList(selectedProductId);
        }
        if (!portionResponseList.isEmpty()) {
            portionAdapter = new PortionAdapter(activity, selectedProductId, portionResponseList);
            portionRecyclerview.setLayoutManager(new GridLayoutManager(activity, 1));
            portionRecyclerview.setAdapter(portionAdapter);
            portionRecyclerview.addItemDecoration(new SimpleDividerItemDecoration(activity));

            portionListCardView.setVisibility(View.VISIBLE);
            noDataFound.setVisibility(View.GONE);
        } else {
            portionListCardView.setVisibility(View.GONE);
            noDataFound.setVisibility(View.VISIBLE);
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
        suggestNextSortOrder();

        binding.portionName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                    ((MainActivity) activity).loadFragment(new ProductMaster(), false);
                    return true;
                }
                return false;
            }
        });

        return view;
    }

    private void initViews() {
        portionRecyclerview = view.findViewById(R.id.portionRecyclerview);
        portionListCardView = view.findViewById(R.id.portionListCardView);
        noDataFound = view.findViewById(R.id.noDataFound);

        binding.backToProductMaster.setOnClickListener(this);
        binding.addPortion.setOnClickListener(this);
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
        binding.productInfo.setText(productResponse.getProductName() + " (" + category + ")\n"
                + "Base price: " + MainActivity.currencyName + " " + productResponse.getProductPrice());
    }

    private void suggestNextSortOrder() {
        int next = posBillingWalaDatabase.countActiveProductPortions(productId) + 1;
        binding.portionSortOrder.setText(String.valueOf(next));
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToProductMaster) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new ProductMaster(), false);
        } else if (id == R.id.addPortion) {
            addProductPortion();
        }
    }

    private void addProductPortion() {
        if (productId == null || productId.isEmpty()) {
            Toast.makeText(activity, "Invalid product", Toast.LENGTH_SHORT).show();
            return;
        }
        String name = binding.portionName.getText().toString().trim();
        String price = binding.portionPrice.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(activity, "Please add portion name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (price.isEmpty()) {
            Toast.makeText(activity, "Please add portion price", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ProductPortionResponse> existing = posBillingWalaDatabase.getProductPortionNameList(productId, name);
        if (!existing.isEmpty()) {
            Toast.makeText(activity, "Portion already exists for this product", Toast.LENGTH_SHORT).show();
            return;
        }

        int sortOrder = parseSortOrder(binding.portionSortOrder.getText().toString(),
                posBillingWalaDatabase.countActiveProductPortions(productId) + 1);

        posBillingWalaDatabase.insertProductPortion(
                productId,
                name,
                price,
                sortOrder,
                "0",
                getRandomString(10),
                0);

        Toast.makeText(activity, "Portion added successfully", Toast.LENGTH_SHORT).show();
        binding.portionName.setText("");
        binding.portionPrice.setText("");
        suggestNextSortOrder();
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
        getPortionList();
    }
}
