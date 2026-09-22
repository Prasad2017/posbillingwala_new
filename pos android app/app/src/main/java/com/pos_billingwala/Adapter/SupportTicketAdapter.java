package com.pos_billingwala.Adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Model.SupportTicketItem;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.ItemSupportTicketBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SupportTicketAdapter extends RecyclerView.Adapter<SupportTicketAdapter.ViewHolder> {

    public interface Listener {
        void onTicketClick(SupportTicketItem ticket);
    }

    private final List<SupportTicketItem> tickets = new ArrayList<>();
    private final Listener listener;

    public SupportTicketAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setTickets(List<SupportTicketItem> items) {
        tickets.clear();
        if (items != null) {
            tickets.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemSupportTicketBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(tickets.get(position));
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSupportTicketBinding binding;

        ViewHolder(ItemSupportTicketBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(SupportTicketItem ticket) {
            binding.ticketNo.setText(nz(ticket.getTicketNo()));
            binding.ticketSubject.setText(nz(ticket.getSubject()));
            binding.ticketDate.setText(nz(ticket.getCreatedAt()));

            String status = ticket.getStatus() != null ? ticket.getStatus() : "";
            boolean closed = status.equalsIgnoreCase("closed") || status.equalsIgnoreCase("resolved");
            binding.ticketStatus.setText(formatStatus(status));
            binding.ticketStatus.setBackgroundResource(closed
                    ? R.drawable.bg_support_status_closed
                    : R.drawable.bg_support_status_open);
            binding.ticketStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), closed
                    ? R.color.statusExpired
                    : R.color.statusActive));

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTicketClick(ticket);
                }
            });
        }

        private String formatStatus(String status) {
            if (status == null || status.isEmpty()) {
                return "—";
            }
            return status.substring(0, 1).toUpperCase(Locale.US) + status.substring(1).toLowerCase(Locale.US);
        }

        private String nz(String value) {
            return value == null || value.isEmpty() ? "—" : value;
        }
    }
}
