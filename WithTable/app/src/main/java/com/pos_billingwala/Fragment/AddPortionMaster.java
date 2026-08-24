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
import com.pos_billingwala.Adapter.PortionMasterAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.SimpleDividerItemDecoration;
import com.pos_billingwala.Model.PortionMasterResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentAddPortionMasterBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AddPortionMaster extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public static POSBillingWalaDatabase posBillingWalaDatabase;
    public static List<PortionMasterResponse> portionMasterResponseList = new ArrayList<>();
    public static PortionMasterAdapter portionMasterAdapter;
    public static RecyclerView portionMasterRecyclerview;
    public static CardView portionMasterListCardView;
    public static TextView noDataFound;

    View view;
    FragmentAddPortionMasterBinding binding;

    public static void getPortionMasterList() {
        portionMasterResponseList.clear();
        portionMasterResponseList = posBillingWalaDatabase.getPortionMasterList();
        if (!portionMasterResponseList.isEmpty()) {
            portionMasterAdapter = new PortionMasterAdapter(activity, portionMasterResponseList);
            portionMasterRecyclerview.setLayoutManager(new GridLayoutManager(activity, 1));
            portionMasterRecyclerview.setAdapter(portionMasterAdapter);
            portionMasterRecyclerview.addItemDecoration(new SimpleDividerItemDecoration(activity));

            portionMasterListCardView.setVisibility(View.VISIBLE);
            noDataFound.setVisibility(View.GONE);
        } else {
            portionMasterListCardView.setVisibility(View.GONE);
            noDataFound.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddPortionMasterBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();
        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        initViews();

        binding.portionMasterName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

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
        portionMasterRecyclerview = view.findViewById(R.id.portionMasterRecyclerview);
        portionMasterListCardView = view.findViewById(R.id.portionMasterListCardView);
        noDataFound = view.findViewById(R.id.noDataFound);

        binding.backToCategory.setOnClickListener(this);
        binding.addPortionMaster.setOnClickListener(this);
    }

    private void navigateBack() {
        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
        if (getArguments() == null) {
            ((MainActivity) activity).loadFragment(new AddCategory(), false);
            return;
        }
        String returnTo = getArguments().getString("returnTo");
        if ("productPortions".equals(returnTo)) {
            ManageProductPortions manageProductPortions = new ManageProductPortions();
            Bundle bundle = new Bundle();
            bundle.putString("productId", getArguments().getString("productId"));
            manageProductPortions.setArguments(bundle);
            ((MainActivity) activity).loadFragment(manageProductPortions, false);
        } else if ("addProduct".equals(returnTo)) {
            ((MainActivity) activity).loadFragment(new AddProduct(), false);
        } else if ("masterData".equals(returnTo)) {
            ((MainActivity) activity).loadFragment(new MasterData(), true);
        } else if ("updateProduct".equals(returnTo)) {
            UpdateProduct updateProduct = new UpdateProduct();
            Bundle bundle = new Bundle();
            bundle.putString("productId", getArguments().getString("productId"));
            updateProduct.setArguments(bundle);
            ((MainActivity) activity).loadFragment(updateProduct, false);
        } else {
            ((MainActivity) activity).loadFragment(new AddCategory(), false);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToCategory) {
            navigateBack();
        } else if (id == R.id.addPortionMaster) {
            addPortionMaster();
        }
    }

    private void addPortionMaster() {
        String name = binding.portionMasterName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(activity, getString(R.string.toast_please_add_portion_name), Toast.LENGTH_SHORT).show();
            return;
        }
        List<PortionMasterResponse> existing = posBillingWalaDatabase.getPortionMasterByName(name);
        if (!existing.isEmpty()) {
            Toast.makeText(activity, getString(R.string.toast_portion_name_already_exists), Toast.LENGTH_SHORT).show();
            return;
        }

        posBillingWalaDatabase.insertPortionMaster(name, "0", getRandomString(10), 0);

        Toast.makeText(activity, getString(R.string.toast_portion_added_successfully), Toast.LENGTH_SHORT).show();
        binding.portionMasterName.setText("");
        getPortionMasterList();
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
        getPortionMasterList();
    }
}
