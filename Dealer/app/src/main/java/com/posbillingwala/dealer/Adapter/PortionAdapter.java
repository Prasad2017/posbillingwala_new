package com.posbillingwala.dealer.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.dealer.Activity.MainActivity;
import com.posbillingwala.dealer.Model.ProductPortionResponse;
import com.posbillingwala.dealer.databinding.PortionListBinding;

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
