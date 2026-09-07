package com.posbillingwala.owner.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.owner.Model.ServiceAppointmentResponse;
import com.posbillingwala.owner.R;

import java.util.ArrayList;
import java.util.List;

public class OutletAppointmentAdapter extends RecyclerView.Adapter<OutletAppointmentAdapter.Holder> {

    public interface OnDayClickListener {
        void onDayKeyClick(String dayKey);
    }

    public interface OnAppointmentClickListener {
        void onAppointmentClick(ServiceAppointmentResponse appointment);
    }

    private final List<ServiceAppointmentResponse> items = new ArrayList<>();
    private boolean weekMode;
    private OnDayClickListener dayClickListener;
    private OnAppointmentClickListener appointmentClickListener;

    public void setWeekMode(boolean weekMode) {
        this.weekMode = weekMode;
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        this.dayClickListener = listener;
    }

    public void setOnAppointmentClickListener(OnAppointmentClickListener listener) {
        this.appointmentClickListener = listener;
    }

    public void setItems(List<ServiceAppointmentResponse> list) {
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
        ServiceAppointmentResponse a = items.get(position);
        String when = a.getAppointmentAt();
        String time = when;
        if (when.length() >= 16) {
            time = when.substring(11); // HH:mm from dd-MM-yyyy HH:mm
        }
        String title = time + " · " + (a.getCustomerName().isEmpty() ? "—" : a.getCustomerName());
        if (!a.getCustomerMobile().isEmpty()) {
            title = title + " (" + a.getCustomerMobile() + ")";
        }
        holder.lineTitle.setText(title);

        StringBuilder sub = new StringBuilder();
        if (!a.getProductName().isEmpty()) {
            sub.append(a.getProductName());
        }
        if (sub.length() > 0) {
            sub.append(" · ");
        }
        sub.append(a.getAppointmentStatus());
        if (!a.getStaffName().isEmpty()) {
            sub.append(" · ").append(a.getStaffName());
        }
        if (!a.getNotes().isEmpty()) {
            sub.append("\n").append(a.getNotes());
        }
        if (weekMode && when.length() >= 10) {
            sub.insert(0, when.substring(0, 10) + " · ");
        }
        holder.lineSubtitle.setText(sub.toString());

        holder.itemView.setOnClickListener(v -> {
            if (appointmentClickListener != null) {
                appointmentClickListener.onAppointmentClick(a);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (weekMode && dayClickListener != null && when.length() >= 10) {
                dayClickListener.onDayKeyClick(when.substring(0, 10));
                return true;
            }
            return false;
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
