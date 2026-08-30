package com.pos_billingwala.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Fragment.CreatePos;
import com.pos_billingwala.Interface.ClickListerInterface;
import com.pos_billingwala.Model.ProductCategoryResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.HomeCategoryListBinding;

import java.util.Collections;
import java.util.List;


public class HomeCategoryAdapter extends RecyclerView.Adapter<HomeCategoryAdapter.MyViewHolder> {

    Context context;
    List<ProductCategoryResponse> productCategoryResponseList;
    ClickListerInterface clickListerInterface;
    String selectedCategoryId;

    public HomeCategoryAdapter(Context context, List<ProductCategoryResponse> productCategoryResponseList, ClickListerInterface clickListerInterface) {
        this.context = context;
        this.productCategoryResponseList = productCategoryResponseList;
        this.clickListerInterface = clickListerInterface;
    }

    public void setSelectedCategoryId(String selectedCategoryId) {
        this.selectedCategoryId = selectedCategoryId;
        notifyDataSetChanged();
    }

    public List<ProductCategoryResponse> getItems() {
        return productCategoryResponseList;
    }

    public boolean moveItem(int fromPosition, int toPosition) {
        if (fromPosition < 0 || toPosition < 0
                || fromPosition >= productCategoryResponseList.size()
                || toPosition >= productCategoryResponseList.size()) {
            return false;
        }
        // Keep synthetic "All" pinned at index 0
        if (fromPosition == 0 || toPosition == 0) {
            return false;
        }
        ProductCategoryResponse from = productCategoryResponseList.get(fromPosition);
        if (CreatePos.CATEGORY_ALL_ID.equals(from.getCategoryId())) {
            return false;
        }
        ProductCategoryResponse to = productCategoryResponseList.get(toPosition);
        if (CreatePos.CATEGORY_ALL_ID.equals(to.getCategoryId())) {
            return false;
        }
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(productCategoryResponseList, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(productCategoryResponseList, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(HomeCategoryListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        ProductCategoryResponse productCategoryResponse = productCategoryResponseList.get(position);

        holder.binding.categoryName.setText(productCategoryResponse.getCategoryName());

        boolean selected = selectedCategoryId != null
                && selectedCategoryId.equals(productCategoryResponse.getCategoryId());
        int bg = ContextCompat.getColor(context, selected ? R.color.colorPrimary : R.color.white);
        int text = ContextCompat.getColor(context, selected ? R.color.white : R.color.colorTextPrimary);
        int stroke = ContextCompat.getColor(context, selected ? R.color.colorPrimary : R.color.colorBorder);
        holder.binding.categoryCardView.setCardBackgroundColor(bg);
        holder.binding.categoryCardView.setStrokeColor(stroke);
        holder.binding.categoryName.setTextColor(text);

        holder.binding.categoryCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListerInterface.categoryClicked(productCategoryResponse.getCategoryId());
            }
        });

    }

    @Override
    public int getItemCount() {
        return productCategoryResponseList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {


        HomeCategoryListBinding binding;

        public MyViewHolder(HomeCategoryListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

        }
    }
}
