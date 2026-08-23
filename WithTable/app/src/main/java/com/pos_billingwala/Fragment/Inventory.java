package com.pos_billingwala.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.InventoryAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.SimpleDividerItemDecoration;
import com.pos_billingwala.Model.InventoryResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentInventoryBinding;

import java.util.ArrayList;
import java.util.List;


@SuppressLint("StaticFieldLeak")
public class Inventory extends Fragment implements View.OnClickListener {

    public static Activity activity;
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    List<InventoryResponse> inventoryResponseList = new ArrayList<>();
    InventoryAdapter adapter;
    FragmentInventoryBinding binding;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentInventoryBinding.inflate(inflater, container, false);
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
                    ((MainActivity) activity).loadFragment(new UserSetting(), true);
                    return true;
                }
                return false;
            }
        });

        binding.backToSetting.setOnClickListener(this);
        binding.addInventory.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToSetting) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new UserSetting(), true);
        } else if (id == R.id.addInventory) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new AddInventory(), true);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        getInventoryList();
    }

    public void getInventoryList() {

        inventoryResponseList.clear();
        inventoryResponseList = posBillingWalaDatabase.getInventoryList();
        if (!inventoryResponseList.isEmpty()) {

            adapter = new InventoryAdapter(activity, inventoryResponseList);
            binding.recyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
            binding.recyclerView.addItemDecoration(new SimpleDividerItemDecoration(activity));
            binding.recyclerView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
            //  adapter.notifyItemInserted(inventoryResponseList.size() - 1);

            binding.noDataFound.setVisibility(View.GONE);
            binding.linearLayout.setVisibility(View.VISIBLE);

        } else {
            binding.noDataFound.setVisibility(View.VISIBLE);
            binding.addInventory.setVisibility(View.GONE);
        }

    }
}