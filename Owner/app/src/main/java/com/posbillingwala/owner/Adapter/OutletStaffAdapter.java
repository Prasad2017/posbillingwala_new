package com.posbillingwala.owner.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.owner.Model.StaffUserResponse;
import com.posbillingwala.owner.R;

import java.util.ArrayList;
import java.util.List;

public class OutletStaffAdapter extends RecyclerView.Adapter<OutletStaffAdapter.Holder> {

    public interface OnStaffClickListener {
        void onStaffClick(StaffUserResponse staff);
    }

    private final List<StaffUserResponse> items = new ArrayList<>();
    private OnStaffClickListener clickListener;

    public void setOnStaffClickListener(OnStaffClickListener listener) {
        this.clickListener = listener;
    }

    public void setItems(List<StaffUserResponse> list) {
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
        StaffUserResponse s = items.get(position);
        String name = s.getStaffName().isEmpty() ? "—" : s.getStaffName();
        holder.lineTitle.setText(name + " · " + s.getStaffRole());

        String state;
        if (s.isDeleted()) {
            state = "deleted";
        } else if (s.isActive()) {
            state = "active";
        } else {
            state = "inactive";
        }
        holder.lineSubtitle.setText(state);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onStaffClick(s);
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
