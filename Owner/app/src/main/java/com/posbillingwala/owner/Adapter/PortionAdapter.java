package com.posbillingwala.owner.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Extra.BottomSheetUi;
import com.posbillingwala.owner.Extra.RowDividerUi;
import com.posbillingwala.owner.Fragment.ManageCustomerProductPortions;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.ProductPortionResponse;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.PortionListBinding;

import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PortionAdapter extends RecyclerView.Adapter<PortionAdapter.MyViewHolder> {

    private final Context context;
    private final List<ProductPortionResponse> portionList;

    public PortionAdapter(Context context, List<ProductPortionResponse> portionList) {
        this.context = context;
        this.portionList = portionList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
        PortionListBinding binding = PortionListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ProductPortionResponse item = portionList.get(position);
        holder.binding.srNo.setText(String.valueOf(position + 1));
        holder.binding.portionName.setText(item.getPortionName());
        holder.binding.portionPrice.setText(MainActivity.currency + " " + item.getPortionPrice());
        holder.binding.portionRemove.setVisibility(android.view.View.VISIBLE);
        holder.binding.portionRemove.setOnClickListener(v ->
                BottomSheetUi.showConfirm(context, "Are you Sure?", "Do you want to delete this portion?",
                        "YES", "NO", true, () -> deletePortion(item.getPortionId())));

        RowDividerUi.bindLastItem(holder.binding.rowDivider, position, getItemCount());
    }

    private void deletePortion(String portionId) {
        SweetAlertDialog pDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Api.getClient().deleteProductPortion(MainActivity.userId, portionId)
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                        pDialog.dismiss();
                        if (response.body() != null) {
                            Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                            if ("1".equalsIgnoreCase(response.body().getStatus())
                                    && ManageCustomerProductPortions.refreshPortions != null) {
                                ManageCustomerProductPortions.refreshPortions.run();
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                        pDialog.dismiss();
                        Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
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
