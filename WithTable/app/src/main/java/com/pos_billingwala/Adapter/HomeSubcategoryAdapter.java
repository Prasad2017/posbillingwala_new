package com.pos_billingwala.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Model.ProductSubcategoryResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.ItemHomeSubcategoryBinding;

import java.util.Collections;
import java.util.List;

public class HomeSubcategoryAdapter extends RecyclerView.Adapter<HomeSubcategoryAdapter.MyViewHolder> {

    public interface SubcategoryClickListener {
        void onSubcategoryClicked(String subcategoryId);
    }

    public static final String ALL_ID = "";

    private final Context context;
    private final List<ProductSubcategoryResponse> items;
    private final SubcategoryClickListener listener;
    private String selectedSubcategoryId = ALL_ID;

    public HomeSubcategoryAdapter(Context context, List<ProductSubcategoryResponse> items,
                                  SubcategoryClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    public void setSelectedSubcategoryId(String selectedSubcategoryId) {
        this.selectedSubcategoryId = selectedSubcategoryId != null ? selectedSubcategoryId : ALL_ID;
        notifyDataSetChanged();
    }

    public List<ProductSubcategoryResponse> getItems() {
        return items;
    }

    public boolean moveItem(int fromPosition, int toPosition) {
        if (fromPosition < 0 || toPosition < 0
                || fromPosition >= items.size() || toPosition >= items.size()) {
            return false;
        }
        // Keep synthetic "All" pinned at index 0
        if (fromPosition == 0 || toPosition == 0) {
            return false;
        }
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(items, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(items, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(ItemHomeSubcategoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ProductSubcategoryResponse item = items.get(position);
        String id = item.getSubcategoryId() != null ? item.getSubcategoryId() : ALL_ID;
        holder.binding.subcategoryName.setText(item.getSubcategoryName());

        boolean selected = selectedSubcategoryId.equals(id)
                || (ALL_ID.equals(selectedSubcategoryId) && ALL_ID.equals(id));
        int bg = ContextCompat.getColor(context, selected ? R.color.colorPrimary : R.color.white);
        int text = ContextCompat.getColor(context, selected ? R.color.white : R.color.blue_grey_900);
        holder.binding.subcategoryCardView.setCardBackgroundColor(bg);
        holder.binding.subcategoryCardView.setStrokeColor(
                ContextCompat.getColor(context, selected ? R.color.colorPrimary : R.color.blue_grey_900));
        holder.binding.subcategoryName.setTextColor(text);

        holder.binding.subcategoryCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onSubcategoryClicked(id);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        ItemHomeSubcategoryBinding binding;

        public MyViewHolder(ItemHomeSubcategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
