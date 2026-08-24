package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentMasterDataBinding;

public class MasterData extends Fragment implements View.OnClickListener {

    public static final String OPENED_FROM_MASTER = "master";

    public static Activity activity;
    public static FragmentMasterDataBinding binding;
    View view;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMasterDataBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    goToSettings();
                    return true;
                }
                return false;
            }
        });

        initViews();
        return view;
    }

    public void initViews() {
        binding.backToSetting.setOnClickListener(this);
        binding.categoryLayout.setOnClickListener(this);
        binding.subcategoryLayout.setOnClickListener(this);
        binding.portionLayout.setOnClickListener(this);
        binding.productLayout.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToSetting) {
            goToSettings();
        } else if (id == R.id.categoryLayout) {
            AddCategory addCategory = new AddCategory();
            addCategory.setArguments(openedFromMaster());
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(addCategory, true);
        } else if (id == R.id.subcategoryLayout) {
            AddSubcategory addSubcategory = new AddSubcategory();
            addSubcategory.setArguments(openedFromMaster());
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(addSubcategory, true);
        } else if (id == R.id.portionLayout) {
            AddPortionMaster addPortionMaster = new AddPortionMaster();
            Bundle bundle = openedFromMaster();
            bundle.putString("returnTo", "masterData");
            addPortionMaster.setArguments(bundle);
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(addPortionMaster, true);
        } else if (id == R.id.productLayout) {
            ProductMaster productMaster = new ProductMaster();
            productMaster.setArguments(openedFromMaster());
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(productMaster, true);
        }
    }

    private void goToSettings() {
        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
        ((MainActivity) activity).loadFragment(new UserSetting(), true);
    }

    static Bundle openedFromMaster() {
        Bundle bundle = new Bundle();
        bundle.putString("openedFrom", OPENED_FROM_MASTER);
        return bundle;
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
    }
}
