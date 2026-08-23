package com.posbillingwala.owner.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.owner.Model.PortionMasterResponse;
import com.posbillingwala.owner.databinding.PortionMasterListBinding;

import java.util.List;

public class PortionMasterAdapter extends RecyclerView.Adapter<PortionMasterAdapter.MyViewHolder> {

    private final Context context;
    private final List<PortionMasterResponse> portionMasterList;

    public PortionMasterAdapter(Context context, List<PortionMasterResponse> portionMasterList) {
        this.context = context;
        this.portionMasterList = portionMasterList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PortionMasterListBinding binding = PortionMasterListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        PortionMasterResponse item = portionMasterList.get(position);
        holder.binding.srNo.setText(String.valueOf(position + 1));
        holder.binding.portionMasterName.setText(item.getPortionName());
    }

    @Override
    public int getItemCount() {
        return portionMasterList.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        private final PortionMasterListBinding binding;

        MyViewHolder(@NonNull PortionMasterListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
