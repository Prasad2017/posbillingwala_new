package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.Fragment.ManageProductPortions;
import com.pos_billingwala.Model.ProductPortionResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.PortionListBinding;

import java.util.List;

@SuppressLint("SetTextI18n, NotifyDataSetChanged")
public class PortionAdapter extends RecyclerView.Adapter<PortionAdapter.MyViewHolder> {

    Context context;
    List<ProductPortionResponse> portionList;
    POSBillingWalaDatabase posBillingWalaDatabase;
    String productId;

    public PortionAdapter(Context context, String productId, List<ProductPortionResponse> portionList) {
        this.context = context;
        this.productId = productId;
        this.portionList = portionList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(PortionListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ProductPortionResponse item = portionList.get(position);
        posBillingWalaDatabase = new POSBillingWalaDatabase(context);

        holder.binding.srNo.setText("" + (position + 1));
        holder.binding.portionName.setText(item.getPortionName());
        holder.binding.portionPrice.setText(MainActivity.currencyName + " " + item.getPortionPrice());

        holder.binding.portionEdit.setOnClickListener(v -> updatePortion(item));
        holder.binding.portionRemove.setOnClickListener(v -> deletePortion(item.getPortionId()));
    }

    private void updatePortion(ProductPortionResponse item) {
        Activity activity = (Activity) context;
        View content = LayoutInflater.from(activity).inflate(R.layout.update_portion_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, content, false);

        TextInputEditText portionNameTxt = content.findViewById(R.id.portionName);
        TextInputEditText portionPriceTxt = content.findViewById(R.id.portionPrice);
        TextInputEditText portionSortOrderTxt = content.findViewById(R.id.portionSortOrder);
        TextView updatePortionTxt = content.findViewById(R.id.updatePortion);
        TextView dismissPortionTxt = content.findViewById(R.id.dismissPortion);

        portionNameTxt.setText(item.getPortionName());
        portionPriceTxt.setText(item.getPortionPrice());
        portionSortOrderTxt.setText(item.getPortionSortOrder() != null ? item.getPortionSortOrder() : "0");
        portionNameTxt.setEnabled(false);
        portionNameTxt.setFocusable(false);

        dismissPortionTxt.setOnClickListener(v -> sheet.dismiss());

        updatePortionTxt.setOnClickListener(v -> {
            String price = portionPriceTxt.getText().toString().trim();
            if (price.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.toast_please_enter_portion_price), Toast.LENGTH_SHORT).show();
                return;
            }
            int sortOrder = parseSortOrder(portionSortOrderTxt.getText().toString(), item.getPortionSortOrder());
            sheet.dismiss();
            posBillingWalaDatabase.updateProductPortionPriceAndSort(item.getPortionId(), price, sortOrder);
            Toast.makeText(context, context.getString(R.string.toast_portion_updated), Toast.LENGTH_SHORT).show();
            ManageProductPortions.getPortionList();
        });
    }

    private int parseSortOrder(String input, String fallback) {
        try {
            if (input != null && !input.trim().isEmpty()) {
                return Integer.parseInt(input.trim());
            }
        } catch (NumberFormatException ignored) {
        }
        try {
            if (fallback != null && !fallback.trim().isEmpty()) {
                return Integer.parseInt(fallback.trim());
            }
        } catch (NumberFormatException ignored) {
        }
        return 0;
    }

    private void deletePortion(String portionId) {
        BottomSheetUi.showConfirm(
                context,
                context.getString(R.string.toast_are_you_sure),
                context.getString(R.string.toast_do_you_want_to_delete_this_portion),
                "YES",
                "NO",
                true,
                () -> {
                    posBillingWalaDatabase.deleteProductPortion(portionId);
                    Toast.makeText(context, context.getString(R.string.toast_portion_deleted), Toast.LENGTH_SHORT).show();
                    ManageProductPortions.getPortionList();
                });
    }

    @Override
    public int getItemCount() {
        return portionList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        PortionListBinding binding;

        public MyViewHolder(PortionListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
