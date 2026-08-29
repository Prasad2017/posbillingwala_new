package com.posbillingwala.owner.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Extra.BottomSheetUi;
import com.posbillingwala.owner.Fragment.AddCustomerPortionMaster;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.PortionMasterResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.PortionMasterListBinding;

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
    public MyViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
        PortionMasterListBinding binding = PortionMasterListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        PortionMasterResponse item = portionMasterList.get(position);
        holder.binding.srNo.setText(String.valueOf(position + 1));
        holder.binding.portionMasterName.setText(item.getPortionName());
        holder.binding.portionMasterEdit.setOnClickListener(v -> updateDialog(item));
        holder.binding.portionMasterRemove.setOnClickListener(v ->
                BottomSheetUi.showConfirm(context, "Are you Sure?", "Do you want to delete this portion?",
                        "YES", "NO", true, () -> savePortionMaster(item, item.getPortionName(), "1")));
    }

    private void updateDialog(PortionMasterResponse item) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.update_category_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(context, dialogView, false);
        if (sheet == null) {
            return;
        }

        TextInputEditText nameTxt = dialogView.findViewById(R.id.categoryName);
        TextView updateTxt = dialogView.findViewById(R.id.updateCategory);
        TextView dismissTxt = dialogView.findViewById(R.id.dismissCategory);

        nameTxt.setHint("Portion Name");
        nameTxt.setText(item.getPortionName());
        if (item.getPortionName() != null) {
            nameTxt.setSelection(item.getPortionName().length());
        }

        dismissTxt.setOnClickListener(v -> sheet.dismiss());
        updateTxt.setOnClickListener(v -> {
            String newName = nameTxt.getText() != null ? nameTxt.getText().toString().trim() : "";
            if (newName.isEmpty()) {
                Toast.makeText(context, "Please enter portion name", Toast.LENGTH_SHORT).show();
                return;
            }
            sheet.dismiss();
            savePortionMaster(item, newName, "0");
        });
    }

    private void savePortionMaster(PortionMasterResponse item, String name, String deletedStatus) {
        SweetAlertDialog pDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        String network = item.getPortionMasterNetworkStatus();
        if (network == null || network.isEmpty()) {
            network = "own-" + item.getPortionMasterId();
        }

        Api.getClient().savePortionMaster(MainActivity.userId, name, deletedStatus, network)
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                        pDialog.dismiss();
                        if (response.body() != null) {
                            Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                            if ("1".equalsIgnoreCase(response.body().getStatus())) {
                                AddCustomerPortionMaster.getPortionMasterList();
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
