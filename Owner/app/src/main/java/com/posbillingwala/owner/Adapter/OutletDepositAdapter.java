package com.posbillingwala.owner.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.owner.Model.CustomOrderDepositResponse;
import com.posbillingwala.owner.R;

import java.util.ArrayList;
import java.util.List;

public class OutletDepositAdapter extends RecyclerView.Adapter<OutletDepositAdapter.Holder> {

    public interface OnDepositClickListener {
        void onDepositClick(CustomOrderDepositResponse deposit);
    }

    private final List<CustomOrderDepositResponse> items = new ArrayList<>();
    private OnDepositClickListener clickListener;

    public void setOnDepositClickListener(OnDepositClickListener listener) {
        this.clickListener = listener;
    }

    public void setItems(List<CustomOrderDepositResponse> list) {
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
                .inflate(R.layout.item_outlet_appointment, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        CustomOrderDepositResponse d = items.get(position);
        String amount = d.getDepositAmount().isEmpty() ? "—" : d.getDepositAmount();
        String name = d.getProductName().isEmpty() ? "Custom order" : d.getProductName();
        holder.lineTitle.setText(amount + " · " + name);

        StringBuilder sub = new StringBuilder(d.getDepositStatus());
        if (!d.getDueDate().isEmpty()) {
            sub.append(" · due ").append(d.getDueDate());
        }
        if (!d.getOrderNote().isEmpty()) {
            sub.append("\n").append(d.getOrderNote());
        }
        if (!d.getCreatedAt().isEmpty()) {
            sub.append("\n").append(d.getCreatedAt());
        }
        holder.lineSubtitle.setText(sub.toString());

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onDepositClick(d);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView lineTitle;
        final TextView lineSubtitle;

        Holder(@NonNull View itemView) {
            super(itemView);
            lineTitle = itemView.findViewById(R.id.lineTitle);
            lineSubtitle = itemView.findViewById(R.id.lineSubtitle);
        }
    }
}
