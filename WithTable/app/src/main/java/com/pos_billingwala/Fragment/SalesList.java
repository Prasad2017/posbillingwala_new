package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.ReportAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Extra.LocalSalesAnalytics;
import com.pos_billingwala.Extra.ReportCursorHelper;
import com.pos_billingwala.Extra.ReportUiHelper;
import com.pos_billingwala.Model.InvoiceProductResponse;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.Extra.TabletUi;
import com.pos_billingwala.Model.LocalSalesSnapshot;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentSalesListBinding;
import com.pos_billingwala.databinding.SalesProductListBinding;

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
    private ReportAdapter reportAdapter;
    private SalesDetailProductAdapter detailProductAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSalesListBinding.inflate(inflater, container, false);
        activity = getActivity();

        binding.toolbar.toolbarTitle.setText(getString(R.string.ui_sales_list));
        binding.toolbar.backButton.setOnClickListener(v -> ((MainActivity) activity).navigateBack());

        if (binding.detailProductsRecyclerView != null) {
            binding.detailProductsRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
            detailProductAdapter = new SalesDetailProductAdapter();
            binding.detailProductsRecyclerView.setAdapter(detailProductAdapter);
        }

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

                reportAdapter = new ReportAdapter(activity, invoiceResponseList);
                if (hasTabletDetailPanel()) {
                    reportAdapter.setOnInvoiceClickListener(this::showInvoiceDetail);
                }
                binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                binding.recyclerView.setAdapter(reportAdapter);

                if (hasTabletDetailPanel() && !invoiceResponseList.isEmpty()) {
                    showInvoiceDetail(invoiceResponseList.get(0), 0);
                } else {
                    hideInvoiceDetail();
                }
            });
        });
    }

    private boolean hasTabletDetailPanel() {
        return binding != null && binding.salesDetailPanel != null;
    }

    private void hideInvoiceDetail() {
        if (!hasTabletDetailPanel()) {
            return;
        }
        binding.salesDetailEmpty.setVisibility(View.VISIBLE);
        binding.salesDetailContent.setVisibility(View.GONE);
    }

    private void showInvoiceDetail(InvoiceResponse invoice, int position) {
        if (!hasTabletDetailPanel() || invoice == null) {
            return;
        }
        reportAdapter.setSelectedPosition(position);
        binding.salesDetailEmpty.setVisibility(View.GONE);
        binding.salesDetailContent.setVisibility(View.VISIBLE);

        binding.detailInvoiceNumber.setText(invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "");
        binding.detailInvoiceDate.setText(ReportCursorHelper.formatInvoiceDate(invoice.getInvoiceDate()));
        String paymentMode = invoice.getPaymentMode();
        binding.detailPaymentMode.setText(getString(R.string.ui_payment_mode) + ": "
                + (paymentMode != null && !paymentMode.isEmpty() ? paymentMode : "-"));
        float amount = ReportCursorHelper.parseAmount(invoice.getTotalAmount());
        binding.detailInvoiceAmount.setText(MainActivity.currencyName + " "
                + String.format(Locale.US, "%.2f", amount));

        final String invoiceNumber = invoice.getInvoiceNumber();
        executor.execute(() -> {
            List<InvoiceProductResponse> lines = new ArrayList<>();
            if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
                lines = new POSBillingWalaDatabase(activity).getInvoiceProductList(invoiceNumber);
            }
            if (activity == null) {
                return;
            }
            List<InvoiceProductResponse> finalLines = lines != null ? lines : new ArrayList<>();
            activity.runOnUiThread(() -> {
                if (!isAdded() || detailProductAdapter == null) {
                    return;
                }
                detailProductAdapter.setItems(finalLines);
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        reportAdapter = null;
        detailProductAdapter = null;
    }

    private static final class SalesDetailProductAdapter extends RecyclerView.Adapter<SalesDetailProductAdapter.Holder> {

        private final List<InvoiceProductResponse> items = new ArrayList<>();

        void setItems(List<InvoiceProductResponse> lines) {
            items.clear();
            if (lines != null) {
                items.addAll(lines);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(SalesProductListBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            InvoiceProductResponse line = items.get(position);
            holder.binding.productName.setText(line.getProductName() != null ? line.getProductName() : "");
            holder.binding.productQuantity.setText(line.getProductQuantity() != null ? line.getProductQuantity() : "");
            float price = 0f;
            try {
                price = Float.parseFloat(line.getProductPrice());
            } catch (Exception ignored) {
            }
            holder.binding.productRate.setText(String.format(Locale.US, "%.2f", price));
            float qty = 0f;
            try {
                qty = Float.parseFloat(line.getProductQuantity());
            } catch (Exception ignored) {
            }
            holder.binding.productAmount.setText(MainActivity.currencyName + " "
                    + String.format(Locale.US, "%.2f", price * qty));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final SalesProductListBinding binding;

            Holder(SalesProductListBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
