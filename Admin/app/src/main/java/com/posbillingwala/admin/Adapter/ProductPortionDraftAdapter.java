package com.posbillingwala.admin.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Model.ProductPortionDraft;
import com.posbillingwala.admin.R;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ProductPortionDraftAdapter extends RecyclerView.Adapter<ProductPortionDraftAdapter.MyViewHolder> {

    public interface Listener {
        void onRemove(int position);
    }

    private final Context context;
    private final List<ProductPortionDraft> drafts;
    private final Listener listener;

    public ProductPortionDraftAdapter(Context context, List<ProductPortionDraft> drafts, Listener listener) {
        this.context = context;
        this.drafts = drafts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.product_portion_draft_list, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ProductPortionDraft item = drafts.get(position);
        holder.srNo.setText(String.valueOf(position + 1));
        holder.portionName.setText(item.getPortionName());
        holder.portionPrice.setText(MainActivity.currency + " " + item.getPortionPrice());
        holder.portionRemove.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onRemove(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return drafts.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.srNo)
        TextView srNo;
        @BindView(R.id.portionName)
        TextView portionName;
        @BindView(R.id.portionPrice)
        TextView portionPrice;
        @BindView(R.id.portionRemove)
        ImageView portionRemove;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
