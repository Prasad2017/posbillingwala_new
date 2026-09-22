package com.pos_billingwala.Adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.databinding.ComboProductPickListBinding;

import java.util.List;
import java.util.Locale;

public class ComboProductPickAdapter extends RecyclerView.Adapter<ComboProductPickAdapter.Holder> {

    public interface Listener {
        void onProductPicked(ProductResponse product);
    }

    private final List<ProductResponse> products;
    private final Listener listener;

    public ComboProductPickAdapter(List<ProductResponse> products, Listener listener) {
        this.products = products;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ComboProductPickListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ProductResponse product = products.get(position);
        holder.binding.productName.setText(product.getProductName() != null ? product.getProductName() : "");
        holder.binding.productPrice.setText(formatPrice(product.getProductPrice()));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductPicked(product);
            }
        });
    }

    private static String formatPrice(String rawPrice) {
        String currency = MainActivity.currencyName != null ? MainActivity.currencyName : "";
        String price = rawPrice != null ? rawPrice.trim() : "";
        if (price.isEmpty()) {
            price = "0";
        }
        try {
            double value = Double.parseDouble(price);
            return String.format(Locale.US, "%s %.2f", currency, value).trim();
        } catch (NumberFormatException e) {
            return (currency + " " + price).trim();
        }
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ComboProductPickListBinding binding;

        Holder(ComboProductPickListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
