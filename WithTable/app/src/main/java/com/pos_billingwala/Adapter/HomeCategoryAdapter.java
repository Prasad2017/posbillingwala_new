package com.pos_billingwala.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Interface.ClickListerInterface;
import com.pos_billingwala.Model.ProductCategoryResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.HomeCategoryListBinding;

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
        int bg = ContextCompat.getColor(context, selected ? R.color.colorPrimary : R.color.blue_grey_900);
        holder.binding.categoryCardView.setCardBackgroundColor(bg);

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
