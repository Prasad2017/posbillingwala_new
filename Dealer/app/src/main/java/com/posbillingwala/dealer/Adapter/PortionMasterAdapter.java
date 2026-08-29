package com.posbillingwala.dealer.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.posbillingwala.dealer.Extra.BottomSheetUi;
import com.posbillingwala.dealer.Fragment.AddCustomerPortionMaster;
import com.posbillingwala.dealer.Model.AllApiResponse;
import com.posbillingwala.dealer.Model.PortionMasterResponse;
import com.posbillingwala.dealer.R;
import com.posbillingwala.dealer.Retrofit.Api;
import com.posbillingwala.dealer.databinding.PortionMasterListBinding;

import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PortionMasterAdapter extends RecyclerView.Adapter<PortionMasterAdapter.MyViewHolder> {

    private final Context context;
    private final List<PortionMasterResponse> portionMasterList;

    public PortionMasterAdapter(Context context, List<PortionMasterResponse> portionMasterList) {
        this.context = context;
        this.portionMasterList = portionMasterList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PortionMasterListBinding binding = PortionMasterListBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
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
        View dialogView = LayoutInflater.from(context).inflate(R.layout.update_portion_master_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(context, dialogView, false);
        if (sheet == null) {
            return;
        }

        TextInputEditText nameTxt = dialogView.findViewById(R.id.portionMasterName);
        nameTxt.setText(item.getPortionName());
        if (nameTxt.getText() != null) {
            nameTxt.setSelection(nameTxt.getText().length());
        }

        dialogView.findViewById(R.id.dismissPortionMaster).setOnClickListener(v -> sheet.dismiss());
        dialogView.findViewById(R.id.updatePortionMaster).setOnClickListener(v -> {
            String newName = nameTxt.getText() != null ? nameTxt.getText().toString().trim() : "";
            if (newName.isEmpty()) {
                Toast.makeText(context, "Please enter portion name", Toast.LENGTH_SHORT).show();
                return;
            }
            sheet.dismiss();
            savePortionMaster(item, newName, "0");
        });
    }

    private void deletePortionMasterDialog(PortionMasterResponse item) {
        BottomSheetUi.showConfirm(context, "Are you Sure?", "Do you want to delete this portion master?",
                "YES", "NO", true, () -> savePortionMaster(item, item.getPortionName() != null ? item.getPortionName() : "", "1"));
    }

    private void savePortionMaster(PortionMasterResponse item, String name, String deletedStatus) {
        SweetAlertDialog pDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        String networkStatus = item.getPortionMasterNetworkStatus();
        if (networkStatus == null || networkStatus.trim().isEmpty()) {
            networkStatus = "pm-" + item.getPortionMasterId();
        }

        Call<AllApiResponse> call = Api.getClient().savePortionMaster(
                AddCustomerPortionMaster.customerId, name, networkStatus, deletedStatus);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                if (response.isSuccessful() && response.body() != null && "1".equalsIgnoreCase(response.body().getStatus())) {
                    Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    AddCustomerPortionMaster.getPortionMasterList();
                } else if (response.body() != null) {
                    Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Log.e("portionMasterUpdate", "" + t.getMessage());
            }
        });
    }

    @Override
    public int getItemCount() {
        return portionMasterList.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        private final PortionMasterListBinding binding;

        MyViewHolder(@NonNull PortionMasterListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
