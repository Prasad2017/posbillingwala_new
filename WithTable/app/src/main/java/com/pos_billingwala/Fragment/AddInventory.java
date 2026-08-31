package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.InventoryResponse;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentAddInventoryBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;


public class AddInventory extends Fragment implements View.OnClickListener {

    public static Activity activity;
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    List<ProductResponse> productResponseList = new ArrayList<>();
    List<InventoryResponse> inventoryResponseList = new ArrayList<>();
    String[] productIdList, productNameList;
    String productId;
    FragmentAddInventoryBinding binding;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddInventoryBinding.inflate(inflater, container, false);
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
                    ((MainActivity) activity).navigateBack();
                    return true;
                }
                return false;
            }
        });

        binding.productSpinner.setOnItemSelectedListener((position, label) -> productId = productIdList[position]);

        binding.backToInventory.setOnClickListener(this);
        binding.addInventory.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToInventory) {
            ((MainActivity) activity).navigateBack();
        } else if (id == R.id.addInventory) {
            if (productId != null) {
                if (!binding.inventoryQty.toString().isEmpty()) {
                    addInventory();
                } else {
                    Toast.makeText(activity, getString(R.string.toast_please_add_inventory_qty), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(activity, getString(R.string.toast_please_select_product), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void addInventory() {

        Date c = Calendar.getInstance().getTime();
        System.out.println("Current time => " + c);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String inventoryDate = df.format(c);

        inventoryResponseList = posBillingWalaDatabase.getInventoryDetails(productId);
        if (!inventoryResponseList.isEmpty()) {

            int newInventoryQty = Integer.parseInt(binding.inventoryQty.getText().toString());
            int oldInventoryQty = Integer.parseInt(inventoryResponseList.get(0).getProductInventoryQuantity());
            int afterSaleInventoryQuantity = Integer.parseInt(inventoryResponseList.get(0).getAfterSaleInventoryQuantity());
            int totalQty = newInventoryQty + afterSaleInventoryQuantity;

            posBillingWalaDatabase.addInventory(productId, "" + totalQty, "0", "0", inventoryDate, 0, getRandomString(10));
            Toast.makeText(activity, getString(R.string.toast_update_inventory_successfully), Toast.LENGTH_SHORT).show();
        } else {
            posBillingWalaDatabase.addInventory(productId, binding.inventoryQty.getText().toString(), binding.inventoryQty.getText().toString(), "0", inventoryDate, 0, getRandomString(10));

            Toast.makeText(activity, getString(R.string.toast_add_inventory_successfully), Toast.LENGTH_SHORT).show();
        }

        ((MainActivity) activity).navigateBack();

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
        getProductList();
    }

    public void getProductList() {

        productResponseList = posBillingWalaDatabase.getAllProductList("", "");
        if (!productResponseList.isEmpty()) {

            productIdList = new String[productResponseList.size()];
            productNameList = new String[productResponseList.size()];

            for (int i = 0; i < productResponseList.size(); i++) {
                productIdList[i] = productResponseList.get(i).getProductId();
                productNameList[i] = productResponseList.get(i).getProductName();
            }

            try {
                binding.productSpinner.setItems(productNameList);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

}