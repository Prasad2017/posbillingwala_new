package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.pos_billingwala.Activity.BluetoothPrint;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.Extra.DineInTableHelper;
import com.pos_billingwala.Extra.PaymentSettlementBinder;
import com.pos_billingwala.Extra.ReportCursorHelper;
import com.pos_billingwala.Fragment.CreatePos;
import com.pos_billingwala.Fragment.InvoiceCompanyTable;
import com.pos_billingwala.Model.DiningSessionResponse;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.Model.PosTableResponse;
import com.pos_billingwala.Model.TableStatus;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.BottomSheetOccupiedTableBinding;
import com.pos_billingwala.databinding.TableListBinding;

import java.util.ArrayList;
import java.util.List;


@SuppressLint("SetTextI18n, UseCompatLoadingForDrawables")
public class TableAdapter extends RecyclerView.Adapter<TableAdapter.MyViewHolder> {

    private final Context context;
    private final List<PosTableResponse> allTables = new ArrayList<>();
    private final List<PosTableResponse> tables = new ArrayList<>();
    private final POSBillingWalaDatabase posBillingWalaDatabase;
    private String selectedAreaId = null; // null = All

    public TableAdapter(Context context, List<PosTableResponse> tables) {
        this.context = context;
        this.posBillingWalaDatabase = new POSBillingWalaDatabase(context);
        if (tables != null) {
            this.allTables.addAll(tables);
        }
        applyAreaFilter();
    }

    /** Legacy constructor — seeds from company.noOfTable then loads master list. */
    public TableAdapter(Context context, int noOfTablesList) {
        this.context = context;
        this.posBillingWalaDatabase = new POSBillingWalaDatabase(context);
        posBillingWalaDatabase.ensureDineInMastersSeeded();
        List<PosTableResponse> loaded = DineInTableHelper.buildFloorTableList(posBillingWalaDatabase);
        if (loaded != null && !loaded.isEmpty()) {
            this.allTables.addAll(loaded);
        } else {
            for (int i = 1; i <= noOfTablesList; i++) {
                PosTableResponse fallback = new PosTableResponse();
                fallback.setTableNumber(String.valueOf(i));
                fallback.setTableName("T" + i);
                fallback.setCapacity("4");
                fallback.setDisplayStatus(TableStatus.AVAILABLE);
                DineInTableHelper.enrichTableRuntime(posBillingWalaDatabase, fallback);
                this.allTables.add(fallback);
            }
        }
        applyAreaFilter();
    }

    public void replaceTables(List<PosTableResponse> next) {
        allTables.clear();
        if (next != null) {
            allTables.addAll(next);
        }
        applyAreaFilter();
    }

    /** Pass null areaId for All. */
    public void setAreaFilter(String areaId) {
        selectedAreaId = areaId;
        applyAreaFilter();
    }

    public String getSelectedAreaId() {
        return selectedAreaId;
    }

    private void applyAreaFilter() {
        tables.clear();
        if (selectedAreaId == null || selectedAreaId.trim().isEmpty()) {
            tables.addAll(allTables);
        } else {
            for (PosTableResponse table : allTables) {
                if (selectedAreaId.equals(table.getAreaId())) {
                    tables.add(table);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(TableListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        PosTableResponse table = tables.get(position);
        String status = table.getDisplayStatus() != null ? table.getDisplayStatus() : TableStatus.AVAILABLE;

        holder.binding.tableNumber.setText(table.getDisplayCode());
        holder.binding.tableStatus.setText(TableStatus.displayLabel(status));
        applyStatusAccent(holder, status);

        if (TableStatus.AVAILABLE.equals(status)) {
            holder.binding.billAmount.setVisibility(View.GONE);
            StringBuilder meta = new StringBuilder();
            String cap = table.getCapacity();
            if (cap != null && !cap.trim().isEmpty()) {
                meta.append(cap).append(" seats");
            }
            if (table.getAreaName() != null && !table.getAreaName().trim().isEmpty()) {
                if (meta.length() > 0) {
                    meta.append(" · ");
                }
                meta.append(table.getAreaName().trim());
            }
            if (meta.length() > 0) {
                holder.binding.tableMeta.setVisibility(View.VISIBLE);
                holder.binding.tableMeta.setText(meta.toString());
            } else {
                holder.binding.tableMeta.setVisibility(View.GONE);
            }
        } else if (TableStatus.BLOCKED.equals(status) || TableStatus.RESERVED.equals(status)) {
            holder.binding.billAmount.setVisibility(View.GONE);
            holder.binding.tableMeta.setVisibility(View.GONE);
        } else {
            holder.binding.billAmount.setVisibility(View.VISIBLE);
            holder.binding.billAmount.setText(DineInTableHelper.formatAmount(table.getCurrentAmount()));
            StringBuilder meta = new StringBuilder();
            if (table.getGuestCount() > 0) {
                meta.append(table.getGuestCount()).append(" Guests");
            }
            String elapsed = DineInTableHelper.formatElapsed(table.getSessionStartedAt());
            if (!elapsed.isEmpty()) {
                if (meta.length() > 0) {
                    meta.append(" · ");
                }
                meta.append(elapsed);
            }
            if (table.getRemainingAmount() > 0.05f
                    && TableStatus.PARTIALLY_PAID.equals(status)) {
                if (meta.length() > 0) {
                    meta.append(" · ");
                }
                meta.append("Due ").append(DineInTableHelper.formatAmount(table.getRemainingAmount()));
            }
            if (meta.length() > 0) {
                holder.binding.tableMeta.setVisibility(View.VISIBLE);
                holder.binding.tableMeta.setText(meta.toString());
            } else if (table.getAreaName() != null && !table.getAreaName().trim().isEmpty()) {
                holder.binding.tableMeta.setVisibility(View.VISIBLE);
                holder.binding.tableMeta.setText(table.getAreaName().trim());
            } else {
                holder.binding.tableMeta.setVisibility(View.GONE);
            }
        }

        holder.binding.tableNumberCardView.setOnClickListener(v -> onTableTapped(table));
        holder.binding.addItems.setOnClickListener(v -> onTableTapped(table));
        View.OnLongClickListener longPressMore = v -> {
            showMoreActions(table);
            return true;
        };
        holder.binding.tableNumberCardView.setOnLongClickListener(longPressMore);
        holder.binding.addItems.setOnLongClickListener(longPressMore);
    }

    private void applyStatusAccent(MyViewHolder holder, String status) {
        int colorRes = TableStatus.colorRes(status);
        int color = ContextCompat.getColor(context, colorRes);
        android.graphics.drawable.Drawable dot =
                ContextCompat.getDrawable(context, R.drawable.bg_table_status_dot);
        if (dot != null) {
            android.graphics.drawable.Drawable tinted = dot.mutate();
            androidx.core.graphics.drawable.DrawableCompat.setTint(tinted, color);
            holder.binding.statusDot.setBackground(tinted);
        } else {
            holder.binding.statusDot.setBackgroundColor(color);
        }
        holder.binding.tableStatus.setTextColor(color);
    }

    private void onTableTapped(PosTableResponse table) {
        if (table == null || table.getTableNumber() == null) {
            return;
        }
        String status = table.getDisplayStatus() != null ? table.getDisplayStatus() : TableStatus.AVAILABLE;
        if (TableStatus.BLOCKED.equals(status)) {
            Toast.makeText(context, "Table is blocked", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TableStatus.RESERVED.equals(status)) {
            Toast.makeText(context, "Table is reserved", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TableStatus.AVAILABLE.equals(status)) {
            openBilling(table.getTableNumber(), false);
            return;
        }
        // Occupied / payment pending — never start an unrelated new bill
        showOccupiedSheet(table);
    }

    private void showOccupiedSheet(PosTableResponse table) {
        Activity activity = (Activity) context;
        BottomSheetOccupiedTableBinding sheetBinding =
                BottomSheetOccupiedTableBinding.inflate(LayoutInflater.from(activity));
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, sheetBinding.getRoot(), true);

        sheetBinding.occupiedTitle.setText(table.getDisplayCode() + " IS OCCUPIED");
        StringBuilder amountText = new StringBuilder("Existing Bill:\n")
                .append(DineInTableHelper.formatAmount(table.getCurrentAmount()));
        if (table.getRemainingAmount() > 0.05f
                && table.getRemainingAmount() + 0.05f < table.getCurrentAmount()) {
            amountText.append("\nRemaining: ")
                    .append(DineInTableHelper.formatAmount(table.getRemainingAmount()));
        }
        if (table.isPrintRetryAvailable()) {
            amountText.append("\nPrint: Failed — retry available");
        }
        sheetBinding.occupiedAmount.setText(amountText.toString());

        boolean paymentPending = TableStatus.PAYMENT_PENDING.equals(table.getDisplayStatus())
                || TableStatus.PARTIALLY_PAID.equals(table.getDisplayStatus());

        if (table.isPrintRetryAvailable()) {
            sheetBinding.occupiedActionSettle.setText("Retry Print");
        }

        sheetBinding.occupiedActionAdd.setOnClickListener(v -> {
            sheet.dismiss();
            openBilling(table.getTableNumber(), true);
        });
        sheetBinding.occupiedActionView.setOnClickListener(v -> {
            sheet.dismiss();
            openBillPreview(table.getTableNumber());
        });
        sheetBinding.occupiedActionSettle.setOnClickListener(v -> {
            sheet.dismiss();
            if (table.isPrintRetryAvailable()) {
                retryFailedBillPrint(table);
                return;
            }
            if (paymentPending && table.getUnpaidInvoiceNumber() != null) {
                List<InvoiceResponse> unpaid = posBillingWalaDatabase.checkTablePaymentMode(table.getTableNumber());
                if (unpaid != null && !unpaid.isEmpty()) {
                    setPaymentMode(table.getTableNumber(), unpaid.get(0).getInvoiceNumber(), unpaid.get(0));
                    return;
                }
            }
            openBillPreview(table.getTableNumber());
        });
        sheetBinding.occupiedActionCancel.setOnClickListener(v -> sheet.dismiss());
    }

    private void showMoreActions(PosTableResponse table) {
        if (table == null || table.getTableNumber() == null) {
            return;
        }
        String status = table.getDisplayStatus() != null ? table.getDisplayStatus() : TableStatus.AVAILABLE;
        if (TableStatus.BLOCKED.equals(status)) {
            Toast.makeText(context, "Table is blocked", Toast.LENGTH_SHORT).show();
            return;
        }
        Activity activity = (Activity) context;
        com.pos_billingwala.Extra.DineInOpsUi.showTableActionsMenu(
                activity, posBillingWalaDatabase, table.getTableNumber(),
                navigateTo -> InvoiceCompanyTable.getCompanyDetails());
    }

    private void retryFailedBillPrint(PosTableResponse table) {
        InvoiceResponse failed = posBillingWalaDatabase.getLatestFailedPrintInvoiceForTable(table.getTableNumber());
        if (failed == null || failed.getInvoiceNumber() == null) {
            Toast.makeText(context, "No failed bill to reprint", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(context, com.pos_billingwala.Activity.DuplicateBluetoothPrint.class);
        intent.putExtra("invoiceRunningStatus", "printBill");
        intent.putExtra("invoiceNumber", failed.getInvoiceNumber());
        intent.putExtra("cartOrderStatus", DineInTableHelper.CART_ORDER_TABLE);
        intent.putExtra("tableNumber", table.getTableNumber());
        context.startActivity(intent);
        // Mark printed on successful duplicate path is best-effort; update status optimistically after launch
        posBillingWalaDatabase.updateInvoiceBillPrintStatus(failed.getInvoiceNumber(),
                com.pos_billingwala.Extra.DineInSettlementHelper.PRINT_PENDING);
    }

    private void openBilling(String tableNumber, boolean additionalOrder) {
        DineInTableHelper.openOrGetSession(posBillingWalaDatabase, tableNumber, 0);
        CreatePos createPos = new CreatePos();
        Bundle bundle = new Bundle();
        bundle.putString("tableNumber", tableNumber);
        bundle.putString("cartOrderStatus", DineInTableHelper.CART_ORDER_TABLE);
        bundle.putBoolean("additionalOrder", additionalOrder);
        createPos.setArguments(bundle);
        ((MainActivity) context).loadFragment(createPos, true);
    }

    private void openBillPreview(String tableNumber) {
        List<InvoiceResponse> unpaid = posBillingWalaDatabase.checkTablePaymentMode(tableNumber);
        if (unpaid != null && !unpaid.isEmpty()) {
            // Existing unpaid invoice — open duplicate/print path via settlement UI already handles pay
            Intent intent = new Intent(context, BluetoothPrint.class);
            intent.putExtra("invoiceRunningStatus", "printBill");
            intent.putExtra("tableNumber", tableNumber);
            intent.putExtra("cartOrderStatus", DineInTableHelper.CART_ORDER_TABLE);
            context.startActivity(intent);
            return;
        }
        if (posBillingWalaDatabase.getCartProductList(tableNumber, DineInTableHelper.CART_ORDER_TABLE).isEmpty()) {
            Toast.makeText(context, context.getString(R.string.toast_cart_is_empty), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(context, BluetoothPrint.class);
        intent.putExtra("invoiceRunningStatus", "printBill");
        intent.putExtra("tableNumber", tableNumber);
        intent.putExtra("cartOrderStatus", DineInTableHelper.CART_ORDER_TABLE);
        context.startActivity(intent);
    }

    public void setPaymentMode(String tableNumber, String invoiceNumber, InvoiceResponse invoiceResponse) {
        Activity activity = (Activity) context;
        View content = LayoutInflater.from(activity).inflate(R.layout.set_payment_mode_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, content, false);

        float total = ReportCursorHelper.parseAmount(invoiceResponse.getTotalAmount());
        PaymentSettlementBinder.bind(content, total, MainActivity.currencyName,
                invoiceResponse.getPaymentMode(),
                new PaymentSettlementBinder.Callback() {
                    @Override
                    public void onConfirmed(String mode, String cashAmount, String upiAmount) {
                        if (mode == null || mode.isEmpty()) {
                            Toast.makeText(context, context.getString(R.string.toast_please_select_payment_mode),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        sheet.dismiss();
                        posBillingWalaDatabase.updateInvoiceTablePaymentMode(
                                invoiceNumber, tableNumber, mode, cashAmount, upiAmount);
                        DiningSessionResponse session =
                                posBillingWalaDatabase.getOpenDiningSessionForTable(tableNumber);
                        if (session != null) {
                            posBillingWalaDatabase.closeDiningSession(session.getSessionId());
                        }
                        InvoiceCompanyTable.getCompanyDetails();
                    }

                    @Override
                    public void onDismissed() {
                        sheet.dismiss();
                    }
                });
    }

    @Override
    public int getItemCount() {
        return tables.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        final TableListBinding binding;

        public MyViewHolder(@NonNull TableListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
