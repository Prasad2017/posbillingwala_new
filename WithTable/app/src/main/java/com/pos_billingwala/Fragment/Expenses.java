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
import com.pos_billingwala.Adapter.ExpenseAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Model.ExpenseResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentExpensesBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cn.pedant.SweetAlert.SweetAlertDialog;


public class Expenses extends Fragment implements View.OnClickListener {

    public static Activity activity;
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    List<ExpenseResponse> expenseResponseList = new ArrayList<>();
    ExpenseAdapter adapter;
    FragmentExpensesBinding binding;


    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentExpensesBinding.inflate(inflater, container, false);
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

        binding.backToSetting.setOnClickListener(this);
        binding.addExpense.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToSetting) {
            ((MainActivity) activity).navigateBack();
        } else if (id == R.id.addExpense) {
            ((MainActivity) activity).loadFragment(new AddExpenses(), true);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        getExpenseList();
    }

    @SuppressLint("SetTextI18n")
    public void getExpenseList() {
        SweetAlertDialog loader = ListLoader.show(activity);
        try {
            expenseResponseList.clear();
            expenseResponseList = posBillingWalaDatabase.getExpenseList();
            if (!expenseResponseList.isEmpty()) {
                adapter = new ExpenseAdapter(activity, expenseResponseList);
                binding.recyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
                binding.recyclerView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                // adapter.notifyItemInserted(expenseResponseList.size() - 1);

                float totalExpenseAmount = 0f;
                for (ExpenseResponse expenseResponse : expenseResponseList) {
                    totalExpenseAmount += Float.parseFloat(expenseResponse.getExpenseAmount());
                }
                binding.totalAmount.setText(activity.getString(R.string.inr) + " " + String.format(Locale.US, "%.2f", totalExpenseAmount));

                binding.noDataFound.setVisibility(View.GONE);
                binding.nestedScrollView.setVisibility(View.VISIBLE);

            } else {
                binding.noDataFound.setVisibility(View.VISIBLE);
                binding.nestedScrollView.setVisibility(View.GONE);
            }
        } finally {
            ListLoader.dismiss(loader);
        }
    }
}