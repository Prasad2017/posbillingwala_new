package com.posbillingwala.admin.Adapter;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.posbillingwala.admin.Fragment.AddCustomerPortionMaster;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.PortionMasterResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.portion_master_list, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        PortionMasterResponse item = portionMasterList.get(position);
        holder.srNo.setText(String.valueOf(position + 1));
        holder.portionMasterName.setText(item.getPortionName());
        holder.portionMasterEdit.setOnClickListener(v -> updatePortionMasterDialog(item));
        holder.portionMasterRemove.setOnClickListener(v -> deletePortionMasterDialog(item));
    }

    private void updatePortionMasterDialog(PortionMasterResponse item) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.update_portion_master_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        TextInputEditText nameTxt = dialog.findViewById(R.id.portionMasterName);
        TextView updateTxt = dialog.findViewById(R.id.updatePortionMaster);
        TextView dismissTxt = dialog.findViewById(R.id.dismissPortionMaster);

        nameTxt.setText(item.getPortionName());
        if (nameTxt.getText() != null) {
            nameTxt.setSelection(nameTxt.getText().length());
        }

        dismissTxt.setOnClickListener(v -> dialog.dismiss());
        updateTxt.setOnClickListener(v -> {
            String newName = nameTxt.getText() != null ? nameTxt.getText().toString().trim() : "";
            if (newName.isEmpty()) {
                Toast.makeText(context, "Please enter portion name", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            savePortionMaster(item.getPortionMasterNetworkStatus(), newName, "0");
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }

    private void deletePortionMasterDialog(PortionMasterResponse item) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Are you Sure?")
                .setMessage("Do you want to delete this portion master?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        String name = item.getPortionName() != null ? item.getPortionName() : "";
                        savePortionMaster(item.getPortionMasterNetworkStatus(), name, "1");
                    }
                })
                .setNegativeButton("NO", (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
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

    static class MyViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.srNo)
        TextView srNo;
        @BindView(R.id.portionMasterName)
        TextView portionMasterName;
        @BindView(R.id.portionMasterEdit)
        ImageView portionMasterEdit;
        @BindView(R.id.portionMasterRemove)
        ImageView portionMasterRemove;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
