package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.Extra.RowDividerUi;
import com.pos_billingwala.Extra.ReportCursorHelper;
import com.pos_billingwala.Model.InvoiceProductResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.CartProductListBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressLint("NotifyDataSetChanged")
public class EditInvoiceProductAdapter extends RecyclerView.Adapter<EditInvoiceProductAdapter.MyViewHolder> {

    public interface Listener {
        void onLinesChanged();
    }

    private final Context context;
    private final List<InvoiceProductResponse> lines;
    private final List<String> deletedProductIds = new ArrayList<>();
    private final Listener listener;

    public EditInvoiceProductAdapter(Context context, List<InvoiceProductResponse> lines, Listener listener) {
        this.context = context;
        this.lines = lines;
        this.listener = listener;
    }

    public List<String> getDeletedProductIds() {
        return deletedProductIds;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(CartProductListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        InvoiceProductResponse line = lines.get(position);
        holder.binding.productName.setText(line.getDisplayLineName());
        holder.binding.productQuantity.setText(formatQty(ReportCursorHelper.parseAmount(line.getProductQuantity())));

        float price = ReportCursorHelper.parseAmount(line.getResolvedLinePrice());
        float qty = ReportCursorHelper.parseAmount(line.getProductQuantity());
        holder.binding.productPrice.setText(MainActivity.currencyName + " "
                + String.format(Locale.US, "%.2f", price * qty));
        holder.binding.productIcon.setVisibility(android.view.View.GONE);
        holder.binding.productLineTotal.setVisibility(android.view.View.GONE);

        holder.binding.productRemove.setOnClickListener(v -> changeQty(holder.getBindingAdapterPosition(), -1));
        holder.binding.productAdd.setOnClickListener(v -> changeQty(holder.getBindingAdapterPosition(), 1));
        holder.binding.productDelete.setOnClickListener(v -> confirmDelete(holder.getBindingAdapterPosition()));
        holder.binding.productQuantity.setOnClickListener(null);
        holder.binding.productPrice.setOnClickListener(null);

        RowDividerUi.bindLastItem(holder.binding.rowDivider, position, getItemCount());
    }

    private void changeQty(int position, int delta) {
        if (position < 0 || position >= lines.size()) {
            return;
        }
        InvoiceProductResponse line = lines.get(position);
        float qty = ReportCursorHelper.parseAmount(line.getProductQuantity()) + delta;
        if (qty <= 0) {
            removeLine(position);
            return;
        }
        line.setProductQuantity(formatQty(qty));
        notifyItemChanged(position);
        if (listener != null) {
            listener.onLinesChanged();
        }
    }

    private void confirmDelete(int position) {
        if (position < 0 || position >= lines.size()) {
            return;
        }
        BottomSheetUi.showConfirm(
                context,
                context.getString(R.string.toast_delete_product),
                context.getString(R.string.toast_do_you_want_to_delete_from_bill),
                context.getString(android.R.string.yes),
                context.getString(android.R.string.no),
                true,
                () -> removeLine(position));
    }

    private void removeLine(int position) {
        if (lines.size() <= 1) {
            Toast.makeText(context, context.getString(R.string.toast_keep_one_item), Toast.LENGTH_SHORT).show();
            return;
        }
        InvoiceProductResponse removed = lines.remove(position);
        if (removed.getInvoiceProductId() != null && !removed.getInvoiceProductId().isEmpty()) {
            deletedProductIds.add(removed.getInvoiceProductId());
        }
        notifyDataSetChanged();
        if (listener != null) {
            listener.onLinesChanged();
        }
    }

    private static String formatQty(float qty) {
        if (qty == (long) qty) {
            return String.valueOf((long) qty);
        }
        return String.format(Locale.US, "%.2f", qty);
    }

    @Override
    public int getItemCount() {
        return lines.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        final CartProductListBinding binding;

        public MyViewHolder(@NonNull CartProductListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
