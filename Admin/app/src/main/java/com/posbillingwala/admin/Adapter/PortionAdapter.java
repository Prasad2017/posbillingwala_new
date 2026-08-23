package com.posbillingwala.admin.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Model.ProductPortionResponse;
import com.posbillingwala.admin.R;

import java.util.List;

import butterknife.BindViews;
import butterknife.ButterKnife;

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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.portion_list, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ProductPortionResponse item = portionList.get(position);
        holder.textViews.get(0).setText(String.valueOf(position + 1));
        holder.textViews.get(1).setText(item.getPortionName());
        holder.textViews.get(2).setText(MainActivity.currency + " " + item.getPortionPrice());
    }

    @Override
    public int getItemCount() {
        return portionList.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        @BindViews({R.id.srNo, R.id.portionName, R.id.portionPrice})
        List<TextView> textViews;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
