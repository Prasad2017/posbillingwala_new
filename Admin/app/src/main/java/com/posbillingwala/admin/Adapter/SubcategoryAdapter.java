package com.posbillingwala.admin.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.admin.Model.ProductSubcategoryResponse;
import com.posbillingwala.admin.R;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SubcategoryAdapter extends RecyclerView.Adapter<SubcategoryAdapter.MyViewHolder> {

    private final Context context;
    private final List<ProductSubcategoryResponse> subcategoryList;

    public SubcategoryAdapter(Context context, List<ProductSubcategoryResponse> subcategoryList) {
        this.context = context;
        this.subcategoryList = subcategoryList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.subcategory_list, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ProductSubcategoryResponse item = subcategoryList.get(position);
        holder.srNo.setText(String.valueOf(position + 1));
        holder.subcategoryName.setText(item.getSubcategoryName());
    }

    @Override
    public int getItemCount() {
        return subcategoryList.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.srNo)
        TextView srNo;
        @BindView(R.id.subcategoryName)
        TextView subcategoryName;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
