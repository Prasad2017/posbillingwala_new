package com.pos_billingwala.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Fragment.CreatePos;
import com.pos_billingwala.Model.ComboResponse;
import com.pos_billingwala.databinding.HomeProductListBinding;

import java.util.List;

public class HomeComboAdapter extends RecyclerView.Adapter<HomeComboAdapter.Holder> {

    public interface Listener {
        void comboClicked(ComboResponse combo);
    }

    private final Context context;
    private final List<ComboResponse> combos;
    private final Listener listener;

    public HomeComboAdapter(Context context, List<ComboResponse> combos, Listener listener) {
        this.context = context;
        this.combos = combos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(HomeProductListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ComboResponse combo = combos.get(position);
        holder.binding.productName.setText(combo.getComboName());
        float price = 0f;
        try {
            price = Float.parseFloat(combo.getComboPrice() != null ? combo.getComboPrice() : "0");
        } catch (NumberFormatException ignored) {
        }
        String priceText = MainActivity.currencyName + " " + price;
        if (!CreatePos.companyResponseList.isEmpty()
                && CreatePos.companyResponseList.get(0).getGstStatus() != null
                && CreatePos.companyResponseList.get(0).getGstStatus().equalsIgnoreCase("On")) {
            float cgst = parseRate(combo.getComboCGST());
            float sgst = parseRate(combo.getComboSGST());
            priceText = MainActivity.currencyName + " " + (price + (price * ((cgst + sgst) / 100f)));
        }
        holder.binding.productPriceUnit.setText(priceText);
        if (combo.getComboCartQuantity() != null && !combo.getComboCartQuantity().trim().isEmpty()) {
            holder.binding.productQuantity.setText(combo.getComboCartQuantity());
            holder.binding.productQuantity.setVisibility(View.VISIBLE);
        } else {
            holder.binding.productQuantity.setVisibility(View.GONE);
        }
        holder.binding.productCardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.comboClicked(combo);
            }
        });
    }

    private float parseRate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0f;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    @Override
    public int getItemCount() {
        return combos.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final HomeProductListBinding binding;

        Holder(HomeProductListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
