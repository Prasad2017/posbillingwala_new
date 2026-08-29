package com.posbillingwala.admin.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.posbillingwala.admin.Extra.BottomSheetUi;
import com.posbillingwala.admin.Fragment.AddCustomerPortionMaster;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.PortionMasterResponse;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.PortionMasterListBinding;
import com.posbillingwala.admin.databinding.UpdatePortionMasterDialogBinding;

import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PortionMasterAdapter extends RecyclerView.Adapter<PortionMasterAdapter.MyViewHolder> {

    private final Context context;
    private final List<PortionMasterResponse> portionMasterList;
    private final String customerId;

    public PortionMasterAdapter(Context context, String customerId, List<PortionMasterResponse> portionMasterList) {
        this.context = context;
        this.customerId = customerId;
        this.portionMasterList = portionMasterList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PortionMasterListBinding binding = PortionMasterListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        PortionMasterResponse item = portionMasterList.get(position);
        holder.binding.srNo.setText(String.valueOf(position + 1));
        holder.binding.portionMasterName.setText(item.getPortionName());
        holder.binding.portionMasterEdit.setOnClickListener(v -> updatePortionMasterDialog(item));
        holder.binding.portionMasterRemove.setOnClickListener(v -> deletePortionMasterDialog(item));
    }

    private void updatePortionMasterDialog(PortionMasterResponse item) {
        UpdatePortionMasterDialogBinding dialogBinding = UpdatePortionMasterDialogBinding.inflate(LayoutInflater.from(context));
        BottomSheetDialog sheet = BottomSheetUi.showContent(context, dialogBinding.getRoot(), false);
        if (sheet == null) {
            return;
        }

        dialogBinding.portionMasterName.setText(item.getPortionName());
        if (dialogBinding.portionMasterName.getText() != null) {
            dialogBinding.portionMasterName.setSelection(dialogBinding.portionMasterName.getText().length());
        }

        dialogBinding.dismissPortionMaster.setOnClickListener(v -> sheet.dismiss());
        dialogBinding.updatePortionMaster.setOnClickListener(v -> {
            String newName = dialogBinding.portionMasterName.getText() != null
                    ? dialogBinding.portionMasterName.getText().toString().trim() : "";
            if (newName.isEmpty()) {
                Toast.makeText(context, "Please enter portion name", Toast.LENGTH_SHORT).show();
                return;
            }
            sheet.dismiss();
            savePortionMaster(item.getPortionMasterNetworkStatus(), newName, "0");
        });
    }

    private void deletePortionMasterDialog(PortionMasterResponse item) {
        BottomSheetUi.showConfirm(context, "Are you Sure?", "Do you want to delete this portion master?",
                "YES", "NO", true, () -> {
                    String name = item.getPortionName() != null ? item.getPortionName() : "";
                    savePortionMaster(item.getPortionMasterNetworkStatus(), name, "1");
                });
    }

    private void savePortionMaster(String networkStatus, String portionName, String deletedStatus) {
        SweetAlertDialog pDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().savePortionMaster(
                customerId, portionName, networkStatus, deletedStatus);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    if ("1".equalsIgnoreCase(response.body().getStatus())) {
                        AddCustomerPortionMaster.getPortionMasterList();
                    }
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return portionMasterList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private final PortionMasterListBinding binding;

        public MyViewHolder(@NonNull PortionMasterListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
