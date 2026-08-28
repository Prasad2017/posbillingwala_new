package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.ReportAdapter;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Extra.LocalSalesAnalytics;
import com.pos_billingwala.Extra.ReportUiHelper;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.Model.LocalSalesSnapshot;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentSalesListBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class SalesList extends Fragment {

    private Activity activity;
    private FragmentSalesListBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<InvoiceResponse> invoiceResponseList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSalesListBinding.inflate(inflater, container, false);
        activity = getActivity();

        binding.toolbar.toolbarTitle.setText(getString(R.string.ui_sales_list));
        binding.toolbar.backButton.setOnClickListener(v -> ((MainActivity) activity).navigateBack());

        View root = binding.getRoot();
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        root.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                ((MainActivity) activity).navigateBack();
                return true;
            }
            return false;
        });
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        loadSales();
    }

    private void loadSales() {
        SweetAlertDialog loader = ListLoader.show(activity);
        executor.execute(() -> {
            LocalSalesSnapshot snapshot = new LocalSalesAnalytics(activity).loadRecentSalesList();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(() -> {
                ListLoader.dismiss(loader);
                if (!isAdded() || binding == null) {
                    return;
                }
                invoiceResponseList = snapshot.getRecentInvoices() != null
                        ? snapshot.getRecentInvoices() : new ArrayList<>();
                String currency = MainActivity.currencyName;
                binding.salesSummary.setText(getString(R.string.ui_total_bills) + ": " + snapshot.getBillCount()
                        + "  ·  " + getString(R.string.ui_net_sales) + ": "
                        + ReportUiHelper.money(currency, snapshot.getNetSales()));
                binding.emptySales.setVisibility(invoiceResponseList.isEmpty() ? View.VISIBLE : View.GONE);
                binding.recyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
                binding.recyclerView.setAdapter(new ReportAdapter(activity, invoiceResponseList));
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
