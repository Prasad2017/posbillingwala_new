package com.pos_billingwala.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.pos_billingwala.Activity.DuplicateBluetoothPrint;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.InvoiceTakAwayAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.AppExecutors;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.ProductCartResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentInvoiceTakeAwayBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class InvoiceTakeAway extends Fragment implements View.OnClickListener {

    public static Activity activity;
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    List<CompanyResponse> companyResponseList = new ArrayList<>();
    List<ProductCartResponse> productTakeAwayResponseList = new ArrayList<>();
    InvoiceTakAwayAdapter invoiceTakAwayAdapter;
    PopupWindow mypopupWindow;
    FragmentInvoiceTakeAwayBinding binding;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentInvoiceTakeAwayBinding.inflate(inflater, container, false);
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

        binding.homeCardView.setOnClickListener(this);
        binding.menuIcon.setOnClickListener(this);

        return view;

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.homeCardView) {
            ((MainActivity) activity).navigateBack();
        } else if (id == R.id.menuIcon) {
            setPopUpWindow();
        }
    }

    @SuppressLint("SetTextI18n")
    public void setPopUpWindow() {

        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.share_dialog, null);
        mypopupWindow = new PopupWindow(view, RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT, true);

        LinearLayout saveInvoiceLayout = view.findViewById(R.id.saveInvoiceLayout);
        TextView saveInvoiceTxt = view.findViewById(R.id.saveInvoice);
        LinearLayout duplicateInvoicePrintLayout = view.findViewById(R.id.duplicateInvoicePrintLayout);

        saveInvoiceTxt.setText("Add New Bill");

        saveInvoiceLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mypopupWindow.dismiss();

                CreatePos createPos = new CreatePos();
                Bundle bundle = new Bundle();
                bundle.putString("tableNumber", "TA" + getRandomString(3));
                bundle.putString("cartOrderStatus", "take_away");
                createPos.setArguments(bundle);
                ((MainActivity) activity).loadFragment(createPos, true);

            }
        });

        duplicateInvoicePrintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mypopupWindow.dismiss();

                Intent intent = new Intent(activity, DuplicateBluetoothPrint.class);
                intent.putExtra("invoiceRunningStatus", "printBill");
                intent.putExtra("cartOrderStatus", "take_away");
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

    }

    public void getCompanyDetails() {
        final cn.pedant.SweetAlert.SweetAlertDialog loader = ListLoader.show(activity);
        AppExecutors.get().runDbThenMain(this, () -> {
            companyResponseList = posBillingWalaDatabase.getCompanyDetails();
        }, () -> {
            if (companyResponseList != null && !companyResponseList.isEmpty()) {
                getTakeWayCartList(loader);
            } else {
                ListLoader.dismiss(loader);
                Toast.makeText(activity, getString(R.string.toast_please_fill_shop_details), Toast.LENGTH_SHORT).show();
                ((MainActivity) activity).loadFragment(new CompanyDetailSetting(), true);
            }
        });
    }

    public String getRandomString(final int sizeOfRandomString) {

        String ALLOWED_CHARACTERS = "0123456789";

        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }

    public void getTakeWayCartList() {
        getTakeWayCartList(null);
    }

    private void getTakeWayCartList(cn.pedant.SweetAlert.SweetAlertDialog loader) {
        final cn.pedant.SweetAlert.SweetAlertDialog activeLoader =
                loader != null ? loader : ListLoader.show(activity);
        AppExecutors.get().runDbThenMain(this, () -> {
            productTakeAwayResponseList = posBillingWalaDatabase.getTakeWayCartList("take_away");
        }, () -> {
            try {
                if (binding == null) {
                    return;
                }
                if (productTakeAwayResponseList != null && !productTakeAwayResponseList.isEmpty()) {
                    invoiceTakAwayAdapter = new InvoiceTakAwayAdapter(activity, productTakeAwayResponseList);
                    binding.recyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
                    binding.recyclerView.setAdapter(invoiceTakAwayAdapter);
                    invoiceTakAwayAdapter.notifyDataSetChanged();
                    binding.takeAwayOrderLayout.setVisibility(View.VISIBLE);
                } else {
                    binding.takeAwayOrderLayout.setVisibility(View.GONE);
                }
            } finally {
                ListLoader.dismiss(activeLoader);
            }
        });
    }


}