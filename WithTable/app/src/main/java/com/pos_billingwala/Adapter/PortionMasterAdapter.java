package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Fragment.AddPortionMaster;
import com.pos_billingwala.Model.PortionMasterResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.PortionMasterListBinding;

import java.util.List;

@SuppressLint("SetTextI18n, NotifyDataSetChanged")
public class PortionMasterAdapter extends RecyclerView.Adapter<PortionMasterAdapter.MyViewHolder> {

    Context context;
    List<PortionMasterResponse> portionMasterList;
    POSBillingWalaDatabase posBillingWalaDatabase;

    public PortionMasterAdapter(Context context, List<PortionMasterResponse> portionMasterList) {
        this.context = context;
        this.portionMasterList = portionMasterList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(PortionMasterListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        PortionMasterResponse item = portionMasterList.get(position);
        posBillingWalaDatabase = new POSBillingWalaDatabase(context);

        holder.binding.srNo.setText("" + (position + 1));
        holder.binding.portionMasterName.setText(item.getPortionName());

        holder.binding.portionMasterEdit.setOnClickListener(v -> updatePortionMaster(item));
        holder.binding.portionMasterRemove.setOnClickListener(v -> deletePortionMaster(item));
    }

    private void updatePortionMaster(PortionMasterResponse item) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.update_portion_master_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCancelable(false);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        TextInputEditText nameTxt = dialog.findViewById(R.id.portionMasterName);
        TextView updateTxt = dialog.findViewById(R.id.updatePortionMaster);
        TextView dismissTxt = dialog.findViewById(R.id.dismissPortionMaster);

        nameTxt.setText(item.getPortionName());
        nameTxt.setSelection(nameTxt.getText().length());

        dismissTxt.setOnClickListener(v -> dialog.dismiss());

        updateTxt.setOnClickListener(v -> {
            String newName = nameTxt.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.toast_please_enter_portion_name), Toast.LENGTH_SHORT).show();
                return;
            }
            List<PortionMasterResponse> existing = posBillingWalaDatabase.getPortionMasterByName(newName);
            if (!existing.isEmpty()
                    && !existing.get(0).getPortionMasterId().equals(item.getPortionMasterId())) {
                Toast.makeText(context, context.getString(R.string.toast_portion_name_already_exists), Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            posBillingWalaDatabase.updatePortionMaster(item.getPortionMasterId(), newName, 0);
            Toast.makeText(context, context.getString(R.string.toast_portion_updated), Toast.LENGTH_SHORT).show();
            AddPortionMaster.getPortionMasterList();
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }

    private void deletePortionMaster(PortionMasterResponse item) {
        int usage = posBillingWalaDatabase.countUsageOnProducts(item.getPortionMasterId());
        if (usage > 0) {
            Toast.makeText(context,
                    "Cannot delete — used by " + usage + " product portion(s). Remove those links first.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.toast_are_you_sure))
                .setMessage(context.getString(R.string.toast_do_you_want_to_delete_this_portion_maste))
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        posBillingWalaDatabase.deletePortionMaster(item.getPortionMasterId());
                        Toast.makeText(context, context.getString(R.string.toast_portion_deleted), Toast.LENGTH_SHORT).show();
                        AddPortionMaster.getPortionMasterList();
                    }
                })
                .setNegativeButton("NO", (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }

    @Override
    public int getItemCount() {
        return portionMasterList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        PortionMasterListBinding binding;

        public MyViewHolder(PortionMasterListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
