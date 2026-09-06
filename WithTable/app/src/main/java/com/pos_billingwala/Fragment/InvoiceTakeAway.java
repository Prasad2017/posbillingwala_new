package com.pos_billingwala.Fragment;

import com.pos_billingwala.Extra.PopupUi;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

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
        view = binding.getRoot();

        activity = getActivity();

        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                Log.i("tag", "onKey Back listener is working!!!");
                ((MainActivity) activity).navigateBack();
                return true;
            }
            return false;
        });

        binding.homeCardView.setOnClickListener(this);
        binding.menuIcon.setOnClickListener(this);
        if (binding.toolbarSubtitle != null) {
            binding.toolbarSubtitle.setText(R.string.ui_parcel_counter_hint);
            binding.toolbarSubtitle.setVisibility(View.VISIBLE);
        }
        if (binding.btnNewParcel != null) {
            binding.btnNewParcel.setOnClickListener(v -> openNewParcel());
        }

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
        mypopupWindow = PopupUi.create(activity, view);

        LinearLayout saveInvoiceLayout = view.findViewById(R.id.saveInvoiceLayout);
        TextView saveInvoiceTxt = view.findViewById(R.id.saveInvoice);
        LinearLayout duplicateInvoicePrintLayout = view.findViewById(R.id.duplicateInvoicePrintLayout);

        saveInvoiceTxt.setText(getString(R.string.ui_new_parcel));

        saveInvoiceLayout.setOnClickListener(v -> {
            mypopupWindow.dismiss();
            openNewParcel();
        });

        duplicateInvoicePrintLayout.setOnClickListener(v -> {
            mypopupWindow.dismiss();
            Intent intent = new Intent(activity, DuplicateBluetoothPrint.class);
            intent.putExtra("invoiceRunningStatus", "printBill");
            intent.putExtra("cartOrderStatus", "take_away");
            activity.startActivity(intent);
        });

        PopupUi.showAsToolbarMenu(mypopupWindow, binding.menuIcon);
    }

    private void openNewParcel() {
        String parcelNo = posBillingWalaDatabase.nextTakeAwayParcelNumber();
        CreatePos createPos = new CreatePos();
        Bundle bundle = new Bundle();
        bundle.putString("tableNumber", parcelNo);
        bundle.putString("cartOrderStatus", "take_away");
        createPos.setArguments(bundle);
        ((MainActivity) activity).loadFragment(createPos, true);
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
                boolean hasParcels = productTakeAwayResponseList != null && !productTakeAwayResponseList.isEmpty();
                if (hasParcels) {
                    invoiceTakAwayAdapter = new InvoiceTakAwayAdapter(activity, productTakeAwayResponseList);
                    binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                    binding.recyclerView.setAdapter(invoiceTakAwayAdapter);
                    invoiceTakAwayAdapter.notifyDataSetChanged();
                    binding.takeAwayOrderLayout.setVisibility(View.VISIBLE);
                    if (binding.emptyParcelState != null) {
                        binding.emptyParcelState.setVisibility(View.GONE);
                    }
                } else {
                    binding.takeAwayOrderLayout.setVisibility(View.GONE);
                    if (binding.emptyParcelState != null) {
                        binding.emptyParcelState.setVisibility(View.VISIBLE);
                    }
                }
            } finally {
                ListLoader.dismiss(activeLoader);
            }
        });
    }
}
