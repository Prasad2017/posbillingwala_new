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
import com.pos_billingwala.Activity.BluetoothPrint;
import com.pos_billingwala.Extra.AppExecutors;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Fragment.CreatePos;
import com.pos_billingwala.Model.ProductCartResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.CartProductListBinding;

import java.util.List;
import java.util.Locale;


@SuppressLint("SetTextI18n, NotifyDataSetChanged")
public class CartAdapter extends RecyclerView.Adapter<CartAdapter.MyViewHolder> {

    Context context;
    List<ProductCartResponse> productCartResponseList;
    POSBillingWalaDatabase posBillingWalaDatabase;
    String productPriceUnit;

    public CartAdapter(Context context, List<ProductCartResponse> productCartResponseList) {
        this.context = context;
        this.productCartResponseList = productCartResponseList;
        this.posBillingWalaDatabase = new POSBillingWalaDatabase(context);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(CartProductListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        ProductCartResponse productCartResponse = productCartResponseList.get(position);

        holder.binding.productName.setText(productCartResponse.getDisplayLineName());
        float productQuantity = parseQty(productCartResponse.getProductQuantity());
        holder.binding.productQuantity.setText(formatQty(productQuantity));

        float productPrice = Float.parseFloat(productCartResponse.getResolvedLinePrice());
        if (!CreatePos.companyResponseList.isEmpty()) {
            if (CreatePos.companyResponseList.get(0).getGstStatus() != null) {
                if (CreatePos.companyResponseList.get(0).getGstStatus().equalsIgnoreCase("On")) {
                    float totalCGST = 0f, totalSGST = 0f;
                    if (!productCartResponse.getProductCGST().equalsIgnoreCase("")) {
                        totalCGST += Float.parseFloat(productCartResponse.getProductCGST());
                    }
                    if (!productCartResponse.getProductSGST().equalsIgnoreCase("")) {
                        totalSGST += Float.parseFloat(productCartResponse.getProductSGST());
                    }
                    float totalPerProductGST = (productPrice + (productPrice * ((totalCGST + totalSGST) / 100))) * productQuantity;
                    productPriceUnit = MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", totalPerProductGST);
                } else {
                    productPriceUnit = MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", productPrice * productQuantity);
                }
            } else {
                productPriceUnit = MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", productPrice * productQuantity);
            }
        } else {
            productPriceUnit = MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", productPrice * productQuantity);
        }

        holder.binding.productPrice.setText(productPriceUnit);
        holder.binding.productPrice.setVisibility(View.GONE);

        // Qty bottom sheet only from the quantity number — not − / + / row.
        holder.binding.getRoot().setOnClickListener(null);
        if (productCartResponse.isOpenPrice()) {
            holder.binding.productName.setClickable(true);
            holder.binding.productName.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) {
                    return;
                }
                setUpdateOpenPrice(productCartResponseList.get(pos));
            });
        } else {
            holder.binding.productName.setOnClickListener(null);
            holder.binding.productName.setClickable(false);
        }

        holder.binding.productRemove.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) {
                return;
            }
            ProductCartResponse line = productCartResponseList.get(pos);
            float quantity = parseQty(line.getProductQuantity());
            float totalQuantity = quantity - 1;
            if (totalQuantity > 0) {
                updateCart(pos, line.getCartId(), formatQty(totalQuantity), line.getResolvedLinePrice());
            } else {
                deleteCartProduct(pos, line.getCartId());
            }
        });

        holder.binding.productAdd.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) {
                return;
            }
            ProductCartResponse line = productCartResponseList.get(pos);
            float quantity = parseQty(line.getProductQuantity());
            updateCart(pos, line.getCartId(), formatQty(quantity + 1), line.getResolvedLinePrice());
        });

        holder.binding.productDelete.setOnClickListener(v -> BottomSheetUi.showConfirm(
                context,
                context.getString(R.string.toast_delete_product),
                context.getString(R.string.toast_do_you_want_to_delete_from_bill),
                "YES",
                "NO",
                true,
                () -> {
                    int pos = holder.getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) {
                        return;
                    }
                    deleteCartProduct(pos, productCartResponseList.get(pos).getCartId());
                }));

        holder.binding.productQuantity.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) {
                return;
            }
            setUpdateQuantity(productCartResponseList.get(pos));
        });

        holder.binding.productPrice.setOnClickListener(null);
        holder.binding.productPrice.setClickable(false);
        holder.binding.productPrice.setFocusable(false);
    }

    public void setUpdateQuantity(ProductCartResponse productCartResponse) {
        Activity activity = (Activity) context;
        View content = LayoutInflater.from(activity).inflate(R.layout.update_quantity_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, content, false);

        TextView continueToQuantity = content.findViewById(R.id.continueToQuantity);
        TextView dismissQuantity = content.findViewById(R.id.dismissQuantity);
        TextInputEditText quantityTxt = content.findViewById(R.id.quantity);
        TextView detailsTxt = content.findViewById(R.id.details);
        detailsTxt.setText(context.getString(R.string.ui_quantity_detail));

        quantityTxt.setText(formatQty(parseQty(productCartResponse.getProductQuantity())));
        quantityTxt.setSelection(quantityTxt.getText().toString().length());

        dismissQuantity.setOnClickListener(v -> sheet.dismiss());

        continueToQuantity.setOnClickListener(v -> {
            if (!quantityTxt.getText().toString().isEmpty()) {
                float entered = parseQty(quantityTxt.getText().toString());
                if (entered > 0) {
                    int pos = indexOfCartId(productCartResponse.getCartId());
                    updateCart(pos, productCartResponse.getCartId(), formatQty(entered),
                            productCartResponse.getResolvedLinePrice());
                    sheet.dismiss();
                } else {
                    quantityTxt.setError(context.getString(R.string.ui_enter_quantity));
                    quantityTxt.requestFocus();
                }
            } else {
                quantityTxt.setError(context.getString(R.string.ui_enter_quantity));
                quantityTxt.requestFocus();
            }
        });
    }

    public void setUpdateOpenPrice(ProductCartResponse productCartResponse) {
        Activity activity = (Activity) context;
        View content = LayoutInflater.from(activity).inflate(R.layout.update_amount_quantity_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, content, false);

        TextView continueToQuantity = content.findViewById(R.id.continueToQuantity);
        TextView dismissQuantity = content.findViewById(R.id.dismissQuantity);
        TextInputEditText amountTxt = content.findViewById(R.id.amount);
        TextInputEditText quantityTxt = content.findViewById(R.id.quantity);
        TextView detailsTxt = content.findViewById(R.id.details);
        detailsTxt.setText(context.getString(R.string.ui_open_price));

        amountTxt.setText(productCartResponse.getResolvedLinePrice());
        quantityTxt.setText(formatQty(parseQty(productCartResponse.getProductQuantity())));
        amountTxt.setSelection(amountTxt.getText() != null ? amountTxt.getText().length() : 0);
        quantityTxt.setSelection(quantityTxt.getText() != null ? quantityTxt.getText().length() : 0);

        dismissQuantity.setOnClickListener(v -> sheet.dismiss());

        continueToQuantity.setOnClickListener(v -> {
            String amountStr = amountTxt.getText() != null ? amountTxt.getText().toString().trim() : "";
            String qtyStr = quantityTxt.getText() != null ? quantityTxt.getText().toString().trim() : "";
            if (amountStr.isEmpty()) {
                amountTxt.setError(context.getString(R.string.ui_enter_amount));
                amountTxt.requestFocus();
                return;
            }
            float amount = parseQty(amountStr);
            if (amount <= 0) {
                amountTxt.setError(context.getString(R.string.ui_enter_amount));
                amountTxt.requestFocus();
                return;
            }
            if (qtyStr.isEmpty()) {
                quantityTxt.setError(context.getString(R.string.ui_enter_quantity));
                quantityTxt.requestFocus();
                return;
            }
            float qty = parseQty(qtyStr);
            if (qty <= 0) {
                quantityTxt.setError(context.getString(R.string.ui_enter_quantity));
                quantityTxt.requestFocus();
                return;
            }
            int pos = indexOfCartId(productCartResponse.getCartId());
            updateCart(pos, productCartResponse.getCartId(), formatQty(qty),
                    String.format(Locale.US, "%.2f", amount));
            sheet.dismiss();
        });
    }

    private int indexOfCartId(String cartId) {
        if (cartId == null) {
            return -1;
        }
        for (int i = 0; i < productCartResponseList.size(); i++) {
            if (cartId.equals(productCartResponseList.get(i).getCartId())) {
                return i;
            }
        }
        return -1;
    }

    private static float parseQty(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0f;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    private static String formatQty(float qty) {
        if (qty == (long) qty) {
            return String.valueOf((long) qty);
        }
        return String.format(Locale.US, "%.2f", qty);
    }

    /**
     * Instant UI update + async DB write. Does not enqueue WorkManager / full cart reload.
     */
    public void updateCart(int position, String cartId, String productQuantity, String productAmount) {
        if (position >= 0 && position < productCartResponseList.size()) {
            ProductCartResponse line = productCartResponseList.get(position);
            line.setProductQuantity(productQuantity);
            if (productAmount != null) {
                line.setSnapshotLinePrice(productAmount);
                line.setProductNewPrice(productAmount);
                line.setProductOldPrice(productAmount);
            }
            notifyItemChanged(position);
        }
        final String id = cartId;
        final String qty = productQuantity;
        final String amount = productAmount;
        AppExecutors.get().db().execute(() -> posBillingWalaDatabase.updateCart(id, qty, amount));
        BluetoothPrint.refreshCartUiAfterLocalEdit();
    }

    public void deleteCartProduct(int position, String cartId) {
        if (position >= 0 && position < productCartResponseList.size()) {
            productCartResponseList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, productCartResponseList.size() - position);
        }
        final String id = cartId;
        AppExecutors.get().db().execute(() -> posBillingWalaDatabase.deleteCartProduct(id));
        Toast.makeText(context, context.getString(R.string.toast_product_deleted_from_cart), Toast.LENGTH_SHORT).show();
        BluetoothPrint.refreshCartUiAfterLocalEdit();
    }

    @Override
    public int getItemCount() {
        return productCartResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        CartProductListBinding binding;

        public MyViewHolder(CartProductListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
