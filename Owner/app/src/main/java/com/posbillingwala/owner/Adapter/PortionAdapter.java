package com.posbillingwala.owner.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Model.ProductPortionResponse;
import com.posbillingwala.owner.databinding.PortionListBinding;

import java.util.List;

public class PortionAdapter extends RecyclerView.Adapter<PortionAdapter.MyViewHolder> {

    private final Context context;
    private final List<ProductPortionResponse> portionList;

    public PortionAdapter(Context context, List<ProductPortionResponse> portionList) {
        this.context = context;
        this.portionList = portionList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PortionListBinding binding = PortionListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ProductPortionResponse item = portionList.get(position);
        holder.binding.srNo.setText(String.valueOf(position + 1));
        holder.binding.portionName.setText(item.getPortionName());
        holder.binding.portionPrice.setText(MainActivity.currency + " " + item.getPortionPrice());
        holder.binding.portionRemove.setVisibility(android.view.View.GONE);
    }

    @Override
    public int getItemCount() {
        return portionList.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        private final PortionListBinding binding;

        MyViewHolder(@NonNull PortionListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
