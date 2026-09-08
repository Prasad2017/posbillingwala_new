package com.posbillingwala.dealer.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.dealer.Model.MessMemberResponse;
import com.posbillingwala.dealer.R;

import java.util.ArrayList;
import java.util.List;

public class MessMemberManageAdapter extends RecyclerView.Adapter<MessMemberManageAdapter.Holder> {

    public interface Listener {
        void onEdit(MessMemberResponse member);

        void onPayments(MessMemberResponse member);
    }

    private final List<MessMemberResponse> items = new ArrayList<>();
    private final Listener listener;

    public MessMemberManageAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<MessMemberResponse> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mess_member_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        MessMemberResponse m = items.get(position);
        holder.txtName.setText(m.getMemberName() != null ? m.getMemberName() : "");
        holder.txtMobile.setText(m.getMemberMobileNumber() != null ? m.getMemberMobileNumber() : "");
        String type = m.getMemberType() != null ? m.getMemberType() : "student";
        String reg = m.getRegistrationNo() != null ? m.getRegistrationNo() : "";
        String status = m.getMemberStatus() != null ? m.getMemberStatus() : "";
        holder.txtMeta.setText(type + " · Reg: " + reg + " · " + status);
        holder.btnEdit.setOnClickListener(v -> { if (listener != null) listener.onEdit(m); });
        holder.btnPayments.setOnClickListener(v -> { if (listener != null) listener.onPayments(m); });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView txtName, txtMobile, txtMeta, btnEdit, btnPayments;

        Holder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtName);
            txtMobile = itemView.findViewById(R.id.txtMobile);
            txtMeta = itemView.findViewById(R.id.txtMeta);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnPayments = itemView.findViewById(R.id.btnPayments);
        }
    }
}
