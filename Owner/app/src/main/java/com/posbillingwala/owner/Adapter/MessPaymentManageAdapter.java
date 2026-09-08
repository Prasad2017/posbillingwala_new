package com.posbillingwala.owner.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.owner.Model.MessMemberResponse;
import com.posbillingwala.owner.R;

import java.util.ArrayList;
import java.util.List;

public class MessPaymentManageAdapter extends RecyclerView.Adapter<MessPaymentManageAdapter.Holder> {

    private final List<MessMemberResponse> items = new ArrayList<>();

    public void setItems(List<MessMemberResponse> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mess_payment_row, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        MessMemberResponse p = items.get(position);
        String paid = p.getPaymentPaidAmount() != null ? p.getPaymentPaidAmount() : "0";
        String mess = p.getPaymentMessAmount() != null ? p.getPaymentMessAmount() : "0";
        holder.line.setText("Paid " + paid + " / Mess " + mess);
        String date = p.getPaymentDate() != null ? p.getPaymentDate() : "";
        String days = p.getMessTotalDays() != null ? p.getMessTotalDays() : "";
        String status = p.getPaymentStatus() != null ? p.getPaymentStatus() : "";
        holder.meta.setText(date + " · Days: " + days + " · " + status);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView line, meta;

        Holder(@NonNull View itemView) {
            super(itemView);
            line = itemView.findViewById(R.id.txtPaymentLine);
            meta = itemView.findViewById(R.id.txtPaymentMeta);
        }
    }
}
