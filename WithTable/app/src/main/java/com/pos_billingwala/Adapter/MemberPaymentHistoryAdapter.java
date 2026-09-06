package com.pos_billingwala.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Model.MemberPaymentMonthItem;
import com.pos_billingwala.R;

import java.util.List;

public class MemberPaymentHistoryAdapter extends RecyclerView.Adapter<MemberPaymentHistoryAdapter.VH> {

    private final Context context;
    private final List<MemberPaymentMonthItem> items;
    private final String currency;

    public MemberPaymentHistoryAdapter(Context context, List<MemberPaymentMonthItem> items) {
        this.context = context;
        this.items = items;
        this.currency = MainActivity.currencyName != null ? MainActivity.currencyName : "₹";
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member_payment_history_row, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        MemberPaymentMonthItem item = items.get(position);
        boolean pending = "Pending".equalsIgnoreCase(item.status);

        holder.monthTxt.setText(item.yearMonth != null ? item.yearMonth : "-");
        holder.messAmountTxt.setText(currency + " " + safe(item.messAmount));
        holder.paidAmountTxt.setText(currency + " " + safe(item.paidAmount));
        holder.pendingAmountTxt.setText(currency + " " + safe(item.pendingAmount));
        holder.statusTxt.setText(item.status != null ? item.status : "-");

        int rowBg = position % 2 == 0 ? R.color.colorSurface : R.color.colorPrimaryLight;
        holder.itemView.setBackgroundColor(ContextCompat.getColor(context, rowBg));

        if (pending) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorItem2Background));
            holder.pendingAmountTxt.setTextColor(ContextCompat.getColor(context, R.color.statusTrial));
            holder.statusTxt.setTextColor(ContextCompat.getColor(context, R.color.statusTrial));
        } else {
            holder.pendingAmountTxt.setTextColor(ContextCompat.getColor(context, R.color.statusActive));
            holder.statusTxt.setTextColor(ContextCompat.getColor(context, R.color.statusActive));
        }
    }

    private String safe(String value) {
        return value != null && !value.trim().isEmpty() ? value : "0.00";
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView monthTxt, messAmountTxt, paidAmountTxt, pendingAmountTxt, statusTxt;

        VH(@NonNull View itemView) {
            super(itemView);
            monthTxt = itemView.findViewById(R.id.monthTxt);
            messAmountTxt = itemView.findViewById(R.id.messAmountTxt);
            paidAmountTxt = itemView.findViewById(R.id.paidAmountTxt);
            pendingAmountTxt = itemView.findViewById(R.id.pendingAmountTxt);
            statusTxt = itemView.findViewById(R.id.statusTxt);
        }
    }
}
